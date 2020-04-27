(ns checkout.cart-v2020-04
  (:require
   [adventure.keypaths :as adventure.k]
   [catalog.facets :as facets]
   [checkout.header :as header]
   [checkout.ui.begin-checkout :as begin-checkout]
   [checkout.ui.empty-cart :as empty-cart]
   [checkout.ui.return-link :as return-link]
   [checkout.ui.service-cart-item :as service-cart-item]
   [checkout.ui.stylist-cart-item :as stylist-cart-item]
   [storefront.accessors.mayvenn-install :as mayvenn-install]
   [storefront.accessors.stylists :as stylists]
   [storefront.accessors.orders :as orders]
   [storefront.accessors.products :as products]
   [storefront.accessors.sites :as sites]
   [storefront.component :as c]
   [storefront.components.flash :as flash]
   [storefront.components.footer :as footer]
   [storefront.components.money-formatters :as mf]
   [storefront.components.ui :as ui]
   [storefront.events :as e]
   [storefront.keypaths :as k]
   [storefront.platform.component-utils :as utils]
   [storefront.request-keys :as request-keys]
   [ui.promo-banner :as promo-banner]
   ))

(c/defcomponent template
  [{:keys [header
           flash
           empty-cart
           return-link
           begin-checkout
           begin-checkout-paypal
           static-promo-banner
           stylist-cart-item
           service-cart-item
           footer]} _ _]
  [:div
   ;; TODO(corey) A lot of these components are not
   ;; trivially converted to form. They are often used
   ;; by other files.
   (promo-banner/static-organism static-promo-banner nil nil)
   (header/built-component header nil)
   (c/build return-link/organism return-link)
   (flash/built-component flash nil)

   [:div.col-7-on-dt.mx-auto
    (when empty-cart
      (c/build empty-cart/organism empty-cart))
    [:div.title-2.proxima "Services"]
    (c/build stylist-cart-item/organism stylist-cart-item)
    (c/build service-cart-item/organism service-cart-item)

    (c/build begin-checkout/organism begin-checkout)

    (footer/built-component footer nil)]])

(def return-link-query
  {:return-link.back-link/id     "start-shopping"
   :return-link.back-link/label  "Start Shopping"
   :return-link.back-link/target [e/navigate-category
                                  {:catalog/category-id "23"
                                   :page/slug           "mayvenn-install"}]})

(defn ^:private update-pending? [data]
  (let [request-key-prefix (comp vector first :request-key)]
    (some #(apply utils/requesting? data %)
          [request-keys/add-promotion-code
           [request-key-prefix request-keys/add-to-bag]
           [request-key-prefix request-keys/update-line-item]
           [request-key-prefix request-keys/delete-line-item]])))

(defn begin-checkout-query
  [mayvenn-install update-pending? redirecting-to-paypal?]
  (let [locked-install? (:mayvenn-install/locked? mayvenn-install)
        disabled        (or locked-install? update-pending?)]
    {:begin-checkout.button/elements
     [{:begin-checkout.button/id       "start-checkout-button"
       :begin-checkout.button/style    :p-color
       :begin-checkout.button/disabled disabled
       :begin-checkout.button/label    "Check out"
       :begin-checkout.button/target   e/control-checkout-cart-submit}
      {:begin-checkout.button/id       "paypal-checkout"
       :begin-checkout.button/style    :paypal
       :begin-checkout.button/spinning redirecting-to-paypal?
       :begin-checkout.button/disabled disabled
       :begin-checkout.button/label    [:div
                                        "Check out with "
                                        [:span.medium.italic "PayPal™"]]
       :begin-checkout.button/target   e/control-checkout-cart-paypal-setup}]}))

(defn empty-cart-query
  [site]
  (cond->
      {:empty-cart.title/primary "Your Cart is Empty"
       :empty-cart.body/primary  (str "Did you know that you'd qualify for a free"
                                      " Mayvenn Install when you purchase 3 or more items?")
       :empty-cart.action/id     "browse-stylists"
       :empty-cart.action/label  "Browse Stylists"
       :empty-cart.action/target [e/navigate-adventure-match-stylist]}

    (= :aladdin site)
    (merge
     {:empty-cart.action/id     "start-shopping"
      :empty-cart.action/label  "Start Shopping"
      :empty-cart.action/target
      [e/navigate-category
       {:catalog/category-id "23"
        :page/slug           "mayvenn-install"}]})))

