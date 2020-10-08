(ns stylist-matching.selected
  "Programming note: This will probably be moved to its own domain"
  (:require #?@(:cljs [[storefront.history :as history]
                       [storefront.hooks.facebook-analytics :as facebook-analytics]
                       [storefront.hooks.stringer :as stringer]])
            adventure.keypaths
            api.orders
            [storefront.accessors.orders :as orders]
            [storefront.effects :as fx]
            [storefront.events :as e]
            storefront.keypaths
            [storefront.trackings :as trackings]
            [storefront.transitions :as t]))

(defmethod t/transition-state e/api-success-assign-servicing-stylist
  [_ _ {:keys [order servicing-stylist]} state]
  (-> state
      (assoc-in storefront.keypaths/order
                order)
      (assoc-in adventure.keypaths/adventure-servicing-stylist
                servicing-stylist)))

(defmethod fx/perform-effects e/api-success-assign-servicing-stylist
  [_ _ _ _ app-state]
  (let [current-order            (api.orders/current app-state)
        {services       "service"
         physical-items "spree"} (group-by :source (orders/product-and-service-items (:waiter/order current-order)))]
    (-> (cond
          (and (seq services)
               (seq physical-items)) e/navigate-cart
          (seq services)             e/navigate-adventure-match-success
          :else                      e/navigate-adventure-match-success-pick-service)

        #?(:clj identity
           :cljs history/enqueue-navigate))))

;; FIXME(matching) ain't this just more effects from the flow's matched event?
(defmethod trackings/perform-track e/api-success-assign-servicing-stylist
  [_ _ {:keys [stylist order result-index]} _]
  #?(:cljs (facebook-analytics/track-event "AddToCart"
                                           {:content_type "stylist"
                                            :content_ids  [(:stylist-id stylist)]
                                            :num_items    1}))
  #?(:cljs (stringer/track-event "stylist_selected"
                                 {:stylist_id     (:stylist-id stylist)
                                  :card_index     result-index
                                  :current_step   2
                                  :order_number   (:number order)
                                  :stylist_rating (:rating stylist)})))
