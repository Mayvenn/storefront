(ns storefront.components.top-level
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.state :as state]
            [storefront.events :as events]
            [storefront.routes :as routes]
            [cljs.core.async :refer [put!]]))

(defn enqueue-navigate [app-state navigation-event & [args]]
  (fn [e]
    (.preventDefault e)
    (apply routes/enqueue-navigate @app-state navigation-event (or args []))))

(defn top-level-component [data owner]
  (om/component
   (html
    [:div
     [:a {:on-click (enqueue-navigate data events/navigate-another) :href "#"} "hello there"]
     (condp = (get-in data state/navigation-event-path)
       events/navigate-home [:h1 "h1"]
       events/navigate-another [:h2 "h2"])])))