(defn stylist-cart-item-query
  [stylist]
  (if stylist
    {:stylist-cart-item.title/primary   (stylists/->display-name stylist)
     :stylist-cart-item.title/secondary "Your Certified Mayvenn Stylist"
     :stylist-cart-item.rating/value    (:rating stylist)
     :stylist-cart-item.image/image-url (some-> stylist :portrait :resizable-url)
     :stylist-cart-item.action/target   [e/control-change-stylist
                                         {:stylist-id (:stylist-id stylist)}]
     :stylist-cart-item.action/id       "stylist-swap"}
    {:stylist-cart-item.title/primary   "No Stylist Selected"
     :stylist-cart-item.image/image-url "//ucarecdn.com/123d4b51-dd60-46d3-9db4-f932c124d6da/"
     :stylist-cart-item.link/id         "pick-a-stylist"
     :stylist-cart-item.link/label      "Pick your stylist"
     :stylist-cart-item.link/target     [e/control-pick-stylist-button]}))

(defn ^:private remove-freeinstall-pending?
  [data]
  (utils/requesting? data request-keys/remove-freeinstall-line-item))

(defn service-cart-item-query
  [{:mayvenn-install/keys    [service-title service-image-url service-discount locked?
                             quantity-required quantity-added matched? applied?
                             any-wig?] :as mi}
   add-items-action
   remove-freeinstall-pending?
   highlighted?
   confetti-mode]
  ;; What ui states does this query have to handle?
  ;; whether or not a free install applied: #{} or #{:title :image :highlighted? :removal-button}
  ;; if wig in the cart--> wig customization copy: #{}
  ;; if no wig --> how many physical items: #{}
  ;; if stylist matched --> add on button/modal activation: #{}
  ;; was the service item recently added? --> confetti: #{}
  (let [quantity-needed (- quantity-required quantity-added)]
    (merge
     {}
     (when applied?
       {:service-cart-item.title/primary       service-title
        :service-cart-item.title/secondary     "You're all set! Shampoo, braiding, and basic styling included."
        :service-cart-item.image/id            "freeinstall"
        :service-cart-item.image/image-url     (or service-image-url "//ucarecdn.com/3a25c870-fac1-4809-b575-2b130625d22a/")
        :service-cart-item.image/highlighted?  highlighted?
        :service-cart-item.image/confetti-mode confetti-mode
        :service-cart-item.action/id           "line-item-remove-freeinstall"
        :service-cart-item.action/target       [e/control-checkout-remove-promotion {:code "freeinstall"}]
        :service-cart-item.action/spinning?    remove-freeinstall-pending?
        :service-cart-item.price/id            "line-item-freeinstall-price"
        :service-cart-item.price/value         (some-> service-discount - mf/as-money)})
     (when (and locked? applied?)
       {:service-cart-item.title/secondary                (str "Add " quantity-needed " more hair " (ui/pluralize quantity-needed "item") " to receive this free service.")
        :service-cart-item.image/locked?                  true
        :service-cart-item.steps-to-complete/target       add-items-action
        :service-cart-item.steps-to-complete/label        "add items"
        :service-cart-item.steps-to-complete/id           "add-items"
        :service-cart-item.steps-to-complete/steps        quantity-required
        :service-cart-item.steps-to-complete/current-step quantity-added})
     (when (and applied? matched?)
       {:service-cart-item.modal-button/id              "browse-addons"
        :service-cart-item.modal-button/target          [e/control-show-addon-service-menu]
        :service-cart-item.modal-button/tracking-target [e/browse-addon-service-menu-button-enabled]
        :service-cart-item.modal-button/locked?         locked?
        :service-cart-item.modal-button/label           "+ Browse Add-Ons"})
     (when (and applied? any-wig?)
       {:service-cart-item.title/secondary     "You're all set! Bleaching knots, tinting & cutting lace and hairline customization included."
        :service-cart-item.image/id            "wig-customization"
        :service-cart-item.action/id           "line-item-remove-wig-customization"
        :service-cart-item.price/id            "wig-customization-price"}))))

