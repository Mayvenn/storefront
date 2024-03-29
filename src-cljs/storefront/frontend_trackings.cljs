(ns storefront.frontend-trackings
  (:require [api.orders :as api.orders]
            [catalog.keypaths]
            [catalog.products :as products]
            [clojure.string :as string]
            [clojure.set :as set]
            [spice.maps :as maps]
            [storefront.accessors.line-items :as line-items]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.videos :as videos]
            [storefront.accessors.stylist-urls :as stylist-urls]
            [storefront.events :as events]
            [storefront.hooks.google-analytics :as google-analytics]
            [storefront.hooks.riskified :as riskified]
            [storefront.hooks.stringer :as stringer]
            [storefront.keypaths :as keypaths]
            [storefront.routes :as routes]
            [storefront.trackings :refer [perform-track]]
            [storefront.accessors.images :as images]))

(defn waiter-line-item->line-item-skuer
  [skus-db waiter-line-item]
  (merge (get skus-db (:sku waiter-line-item))
         (maps/select-rename-keys waiter-line-item {:quantity        :item/quantity
                                                    :line-item-group :line-item-group})))

(defn waiter-line-items->line-item-skuer
  "This is a stopgap measure to stand in for when waiter will one day return
  line items in skuer format. The domain around selling needs to be defined.

  Does not handle shipping line-items."
  [skus-db line-items]
  (mapv (partial waiter-line-item->line-item-skuer skus-db) line-items))

(defn sku-id->quantity-to-line-item-skuer
  [skus-db sku-id->quantity]
  (mapv (fn [[sku-id quantity]]
          (assoc (get skus-db sku-id) :item/quantity quantity))
        sku-id->quantity))

(defn line-item-skuer->stringer-cart-item
  "Converts line item skuers into the format that stringer expects"
  [images-catalog line-item-skuer]
  (let [image (images/cart-image images-catalog line-item-skuer)]
    {:variant_id           (:legacy/variant-id line-item-skuer)
     :variant_sku          (:catalog/sku-id line-item-skuer)
     :variant_price        (:sku/price line-item-skuer)
     :variant_quantity     (:item/quantity line-item-skuer)
     :variant_name         (or (:legacy/product-name line-item-skuer)
                               (:sku/title line-item-skuer))
     :variant_origin       (-> line-item-skuer :hair/origin first)
     :variant_style        (-> line-item-skuer :hair/texture first)
     :variant_color        (-> line-item-skuer :hair/color first)
     :variant_length       (-> line-item-skuer :hair/length first)
     :variant_material     (-> line-item-skuer :hair/base-material first)
     :variant_service_type (-> line-item-skuer :service/type first)
     :variant_image        (when image
                             (update image :src (partial str "https:")))
     :variant_department   (-> line-item-skuer :catalog/department first)
     :line_item_group      (-> line-item-skuer :line-item-group)}))

(defn- nav-was-selecting-bundle-option? [app-state]
  (when-let [prev-nav-message (:navigation-message (first (get-in app-state keypaths/navigation-undo-stack)))]
    (let [[nav-event nav-args]           (get-in app-state keypaths/navigation-message)
          [prev-nav-event prev-nav-args] prev-nav-message]
      (and (= nav-event prev-nav-event events/navigate-product-details)
           (= (:catalog/product-id nav-args) (:catalog/product-id prev-nav-args))))))

