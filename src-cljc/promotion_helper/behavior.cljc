(ns promotion-helper.behavior
  (:require #?@(:cljs [[storefront.frontend-trackings :as frontend-trackings]
                       [storefront.hooks.stringer :as stringer]])
            [storefront.transitions :as t]
            [storefront.events :as e]
            [storefront.effects :as effects]
            [storefront.keypaths :as keypaths]
            [storefront.platform.messages :as messages]
            [storefront.accessors.orders :as orders]
            [promotion-helper.keypaths :as k]
            [storefront.trackings :refer [perform-track]]))

;; Promotion Helper events
(def ui-promotion-helper-closed [:ui :promotion-helper :closed])
(def ui-promotion-helper-opened [:ui :promotion-helper :opened])
(def control-promotion-helper-add-stylist-button-clicked [:control :promotion-helper :add-stylist-button-clicked])

(defmethod t/transition-state ui-promotion-helper-opened
  [_ event args app-state]
  (-> app-state
      (assoc-in k/ui-promotion-helper-opened true)))

(defmethod t/transition-state ui-promotion-helper-closed
  [_ event args app-state]
  (-> app-state
      (assoc-in k/ui-promotion-helper-opened false)))

(defmethod effects/perform-effects control-promotion-helper-add-stylist-button-clicked
  [_ _ _ _ app-state]
  (messages/handle-message e/navigate-adventure-match-stylist {}))

(comment
  (defmethod perform-track ui-promotion-helper-opened
    [_ _ _ app-state]
    #?(:cljs
       (let [order          (get-in app-state keypaths/order)
             images-catalog (get-in app-state keypaths/v2-images)
             cart-items     (->> order
                                 orders/product-and-service-items
                                 (frontend-trackings/waiter-line-items->line-item-skuer
                                  (get-in app-state keypaths/v2-skus))
                                 (mapv (partial frontend-trackings/line-item-skuer->stringer-cart-item images-catalog)))]
         (stringer/track-event "helper_opened" {:current_servicing_stylist_id (:servicing-stylist-id order)
                                                :context                      {:cart_items cart-items}
                                                :helper_name                  "promotion-helper"}))))

  (defmethod perform-track ui-promotion-helper-closed
    [_ _ _ app-state]
    #?(:cljs
       (let [order          (get-in app-state keypaths/order)
             images-catalog (get-in app-state keypaths/v2-images)
             cart-items     (->> order
                                 orders/product-and-service-items
                                 (frontend-trackings/waiter-line-items->line-item-skuer
                                  (get-in app-state keypaths/v2-skus))
                                 (mapv (partial frontend-trackings/line-item-skuer->stringer-cart-item images-catalog)))]
         (stringer/track-event "helper_closed" {:current_servicing_stylist_id (:servicing-stylist-id order)
                                                :context                      {:cart_items cart-items}
                                                :helper_name                  "promotion-helper"}))))

  (defmethod perform-track control-promotion-helper-add-stylist-button-clicked
    [_ _ _ app-state]
    #?(:cljs
       (let [order          (get-in app-state keypaths/order)
             images-catalog (get-in app-state keypaths/v2-images)
             cart-items     (->> order
                                 orders/product-and-service-items
                                 (frontend-trackings/waiter-line-items->line-item-skuer
                                  (get-in app-state keypaths/v2-skus))
                                 (mapv (partial frontend-trackings/line-item-skuer->stringer-cart-item images-catalog)))]
         (stringer/track-event "helper_add_hair_button_pressed" {:current_servicing_stylist_id (:servicing-stylist-id order)
                                                                 :context                      {:cart_items cart-items}
                                                                 :helper_name                  "promotion-helper"})))))
