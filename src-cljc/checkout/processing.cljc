(ns checkout.processing
  (:require [storefront.components.ui :as ui]
            #?@(:cljs [[storefront.browser.cookie-jar :as cookie-jar]
                       [storefront.api :as api]
                       [storefront.history :as history]])
            [storefront.component :as component]
            [storefront.keypaths :as keypaths]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.platform.messages :as messages]))

(defn component
  [{:keys []} _ _]
  (component/create
   (ui/narrow-container
    [:div.py6.h2
     [:div.py4 (ui/large-spinner {:style {:height "6em"}})]
     [:h2.center.navy "Processing your order..."]])))

(defn query [data] {})

(defn built-component [data opts]
  (component/build component (query data) opts))

(defn place-order [app-state]
  #?(:cljs
     (api/place-order (get-in app-state keypaths/session-id)
                      (get-in app-state keypaths/order)
                      (cookie-jar/retrieve-utm-params (get-in app-state keypaths/cookie))
                      {:error-handler #(let [{:keys [error-code error-message]} (-> % :response :body)]
                                         (when (= error-code "ineligible-for-free-install")
                                           (messages/handle-message events/order-remove-promotion
                                                                    {:code         "freeinstall"
                                                                     :hide-success true}))
                                         (if (= error-code "quadpay-failed-to-capture-payment")
                                           (do
                                             (history/enqueue-navigate events/navigate-checkout-payment
                                                                       {:query-params {:error error-code}})
                                             (messages/handle-later events/flash-show-failure {:message error-message}))
                                           (history/enqueue-navigate events/navigate-cart
                                                                     {:query-params {:error error-code}})))})))

(defn get-order-status
  [{:keys [number token]} freeinstall-domain?]
  #?(:cljs
     (api/poll-order number token
                     (fn [{:keys [state] :as order'}]
                       (do
                         (case state
                           "cart"
                           (js/setTimeout #(get-order-status order'
                                                             freeinstall-domain?)
                                          3000)

                           "submitted"
                           (if freeinstall-domain?
                             (history/enqueue-navigate events/navigate-adventure-checkout-wait)
                             (history/enqueue-navigate events/navigate-order-complete
                                                       {:query-params {:number number}})))

                         (messages/handle-message events/api-success-get-order
                                                  order'))))))

(defmethod effects/perform-effects events/navigate-checkout-processing
  [dispatch event args _ app-state]
  #?(:cljs
     (let [order                                (get-in app-state keypaths/order)
           {:keys [state number cart-payments]} order
           freeinstall-domain?                  (= "freeinstall"
                                                   (get-in app-state keypaths/store-slug))]
       (cond
         (seq (:quadpay cart-payments))
         (get-order-status order freeinstall-domain?)

         (= "cart" state)
         (place-order app-state)

         (= "submitted" state)
         (if freeinstall-domain?
           (history/enqueue-navigate events/navigate-adventure-checkout-wait)
           (history/enqueue-navigate events/navigate-order-complete
                                     {:query-params {:number number}}))))))

