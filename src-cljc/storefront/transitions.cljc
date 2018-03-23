(ns storefront.transitions
  (:require [storefront.keypaths :as keypaths]))

(defmulti transition-state identity)

(defmethod transition-state :default
  [dispatch event args app-state]
  ;; (js/console.log "IGNORED transition" (clj->js event) (clj->js args)) ;; enable to see ignored transitions
  app-state)

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
         (assoc-in keypaths/user-total-available-store-credit (js/parseFloat total_available_store_credit)))))

(defn clear-fields [app-state & fields]
  (reduce #(assoc-in %1 %2 "") app-state fields))
