(ns storefront.trackings
  (:require [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [catalog.selector :as selector]
            [storefront.routes :as routes]
            [storefront.hooks.facebook-analytics :as facebook-analytics]
            [storefront.hooks.google-analytics :as google-analytics]
            [storefront.hooks.convert :as convert]
            [storefront.hooks.riskified :as riskified]
            [storefront.hooks.stringer :as stringer]
            [storefront.hooks.sift :as sift]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.videos :as videos]
            [leads.accessors :as leads.accessors]
            [storefront.utils.query :as query]
            [clojure.string :as string]
            [storefront.accessors.products :as products]
            [spice.maps :as maps]
            [storefront.hooks.pinterest :as pinterest]
            [spice.core :as spice]))

(defn ^:private convert-revenue [{:keys [number total] :as order}]
  {:order-number   number
   :revenue        total
   :products-count (orders/product-quantity order)})

(defmulti perform-track identity)

(defmethod perform-track :default [dispatch event args app-state])

(defmethod perform-track events/app-start [_ event args app-state]
  (when (get-in app-state keypaths/user-id)
    (stringer/identify (get-in app-state keypaths/user))))

(defn- nav-was-selecting-bundle-option? [app-state]
  (when-let [prev-nav-message (:navigation-message (first (get-in app-state keypaths/navigation-undo-stack)))]
    (let [[nav-event nav-args]           (get-in app-state keypaths/navigation-message)
          [prev-nav-event prev-nav-args] prev-nav-message]
      (and (= nav-event prev-nav-event events/navigate-product-details)
           (= (:catalog/product-id nav-args) (:catalog/product-id prev-nav-args))))))

(defmethod perform-track events/navigate [_ event args app-state]
  (when (not (get-in app-state keypaths/redirecting?))
    (let [path (routes/current-path app-state)]
      (google-analytics/track-page path)
      (when (not (nav-was-selecting-bundle-option? app-state))
        (pinterest/track-page)
        (sift/track-page (get-in app-state keypaths/user-id)
                         (get-in app-state keypaths/session-id))
        (riskified/track-page path)
        (stringer/track-page)
        (facebook-analytics/track-page path)))))

(defmethod perform-track events/control-category-panel-open
  [_ event {:keys [selected]} app-state]
  (stringer/track-event "category_page_filter-select"
                        {:filter_name (pr-str selected)}))

(defmethod perform-track events/control-category-option-select
  [_ event {:keys [facet option]} app-state]
  (stringer/track-event "category_page_option-select"
                        {:filter_name     (pr-str facet)
                         :selected_option option}))

(defmethod perform-track events/api-success-lead-created
  [_ _ {{:keys [first-name last-name email phone id flow-id]} :lead} _]
  (stringer/track-event "lead_identified" {:lead_id    id
                                           :email      email
                                           :phone      phone
                                           :first_name first-name
                                           :last_name  last-name})
  (if (leads.accessors/self-reg? flow-id)
    (do
      (pinterest/track-event "lead" {:lead_type "self_reg"})
      (facebook-analytics/track-custom-event "Lead_Self_Reg"))
    (do
      (pinterest/track-event "lead" {:lead_type "sales_rep"})
      (facebook-analytics/track-event "Lead"))))

(defmethod perform-track events/api-success-lead-registered
  [_ _ _ _]
  (pinterest/track-event "signup")
  (facebook-analytics/track-custom-event "Lead_Self_Reg_Complete"))

(defmethod perform-track events/navigate-product-details [_ event args app-state]
  (facebook-analytics/track-event "ViewContent"))

(defmethod perform-track events/control-bundle-option-select [_ event {:keys [selection value]} app-state]
  (stringer/track-event "select_bundle_option" {:option_name  (name selection)
                                                :option_value value}))

