(ns storefront.components.utils
  (:require [storefront.routes :as routes]))

(defn route-to [app-state navigation-event & [args]]
  {:href
   (routes/path-for @app-state navigation-event args)
   :on-click
   (fn [e]
     (.preventDefault e)
     (apply routes/enqueue-navigate @app-state navigation-event args))})
