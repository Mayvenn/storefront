(ns storefront.components.utils
  (:require [storefront.routes :as routes]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [cljs.core.async :refer [put!]]))

(defn put-event [app-state event & [args]]
  (put! (get-in @app-state keypaths/event-ch) [event args]))

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
     (put! (get-in @app-state keypaths/event-ch)
           [control-event {arg-name (.. e -target -value)}]))})

(defn change-text [app-state keypath]
  {:on-change
   (fn [e]
     (.preventDefault e)
     (put! (get-in @app-state keypaths/event-ch)
           [events/control-change-state {:keypath keypath
                                         :value (.. e -target -value)}]))
   :value (get-in app-state keypath)})

(defn update-checkbox [app-state checked? control-event arg-name]
  (let [checked-str (when checked? "checked")]
    {:checked checked-str
     :value checked-str
     :on-change
     (fn [e]
       (.preventDefault e)
       (put! (get-in @app-state keypaths/event-ch)
             [control-event {arg-name (.. e -target -checked)}]))}))

(defn change-checkbox [app-state keypath]
  (let [checked-str (when (get-in app-state keypath) "checked")]
    {:checked checked-str
     :value checked-str
     :on-change
     (fn [e]
       (.preventDefault e)
       (put! (get-in @app-state keypaths/event-ch)
             [events/control-change-state {:keypath keypath
                                           :value (.. e -target -checked)}]))}))

(defn link-with-selected [data event label]
  (let [navigation-state (get-in data keypaths/navigation-event)
        selected (if (= navigation-keypaths event) {:class "selected"} {})]
    [:a (merge selected (route-to data event)) label]))
