(ns checkout.cart
  (:require
   #?@(:cljs [[storefront.api :as api]
              [storefront.history :as history]])
   [storefront.effects :as effects]
   [storefront.events :as events]
   [storefront.keypaths :as keypaths]
   [storefront.platform.messages :as messages]
   [storefront.transitions :as transitions]))

(defmethod effects/perform-effects events/control-pick-stylist-button
  [_ _ _ _ _]
  #?(:cljs (history/enqueue-navigate events/navigate-shopping-quiz-unified-freeinstall-find-your-stylist)))

(defmethod effects/perform-effects events/control-change-stylist
  [_ _ _ _ _]
  #?(:cljs (history/enqueue-navigate events/navigate-shopping-quiz-unified-freeinstall-find-your-stylist)))

(defmethod effects/perform-effects events/control-remove-stylist
  [_ _ {:keys [stylist-id]} _ app-state]
  #?(:cljs (let [order (get-in app-state keypaths/order)]
             (api/remove-servicing-stylist stylist-id
                                           (:number order)
                                           (:token order)))))

(defmethod effects/perform-effects events/api-success-update-order-remove-servicing-stylist
  [_ _ _ _ app-state]
  #?(:cljs (let [order (get-in app-state keypaths/order)]
             (api/remove-appointment-time-slot {:number (:number order)
                                                :token  (:token order)}))))

(defmethod transitions/transition-state events/control-toggle-promo-code-entry
  [_ _ _ app-state]
  (update-in app-state keypaths/promo-code-entry-open? not))

(defmethod effects/perform-effects events/control-cart-update-coupon
  [_ _ _ _ app-state]
  #?(:cljs
     (let [coupon-code (get-in app-state keypaths/cart-coupon-code)]
       (when-not (empty? coupon-code)
         (api/add-promotion-code {:session-id     (get-in app-state keypaths/session-id)
                                  :number         (get-in app-state keypaths/order-number)
                                  :token          (get-in app-state keypaths/order-token)
                                  :promo-code     coupon-code
                                  :allow-dormant? false})))))

(defmethod effects/perform-effects events/control-cart-share-show
  [_ _ _ _ app-state]
  #?(:cljs
     (api/create-shared-cart (get-in app-state keypaths/session-id)
                             (get-in app-state keypaths/order-number)
                             (get-in app-state keypaths/order-token))))

(defmethod effects/perform-effects events/control-cart-remove
  [_ event variant-id _ app-state]
  #?(:cljs
     (api/delete-line-item (get-in app-state keypaths/session-id) (get-in app-state keypaths/order) variant-id)))

(defmethod effects/perform-effects events/control-cart-line-item-inc
  [_ event {:keys [variant]} _ app-state]
  #?(:cljs
     (let [sku      (get (get-in app-state keypaths/v2-skus) (:sku variant))
           order    (get-in app-state keypaths/order)
           quantity 1]
       (api/add-sku-to-bag (get-in app-state keypaths/session-id)
                           {:sku                sku
                            :token              (:token order)
                            :number             (:number order)
                            :quantity           quantity
                            :heat-feature-flags (get-in app-state keypaths/features)}
                           #(messages/handle-message events/api-success-add-sku-to-bag
                                                     {:order    %
                                                      :quantity quantity
                                                      :sku      sku})))))

(defmethod effects/perform-effects events/control-cart-line-item-dec
  [_ event {:keys [variant]} _ app-state]
  #?(:cljs
     (let [order (get-in app-state keypaths/order)]
       (api/remove-line-item (get-in app-state keypaths/session-id)
                             {:number     (:number order)
                              :token      (:token order)
                              :variant-id (:id variant)
                              :sku-code   (:sku variant)}
                             #(messages/handle-message events/api-success-decrease-quantity {:order %})))))
