(ns storefront.components.utils
  (:require [storefront.routes :as routes]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.messages :refer [send]]))

(defn noop-callback [e] (.preventDefault e))

(defn send-event-callback [app-state event & [args]]
  (fn [e]
    (.preventDefault e)
    (send app-state event args)
    nil))

(defn route-to [app-state navigation-event & [args]]
  {:href (routes/path-for @app-state navigation-event args)
   :on-click
   (fn [e]
     (.preventDefault e)
     (routes/enqueue-navigate @app-state navigation-event args))})

(defn change-text [app-state owner keypath]
  {:value (get-in app-state keypath)
   :on-change
   (fn [e]
     (send app-state
           events/control-change-state {:keypath keypath
                                        :value (.. e -target -value)}))})

(defn change-checkbox [app-state keypath]
  (let [checked-val (when (get-in app-state keypath) "checked")]
    {:checked checked-val
     :value checked-val
     :on-change
     (fn [e]
       (send app-state
             events/control-change-state {:keypath keypath
                                          :value (.. e -target -checked)}))}))

(defn change-radio [app-state keypath value]
  (let [keypath-val (get-in app-state keypath)
        checked-val (when (= keypath-val (name value)) "checked")]
    {:checked checked-val
     :on-change
     (fn [e]
       (send app-state
             events/control-change-state {:keypath keypath
                                          :value value}))}))
(defn change-file [app-state event]
  {:on-change (fn [e]
                (send app-state event {:file (-> (.. e -target -files)
                                                 array-seq
                                                 first)}))})

(defn link-with-selected [data event label]
  (let [navigation-state (get-in data keypaths/navigation-event)
        selected (if (= navigation-state event) {:class "selected"} {})]
    [:a (merge selected (route-to data event)) label]))