(defmethod perform-track events/navigate [_ event {:keys [navigate/caused-by] :as args} app-state]
  (when (not (#{:module-load} caused-by))
    (let [path (routes/current-path app-state)]
      (when (not (nav-was-selecting-bundle-option? app-state))
        (when (experiments/natural-page-view? app-state)
          (google-analytics/track-page (google-analytics/retrieve-user-ecd app-state)))
        (riskified/track-page path)
        (stringer/track-page (get-in app-state keypaths/store-experience))))))

(defmethod perform-track events/control-category-panel-open
  [_ event {:keys [selected]} app-state]
  (stringer/track-event "category_page_filter-select"
                        {:filter_name (pr-str selected)}))

(defmethod perform-track events/control-category-option-select
  [_ event {:keys [facet option]} app-state]
  (stringer/track-event "category_page_option-select"
                        {:filter_name     (pr-str facet)
                         :selected_option option}))

(defn track-select-bundle-option [selection value]
  (stringer/track-event "select_bundle_option" {:option_name  (name selection)
                                                :option_value value}))

(defn ^:private sku->data-sku-reference
  [s]
  (select-keys s [:catalog/sku-id :legacy/variant-id]))

(defn ^:private selections->product-selections
  [selections]
  (mapv
   #(merge (select-keys selections [:hair/color :hair/length])
           (select-keys % [:hair/color :hair/length]))
   selections))

(defmethod perform-track events/control-product-detail-picker-option-select
  [_ event {:keys [selection value] :as options} app-state]
  (track-select-bundle-option selection value))

(defmethod perform-track events/api-success-suggested-add-to-bag [_ event {:keys [order sku-id->quantity initial-sku]} app-state]
  (let [store-slug       (get-in app-state keypaths/store-slug)
        line-item-skuers (sku-id->quantity-to-line-item-skuer (get-in app-state keypaths/v2-skus) sku-id->quantity)
        images-catalog   (get-in app-state keypaths/v2-images)
        added-skus       (mapv line-item-skuer->stringer-cart-item images-catalog line-item-skuers)]
    (google-analytics/track-add-to-cart
     {:number           (:number order)
      :store-slug       store-slug
      :store-is-stylist (not (or (#{"store" "shop" "internal"} store-slug)
                                 (= "retail-location" (get-in app-state keypaths/store-experience))))
      :order-quantity   (orders/product-and-service-quantity order)
      :line-item-skuers line-item-skuers
      :user-ecd         (google-analytics/retrieve-user-ecd app-state)})
    (stringer/track-event "suggested_line_item_added" {:added_skus   added-skus
                                                       :initial_sku  (dissoc (line-item-skuer->stringer-cart-item images-catalog initial-sku)
                                                                             :variant_quantity)
                                                       :order_number (:number order)
                                                       :order_total  (:total order)})))

(defmethod perform-track events/api-success-add-sku-to-bag
  [_ _ {:keys [quantity sku order] :as args} app-state]
  (when sku
    (let [images-catalog        (get-in app-state keypaths/v2-images)
          skus-db               (get-in app-state keypaths/v2-skus)
          ;; If we added a service, then the SKU we requested (LBI) might not be the one that came back;
          ;; we should use the one that came back. But it might not be in the state catalog, so we shan't
          ;; depend on it.
          free-service-added?   (:promo.mayvenn-install/discountable sku)
          cart-item-being-added (if free-service-added?
                                  (->> order
                                       (api.orders/free-mayvenn-service nil)
                                       :free-mayvenn-service/service-item
                                       :sku
                                       (get skus-db)
                                       (line-item-skuer->stringer-cart-item images-catalog))
                                  (line-item-skuer->stringer-cart-item images-catalog sku))
          cart-items            (->> (orders/product-and-service-items order)
                                     (waiter-line-items->line-item-skuer skus-db)
                                     (filter :catalog/sku-id)
                                     (mapv (partial line-item-skuer->stringer-cart-item images-catalog)))
          store-slug            (get-in app-state keypaths/store-slug)
          order-quantity        (orders/product-and-service-quantity order)
          user-ecd              (google-analytics/retrieve-user-ecd app-state)]
      (stringer/track-event "add_to_cart" (merge cart-item-being-added
                                                 {:order_number     (:number order)
                                                  :order_total      (:total order)
                                                  :order_quantity   order-quantity
                                                  :store_experience (get-in app-state keypaths/store-experience)
                                                  :variant_quantity quantity
                                                  :quantity         quantity
                                                  :context          {:cart-items cart-items}}
                                                 user-ecd))

      (google-analytics/track-add-to-cart
       {:number           (:number order)
        :store-slug       store-slug
        :store-is-stylist (not (or (#{"store" "shop" "internal"} store-slug)
                                   (= "retail-location" (get-in app-state keypaths/store-experience))))
        :order-quantity   order-quantity
        :line-item-skuers [(assoc sku :item/quantity quantity)]
        :user-ecd         user-ecd}))))

(defmethod perform-track events/order-line-item-removed
  [_ _ {:keys [sku-id quantity order]} app-state] 
  (let [skus           (get-in app-state keypaths/v2-skus)
        images-catalog (get-in app-state keypaths/v2-images)]
    (when-let [sku (get skus sku-id)]
      (google-analytics/track-remove-from-cart {:sku      sku
                                                :quantity quantity}))
    (stringer/track-event "remove_from_cart"
                          {:order_number     (:number order)
                           :order_total      (:total order)
                           :order_quantity   (orders/product-and-service-quantity order)
                           :store_experience (get-in app-state keypaths/store-experience)
                           :context          {:cart-items (->> order
                                                               orders/product-and-service-items
                                                               (waiter-line-items->line-item-skuer skus)
                                                               (mapv (partial line-item-skuer->stringer-cart-item images-catalog)))}})))

(defmethod perform-track events/api-success-shared-cart-create [_ _ {:keys [cart]} app-state]
  (let [indexed-sku-db   (get-in app-state keypaths/v2-skus)
        line-item-skuers (->> (:line-items cart)
                              flatten
                              (map (fn [line-item]
                                     (when-let [sku (get indexed-sku-db (:sku-id line-item))]
                                       (merge sku {:item/quantity (:quantity line-item)}))))
                              (remove nil?))]
    (stringer/track-event "shared_cart_created"
                          {:shared_cart_id        (-> cart :number)
                           :stylist_id            (:created-by-stylist-id cart)
                           :servicing_stylist_id  (:servicing-stylist-id cart)
                           :created_by_stylist_id (:created-by-stylist-id cart)
                           :skus                  (->> line-item-skuers (map :catalog/sku-id) (string/join ","))
                           :variant_ids           (->> line-item-skuers (map :legacy/variant-id) (string/join ","))
                           :quantities            (->> line-item-skuers (map :item/quantity) (string/join ","))
                           :total_quantity        (->> line-item-skuers (map :item/quantity) (reduce + 0))
                           :store_id              (get-in app-state keypaths/store-stylist-id)})))

(defn track-cart-initialization
  [initialized-by initialized-id {:keys [skus-db images-catalog store-experience order environment user-ecd]}]
  (let [line-item-skuers   (waiter-line-items->line-item-skuer skus-db (orders/product-and-service-items order))
        line-item-quantity (->> line-item-skuers (map :item/quantity) (reduce + 0))
        cart-items         (mapv (partial line-item-skuer->stringer-cart-item images-catalog) line-item-skuers)]
    (google-analytics/track-add-to-cart {:number           (:number order)
                                         :line-item-skuers line-item-skuers
                                         :user-ecd         user-ecd})
    (stringer/track-event "cart_initialized"
                          (merge {:store_experience store-experience
                                  :order_number     (:number order)
                                  :order_total      (:total order)
                                  :order_quantity   line-item-quantity
                                  :skus             (->> line-item-skuers (map :catalog/sku-id) (string/join ","))
                                  :variant_ids      (->> line-item-skuers (map :legacy/variant-id) (string/join ","))
                                  :context          {:cart-items cart-items}
                                  :initialized_by   (merge
                                                     {:type initialized-by} ;; shop-by-look, shared-cart
                                                     (when initialized-id   ;; look-id, shared-cart-id
                                                       {:id initialized-id}))}))))

;; If you are intending to track some bulk add event which should always create a new order, use `cart-initialized`
(defn track-bulk-add-to-cart
  [{:keys [skus-db images-catalog store-experience order shared-cart-id look-id environment user-ecd]}]
  (let [line-item-skuers       (waiter-line-items->line-item-skuer
                                skus-db
                                (orders/product-and-service-items order))
        line-item-quantity     (->> line-item-skuers (map :item/quantity) (reduce + 0))
        cart-items             (mapv (partial line-item-skuer->stringer-cart-item images-catalog) line-item-skuers)
        {non-upsell-skus false
         upsell-skus     true} (group-by
                                #(->> % :service/type first (= "addon"))
                                line-item-skuers)]
    (google-analytics/track-add-to-cart {:number           (:number order)
                                         :line-item-skuers line-item-skuers
                                         :user-ecd         user-ecd})
    (stringer/track-event "bulk_add_to_cart" (merge {:shared_cart_id       shared-cart-id
                                                     :store_experience     store-experience
                                                     :order_number         (:number order)
                                                     :order_total          (:total order)
                                                     :order_quantity       line-item-quantity
                                                     :inherent_skus        (->> non-upsell-skus (map :catalog/sku-id) (string/join ","))
                                                     :inherent_variant_ids (->> non-upsell-skus (map :legacy/variant-id) (string/join ","))
                                                     :upsell_skus          (->> upsell-skus (mapv :catalog/sku-id) (string/join ","))
                                                     :upsell_variant_ids   (->> upsell-skus (mapv :legacy/variant-id) (string/join ","))
                                                     :skus                 (->> line-item-skuers (map :catalog/sku-id) (string/join ","))
                                                     :variant_ids          (->> line-item-skuers (map :legacy/variant-id) (string/join ","))
                                                     :context              {:cart-items cart-items}}
                                                    (when look-id
                                                      {:look_id look-id})
                                                    user-ecd))))

(defmethod perform-track events/api-success-add-multiple-skus-to-bag
  [_ _ {:keys [sku-id->quantity order]
        :as   args} app-state]
  ;; NOTE: Currently does not handle cases where one of the skus being added in a free-install sku
  (when (some-> sku-id->quantity count pos?)
    (let [images-catalog            (get-in app-state keypaths/v2-images)
          skus-db                   (get-in app-state keypaths/v2-skus)
          store-experience          (get-in app-state keypaths/store-experience)

          added-line-item-skuers    (sku-id->quantity-to-line-item-skuer skus-db sku-id->quantity)
          added-stringer-cart-items (map (partial line-item-skuer->stringer-cart-item images-catalog)
                                         added-line-item-skuers)

          store-slug                (get-in app-state keypaths/store-slug)
          order-quantity            (orders/product-and-service-quantity order)
          user-ecd                  (google-analytics/retrieve-user-ecd app-state)]
      (stringer/track-event "bulk_add_to_cart"
                            (merge
                             {:store_experience store-experience
                              :order_number     (:number order)
                              :order_total      (:total order)
                              :order_quantity   (apply + (vals sku-id->quantity))
                              :skus             (->> added-line-item-skuers (map :catalog/sku-id) (string/join ","))
                              :variant_ids      (->> added-line-item-skuers (map :legacy/variant-id) (string/join ","))
                              :context          {:cart-items added-stringer-cart-items}}
                             user-ecd))
      (google-analytics/track-add-to-cart
       {:number           (:number order)
        :store-slug       store-slug
        :store-is-stylist (not (or (#{"store" "shop" "internal"} store-slug)
                                   (= "retail-location" (get-in app-state keypaths/store-experience))))
        :order-quantity   order-quantity
        :line-item-skuers added-line-item-skuers
        :user-ecd         user-ecd}))))

(def interesting-payment-methods
  #{"apple-pay" "paypal" "quadpay"})

(defn payment-flow [{:keys [payments]}]
  (or (some interesting-payment-methods (map :payment-type payments))
      "mayvenn"))

(defn tracked-payment-method [payments]
  (let [first-payment-method (->> payments (map :payment-type) first)]
    ;; we can just use the first payment method because the specified options are
    ;; apple-pay, paypal, or other.  Store credit and stripe
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

(defmethod perform-track events/order-placed [_ event order app-state]
  (stringer/track-event "checkout-complete" (stringer-order-completed order))
  (let [order-total      (:total order)

        line-item-skuers (waiter-line-items->line-item-skuer (get-in app-state keypaths/v2-skus)
                                                             (orders/product-items order))

        store-slug       (get-in app-state keypaths/store-slug)
        shipping         (orders/shipping-item order)
        user             (get-in app-state keypaths/user)

        order-quantity   (->> line-item-skuers (map :item/quantity) (reduce + 0))

        shared-fields    {:buyer_type            (cond
                                                   (:store-slug user) "stylist"
                                                   (:id user)         "registered_user"
                                                   :else              "guest_user")
                          :currency              "USD"
                          :discount_total        (js/Math.abs (or (:promotion-discount order) 0))
                          :is_stylist_store      (boolean (not (or (#{"store" "shop"} store-slug)
                                                                   (= "retail-location" (get-in app-state keypaths/store-experience)))))
                          :shipping_method_name  (:product-name shipping)
                          :shipping_method_price (:unit-price shipping)
                          :shipping_method_sku   (:sku shipping)
                          :store_slug            store-slug
                          :used_promotion_codes  (:promotion-codes order)}]

    (google-analytics/track-placed-order
     (merge (set/rename-keys shared-fields {:buyer_type            :buyer-type
                                            :is_stylist_store      :is-stylist-store
                                            :shipping_method_name  :shipping-method-name
                                            :shipping_method_price :shipping-method-price
                                            :shipping_method_sku   :shipping-method-sku
                                            :store_slug            :store-slug
                                            :used_promotion_codes  :used-promotion-codes})
            {:total            (:total order)
             :tax              (:tax-total order)
             :number           (:number order)
             :line-item-skuers line-item-skuers
             :user-ecd         (google-analytics/retrieve-user-ecd app-state)}))))

(defmethod perform-track events/api-success-auth-sign-in [_ event {:keys [flow user] :as args} app-state]
  (stringer/track-event "sign_in" {:type flow})
  (google-analytics/track-login))

(defmethod perform-track events/api-success-auth-sign-up [_ event {:keys [flow] :as args} app-state]
  (google-analytics/track-sign-up)
  (stringer/track-event "sign_up" {:type flow}))

(defmethod perform-track events/api-success-auth-reset-password [_ events {:keys [flow] :as args} app-state]
  (stringer/track-event "reset_password" {:type flow}))

(defmethod perform-track events/enable-feature [_ event {:keys [feature experiment]} app-state]
  (stringer/track-event "experiment-joined" {:name experiment
                                             :variation feature}))

(defn- checkout-initiate [app-state flow]
  (let [order (get-in app-state keypaths/order)]
    (stringer/track-event "checkout-initiate" {:flow         flow
                                               :order_number (:number order)})
    (google-analytics/track-begin-checkout {:line-item-skuers     (waiter-line-items->line-item-skuer (get-in app-state keypaths/v2-skus)
                                                                                                      (orders/product-items order))
                                            :used-promotion-codes (:promotion-codes order)
                                            :user-ecd             (google-analytics/retrieve-user-ecd app-state)})))

(defmethod perform-track events/browse-addon-service-menu-button-enabled
  [_ event args app-state]
  (stringer/track-event "display_add_on_services_button"))

(defmethod perform-track events/control-checkout-cart-submit [_ event args app-state]
  (checkout-initiate app-state "mayvenn"))

(defmethod perform-track events/control-checkout-cart-paypal-setup [_ event args app-state]
  (checkout-initiate app-state "paypal"))

(defmethod perform-track events/api-success-update-order-update-guest-address [_ event {:keys [order]} app-state]
  (stringer/track-event "checkout-identify_guest")
  (stringer/track-event "checkout-address_enter" {:order_number (:number order)}))

(defmethod perform-track events/api-success-update-order-update-address [_ events {:keys [order]} app-state]
  (stringer/track-event "checkout-address_enter" {:order_number (:number order)}))

(defmethod perform-track events/api-success-update-order-update-cart-payments [_ events {:keys [order]} app-state]
  (stringer/track-event "checkout-payment_enter" {:order_number (:number order)
                                                  :method       (cond
                                                                  (contains? (:cart-payments order) :paypal)  "paypal"
                                                                  (contains? (:cart-payments order) :quadpay) "quadpay"
                                                                  :else                                       "other")}))

(defmethod perform-track events/api-success-update-order-update-shipping-method [_ events {:keys [order]} app-state]
  (stringer/track-event "checkout-shipping_method_change"
                        {:order_number (:number order)
                         :shipping_method (get-in app-state keypaths/checkout-selected-shipping-method-name)}))

(defmethod perform-track events/api-success-forgot-password [_ events {email :email} app-state]
  (stringer/track-event "request_reset_password" {:email email}))

(defmethod perform-track events/order-promo-code-added 
  [_ events {:keys [order-number promo-code]} app-state]
  (google-analytics/track-select-promotion {:promotion (->> keypaths/promotions
                                                            (get-in app-state)
                                                            (filter #(= promo-code (:code %)))
                                                            first)})
  (stringer/track-event "promo_add" {:order_number   order-number
                                     :promotion_code promo-code}))

(defmethod perform-track events/api-success-update-order-remove-promotion-code [_ events {order :order promo-code :promo-code} app-state]
  (stringer/track-event "promo_remove" {:order_number (:number order)
                                        :promotion_code promo-code}))

(defmethod perform-track events/api-failure-errors-invalid-promo-code [_ events {order :order promo-code :promo-code} app-state]
  (stringer/track-event "promo_invalid" {:order_number (:number order)
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

(defmethod perform-track events/control-stylist-dashboard-cash-out-begin
  [_ _ {:keys [amount payout-method-name]} app-state]
  (let [stylist-id (get-in app-state keypaths/user-store-id)]
    (stringer/track-event "dashboard_cash_out_begin_button_pressed"
                          {:stylist_id         stylist-id
                           :amount             amount
                           :payout_method_name payout-method-name})))

(defmethod perform-track events/control-stylist-dashboard-cash-out-commit
  [_ _ _ app-state]
  (let [stylist-id                     (get-in app-state keypaths/user-store-id)
        store-slug                     (get-in app-state keypaths/user-store-slug)
        {:keys [amount payout-method]} (get-in app-state keypaths/stylist-payout-stats-next-payout)]
    (stringer/track-event "cash_out_commit_button_pressed"
                          {:stylist_id stylist-id
                           :store_slug store-slug
                           :amount     (js/parseFloat amount)
                           :method     (:type payout-method)})))

(defmethod perform-track events/api-success-cash-out-complete
  [_ _ {:keys [amount payout-method] :as cash-out-status} app-state]
  (let [stylist-id (get-in app-state keypaths/user-store-id)
        store-slug (get-in app-state keypaths/user-store-slug)]
    (stringer/track-event "cash_out_succeeded"
                          {:stylist_id stylist-id
                           :store_slug store-slug
                           :amount     (js/parseFloat amount)
                           :method     (:type payout-method)})))

(defmethod perform-track events/api-success-cash-out-failed
  [_ _ {:keys [amount payout-method] :as cash-out-status} app-state]
  (let [stylist-id (get-in app-state keypaths/user-store-id)
        store-slug (get-in app-state keypaths/user-store-slug)]
    (stringer/track-event "cash_out_failed"
                          {:stylist_id stylist-id
                           :store_slug store-slug
                           :amount     (js/parseFloat amount)
                           :method     (:type payout-method)})))

(defmethod perform-track events/control-pick-stylist-button
  [_ _ _ _]
  (stringer/track-event "pick_stylist_button_pressed"))

(defmethod perform-track events/control-change-stylist
  [_ _ {:keys [stylist-id]} _]
  (stringer/track-event "change_stylist_icon_pressed" {:current_servicing_stylist_id stylist-id}))

(defmethod perform-track events/external-redirect-sms
  [_ _ params _]
  (stringer/track-event "external-redirect-sms" params))

(defn cart-items-model<-
  [order images-db skus-db]
  (->> (orders/product-and-service-items order)
       (waiter-line-items->line-item-skuer skus-db)
       (mapv (partial line-item-skuer->stringer-cart-item images-db))))

(defmethod perform-track events/user-identified
  [_ _ {:keys [user]} _]
  (stringer/identify user))

(defmethod perform-track events/external-redirect-instagram-profile
  [_ _ {:keys [ig-username]} _]
  (stringer/track-event "stylist_instagram_link-clicked" {:instagram-username ig-username}))

(defmethod perform-track events/look|viewed
  [_ _ {:keys [skus]} app-state]
  (google-analytics/track-view-items skus (google-analytics/retrieve-user-ecd app-state)))