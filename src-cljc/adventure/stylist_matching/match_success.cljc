(ns adventure.stylist-matching.match-success
  (:require [storefront.events :as events]
            storefront.keypaths
            adventure.keypaths
            [storefront.effects :as effects]
            [storefront.transitions :as transitions]
            #?(:cljs [storefront.history :as history])
            api.orders
            [storefront.accessors.experiments :as experiments]))

(defmethod transitions/transition-state events/api-success-assign-servicing-stylist-pre-purchase
  [_ _ {:keys [order]} app-state]
  (assoc-in app-state storefront.keypaths/order order))

(defmethod transitions/transition-state events/api-success-assign-servicing-stylist-post-purchase
  [_ _ {:keys [order]} app-state]
  (assoc-in app-state storefront.keypaths/completed-order order))

(defmethod transitions/transition-state events/api-success-assign-servicing-stylist
  [_ _ {:keys [servicing-stylist]} app-state]
  (assoc-in app-state adventure.keypaths/adventure-servicing-stylist servicing-stylist))

#?(:cljs
   (defmethod effects/perform-effects events/api-success-assign-servicing-stylist-pre-purchase [_ _ _ _ app-state]
     (let [consolidated-cart?                         (experiments/consolidated-cart? app-state)
           cart-contains-a-freeinstall-eligible-item? (-> app-state
                                                          api.orders/current
                                                          :mayvenn-install/quantity-added
                                                          (> 0))]
       (if (and cart-contains-a-freeinstall-eligible-item? consolidated-cart?)
         (history/enqueue-navigate events/navigate-cart)
         (history/enqueue-navigate events/navigate-adventure-match-success-pre-purchase)))))

#?(:cljs
   (defmethod effects/perform-effects events/navigate-adventure-match-success
     [_ event {:keys [query-params]} app-state-before app-state]
     (let [adventure-choices (get-in app-state adventure.keypaths/adventure-choices)]
       (when (nil? (get-in app-state adventure.keypaths/adventure-servicing-stylist))
         (history/enqueue-redirect events/navigate-adventure-find-your-stylist)))))

#?(:cljs
   (defmethod effects/perform-effects events/api-success-assign-servicing-stylist-post-purchase [_ _ _ _ app-state]
     (history/enqueue-navigate events/navigate-adventure-match-success-post-purchase)))
