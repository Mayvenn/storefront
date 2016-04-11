(ns storefront.accessors.navigation
  (:require [storefront.keypaths :as keypaths]
            [storefront.events :as events]))

(defn shop-now-navigation-message [data]
  ;; If the last page was an individual category (bundle builder) page, return there.
  ;; Otherwise go to the categories page.
  (let [[prev-nav-event _ :as category-page] (get-in data keypaths/previous-navigation-message)
        current-page (get-in data keypaths/navigation-message)]
    (if (and (= prev-nav-event events/navigate-category)
             (not= category-page current-page))
      category-page
      [events/navigate-categories {}])))
