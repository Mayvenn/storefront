(ns storefront.components.cart
  (:require [om.core :as om]
            [sablono.core :refer [html]]
            [cemerick.url :as url]
            [storefront.assets :as assets]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.promos :as promos]
            [storefront.accessors.stylists :as stylists]
            [storefront.components.order-summary :as summary]
            [storefront.components.promotion-banner :as promotion-banner]
            [storefront.components.svg :as svg]
            [storefront.components.share-links :as share-links]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [goog.events]
            [goog.dom]
            [goog.style]
            [goog.events.EventType :as EventType]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]
            [storefront.components.affirm :as affirm]))

(defn- pluralize
  ([cnt singular] (pluralize cnt singular (str singular "s")))
  ([cnt singular plural]
   (str cnt " " (if (= 1 (max cnt (- cnt))) singular plural))))

(defn facebook-link [share-url]
  (share-links/facebook-link (share-links/with-utm-medium share-url "facebook")))

(defn sms-link [share-url]
  ;; the ?& is to get this to work on iOS8 and Android at the same time
  (share-links/sms-link (str "Shop the bundles I picked for you here: "
                             (share-links/with-utm-medium share-url "sms"))))

(defn twitter-link [share-url]
  (share-links/twitter-link (share-links/with-utm-medium share-url "twitter")
                            "Shop my top virgin hair bundle picks here:"))

(defn email-link [share-url store-nickname]
  (share-links/email-link "My recommended bundles for you"
                          (str "Hey,

I've created a ready-to-shop cart with the bundles I recommend for you. Mayvenn is my pick for quality virgin human hair. They offer a totally free 30 day exchange program (if you have any issues with your hair at all). All you have to do is click the link below to check out.

Shop here: " (share-links/with-utm-medium share-url "email") "

Let me know if you have any questions.

Thanks,
"
                               store-nickname)))

(defn share-link-component [{:keys [share-url store-nickname]} owner {:keys [close-attrs]}]
  (om/component
   (html
    (let [utm-url (-> (url/url share-url)
                      (assoc-in [:query :utm_campaign] "sharebuttons"))]
      (ui/modal {:close-attrs close-attrs}
                [:.bg-white.rounded.p4.center
                 (ui/modal-close {:close-attrs close-attrs :data-test "share-url-close"})
                 [:.p1
                  [:.h3.navy.medium "Share your bag"]
                  [:.h5.dark-gray.light.my2 "Share this link so your customers know exactly what to buy"]
                  [:.border-top.border-bottom.border-gray.py2
                   [:div.clearfix.mxn1
                    [:div.p1.col.col-6.col-12-on-dt
                     [:a.h6.col-12.btn.btn-primary.bg-fb-blue
                      {:href   (facebook-link utm-url)
                       :target "_blank"}
                      [:img.align-middle.mr1 {:style {:height "18px"}
                                              :src   (assets/path "/images/share/fb.png")}]
                      "Share"]]
                    [:div.p1.col.col-6.col-12-on-dt
                     [:a.h6.col-12.btn.btn-primary.bg-twitter-blue
                      {:href   (twitter-link utm-url)
                       :target "_blank"}
                      [:img.align-middle.mr1 {:style {:height "18px"}
                                              :src   (assets/path "/images/share/twitter.png")}]
                      "Tweet"]]
                    [:div.p1.col.col-6.col-12-on-dt.hide-on-dt
                     [:a.h6.col-12.btn.btn-primary.bg-sms-green
                      {:href (sms-link utm-url)
                       :target "_blank"}
                      [:img.align-middle.mr1 {:style {:height "18px"}
                                              :src   (assets/path "/images/share/sms.png")}]
                      "SMS"]]
                    [:div.p1.col.col-6.col-12-on-dt
                     [:a.h6.col-12.btn.btn-primary.bg-dark-gray
                      {:href (email-link utm-url store-nickname)
                       :target "_blank"}
                      (svg/mail-envelope {:class "stroke-white align-middle mr1"
                                          :style {:height "18px" :width "18px"}})
                      "Email"]]]]
                  [:div.mt3.mb1
                   [:input.border.border-dark-gray.rounded.pl1.py1.bg-white.teal.col-12
                    {:type      "text"
                     :value     share-url
                     :data-test "share-url"
                     :on-click  utils/select-all-text}]]
                  [:div.navy "(select and copy link to share)"]]])))))

