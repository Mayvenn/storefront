(ns storefront.platform.component-utils
  (:require [storefront.app-routes :as app-routes]))

(defn route-to [navigation-event & [args]]
  {:href (app-routes/path-for navigation-event args)})

(defn noop-callback [e])
(defn change-text
  ;; new style
  ([keypath value])
  ;; old style
  ;; TODO: remove this signature when the last of the old forms are gone
  ([app-state owner keypath]))

(defn fake-href [event & [args]]
  {:href "#"})

(defn send-event-callback [event & [args]]
  noop-callback)

(defn toggle-checkbox [keypath value]
  (let [checked-val (when value "checked")]
    {:checked checked-val
     :value checked-val}))

(defn navigate-community []
  {:href "#"})

(defn expand-menu-callback [keypath]
  noop-callback)

(defn collapse-menus-callback [menus]
  noop-callback)

(defn requesting?
  ([data request-key] false)
  ([data request-search request-key] false))
