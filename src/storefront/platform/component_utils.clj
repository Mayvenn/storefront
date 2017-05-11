(ns storefront.platform.component-utils
  (:require [storefront.routes :as routes]))

(defn route-to [navigation-event & [args]]
  {:href (routes/path-for navigation-event args)})

(defn noop-callback [e])

(defn fake-href [event & [args]]
  {:href "#"})

(defn send-event-callback [event & [args]]
  noop-callback)

(defn stop-propagation [e] false)

(defn toggle-checkbox [keypath value]
  (let [checked-val (when value "checked")]
    {:checked checked-val
     :value checked-val}))

(defn expand-menu-callback [keypath]
  noop-callback)

(defn collapse-menus-callback [menus]
  noop-callback)

(defn requesting?
  ([data request-key] false)
  ([data request-search request-key] false))
