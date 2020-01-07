(ns checkout.shop.cart
  (:require
   #?@(:cljs [[storefront.api :as api]
              [storefront.components.payment-request-button :as payment-request-button]
              [storefront.components.popup :as popup]
              [storefront.confetti :as confetti]
              [storefront.history :as history]
              [storefront.hooks.quadpay :as quadpay]
              [storefront.platform.messages :as messages]])
   [catalog.facets :as facets]
   [catalog.images :as catalog-images]
   [checkout.call-out :as call-out]
   [checkout.header :as header]
   [checkout.suggestions :as suggestions]
   [checkout.ui.cart-item :as cart-item]
   [checkout.ui.cart-summary :as cart-summary]
   [clojure.string :as string]
   [spice.core :as spice]
   [spice.date :as date]
   [storefront.accessors.adjustments :as adjustments]
   [storefront.accessors.mayvenn-install :as mayvenn-install]
   [storefront.accessors.experiments :as experiments]
   [storefront.accessors.orders :as orders]
   [storefront.accessors.products :as products]
   [storefront.accessors.stylists :as stylists]
   [storefront.component :as component :refer [defcomponent defdynamic-component]]
   [storefront.components.checkout-delivery :as checkout-delivery]
   [storefront.components.flash :as flash]
   [storefront.components.footer :as storefront.footer]
   [storefront.components.money-formatters :as mf]
   [storefront.components.svg :as svg]
   [storefront.components.ui :as ui]
   [storefront.events :as events]
   [storefront.keypaths :as keypaths]
   [storefront.platform.component-utils :as utils]
   [storefront.request-keys :as request-keys]
   [storefront.routes :as routes]
   [ui.molecules :as ui-molecules]
   [ui.promo-banner :as promo-banner]))

(def or-separator
  [:div.h5.black.py1.flex.items-center
   [:div.flex-grow-1.border-bottom.border-gray]
   [:div.mx2 "or"]
   [:div.flex-grow-1.border-bottom.border-gray]])

(defn ^:private servicing-stylist-banner-component
  [{:servicing-stylist-banner/keys [id name image-url rating action-id target]}]
  (when id
    [:div.flex.bg-cool-gray.pl5.pr3.py2.items-center {:data-test id}
     (ui/circle-picture {:width 50} (ui/square-image {:resizable-url image-url} 50))
     [:div.flex-grow-1.ml5
      [:div.content-3.proxima "Your Certified Mayvenn Stylist"]
      [:div.content-1.proxima name]
      [:div.mt1 (ui.molecules/stars-rating-molecule rating)]]
     (when action-id
       [:a.block.gray.medium.m1
        (merge {:data-test action-id
                :href (routes/path-for events/navigate-adventure-find-your-stylist)
                :on-click (apply utils/send-event-callback target)})
        (svg/swap-person {:width  "20px"
                          :height "21px"})])]))

