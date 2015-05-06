(ns storefront.components.top-level
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.state :as state]
            [storefront.events :as events]
            [cljs.core.async :refer [put!]]))

(defn enqueue-control-event [event-ch event & [args]]
  (fn [e]
    (.preventDefault e)
    (put! event-ch [event args])))

(defn top-level-component [data owner]
  (om/component
   (html
    [:div
     [:a {:on-click (enqueue-control-event (get-in data state/event-ch-path) events/navigate-another) :href "#"} "hello there"]
     (condp = (get-in data state/navigation-point-path)
       :home [:h1 "h1"]
       :another [:h2 "h2"])])))
