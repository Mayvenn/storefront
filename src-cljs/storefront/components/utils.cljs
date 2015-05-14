(ns storefront.components.utils
  (:require [storefront.routes :as routes]
            [storefront.state :as state]
            [cljs.core.async :refer [put!]]))

(defn put-event [app-state event & [args]]
  (put! (get-in @app-state state/event-ch-path) [event args]))

(defn enqueue-event [app-state event & [args]]
  (fn [e]
    (.preventDefault e)
    (put-event app-state event args)
    nil))

(defn route-to [app-state navigation-event & [args]]
  {:href
   (routes/path-for @app-state navigation-event args)
   :on-click
   (fn [e]
     (.preventDefault e)
     (routes/enqueue-navigate @app-state navigation-event args))})

(defn update-text [app-state control-event arg-name]
  {:on-change
   (fn [e]
     (.preventDefault e)
     (put! (get-in @app-state state/event-ch-path)
           [control-event {arg-name (.. e -target -value)}]))})

(defn update-checkbox [app-state checked? control-event arg-name]
  (let [checked-str (when checked? "checked")]
    {:checked checked-str
     :value checked-str
     :on-change
     (fn [e]
       (.preventDefault e)
       (put! (get-in @app-state state/event-ch-path)
             [control-event {arg-name (.. e -target -checked)}]))}))

(defn link-with-selected [data event label]
  (let [navigation-state (get-in data state/navigation-event-path)
        selected (if (= navigation-state event) {:class "selected"} {})]
    [:a (merge selected (route-to data event)) label]))

