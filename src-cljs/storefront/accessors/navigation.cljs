(ns storefront.accessors.navigation
  (:require [storefront.keypaths :as keypaths]
            [storefront.events :as events]))

(defn shop-now-navigation-message [data]
  (let [[nav-event nav-args :as previous-navigation-message]
        (get-in data keypaths/previous-navigation-message)]
    (if (and (= nav-event events/navigate-category)
             (not= previous-navigation-message (get-in data keypaths/navigation-message)))
      previous-navigation-message
      [events/navigate-categories {}])))
