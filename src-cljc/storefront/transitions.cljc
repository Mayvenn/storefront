(ns storefront.transitions
  (:require [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.accessors.contentful :as contentful]))

(defmulti transition-state
  (fn [dispatch event arguments app-state]
    dispatch))

(defmethod transition-state :default
  [dispatch event args app-state]
  ;; (js/console.log "IGNORED transition" (clj->js event) (clj->js args)) ;; enable to see ignored transitions
  app-state)

(defmethod transition-state events/navigate-shop-by-look [_ event {:keys [album-keyword] :as args} app-state]
  (-> app-state
      (assoc-in keypaths/selected-album-keyword album-keyword)
      (assoc-in keypaths/selected-look-id nil)))

(defmethod transition-state events/navigate-shop-by-look-details [_ event {:keys [look-id]} app-state]
  (let [shared-cart-id      (contentful/shared-cart-id (contentful/selected-look app-state))
        current-shared-cart (get-in app-state keypaths/shared-cart-current)]
    (cond-> app-state
      :always
      (assoc-in keypaths/selected-look-id (keyword look-id))

      (not= shared-cart-id (:number current-shared-cart))
      (assoc-in keypaths/shared-cart-current nil))))

;; Utilities

(defn sign-in-user
  [app-state {:keys [email token store-slug id total-available-store-credit must-set-password]}]
  (-> app-state
      (assoc-in keypaths/user-id id)
      (assoc-in keypaths/user-email email)
      (assoc-in keypaths/user-token token)
      (assoc-in keypaths/user-must-set-password must-set-password)
      (assoc-in keypaths/user-store-slug store-slug)
      (assoc-in keypaths/checkout-as-guest false)
      #?(:cljs
         (assoc-in keypaths/user-total-available-store-credit (js/parseFloat total-available-store-credit)))))

(defn clear-fields [app-state & fields]
  (reduce #(assoc-in %1 %2 "") app-state fields))

(defn clear-field-errors
  [app-state]
  (assoc-in app-state keypaths/errors {}))

(defn clear-flash
  [app-state]
  (-> app-state
      clear-field-errors
      (assoc-in keypaths/flash-now-success nil)
      (assoc-in keypaths/flash-now-failure nil)))