#_(defn ^:private physical-cart-items-query
  [line-items]
  (let [update-line-item-requests (merge-with
                                   #(or %1 %2)
                                   (variants-requests app-state request-keys/add-to-bag (map :sku line-items))
                                   (variants-requests app-state request-keys/update-line-item (map :sku line-items)))
        delete-line-item-requests (variants-requests app-state request-keys/delete-line-item (map :id line-items))

        cart-items                 (for [{sku-id :sku variant-id :id :as line-item} line-items
                                         :let
                                         [sku                  (get skus sku-id)
                                          price                (or (:sku/price line-item)
                                                                   (:unit-price line-item))
                                          qty-adjustment-args {:variant (select-keys line-item [:id :sku])}
                                          removing?            (get delete-line-item-requests variant-id)
                                          updating?            (get update-line-item-requests sku-id)
                                          just-added-to-order? (some #(= sku-id %) (get-in app-state keypaths/cart-recently-added-skus))]]
                                     {:react/key                                      (str sku-id "-" (:quantity line-item))
                                      :cart-item-title/id                             (str "line-item-title-" sku-id)
                                      :cart-item-title/primary                        (or (:product-title line-item)
                                                                                          (:product-name line-item))
                                      :cart-item-title/secondary                      (:color-name line-item)
                                      :cart-item-floating-box/id                      (str "line-item-price-ea-with-label-" sku-id)
                                      :cart-item-floating-box/value                   [:div {:data-test (str "line-item-price-ea-" sku-id)}
                                                                                       (mf/as-money price)
                                                                                       [:div.proxima.content-4 " each"]]
                                      :cart-item-square-thumbnail/id                  sku-id
                                      :cart-item-square-thumbnail/sku-id              sku-id
                                      :cart-item-square-thumbnail/highlighted?        just-added-to-order?
                                      :cart-item-square-thumbnail/sticker-label       (when-let [length-circle-value (-> sku :hair/length first)]
                                                                                        (str length-circle-value "”"))
                                      :cart-item-square-thumbnail/ucare-id            (->> sku (catalog-images/image "cart") :ucare/id)
                                      :cart-item-adjustable-quantity/id               (str "line-item-quantity-" sku-id)
                                      :cart-item-adjustable-quantity/spinning?        updating?
                                      :cart-item-adjustable-quantity/value            (:quantity line-item)
                                      :cart-item-adjustable-quantity/id-suffix        sku-id
                                      :cart-item-adjustable-quantity/decrement-target [events/control-cart-line-item-dec qty-adjustment-args]
                                      :cart-item-adjustable-quantity/increment-target [events/control-cart-line-item-inc qty-adjustment-args]
                                      :cart-item-remove-action/id                     (str "line-item-remove-" sku-id)
                                      :cart-item-remove-action/spinning?              removing?
                                      :cart-item-remove-action/target                 [events/control-cart-remove (:id line-item)]})]
    cart-items)
  )

;;; domain methods

(defn cart-current
  [app-state]
  (get-in app-state k/order))

(defn cart-empty?
  [cart]
  (-> cart orders/product-quantity zero?))
;;;
(defn add-product-title-and-color-to-line-item [products facets line-item]
  (merge line-item {:product-title (->> line-item
                                        :sku
                                        (products/find-product-by-sku-id products)
                                        :copy/title)
                    :color-name    (-> line-item
                                       :variant-attrs
                                       :color
                                       (facets/get-color facets)
                                       :option/name)}))

(defn page
  [app-state opts]
  (let [cart                        (cart-current app-state)
        mayvenn-install             (mayvenn-install/mayvenn-install app-state)
        site                        (sites/determine-site app-state)
        stylist                     (get-in app-state adventure.k/adventure-servicing-stylist)
        redirecting-to-paypal?      (get-in app-state k/cart-paypal-redirect)
        update-pending?             (update-pending? app-state)
        remove-freeinstall-pending? (remove-freeinstall-pending? app-state)
        highlighted?                (get-in app-state k/cart-freeinstall-just-added?)
        confetti-mode               (get-in app-state k/confetti-mode)
        products                    (get-in app-state k/v2-products)
        facets                      (get-in app-state k/v2-facets)
        skus                        (get-in app-state k/v2-skus)
        line-items                  (map (partial add-product-title-and-color-to-line-item products facets)
                                         (orders/product-items cart))
        recently-added-sku-ids      (get-in app-state k/cart-recently-added-skus)
        last-texture-added (->> recently-added-sku-ids
                                last
                                (get skus)
                                :hair/texture
                                first)
        add-items-action [e/navigate-category
                          (merge
                           {:page/slug           "mayvenn-install"
                            :catalog/category-id "23"}
                           (when last-texture-added
                             {:query-params {:subsection last-texture-added}}))]]
    (c/build template
             (cond->
                 {:header      (header/query app-state)
                  :footer      (footer/query app-state)
                  :flash       (flash/query app-state)
                  ;; TODO: Return link should be dynamic depending on cart contents
                  :return-link return-link-query
                  ;; TODO: footer
                  }

               (and (cart-empty? cart)
                    (not
                     (:mayvenn-install/entered? mayvenn-install)))
               (merge {:empty-cart          (empty-cart-query site)
                       :static-promo-banner (promo-banner/query
                                             app-state)})

               (not (cart-empty? cart))
               (merge {:stylist-cart-item (stylist-cart-item-query stylist)
                       :service-cart-item (service-cart-item-query mayvenn-install add-items-action remove-freeinstall-pending? highlighted? confetti-mode)
    ;;                   :physical-items    (physical-cart-items-query line-items)
                       :cart-summary      {}
                       :begin-checkout    (begin-checkout-query
                                           mayvenn-install
                                           update-pending?
                                           redirecting-to-paypal?)}))
             opts)))
