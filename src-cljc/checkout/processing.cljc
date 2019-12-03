(ns checkout.processing
  (:require [storefront.components.ui :as ui]
            #?@(:cljs [[spice.core :as spice]
                       [storefront.browser.cookie-jar :as cookie-jar]
                       [storefront.api :as api]
                       [storefront.history :as history]])
            [storefront.component :as component :refer [defcomponent]]
            [storefront.keypaths :as keypaths]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.platform.messages :as messages]
            [storefront.accessors.stylists :as stylists]))

(defcomponent component
  [{:keys []} _ _]
  (ui/narrow-container
   [:div.py6.h2
    [:div.py4 (ui/large-spinner {:style {:height "6em"}})]
    [:h2.center "Processing your order..."]]))

(defn query [data] {})

(defn ^:export built-component [data opts]
  (component/build component (query data) opts))

(defn place-order [app-state]
  #?(:cljs
     (api/place-order (get-in app-state keypaths/session-id)
                      (get-in app-state keypaths/order)
                      (cookie-jar/retrieve-utm-params (get-in app-state keypaths/cookie))
                      (stylists/retrieve-parsed-affiliate-id app-state)
                      {:error-handler #(let [{:keys [error-code error-message]} (-> % :response :body)]
                                         (when (= error-code "ineligible-for-free-install")
                                           (messages/handle-message events/order-remove-freeinstall-line-item))
                                         (if (= error-code "quadpay-failed-to-capture-payment")
                                           (do
                                             (history/enqueue-navigate events/navigate-checkout-payment
                                                                       {:query-params {:error error-code}})
                                             (messages/handle-later events/flash-show-failure {:message error-message}))
                                           (history/enqueue-navigate events/navigate-cart
                                                                     {:query-params {:error error-code}})))})))

(defn get-order-status
  [{:keys [number token]} freeinstall-domain? confirm-order-fn times-attempted]
  #?(:cljs
     (api/poll-order number token
                     (fn [{:keys [state] :as order'}]
                       (do
                         (if (< times-attempted 5)
                           (case state
                             "cart"
                             (js/setTimeout #(get-order-status order' freeinstall-domain? confirm-order-fn (inc times-attempted))
                                            3000)

                             "submitted"
                             (messages/handle-message events/api-success-update-order-place-order {:order order'}))
                           (confirm-order-fn)))))))

(defn quadpay-confirm-order [app-state freeinstall-domain?]
  ;; tell waiter to hurry up, otherwise just poll for status (webhook should update us)
  #?(:cljs
     (let [order (get-in app-state keypaths/order)]
       (api/confirm-order-was-placed
        (get-in app-state keypaths/session-id)
        order
        (cookie-jar/retrieve-utm-params (get-in app-state keypaths/cookie))
        (fn success-handler [& _]
          (get-order-status order freeinstall-domain? (partial quadpay-confirm-order app-state freeinstall-domain?) 0))
        (fn failure-handler [response]
          (let [response-body (get-in response [:response :body])]
            (if (api/waiter-style? response-body)
              (do
                (history/enqueue-navigate events/navigate-cart {:query-params {:error (:error-code response-body)}})
                (messages/handle-later events/flash-show-failure {:message (:error-message response-body)}))

              (get-order-status order freeinstall-domain? (partial quadpay-confirm-order app-state freeinstall-domain?) 0))))))))

(defmethod effects/perform-effects events/navigate-checkout-processing
  [dispatch event args _ app-state]
  #?(:cljs
     (let [order                                (get-in app-state keypaths/order)
           {:keys [state number cart-payments]} order

           ;; TODO: Is freeinstall-domain? actually used, or just threaded through?
           freeinstall-domain?                  (= "freeinstall"
                                                   (get-in app-state keypaths/store-slug))]
       (cond
         (seq (:quadpay cart-payments))
         (quadpay-confirm-order app-state freeinstall-domain?)

         (= "cart" state)
         (place-order app-state)

         (= "submitted" state)
         (messages/handle-message events/api-success-update-order-place-order {:order order})))))
