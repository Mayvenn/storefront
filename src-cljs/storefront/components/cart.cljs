(ns storefront.components.cart
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.promos :as promos]
            [storefront.accessors.stylists :as stylists]
            [storefront.assets :as assets]
            [storefront.components.order-summary :as order-summary]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]))

(defn- pluralize
  ([cnt singular] (pluralize cnt singular (str singular "s")))
  ([cnt singular plural]
   (str cnt " " (if (= 1 (max cnt (- cnt))) singular plural))))

(defn share-icon [icon-class]
  [:div.bg-no-repeat.bg-center.bg-full
   {:style {:height "30px" :width "70px"}
    :class icon-class}])

(defn facebook-link [share-url]
  (str "https://www.facebook.com/sharer/sharer.php?u=" share-url "%3Futm_medium=facebook%26utm_campaign=sharebuttons"))

(defn sms-link [share-url]
  ;; the ?& is to get this to work on iOS8 and Android at the same time
  (str "sms:?&body="
       (js/encodeURIComponent "Shop the bundles I picked for you here: ")
       share-url
       "%3Futm_medium=sms%26utm_campaign=sharebuttons"))

(defn twitter-link [share-url]
  (str "https://twitter.com/intent/tweet?url="
       share-url "%3Futm_medium=twitter%26utm_campaign=sharebuttons"
       "&text=" (js/encodeURIComponent "Shop my top virgin hair bundle picks here:")
       "&hashtags=mayvennhair"))

(defn email-link [share-url store-nickname]
  (str "mailto:?Subject="
       (js/encodeURIComponent "My recommended bundles for you")
       "&body="
       (js/encodeURIComponent (str "Hey,

I've created a ready-to-shop cart with the bundles I recommend for you. Mayvenn is my pick for the quality virgin human hair. They offer a totally free 30 day exchange program (if you have any issues with your hair at all). All you have to do is click the link below to check out.

Shop here: "
                                   share-url"?utm_medium=email&utm_campaign=sharebuttons"

                                   "

Let me know if you have any questions.

Thanks,
"
                                   store-nickname))))

(defn share-link-component [{:keys [share-url store-nickname]} owner {:keys [on-close]}]
  (om/component
   (html
    (ui/modal {:on-close on-close}
              [:.bg-light-white.rounded.p2.center
               (ui/modal-close {:on-close on-close :data-test "share-url-close"})
               [:.p1
                [:.h2.navy.medium "Share your bag"]
                [:.h4.dark-gray.light.my2 "Share this link so your customers know exactly what to buy"]
                [:.border-top.border-bottom.border-light-silver.py2.flex.justify-center
                 [:a.mx1 {:href (facebook-link share-url) :target "_blank"}
                  (share-icon "img-fb-share")]
                 [:a.mx1 {:href (twitter-link share-url) :target "_blank"}
                  (share-icon "img-twitter-share")]
                 [:a.mx1.lg-up-hide {:href (sms-link share-url)}
                  (share-icon "img-sms-share")]
                 [:a.mx1 {:href (email-link share-url store-nickname)}
                  (share-icon "img-email-share")]]
                [:div.mt3.mb1
                 [:input.border.border-light-gray.rounded.pl1.py1.bg-pure-white.green.col-12
                  {:type "text"
                   :value share-url
                   :data-test "share-url"
                   :on-click utils/select-all-text}]]
                [:div.navy "(select and copy link to share)"]]]))))

(defn query-share-link [data]
  {:share-url (get-in data keypaths/shared-cart-url)
   :store-nickname (:store_nickname (get-in data keypaths/store))})

(defn built-share-link-component [data opts]
  (om/build share-link-component (query-share-link data) opts))

