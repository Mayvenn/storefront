(ns storefront.components.utils
  (:require [storefront.app-routes :as app-routes]))

(defn route-to [navigation-event & [args]]
  {:href (app-routes/path-for navigation-event args)})
