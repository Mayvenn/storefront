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
                      {:error-handler #(let [error-code (-> % :response :body :error-code)]
                                         (when (= error-code "ineligible-for-free-install")
                                           (messages/handle-message events/order-remove-promotion
                                                                    {:code         "freeinstall"
                                                                     :hide-success true}))
                                         (history/enqueue-navigate events/navigate-cart
                                                                   {:query-params {:error error-code}}))})))

(defn affirm-place-order [app-state affirm-token]
  #?(:cljs
     (api/update-cart-payments (get-in app-state keypaths/session-id)
                               {:order (assoc-in (get-in app-state keypaths/order)
                                                 [:cart-payments :affirm]
                                                 {:checkout-token affirm-token})}
                               #(messages/handle-message events/api-success-update-order-update-cart-payments-affirm))))

(defmethod effects/perform-effects events/api-success-update-order-update-cart-payments-affirm [_ event {:keys [order]} _ app-state]
  #?(:cljs (cookie-jar/clear-affirm (get-in app-state keypaths/cookie)))
  (place-order app-state))

(defmethod effects/perform-effects events/navigate-checkout-processing [dispatch event args _ app-state]
  #?(:cljs
     (let [order                  (get-in app-state keypaths/order)
           {:keys [affirm-token]} (cookie-jar/retrieve-affirm (get-in app-state keypaths/cookie))]
       (cond
         (= (:state order) "cart")
         (if affirm-token
           (affirm-place-order app-state affirm-token)
           (place-order app-state))

         (= (:state order) "submitted")
         (history/enqueue-navigate events/navigate-order-complete
                                   {:query-params {:number (:number order)}})))))