(defn query-share-link [data]
  {:share-url (get-in data keypaths/shared-cart-url)
   :store-nickname (:store_nickname (get-in data keypaths/store))})

(defn built-share-link-component [data opts]
  (om/build share-link-component (query-share-link data) opts))

(defn deploy-promotion-banner-component [data owner opts]
  (letfn [(handle-scroll [e] (om/set-state! owner :show? (< 75 (.-y (goog.dom/getDocumentScroll)))))
          (set-height [] (om/set-state! owner :banner-height (some-> owner
                                                                     (om/get-node "banner")
                                                                     goog.style/getSize
                                                                     .-height)))]
    (reify
      om/IInitState
      (init-state [this]
        {:show? false})
      om/IDidMount
      (did-mount [this]
        (om/set-state! owner :description-length (count (:description (:promo data))))
        (set-height)
        (goog.events/listen js/window EventType/SCROLL handle-scroll))
      om/IWillUnmount
      (will-unmount [this]
        (goog.events/unlisten js/window EventType/SCROLL handle-scroll))
      om/IWillReceiveProps
      (will-receive-props [this next-props]
        (set-height))
      om/IRenderState
      (render-state [this {:keys [show? banner-height]}]
        (html
         [:div.fixed.z1.top-0.left-0.right-0
          (if show?
            {:style {:margin-top "0"}
             :class "transition-2"}
            {:class "hide"
             :style {:margin-top (str "-" banner-height "px")}})
          [:div {:ref "banner"}
           (om/build promotion-banner/component data opts)]])))))

(defn full-component [{:keys [focused
                              order
                              skus
                              coupon-code
                              promotion-banner
                              applying-coupon?
                              updating?
                              redirecting-to-paypal?
                              share-carts?
                              requesting-shared-cart?
                              show-apple-pay?
                              disable-apple-pay-button?
                              update-line-item-requests
                              delete-line-item-requests
                              field-errors]} owner]
  (om/component
   (html
    [:div.container.p2
     (om/build deploy-promotion-banner-component promotion-banner)
     [:div.py3.h3.center
      [:.dark-gray
       "You have " (pluralize (orders/product-quantity order) "item") " in your shopping bag."]]

     [:div.h3.py1
      {:data-test "order-summary"}
      "Review your order"]

     [:div.mt2.clearfix.mxn3
      [:div.col-on-tb-dt.col-6-on-tb-dt.px3.mb3
       {:data-test "cart-line-items"}
       (summary/display-adjustable-line-items (orders/product-items order)
                                              skus
                                              update-line-item-requests
                                              delete-line-item-requests)]

      [:div.col-on-tb-dt.col-6-on-tb-dt.px3
       [:form.clearfix.mxn1
        {:on-submit (utils/send-event-callback events/control-cart-update-coupon)}
        [:div.col.col-8.px1
         (ui/text-field {:keypath   keypaths/cart-coupon-code
                         :data-test "promo-code"
                         :focused   focused
                         :label     "Promo code"
                         :value     coupon-code
                         :errors    (get field-errors ["promo-code"])
                         :data-ref  "promo-code"})]
        [:div.col.col-4.px1.mb3.inline-block
         (ui/teal-button {:on-click  (utils/send-event-callback events/control-cart-update-coupon)
                          :data-test "cart-apply-promo"
                          :disabled? updating?
                          :spinning? applying-coupon?}
                         "Apply")]]

       (summary/display-order-summary order
                                      {:read-only?        false
                                       :use-store-credit? false})

       [:form
        {:on-submit (utils/send-event-callback events/control-checkout-cart-submit)}
        (affirm/as-low-as-box {:amount      (:total order)
                               :middle-copy "Just 'Check Out' below."})
        (ui/submit-button "Check out" {:spinning? false
                                       :disabled? updating?
                                       :data-test "start-checkout-button"})]
       [:div.h5.dark-gray.center.py2 "OR"]

       [:div.pb2
        (ui/aqua-button {:on-click  (utils/send-event-callback events/control-checkout-cart-paypal-setup)
                         :spinning? redirecting-to-paypal?
                         :disabled? updating?
                         :data-test "paypal-checkout"}
                        [:div
                         "Check out with "
                         [:span.medium.italic "PayPal™"]])]

       (when show-apple-pay?
         [:div.pb2
          (ui/apple-pay-button
           {:on-click  (utils/send-event-callback events/control-checkout-cart-apple-pay)
            :data-test "apple-pay-checkout"
            :disabled? disable-apple-pay-button?}
           [:div.flex.items-center.justify-center
            "Check out with "
            [:span.img-apple-pay.bg-fill.bg-no-repeat.inline-block.mtp4.ml1 {:style {:width  "4rem"
                                                                                     :height "2rem"}}]])])

       (when share-carts?
         [:div.border-top.border-gray.py2
          (ui/ghost-button {:on-click  (utils/send-event-callback events/control-cart-share-show)
                            :spinning? requesting-shared-cart?
                            :data-test "share-cart"}
                           [:div.flex.items-center.justify-center
                            [:div.flex-none.img-share-icon.bg-center.bg-no-repeat.bg-contain.mr2
                             {:style {:width  "24px"
                                      :height "18px"}}]
                            [:div.flex-grow "Share your bag"]])
          [:div.h5.pt2.dark-gray.light "Click the button above to share this bag with customers."]])]]])))

