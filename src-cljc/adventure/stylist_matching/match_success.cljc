(ns adventure.stylist-matching.match-success
  (:require #?@(:cljs [api.orders
                       [storefront.history :as history]])
            [storefront.effects :as effects]
            [storefront.events :as events]
            storefront.keypaths
            adventure.keypaths
            [storefront.transitions :as transitions]))

(defmethod transitions/transition-state events/api-success-assign-servicing-stylist-pre-purchase
  [_ _ {:keys [order]} app-state]
  (assoc-in app-state storefront.keypaths/order order))

(defmethod transitions/transition-state events/api-success-assign-servicing-stylist-post-purchase
  [_ _ {:keys [order]} app-state]
  (assoc-in app-state storefront.keypaths/completed-order order))

(defmethod transitions/transition-state events/api-success-assign-servicing-stylist
  [_ _ {:keys [servicing-stylist]} app-state]
  (assoc-in app-state adventure.keypaths/adventure-servicing-stylist servicing-stylist))

(defmethod effects/perform-effects events/api-success-assign-servicing-stylist-pre-purchase [_ _ _ _ app-state]
  #?(:cljs
     (let [cart-contains-a-freeinstall-eligible-item? (some-> app-state
                                                              api.orders/current
                                                              :mayvenn-install/quantity-added
                                                              (> 0))]
       (if cart-contains-a-freeinstall-eligible-item?
         (history/enqueue-navigate events/navigate-cart)
         (history/enqueue-navigate events/navigate-adventure-match-success-pre-purchase)))))

(defmethod effects/perform-effects events/navigate-adventure-match-success-pre-purchase
  [_ _ _ _ app-state]
  #?(:cljs
     (when (nil? (get-in app-state adventure.keypaths/adventure-servicing-stylist))
       (history/enqueue-redirect events/navigate-adventure-find-your-stylist))))

(defmethod effects/perform-effects events/api-success-assign-servicing-stylist-post-purchase [_ _ _ _ app-state]
  #?(:cljs (history/enqueue-navigate events/navigate-adventure-match-success-post-purchase)))
