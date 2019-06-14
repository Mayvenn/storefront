(ns storefront.effects
  (:require [storefront.platform.messages :as messages]
            [storefront.events :as events]))

(defmulti perform-effects
  (fn [dispatch event args prev-app-state app-state]
    dispatch))

(defmethod perform-effects :default
  [dispatch event args old-app-state app-state])

;; Utilities

(defn redirect
  ([event]
   (redirect event nil))
  ([event args]
   (redirect event args nil))
  ([event args caused-by]
   (messages/handle-message events/redirect {:nav-message [event args]
                                             :navigate/caused-by caused-by})))

(defn page-not-found
  []
  (redirect events/navigate-home)
  (messages/handle-message events/flash-later-show-failure {:message "Page not found"}))
