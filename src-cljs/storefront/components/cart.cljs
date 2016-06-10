(ns storefront.components.cart
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.accessors.navigation :as navigation]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.promos :as promos]
            [storefront.accessors.stylists :as stylists]
            [storefront.components.order-summary :as order-summary]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.components.utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.request-keys :as request-keys]
            [storefront.hooks.experiments :as experiments]))

(defn- pluralize
  ([cnt singular] (pluralize cnt singular (str singular "s")))
  ([cnt singular plural]
   (str cnt " " (if (= 1 (max cnt (- cnt))) singular plural))))

(defn share-link-component [{:keys [share-url]} owner {:keys [on-close]}]
  (om/component
   (html
    (ui/modal on-close
              [:.bg-light-white.rounded.p3.center.mt3
               [:.h2.black.medium "Share your bag"]
               [:.h5.dark-gray.light.my2 "Share this link so your customers know exactly what to buy"]
               [:.border-top.border-bottom.border-light-silver.py2.my1
                [:input.border.border-light-gray.rounded.pl1.py2.bg-pure-white.green.col-12
                 {:type "text"
                  :value share-url
                  :on-click utils/select-all-text}]]
               [:.navy.h5.my2 "(select and copy link to share)"]
               (ui/button "Close" {:color "bg-navy"
                                   :on-click on-close})]))))

(defn query-share-link [data]
  {:share-url (get-in data keypaths/shared-cart-url)})

(defn full-component [{:keys [order
                              products
                              coupon-code
                              applying-coupon?
                              updating?
                              redirecting-to-paypal?
                              redesigned?
                              share-carts?
                              requesting-shared-cart?
                              update-line-item-requests
                              delete-line-item-requests]} owner]
  (om/component
   (html
    (ui/container
     [:.h2.center.py3.silver.sans-serif
      "You have " (pluralize (orders/product-quantity order) "item") " in your shopping bag."]

     [:.h2.py1
      {:data-test "order-summary"}
      "Review your order"]

     [:.py2.md-flex.justify-between
      [:.md-up-col-6
       {:data-test "cart-line-items"}
       (order-summary/display-adjustable-line-items (orders/product-items order)
                                                    products
                                                    update-line-item-requests
                                                    delete-line-item-requests
                                                    redesigned?)]
      [:.md-up-col-5
       [:form.my1
        {:on-submit (utils/send-event-callback events/control-cart-update-coupon)}
        [:.pt2.flex.items-center
         [:.col-8.pr1
          (ui/text-field "Promo code" keypaths/cart-coupon-code coupon-code {})]
         [:.col-4.pl1.mb2.inline-block (ui/button "Apply"
                                                  {:on-click      (utils/send-event-callback events/control-cart-update-coupon)
                                                   :disabled?     updating?
                                                   :show-spinner? applying-coupon?})]]]

       (order-summary/display-order-summary order)

       [:form
        {:on-submit (utils/send-event-callback events/control-checkout-cart-submit)}
        (ui/submit-button "Check out" {:spinning? false
                                       :disabled? updating?
                                       :data-test "start-checkout-button"})]
       [:div.h4.gray.center.py2 "OR"]
       [:div.pb2 (ui/button
                  [:.col-12.flex.items-center.justify-center
                   [:.right-align.mr1 "Check out with"]
                   [:.h2.medium.sans-serif.italic "PayPal™"]]
                  {:on-click      (utils/send-event-callback events/control-checkout-cart-paypal-setup)
                   :show-spinner? redirecting-to-paypal?
                   :disabled?     updating?
                   :color         "bg-paypal-blue"
                   :data-test     "paypal-checkout"})]

       (when share-carts?
         [:div.border-top.border-bottom.border-light-silver.py2
          (ui/button [:.flex.items-center.justify-center
                      [:.img-share-icon.bg-center.bg-no-repeat.bg-contain.mr2
                       {:style {:width "24px"
                                :height "18px"}}]
                      ;; col-12 is needed for mobile safari
                      [:.col-12 "Share your bag"]]
                     {:btn-type "btn-outline"
                      :color "bg-white"
                      :border "border-navy"
                      :text-color "navy"
                      :on-click (utils/send-event-callback events/control-cart-share-show)
                      :show-spinner? requesting-shared-cart?})
          [:.h4.pt2.dark-gray.light "Click the button above to share this bag with customers."]])]]))))

(defn empty-component [{:keys [shop-now-nav-message promotions]} owner]
  (om/component
   (html
    (ui/narrow-container
     [:.center
      {:data-test "empty-bag"}
      [:.m2
       (svg/bag {:height "70px" :width "70px"} 1)]

      [:p.m2.h1.light "Your bag is empty."]

      [:.m2
       (if-let [promo (promos/default-advertised-promotion promotions)]
         (:description promo)
         promos/bundle-discount-description)]]

     (ui/button "Shop Now" (apply utils/route-to shop-now-nav-message))))))

(defn ^:private variants-requests [data request-key variant-ids]
  (->> variant-ids
       (map (juxt identity
                  #(utils/requesting? data (conj request-key %))))
       (into {})))

(defn ^:private update-pending? [data]
  (let [request-key-prefix (comp vector first :request-key)]
    (some #(apply utils/requesting? data %)
          [request-keys/checkout-cart
           request-keys/add-promotion-code
           [request-key-prefix request-keys/update-line-item]
           [request-key-prefix request-keys/delete-line-item]])))

(defn query [data]
  (let [order       (get-in data keypaths/order)
        line-items  (orders/product-items order)
        variant-ids (map :id line-items)]
    {:order                     order
     :products                  (get-in data keypaths/products)
     :coupon-code               (get-in data keypaths/cart-coupon-code)
     :updating?                 (update-pending? data)
     :applying-coupon?          (utils/requesting? data request-keys/add-promotion-code)
     :redirecting-to-paypal?    (get-in data keypaths/cart-paypal-redirect)
     :redesigned?               (experiments/product-page-redesign? data)
     :share-carts?              (and (experiments/share-carts? data) (stylists/own-store? data))
     :requesting-shared-cart?   (utils/requesting? data request-keys/create-shared-cart-id)
     :update-line-item-requests (variants-requests data request-keys/update-line-item variant-ids)
     :delete-line-item-requests (variants-requests data request-keys/delete-line-item variant-ids)}))

(defn empty-cart-query [data]
  {:promotions           (get-in data keypaths/promotions)
   :shop-now-nav-message (navigation/shop-now-navigation-message data)})

(defn built-component [data owner]
  (om/component
   (html
    (let [item-count (orders/product-quantity (get-in data keypaths/order))]
      (if (zero? item-count)
        (om/build empty-component (empty-cart-query data))
        (om/build full-component (query data)))))))
