(ns storefront.effects
  (:require #?@(:cljs [[storefront.api :as api]])
            [storefront.platform.messages :as messages]
            [storefront.keypaths :as keypaths]
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
   (messages/handle-message events/redirect {:nav-message [event args]})))

(defn page-not-found
  []
  (redirect events/navigate-home)
  (messages/handle-message events/flash-later-show-failure
                           {:message "Page not found"}))

(defn need-cms-keypath?
  [app-state keypath]
  (->> keypath
       (into keypaths/cms)
       (get-in app-state)
       empty?))

(defn fetch-cms-keypath
  [app-state keypath]
  #?(:cljs
     (when (need-cms-keypath? app-state keypath)
       (api/fetch-cms-keypath keypath))))
