(ns storefront.accessors.navigation
  (:require [storefront.keypaths :as keypaths]
            [storefront.events :as events]))

(defn shop-now-navigation-message [data]
  ;; If the last page was an individual product page, return there.
  ;; Otherwise go to the categories page.
  (let [[prev-nav-event _ :as product-page] (get-in data keypaths/previous-navigation-message)
        current-page (get-in data keypaths/navigation-message)]
    (if (and (= prev-nav-event events/navigate-category)
             (not= product-page current-page))
      product-page
      [events/navigate-categories {}])))