(defmethod perform-track events/control-add-sku-to-bag [_ event {:keys [sku quantity] :as args} app-state]
  (facebook-analytics/track-event "AddToCart" {:content_type "product"
                                               :content_ids  [(:sku sku)]
                                               :num_items    quantity})
  (google-analytics/track-page (str (routes/current-path app-state) "/add_to_bag"))
  (let [order      (get-in app-state keypaths/order)
        store-slug (get-in app-state keypaths/store-slug)]
    (pinterest/track-event "AddToCart" (merge (when (:legacy/variant-id sku)
                                                {:product_id (:legacy/variant-id sku)})
                                              {:sku              (:sku sku)
                                               :value            (:price sku)
                                               :order_quantity   (->> (:shipments order)
                                                                      (mapcat :line-items)
                                                                      orders/line-item-quantity)
                                               :currency         "USD"
                                               :department       (:product/department sku)
                                               :product_name     (:sku/name sku)
                                               :quantity         quantity
                                               :category         (:hair/family sku)
                                               :origin           (:hair/origin sku)
                                               :style            (:hair/texture sku)
                                               :color            (:hair/color sku)
                                               :material         (:hair/material sku)
                                               :grade            (:hair/grade sku)
                                               :length           (:hair/length sku)
                                               :store_slug       store-slug
                                               :is_stylist_store (not (#{"store" "shop" "internal"} store-slug))}))))

(defn order-product-items->enriched-products
  "From the Products key with a quantity"
  [products-db line-items]
  (mapv
   (fn [{:keys [id product-id quantity]}]
     (assoc
      (query/get {:id id}
                 (:variants (get products-db product-id)))
      :quantity quantity))
   line-items))

(defn line-item->cart-item [products-db line-item]
  (let [cellar-variant (query/get {:id (:id line-item)}
                                  (:variants (get products-db (:product-id line-item))))
        attrs (:variant_attrs cellar-variant)]
    {:variant_id       (:id cellar-variant)
     :variant_sku      (:sku cellar-variant)
     :variant_price    (:unit-price line-item)
     :variant_quantity (:quantity line-item)
     :variant_name     (:variant-name cellar-variant)
     :variant_origin   (:origin attrs)
     :variant_style    (:style attrs)
     :variant_color    (:color attrs)
     :variant_length   (:length attrs)
     :variant_material (:material attrs)
     :variant_image    (products/product-img-with-size cellar-variant :product)}))

(defn- ->cart-item
  "Makes a flattened key version of variants"
  [variant-or-sku]
  (let [attrs (:variant-attrs variant-or-sku)]
    {:variant_id       (or (:variant_id variant-or-sku)
                           (:legacy/variant-id variant-or-sku))
     :variant_sku      (or (:variant_sku variant-or-sku)
                           (:catalog/sku-id variant-or-sku))
     :variant_price    (or (:unit-price variant-or-sku)
                           (:sku/price variant-or-sku))
     :variant_quantity (or (:variant_quantity variant-or-sku)
                           (:quantity variant-or-sku)
                           1)
     :variant_name     (or (:variant_name variant-or-sku)
                           (:variant-name variant-or-sku))
     :variant_origin   (or (:variant_origin variant-or-sku)
                           (:origin attrs))
     :variant_style    (or (:variant_style variant-or-sku)
                           (:style attrs))
     :variant_color    (or (:variant_color variant-or-sku)
                           (:color attrs))
     :variant_length   (or (:variant_length variant-or-sku)
                           (:length attrs))
     :variant_material (or (:variant_material variant-or-sku)
                           (:material attrs))
     :variant_image    (products/product-img-with-size variant-or-sku :product)}))

(defn- line-items->cart-items
  [products-db line-items]
  (mapv (partial line-item->cart-item products-db) line-items))

(defmethod perform-track events/api-success-add-to-bag
  [_ _ {:keys [variant quantity] :as args} app-state]
  (when variant
    (let [order         (get-in app-state keypaths/order)
          product-items (orders/product-items order)
          products-db   (get-in app-state keypaths/products)
          cart-items    (line-items->cart-items products-db product-items)]
      (stringer/track-event "add_to_cart" (merge (-> variant ->cart-item)
                                                 {:order_number   (get-in app-state keypaths/order-number)
                                                  :order_total    (get-in app-state keypaths/order-total)
                                                  :order_quantity (orders/product-quantity order)
                                                  :quantity       quantity
                                                  :context        {:cart-items cart-items}})))))

(defn product-image [images sku]
  (->> {:use-case "catalog" :image/of #{"product"}}
       (selector/select images sku)
       first))

(defn- sku->cart-item [images sku quantity]
  (let [{:keys [url alt filename] :as image} (->> {:use-case "catalog" :image/of #{"product"}}
                                                  (selector/select images sku)
                                                  first)]
    {:variant_sku      (:sku sku)
     :variant_price    (:price sku)
     :variant_quantity quantity
     :variant_name     (:sku/name sku)
     :variant_origin   (:hair/origin sku)
     :variant_style    (:hair/texture sku)
     :variant_color    (:hair/color sku)
     :variant_length   (:hair/length sku)
     :variant_material (:hair/base-material sku)
     :variant_image    {:src (str "https:" url filename) :alt alt}
     :variant_id       (:legacy/variant-id sku)}))

(defmethod perform-track events/api-success-add-sku-to-bag
  [_ _ {:keys [quantity sku] :as args} app-state]
  (when sku
    (let [order      (get-in app-state keypaths/order)
          line-items (orders/product-items order)
          images     (get-in app-state keypaths/db-images)
          cart-items (mapv (comp (partial apply (partial sku->cart-item images))
                                 (juxt (comp (get-in app-state keypaths/skus) :sku)
                                       :quantity))
                           line-items)]
      (stringer/track-event "add_to_cart" (merge (sku->cart-item images sku quantity)
                                                 {:order_number   (get-in app-state keypaths/order-number)
                                                  :order_total    (get-in app-state keypaths/order-total)
                                                  :order_quantity (orders/product-quantity order)
                                                  :quantity       quantity
                                                  :context        {:cart-items cart-items}})))))

(defmethod perform-track events/api-success-shared-cart-create [_ _ {:keys [cart]} app-state]
  (let [{:keys [stylist-id number line-items]} cart
        line-items    (map (fn [[k v]]
                             {:id       (js/parseInt (name k))
                              :quantity v})
                           line-items)
        all-variants  (->> (get-in app-state keypaths/products)
                           (map val)
                           (mapcat :variants))
        ;; not guaranteed that we've loaded the right products yet, so use this sparingly
        cart-variants (map (fn [{:keys [id]}]
                             (query/get {:id id} all-variants))
                           line-items)]
    (stringer/track-event "shared_cart_created" {:shared_cart_id number
                                                 :stylist_id     stylist-id
                                                 :skus           (->> cart-variants (map :sku) (string/join ","))
                                                 :variant_ids    (->> line-items (map :id) (string/join ","))
                                                 :quantities     (->> line-items (map :quantity) (string/join ","))
                                                 :total_quantity (->> line-items (map :quantity) (reduce + 0))})))

(defmethod perform-track events/api-success-update-order-from-shared-cart
  [_ _ {:keys [order shared-cart-id]} app-state]
  (let [product-items (orders/product-items order)
        cart-items    (mapv ->cart-item product-items)]
    (facebook-analytics/track-event "AddToCart" {:content_type "product"
                                                 :content_ids  (map :sku product-items)
                                                 :num_items    (->> product-items (map :quantity) (apply +))})
    (stringer/track-event "bulk_add_to_cart" {:shared_cart_id shared-cart-id
                                              :order_number   (:number order)
                                              :order_total    (get-in app-state keypaths/order-total)
                                              :order_quantity (orders/product-quantity order)
                                              :skus           (->> product-items (map :sku) (string/join ","))
                                              :variant_ids    (->> product-items (map :id) (string/join ","))
                                              :context        {:cart-items cart-items}})))

(defmethod perform-track events/control-cart-share-show [_ event args app-state]
  (google-analytics/track-page (str (routes/current-path app-state) "/Share_cart")))

(defmethod perform-track events/api-success-get-saved-cards [_ event args app-state]
  (google-analytics/set-dimension "dimension2" (count (get-in app-state keypaths/checkout-credit-card-existing-cards))))

(defn payment-flow [{:keys [payments]}]
  (or (some #{"apple-pay" "paypal" "affirm"} (map :payment-type payments))
      "mayvenn"))

(defn tracked-payment-method [payments]
  (let [interesting-payment-methods #{"apple-pay" "affirm" "paypal"}
        first-payment-method (->> payments (map :payment-type) first)]
    ;; we can just use the first payment method because the specified options are
    ;; apple-pay, paypal, affirm or other.  Store credit and stripe
    ;; are the only ones we allow to coexist.  Must be changed if that changes.
    (or (interesting-payment-methods first-payment-method)
        "other")))

(defn stringer-order-completed [{:keys [number total promotion-codes payments] :as order}]
  (let [items (orders/product-items order)]
    {:flow                    (payment-flow order)
     :order_number            number
     :order_total             total
     :non_store_credit_amount (orders/non-store-credit-payment-amount order)
     :shipping_method         (:product-name (orders/shipping-item order))
     :payment_method          (tracked-payment-method payments)
     :skus                    (->> items (map :sku) (string/join ","))
     :variant_ids             (->> items (map :id) (string/join ","))
     :promo_codes             (->> promotion-codes (string/join ","))
     :total_quantity          (orders/product-quantity order)}))

(defn- enriched-product->pinterest-line-item
  "Makes a flattened key version of variants"
  [variant]
  (let [attrs (or (:variant-attrs variant)
                  (:variant_attrs variant))]
    {:product_id       (or (:variant_id variant)
                           (:id variant))
     :sku              (or (:variant_sku variant)
                           (:sku variant))
     :product_price    (or (:unit-price variant)
                           (:price variant))
     :product_quantity (or (:variant_quantity variant)
                           (:quantity variant))
     :product_name     (or (:variant_name variant)
                           (:variant-name variant))
     :category         (or (:category variant)
                           (:category attrs))
     :origin           (or (:variant_origin variant)
                           (:origin attrs))
     :style            (or (:variant_style variant)
                           (:style attrs))
     :color            (or (:variant_color variant)
                           (:color attrs))
     :length           (or (:variant_length variant)
                           (:length attrs))
     :material         (or (:variant_material variant)
                           (:material attrs))}))

(defmethod perform-track events/order-completed [_ event order app-state]
  (stringer/identify {:id (-> order :user :id)
                      :email (-> order :user :email)})
  (stringer/track-event "checkout-complete" (stringer-order-completed order))
  (let [order-total (:total order)

        product-items     (orders/product-items order)
        enriched-products (order-product-items->enriched-products
                           (get-in app-state keypaths/products)
                           product-items)

        store-slug (get-in app-state keypaths/store-slug)
        shipping   (orders/shipping-item order)
        user       (get-in app-state keypaths/user)

        shared-fields {:buyer_type            (cond
                                                (:store-slug user) "stylist"
                                                (:id user)         "registered_user"
                                                :else              "guest_user")
                       :currency              "USD"
                       :discount_total        (js/Math.abs (or (:promotion-discount order) 0))
                       :is_stylist_store      (boolean (not (#{"shop" "store"} store-slug)))
                       :shipping_method_name  (:product-name shipping)
                       :shipping_method_price (:unit-price shipping)
                       :shipping_method_sku   (:sku shipping)
                       :store_slug            store-slug
                       :used_promotion_codes  (->> order :promotions (map :code))}]
    (facebook-analytics/track-event "Purchase" (merge shared-fields
                                                      {:value        (str order-total) ;; Facebook wants a string
                                                       :content_ids  (map :sku product-items)
                                                       :content_type "product"
                                                       :num_items    (count product-items)}))

    (pinterest/track-event "checkout" (merge shared-fields
                                             {:value          order-total ;; Pinterest wants a number
                                              :order_id       (:number order)
                                              :order_quantity (->> product-items (map :quantity) (apply +))
                                              :line_items     (mapv enriched-product->pinterest-line-item
                                                                    enriched-products)}))

    (convert/track-conversion "place-order")
    (convert/track-revenue (convert-revenue order))
    (google-analytics/track-event "orders" "placed_total" nil (int order-total))
    (google-analytics/track-event "orders" "placed_total_minus_store_credit" nil (int (orders/non-store-credit-payment-amount order)))))

(defmethod perform-track events/api-success-auth [_ event args app-state]
  (stringer/identify (get-in app-state keypaths/user)))

(defmethod perform-track events/api-success-auth-sign-in [_ event {:keys [flow] :as args} app-state]
  (stringer/track-event "sign_in" {:type flow}))

(defmethod perform-track events/api-success-auth-sign-up [_ event {:keys [flow] :as args} app-state]
  (stringer/track-event "sign_up" {:type flow}))

(defmethod perform-track events/api-success-auth-reset-password [_ events {:keys [flow] :as args} app-state]
  (stringer/track-event "reset_password" {:type flow}))

(defmethod perform-track events/enable-feature [_ event {:keys [feature experiment]} app-state]
  (google-analytics/track-event "experiment_join" feature)
  (stringer/track-event "experiment-joined" {:name experiment
                                             :variation feature}))

(defmethod perform-track events/control-email-captured-submit [_ event args app-state]
  (when (empty? (get-in app-state keypaths/errors))
    (let [captured-email (get-in app-state keypaths/captured-email)]
      (stringer/identify {:email captured-email})
      (stringer/track-event "email_capture-capture" {:email captured-email}))))

(defmethod perform-track events/show-email-popup [_ events args app-state]
  (stringer/track-event "email_capture-deploy" {}))

(defmethod perform-track events/control-email-captured-dismiss [_ events args app-state]
  (stringer/track-event "email_capture-dismiss" {}))

(defmethod perform-track events/apple-pay-availability [_ event {:keys [available?]} app-state]
  (when available?
    (convert/track-conversion "apple-pay-available")))

(defn- checkout-initiate [app-state flow]
  (stringer/track-event "checkout-initiate" {:flow flow
                                             :order_number (get-in app-state keypaths/order-number)})
  (google-analytics/track-event "orders" "initiate_checkout")
  (facebook-analytics/track-event "InitiateCheckout"))

(defmethod perform-track events/control-checkout-cart-submit [_ event args app-state]
  (checkout-initiate app-state "mayvenn")
  (convert/track-conversion "checkout"))

(defmethod perform-track events/control-checkout-cart-apple-pay [_ event args app-state]
  (checkout-initiate app-state "apple-pay")
  (convert/track-conversion "apple-pay-checkout"))

(defmethod perform-track events/control-checkout-cart-paypal-setup [_ event args app-state]
  (checkout-initiate app-state "paypal")
  (convert/track-conversion "paypal-checkout"))

(defmethod perform-track events/api-success-update-order-update-guest-address [_ event args app-state]
  (stringer/identify (:user (get-in app-state keypaths/order)))
  (stringer/track-event "checkout-identify_guest")
  (stringer/track-event "checkout-address_enter" {:order_number (get-in app-state keypaths/order-number)}))

(defmethod perform-track events/api-success-update-order-update-address [_ events args app-state]
  (stringer/track-event "checkout-address_enter" {:order_number (get-in app-state keypaths/order-number)}))

(defmethod perform-track events/api-success-update-order-update-cart-payments [_ events args app-state]
  (stringer/track-event "checkout-payment_enter" {:order_number (get-in app-state keypaths/order-number)
                                                  :method (cond (maps/contains-in? app-state keypaths/order-cart-payments-affirm) "affirm"
                                                                (maps/contains-in? app-state keypaths/order-cart-payments-paypal) "paypal"
                                                                :else "other")}))

(defmethod perform-track events/api-success-update-order-update-shipping-method [_ events args app-state]
  (stringer/track-event "checkout-shipping_method_change"
                        {:order_number (get-in app-state keypaths/order-number)
                         :shipping_method (get-in app-state keypaths/checkout-selected-shipping-method-name)}))

(defmethod perform-track events/api-success-forgot-password [_ events {email :email} app-state]
  (stringer/track-event "request_reset_password" {:email email}))

(defmethod perform-track events/api-success-update-order-add-promotion-code [_ events {promo-code :promo-code} app-state]
  (stringer/track-event "promo_add" {:order_number (get-in app-state keypaths/order-number)
                                     :promotion_code promo-code}))

(defmethod perform-track events/api-success-update-order-remove-promotion-code [_ events {promo-code :promo-code} app-state]
  (stringer/track-event "promo_remove" {:order_number (get-in app-state keypaths/order-number)
                                        :promotion_code promo-code}))

(defmethod perform-track events/api-failure-errors-invalid-promo-code [_ events {promo-code :promo-code :as args} app-state]
  (stringer/track-event "promo_invalid" {:order_number (get-in app-state keypaths/order-number)
                                         :promotion_code promo-code}))

(defn track-photo [image]
  (stringer/track-event "photo_uploaded" {:source (-> image :sourceInfo :source)}))

(defmethod perform-track events/uploadcare-api-success-upload-portrait [_ events image app-state]
  (track-photo image))

(defmethod perform-track events/uploadcare-api-success-upload-gallery [_ events image app-state]
  (track-photo image))

(defmethod perform-track events/video-played [_ events {:keys [video-id position]} app-state]
  (when-let [content (videos/id->name video-id)]
    (stringer/track-event "video-play" {:content  (name content)
                                        :position (name position)})))

(defmethod perform-track events/sign-out [_ _ _ _]
  (stringer/track-event "sign_out")
  (stringer/track-clear))