(defn full-component [{:keys [order
                              products
                              coupon-code
                              applying-coupon?
                              updating?
                              redirecting-to-paypal?
                              share-carts?
                              requesting-shared-cart?
                              essence?
                              update-line-item-requests
                              delete-line-item-requests]} owner]
  (om/component
   (html
    (ui/container
     [:div.py3.h2.center
      [:.silver
       "You have " (pluralize (orders/product-quantity order) "item") " in your shopping bag."]]

     [:div.h2.py1
      {:data-test "order-summary"}
      "Review your order"]

     [:div.mt2.clearfix.mxn4
      [:div.md-up-col.md-up-col-6.px4
       {:data-test "cart-line-items"}

       (when essence?
         [:div.clearfix.border.border-orange.py1
          [:div.col.mx1 {:style {:width "7.33em"}}
           [:img {:src (assets/path "/images/essence/essence@2x.png") :width "72px" :height "70px"}]]
          [:div.h5.mb1.line-height-2
           [:div.bold.shout.mb1.h4 "bonus offer!"]
           "A one-year subscription to " [:span.bold "ESSENCE "] "magazine is included with your order ($10 value)."]
          [:a.h5.navy {:href "#"} "Offer and Rebate Details ➤"]])

       (order-summary/display-adjustable-line-items (orders/product-items order)
                                                    products
                                                    update-line-item-requests
                                                    delete-line-item-requests)]
      [:div.md-up-col.md-up-col-6.px4
       [:form.my1
        {:on-submit (utils/send-event-callback events/control-cart-update-coupon)}
        [:div.pt2.flex.items-center
         [:div.col-8.pr1
          (ui/text-field "Promo code" keypaths/cart-coupon-code coupon-code {})]
         [:div.col-4.pl1.mb3.inline-block
          (ui/green-button {:on-click  (utils/send-event-callback events/control-cart-update-coupon)
                            :disabled? updating?
                            :spinning? applying-coupon?}
                           "Apply")]]]

       (order-summary/display-order-summary order)

       [:form
        {:on-submit (utils/send-event-callback events/control-checkout-cart-submit)}
        (ui/submit-button "Check out" {:spinning? false
                                       :disabled? updating?
                                       :data-test "start-checkout-button"})]
       [:div.h4.gray.center.py2 "OR"]
       [:div.pb2 (ui/paypal-button
                  {:on-click  (utils/send-event-callback events/control-checkout-cart-paypal-setup)
                   :spinning? redirecting-to-paypal?
                   :disabled? updating?
                   :data-test "paypal-checkout"}
                  [:div.flex.items-center.justify-center
                   [:div.right-align.mr1 "Check out with"]
                   [:div.h2.medium.italic "PayPal™"]])]

       (when share-carts?
         [:div.border-top.border-light-silver.py2
          (ui/navy-outline-button {:on-click  (utils/send-event-callback events/control-cart-share-show)
                                   :spinning? requesting-shared-cart?
                                   :data-test "share-cart"}
                                  [:div.flex.items-center.justify-center
                                   [:div.flex-none.img-share-icon.bg-center.bg-no-repeat.bg-contain.mr2
                                    {:style {:width  "24px"
                                             :height "18px"}}]
                                   [:div.flex-grow "Share your bag"]])
          [:div.h4.pt2.dark-gray.light "Click the button above to share this bag with customers."]])]]))))

(defn empty-component [{:keys [promotions]} owner]
  (om/component
   (html
    (ui/narrow-container
     [:.center {:data-test "empty-bag"}
      [:div.m2 (svg/bag {:height "70px" :width "70px"} 1)]

      [:p.m2.h1.light "Your bag is empty."]

      [:div.m2
       (if-let [promo (promos/default-advertised-promotion promotions)]
         (:description promo)
         promos/bundle-discount-description)]]

     (ui/green-button (utils/route-to events/navigate-categories)
                      "Shop Now")))))

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

(defn full-cart-query [data]
  (let [order       (get-in data keypaths/order)
        line-items  (orders/product-items order)
        variant-ids (map :id line-items)]
    {:order                     order
     :products                  (get-in data keypaths/products)
     :coupon-code               (get-in data keypaths/cart-coupon-code)
     :updating?                 (update-pending? data)
     :applying-coupon?          (utils/requesting? data request-keys/add-promotion-code)
     :redirecting-to-paypal?    (get-in data keypaths/cart-paypal-redirect)
     :share-carts?              (stylists/own-store? data)
     :requesting-shared-cart?   (utils/requesting? data request-keys/create-shared-cart)
     :essence?                  (experiments/essence? data)
     :update-line-item-requests (variants-requests data request-keys/update-line-item variant-ids)
     :delete-line-item-requests (variants-requests data request-keys/delete-line-item variant-ids)}))

(defn empty-cart-query [data]
  {:promotions           (get-in data keypaths/promotions)})

(defn component [{:keys [fetching-order?
                         item-count
                         empty-cart
                         full-cart]}
                 owner
                 opts]
  (om/component
   (html
    (if fetching-order?
      [:.py3.h1 ui/spinner]
      (if (zero? item-count)
        (om/build empty-component empty-cart)
        (om/build full-component full-cart))))))

(defn query [data]
  {:fetching-order? (utils/requesting? data request-keys/get-order)
   :item-count      (orders/product-quantity (get-in data keypaths/order))
   :empty-cart      (empty-cart-query data)
   :full-cart       (full-cart-query data)})

(defn built-component [data opts]
  (om/build component (query data) opts))