(defdynamic-component ^:private confetti-spout
  (constructor [this props]
               (component/create-ref! this "confetti-spout")
               nil)
  (did-mount [this]
             #?(:cljs
                (when-let [node (component/get-ref this "confetti-spout")]
                  (confetti/burst node))))
  (did-update [this prev-props prev-state snapshot]
              #?(:cljs
                 (let [{:confetti-spout/keys [mode]} (component/get-props this)]
                   (when (= mode "firing")
                     (messages/handle-message events/set-confetti-mode {:mode "fired"})
                     (.then (confetti/burst (component/get-ref this "confetti-spout"))
                            #(messages/handle-message events/set-confetti-mode {:mode "ready"}))))))
  (will-unmount [this]
                #?(:cljs (messages/handle-message events/set-confetti-mode {:mode "ready"})))
  (render [this]
          (component/html
           [:div#confetti-spout.fixed.top-0.mx-auto
            {:style {:right "50%"}}
            [:div.mx-auto {:style {:width "100%"}
                           :ref   (component/use-ref this "confetti-spout")}]])))

(defcomponent full-component
  [{:keys [call-out
           cart-items
           cart-summary
           checkout-caption-copy
           checkout-disabled?
           entered?
           locked?
           promo-banner
           quantity-remaining
           redirecting-to-paypal?
           remove-freeinstall-event
           requesting-shared-cart?
           servicing-stylist-portrait-url
           share-carts?
           show-browser-pay?
           suggestions]
    :as   queried-data}
   _ _]
  [:div.container.px2
   (when (:confetti-spout/id queried-data)
     ;; Moving this check to the inside of the component breaks lifecycle
     ;; methods
     (component/build confetti-spout queried-data nil))

   (component/build promo-banner/sticky-organism promo-banner nil)

   (component/build call-out/component call-out nil)

   [:div.clearfix.mxn3
    (servicing-stylist-banner-component queried-data)
    [:div.col-on-tb-dt.col-6-on-tb-dt.px3.border-top.border-gray
     {:data-test "cart-line-items"}
     ;; HACK: have suggestions be paired with appropriate cart item
     (interpose
      [:div.flex
       [:div.ml2 {:style {:width "78px"}}]
       [:div.flex-grow-1.border-bottom.border-gray.ml-auto]]
      (map-indexed
       (fn [index cart-item]
         (component/build cart-item/organism {:cart-item   cart-item
                                              :suggestions (when (zero? index)
                                                             suggestions)}
                          {:key (str index)}))
       cart-items))]

    [:div.col-on-tb-dt.col-6-on-tb-dt
     (component/build cart-summary/organism cart-summary nil)

     [:div.px4.center ; Checkout buttons
      #?@(:cljs
          [(component/build quadpay/component queried-data nil)])

      [:div.p2
       (when checkout-caption-copy
         [:div.flex.h6.pt1.pr3.pb2
          {:data-test "checkout-caption"}
          [:div
           (ui/circle-picture
            {:width 50}
            ;; Note: We are not using ucare-id because stylist portraits may have
            ;; ucarecdn crop parameters saved into the url
            (ui/square-image {:resizable-url servicing-stylist-portrait-url} 50))]
          [:div.left-align.pl2 checkout-caption-copy]])

       (ui/button-large-primary {:spinning? false
                                 :disabled? checkout-disabled?
                                 :on-click  (utils/send-event-callback events/control-checkout-cart-submit)
                                 :data-test "start-checkout-button"}
                                "Check out")

       (when locked?
         [:div
          [:div.content-3.proxima.center.py1.mt2
           (str "Add " quantity-remaining (ui/pluralize quantity-remaining " more item"))]
          [:div.h5.py1.flex.items-center
           [:div.flex-grow-1.border-bottom.border-gray]
           [:div.content-3.proxima.mx2 "or"]
           [:div.flex-grow-1.border-bottom.border-gray]]
          [:div.mb2.center.content-2.proxima.s-color
           [:a.inherit-color.pointer (apply utils/fake-href remove-freeinstall-event)]
           "Checkout without a free Mayvenn Install"]])


       (when-not locked?
         [:div
          [:div.h5.black.py1.flex.items-center
           [:div.flex-grow-1.border-bottom.border-gray]
           [:div.mx2 "or"]
           [:div.flex-grow-1.border-bottom.border-gray]]

          [:div
           (ui/button-large-paypal {:on-click  (utils/send-event-callback events/control-checkout-cart-paypal-setup)
                                    :spinning? redirecting-to-paypal?
                                    :disabled? checkout-disabled?
                                    :data-test "paypal-checkout"}
                                   [:div
                                    "Check out with "
                                    [:span.medium.italic "PayPal™"]])]])
       #?@(:cljs [(when show-browser-pay? (payment-request-button/built-component nil {}))])]]

     (when share-carts?
       [:div.py2
        [:div.h6.center.pt2.black.bold "Is this bag for a customer?"]
        (ui/button-large-secondary {:on-click  (utils/send-event-callback events/control-cart-share-show)
                                    :class     "border-width-2 border-black"
                                    :spinning? requesting-shared-cart?
                                    :data-test "share-cart"}
                                   [:div.flex.items-center.justify-center.bold
                                    (svg/share-arrow {:class  "stroke-black mr1 fill-black"
                                                      :width  "24px"
                                                      :height "24px"})
                                    "Share your bag"])])]]])

(defn empty-cta-molecule
  [{:cta/keys [id label target]}]
  (when (and id label target)
    (ui/button-large-primary
     (merge {:data-test id} (apply utils/route-to target))
     [:div.flex.items-center.justify-center.inherit-color label])))

(defn empty-cart-body-molecule
  [{:empty-cart-body/keys [id primary secondary image-id]}]
  (when id
    [:div {:style {:margin-top "70px"}}
     [:h1.canela.title-1.mb4 primary]
     [:div.col-8.mx-auto.mt2.mb6 secondary]]))

(defcomponent empty-component [queried-data _ _]
  (ui/narrow-container
   [:div
    [:div.center {:data-test "empty-cart"}
     (empty-cart-body-molecule queried-data)
     [:div.col-9.mx-auto
      (empty-cta-molecule queried-data)]]]))

(defn empty-cart-query
  [data]
  {:return-link/id            "start-shopping"
   :return-link/copy          "Start Shopping"
   :return-link/event-message [events/navigate-category
                               {:catalog/category-id "23"
                                :page/slug           "mayvenn-install"}]

   :cta/id                    "browse-stylists"
   :cta/label                 "Browse Stylists"
   :cta/target                [events/navigate-adventure-match-stylist]
   :empty-cart-body/id        "empty-cart-body"
   :empty-cart-body/primary   "Your Cart is Empty"
   :empty-cart-body/secondary (str "Did you know that you'd qualify for a free"
                                   " Mayvenn Install when you purchase 3 or more items?")
   :empty-cart-body/image-id  "6146f2fe-27ed-4278-87b0-7dc46f344c8c"})

(defn ^:private variants-requests [data request-key variant-ids]
  (->> variant-ids
       (map (juxt identity
                  #(utils/requesting? data (conj request-key %))))
       (into {})))

(defn ^:private update-pending? [data]
  (let [request-key-prefix (comp vector first :request-key)]
    (some #(apply utils/requesting? data %)
          [request-keys/add-promotion-code
           [request-key-prefix request-keys/update-line-item]
           [request-key-prefix request-keys/delete-line-item]])))

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

(defn ^:private text->data-test-name [name]
  (-> name
      (string/replace #"[0-9]" (comp spice/number->word int))
      string/lower-case
      (string/replace #"[^a-z]+" "-")))
;; TODO: suggestions should be paired with appropriate cart item here


(defn cart-items-query
  [app-state
   {:mayvenn-install/keys
    [entered? locked? applied? stylist service-discount quantity-remaining quantity-required quantity-added]}
   line-items
   skus
   add-items-action]
  (let [update-line-item-requests (merge-with
                                   #(or %1 %2)
                                   (variants-requests app-state request-keys/add-to-bag (map :sku line-items))
                                   (variants-requests app-state request-keys/update-line-item (map :sku line-items)))
        delete-line-item-requests (variants-requests app-state request-keys/delete-line-item (map :id line-items))

        cart-items (for [{sku-id :sku variant-id :id :as line-item} line-items
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
                      :cart-item-square-thumbnail/id                sku-id
                      :cart-item-square-thumbnail/sku-id            sku-id
                      :cart-item-square-thumbnail/highlighted?      just-added-to-order?
                      :cart-item-square-thumbnail/sticker-label     (when-let [length-circle-value (-> sku :hair/length first)]
                                                                        (str length-circle-value "”"))
                      :cart-item-square-thumbnail/ucare-id          (->> sku (catalog-images/image "cart") :ucare/id)
                      :cart-item-adjustable-quantity/id               (str "line-item-quantity-" sku-id)
                      :cart-item-adjustable-quantity/spinning?        updating?
                      :cart-item-adjustable-quantity/value            (:quantity line-item)
                      :cart-item-adjustable-quantity/id-suffix        sku-id
                      :cart-item-adjustable-quantity/decrement-target [events/control-cart-line-item-dec qty-adjustment-args]
                      :cart-item-adjustable-quantity/increment-target [events/control-cart-line-item-inc qty-adjustment-args]
                      :cart-item-remove-action/id                     (str "line-item-remove-" sku-id)
                      :cart-item-remove-action/spinning?              removing?
                      :cart-item-remove-action/target                 [events/control-cart-remove (:id line-item)]})
        matched?   (boolean stylist)]

    (cond-> cart-items
      entered?
      (concat
       [(cond-> {:react/key                                "freeinstall-line-item-freeinstall"
                 :cart-item-title/id                       "line-item-title-freeinstall"
                 :cart-item-floating-box/id                "line-item-freeinstall-price"
                 :cart-item-floating-box/value             (some-> service-discount - mf/as-money)
                 :cart-item-remove-action/id               "line-item-remove-freeinstall"
                 :cart-item-remove-action/spinning?        (utils/requesting? app-state request-keys/remove-freeinstall-line-item)
                 :cart-item-remove-action/target           [events/control-checkout-remove-promotion {:code "freeinstall"}]
                 :cart-item-service-thumbnail/id           "freeinstall"
                 :cart-item-service-thumbnail/image-url    "//ucarecdn.com/3a25c870-fac1-4809-b575-2b130625d22a/"
                 :cart-item-service-thumbnail/highlighted? (get-in app-state keypaths/cart-freeinstall-just-added?)
                 :confetti-mode                            (get-in app-state keypaths/confetti-mode)}

          ;; Locked basically means the freeinstall coupon code was entered, yet not all the requirements
          ;; of a free install order to generate a voucher have been satisfied.
          locked?
          (merge  {:cart-item-title/primary                   "Mayvenn Install (locked)"
                   :cart-item-copy/value                      (str "Add " quantity-remaining
                                                                   " or more items to receive your free Mayvenn Install")
                   :cart-item-steps-to-complete/action-target add-items-action
                   :cart-item-steps-to-complete/action-label  "add items"
                   :cart-item-steps-to-complete/id            "add-items"
                   :cart-item-steps-to-complete/steps         (->> quantity-required
                                                                   range
                                                                   (map inc))
                   :cart-item-steps-to-complete/current-step  quantity-added
                   :cart-item-service-thumbnail/locked?       true})

          (and applied? (not matched?))
          (merge {:cart-item-pick-stylist/id      "pick-a-stylist"
                  :cart-item-pick-stylist/target  [events/control-pick-stylist-button]
                  :cart-item-pick-stylist/content "pick stylist"})

          applied?
          (merge {:cart-item-title/primary "Mayvenn Install"
                  :cart-item-copy/value    "Congratulations! You're all set for your Mayvenn Install. Click the button below to pick your stylist."
                  :cart-item-copy/id       "congratulations"})

          (and applied? matched?)
          (merge {:cart-item-copy/value "Congratulations! You're all set for your Mayvenn Install."}))]))))

(defn coupon-code->remove-promo-action [coupon-code]
  {:cart-summary-line/action-id     "cart-remove-promo"
   :cart-summary-line/action-icon   (svg/close-x {:class "stroke-white fill-gray"})
   :cart-summary-line/action-target [events/control-checkout-remove-promotion {:code coupon-code}]})

(defn cart-summary-query
  [{:as order :keys [adjustments]}
   {:mayvenn-install/keys [entered? locked? applied? service-discount quantity-remaining]}]
  (let [total              (:total order)
        tax                (:tax-total order)
        subtotal           (orders/products-subtotal order)
        shipping           (orders/shipping-item order)
        shipping-cost      (some->> shipping
                                    vector
                                    (apply (juxt :quantity :unit-price))
                                    (reduce *))
        shipping-timeframe (some->> shipping
                                    (checkout-delivery/enrich-shipping-method (date/now))
                                    :copy/timeframe)

        adjustment (->> order :adjustments (map :price) (reduce + 0))

        total-savings (- (+ adjustment service-discount))]
    {:cart-summary/id                 "cart-summary"
     :freeinstall-informational/value (not entered?)
     :cart-summary-total-line/id      "total"
     :cart-summary-total-line/label   (if applied? "Hair + Install Total" "Total")
     :cart-summary-total-line/value   (cond
                                        applied?
                                        [:div
                                         [:div.bold.h2
                                          (some-> total mf/as-money)]
                                         [:div.h6.bg-p-color.white.px2.nowrap.mb1
                                          "Includes Mayvenn Install"]
                                         (when (pos? total-savings)
                                           [:div.h6.light.pxp1.nowrap.italic
                                            "You've saved "
                                            [:span.bold.p-color {:data-test "total-savings"}
                                             (mf/as-money total-savings)]])]

                                        locked?
                                        [:div.h7.light
                                         "Add " quantity-remaining
                                         " more " (ui/pluralize quantity-remaining "item")
                                         " to "
                                         [:br]
                                         " calculate total price"]

                                        :else
                                        [:div (some-> total mf/as-money)])
     :cart-summary/lines (concat [{:cart-summary-line/id    "subtotal"
                                   :cart-summary-line/label "Subtotal"
                                   :cart-summary-line/value (mf/as-money (cond-> subtotal
                                                                           (or locked? applied?)
                                                                           ;; Add the service discount to the subtotal
                                                                           (- service-discount)))}]

                                 (when shipping
                                   [{:cart-summary-line/id       "shipping"
                                     :cart-summary-line/label    "Shipping"
                                     :cart-summary-line/sublabel shipping-timeframe
                                     :cart-summary-line/value    (mf/as-money-or-free shipping-cost)}])

                                 (when locked?
                                   ;; When FREEINSTALL is merely locked (and so not yet an adjustment) we must special case it, so:
                                   [(merge
                                     {:cart-summary-line/id    "freeinstall-locked"
                                      :cart-summary-line/icon  (svg/discount-tag {:class  "mxnp6 fill-gray pr1"
                                                                                  :height "2em" :width "2em"})
                                      :cart-summary-line/label "Free Mayvenn Install"
                                      :cart-summary-line/value (mf/as-money-or-free service-discount)
                                      :cart-summary-line/class "p-color"}
                                     (coupon-code->remove-promo-action "freeinstall"))

                                    {:cart-summary-line/action-id     "cart-remove-promo"
                                     :cart-summary-line/action-icon   (svg/close-x {:class "stroke-white fill-gray"})
                                     :cart-summary-line/action-target [events/control-checkout-remove-promotion {:code "freeinstall"}]}])

                                 (for [{:keys [name price coupon-code] :as adjustment}
                                       (filter adjustments/non-zero-adjustment? adjustments)
                                       :let [install-summary-line? (orders/freeinstall-promotion? adjustment)]]
                                   (cond-> {:cart-summary-line/id    (text->data-test-name name)
                                            :cart-summary-line/icon  (svg/discount-tag {:class  "mxnp6 fill-gray pr1"
                                                                                        :height "2em" :width "2em"})
                                            :cart-summary-line/label (orders/display-adjustment-name name)
                                            :cart-summary-line/class "p-color"
                                            :cart-summary-line/value (mf/as-money-or-free price)}

                                     (and install-summary-line?
                                          (empty? coupon-code))
                                     (merge {:cart-summary-line/value (mf/as-money-or-free service-discount)
                                             :cart-summary-line/class "p-color"}
                                            (coupon-code->remove-promo-action "freeinstall"))

                                     install-summary-line?
                                     (merge {:cart-summary-line/value (mf/as-money-or-free service-discount)
                                             :cart-summary-line/class "p-color"})
                                     coupon-code
                                     (merge (coupon-code->remove-promo-action coupon-code))))

                                 (when (pos? tax)
                                   [{:cart-summary-line/id    "tax"
                                     :cart-summary-line/label "Tax"
                                     :cart-summary-line/value (mf/as-money tax)}]))}))

(defn promo-input-query
  [data order entered?]
  (when (and (orders/no-applied-promo? order) (not entered?))
    (let [keypath                keypaths/cart-coupon-code
          value                  (get-in data keypath)
          promo-link?            (experiments/promo-link? data)
          show-promo-code-field? (or (not promo-link?)
                                     (get-in data keypaths/promo-code-entry-open?))
          disabled?              (or (update-pending? data)
                                     (empty? value))
          input-group-attrs      {:text-input-attrs
                                  {:errors        nil
                                   :value         (or value "")
                                   :keypath       keypath
                                   :label         "enter promocode"
                                   :data-test     "promo-code"
                                   :id            "promo-code"
                                   :wrapper-class "col-12 bg-white"
                                   :type          "text"}
                                  :button-attrs
                                  {:args    {:data-test "cart-apply-promo"
                                             :disabled? disabled?
                                             :on-click  (utils/send-event-callback events/control-cart-update-coupon)
                                             :style     {:width   "42px"
                                                         :padding "0"}}
                                   :content (svg/forward-arrow {:class (if disabled?
                                                                         "fill-gray"
                                                                         "fill-white")
                                                                :style {:width  "14px"
                                                                        :height "14px"}})}}]
      (cond-> {}

        show-promo-code-field?
        (merge input-group-attrs)

        promo-link?
        (merge
         {:field-reveal/id     "reveal-promo-entry"
          :field-reveal/target [events/control-toggle-promo-code-entry]
          :field-reveal/label  (if show-promo-code-field?
                                 "Hide promo code"
                                 "Add promo code")})))))

(defn full-cart-query [data]
  (let [shop?                                (#{"shop"} (get-in data keypaths/store-slug))
        order                                (get-in data keypaths/order)
        products                             (get-in data keypaths/v2-products)
        facets                               (get-in data keypaths/v2-facets)
        line-items                           (map (partial add-product-title-and-color-to-line-item products facets)
                                                  (orders/product-items order))
        freeinstall-entered-cart-incomplete? (and (orders/freeinstall-entered? order)
                                                  (not (orders/freeinstall-applied? order)))
        mayvenn-install                      (mayvenn-install/mayvenn-install data)
        entered?                             (:mayvenn-install/entered? mayvenn-install)
        applied?                             (:mayvenn-install/applied? mayvenn-install)
        servicing-stylist                    (:mayvenn-install/stylist mayvenn-install)
        locked?                              (:mayvenn-install/locked? mayvenn-install)
        skus                                 (get-in data keypaths/v2-skus)
        recently-added-sku-ids               (get-in data keypaths/cart-recently-added-skus)
        last-texture-added                   (->> recently-added-sku-ids
                                                  last
                                                  (get skus)
                                                  :hair/texture
                                                  first)
        add-items-action                     [events/navigate-category
                                              (merge
                                               {:page/slug           "mayvenn-install"
                                                :catalog/category-id "23"}
                                               (when last-texture-added
                                                 {:query-params {:subsection last-texture-added}}))]]
    (cond-> {:suggestions                        (suggestions/consolidated-query data)
             :line-items                         line-items
             :skus                               skus
             :products                           products
             :promo-banner                       (when (zero? (orders/product-quantity order))
                                                   (promo-banner/query data))
             :call-out                           (call-out/query data)
             :checkout-disabled?                 (or freeinstall-entered-cart-incomplete?
                                                     (update-pending? data))
             :redirecting-to-paypal?             (get-in data keypaths/cart-paypal-redirect)
             :share-carts?                       (stylists/own-store? data)
             :requesting-shared-cart?            (utils/requesting? data request-keys/create-shared-cart)
             :show-browser-pay?                  (and (get-in data keypaths/loaded-stripe)
                                                      (experiments/browser-pay? data)
                                                      (seq (get-in data keypaths/shipping-methods))
                                                      (seq (get-in data keypaths/states)))
             :recently-added-skus                recently-added-sku-ids
             :return-link/id                     "continue-shopping"
             :return-link/copy                   "Continue Shopping"
             :return-link/event-message          add-items-action
             :quantity-remaining                 (:mayvenn-install/quantity-remaining mayvenn-install)
             :locked?                            locked?
             :entered?                           entered?
             :applied?                           (:mayvenn-install/applied? mayvenn-install)
             :remove-freeinstall-event           [events/control-checkout-remove-promotion {:code "freeinstall"}]
             :cart-summary                       (merge
                                                  (cart-summary-query order mayvenn-install)
                                                  {:promo-field-data (promo-input-query data order entered?)})
             :cart-items                         (cart-items-query data mayvenn-install line-items skus add-items-action)
             :quadpay/order-total                (when-not locked? (:total order))
             :quadpay/show?                      (get-in data keypaths/loaded-quadpay)
             :quadpay/directive                  (if locked? :no-total :just-select)}

      entered?
      (merge {:checkout-caption-copy          "You'll be able to select your Mayvenn Certified Stylist after checkout."
              :servicing-stylist-portrait-url "//ucarecdn.com/bc776b8a-595d-46ef-820e-04915478ffe8/"})

      (and entered? servicing-stylist)
      (merge {:checkout-caption-copy              (str "After your order ships, you'll be connected with " (stylists/->display-name servicing-stylist) " over SMS to make an appointment.")
              :servicing-stylist-banner/id        "servicing-stylist-banner"
              :servicing-stylist-banner/name      (stylists/->display-name servicing-stylist)
              :servicing-stylist-banner/rating    {:rating/value (:rating servicing-stylist)}
              :servicing-stylist-banner/image-url (some-> servicing-stylist :portrait :resizable-url)
              :servicing-stylist-banner/target    [events/control-change-stylist {:stylist-id (:stylist-id servicing-stylist)}]
              :servicing-stylist-banner/action-id "stylist-swap"
              :servicing-stylist-portrait-url     (-> servicing-stylist :portrait :resizable-url)})

      (and entered? servicing-stylist (not shop?))
      (merge {:checkout-caption-copy              (str "After you place your order, please contact "
                                                       (stylists/->display-name servicing-stylist)
                                                       " to make your Mayvenn free install appointment.")
              :servicing-stylist-banner/action-id nil})

      (and (experiments/mayvenn-rating? data)
           entered?
           servicing-stylist
           (:mayvenn-rating servicing-stylist))
      (merge
       {:servicing-stylist-banner/rating {:rating/value (:mayvenn-rating servicing-stylist)}})

      applied?
      (merge {:confetti-spout/mode (get-in data keypaths/confetti-mode)
              :confetti-spout/id   "confetti-spout"}))))

(defcomponent cart-component
  [{:keys [fetching-order?
           item-count
           empty-cart
           full-cart]} owner opts]
  (if fetching-order?
    [:div.py3.h2 ui/spinner]
    (let [empty?    (and (zero? item-count)
                         (not (:entered? full-cart)))
          cart-data (if empty?
                      empty-cart
                      full-cart)]
      [:div
       [:div.border-bottom.border-gray.border-width-1
        [:div.px2.my2 (ui-molecules/return-link cart-data)]]
       [:div.col-7-on-dt.mx-auto
        (component/build (if empty?
                           empty-component
                           full-component) cart-data opts)]])))

(defn query [data]
  {:fetching-order? (utils/requesting? data request-keys/get-order)
   :item-count      (orders/product-quantity (get-in data keypaths/order))
   :empty-cart      (empty-cart-query data)
   :full-cart       (full-cart-query data)})

(defcomponent template
  [{:keys [header footer popup promo-banner flash cart data nav-event]} _ _]
  [:div.flex.flex-column {:style {:min-height    "100vh"
                                  :margin-bottom "-1px"}}
   #?(:cljs (popup/built-component popup nil))

   (when promo-banner
     (promo-banner/built-static-organism promo-banner nil))

   (header/built-component header nil)
   [:div.relative.flex.flex-column.flex-auto
    (flash/built-component flash nil)

    [:main.bg-white.flex-auto {:data-test (keypaths/->component-str nav-event)}
     (component/build cart-component cart nil)]

    [:footer
     (storefront.footer/built-component footer nil)]]])

(defn page
  [app-state nav-event]
  (component/build template
                   (merge
                    (when (and (zero? (orders/product-quantity (get-in app-state keypaths/order)))
                               (-> app-state mayvenn-install/mayvenn-install :mayvenn-install/entered? not))
                      {:promo-banner app-state})
                    {:cart      (query app-state)
                     :header    app-state
                     :footer    app-state
                     :popup     app-state
                     :flash     app-state
                     :data      app-state
                     :nav-event nav-event})))