(defn empty-component [{:keys [promotions]} owner]
  (om/component
   (html
    (ui/narrow-container
     [:div.p2
      [:.center {:data-test "empty-bag"}
       [:div.m2 (svg/bag {:style {:height "70px" :width "70px"}
                          :class "fill-black"})]

       [:p.m2.h2.light "Your bag is empty."]

       [:div.m2
        (if-let [promo (promos/default-advertised-promotion promotions)]
          (:description promo)
          promos/bundle-discount-description)]]

      (ui/teal-button (utils/route-to events/navigate-shop-by-look)
                      "Shop Our Looks")]))))

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

(defn full-cart-query [data]
  (let [order       (get-in data keypaths/order)
        line-items  (orders/product-items order)
        variant-ids (map :id line-items)]
    {:order                     order
     :skus                      (get-in data keypaths/v2-skus)
     :coupon-code               (get-in data keypaths/cart-coupon-code)
     :promotion-banner          (promotion-banner/query data)
     :updating?                 (update-pending? data)
     :applying-coupon?          (utils/requesting? data request-keys/add-promotion-code)
     :redirecting-to-paypal?    (get-in data keypaths/cart-paypal-redirect)
     :share-carts?              (stylists/own-store? data)
     :requesting-shared-cart?   (utils/requesting? data request-keys/create-shared-cart)
     :show-apple-pay?           (and (get-in data keypaths/show-apple-pay?)
                                     (seq (get-in data keypaths/shipping-methods))
                                     (seq (get-in data keypaths/states)))
     :disable-apple-pay-button? (get-in data keypaths/disable-apple-pay-button?)
     :update-line-item-requests (merge-with
                                 #(or %1 %2)
                                 (variants-requests data request-keys/add-to-bag (map :sku line-items))
                                 (variants-requests data request-keys/update-line-item (map :sku line-items)))
     :delete-line-item-requests (variants-requests data request-keys/delete-line-item variant-ids)
     :field-errors              (get-in data keypaths/field-errors)
     :focused                   (get-in data keypaths/ui-focus)}))

(defn empty-cart-query [data]
  {:promotions (get-in data keypaths/promotions)})

(defn component [{:keys [fetching-order?
                         item-count
                         empty-cart
                         full-cart]}
                 owner
                 opts]
  (om/component
   (html
    (if fetching-order?
      [:.py3.h2 ui/spinner]
      [:div
       (if (zero? item-count)
         (om/build empty-component empty-cart)
         (om/build full-component full-cart))]))))

(defn query [data]
  {:fetching-order? (utils/requesting? data request-keys/get-order)
   :item-count      (orders/product-quantity (get-in data keypaths/order))
   :empty-cart      (empty-cart-query data)
   :full-cart       (full-cart-query data)})

(defn built-component [data opts]
  (om/build component (query data) opts))
