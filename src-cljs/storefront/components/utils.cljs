(ns storefront.components.utils
  (:require [storefront.routes :as routes]
            [storefront.state :as state]
            [cljs.core.async :refer [put!]]))

(defn route-to [app-state navigation-event & [args]]
  {:href
   (routes/path-for @app-state navigation-event args)
   :on-click
   (fn [e]
     (.preventDefault e)
     (apply routes/enqueue-navigate @app-state navigation-event args))})

(defn enqueue-event [app-state event & [args]]
  (fn [e]
    (.preventDefault e)
    (put! (get-in @app-state state/event-ch-path) [event args])))
