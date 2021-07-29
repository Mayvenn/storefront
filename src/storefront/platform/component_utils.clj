(ns storefront.platform.component-utils
  (:require [storefront.routes :as routes]))

(defn route-to
  ([navigation-event args _nav-item-stack]
   {:href (routes/path-for navigation-event args)})
  ([navigation-event args]
   (route-to navigation-event args nil))
  ([navigation-event]
   (route-to navigation-event nil nil)))

(defn route-back-or-to [_back navigation-event & [navigation-args]]
  (route-to navigation-event navigation-args))

(defn noop-callback [_e])

(defn fake-href [_event & [_args]]
  {:href "#"})

(defn send-event-callback [_event & [_args]]
  noop-callback)

(defn stop-propagation [_e] false)

(defn toggle-checkbox [_keypath value]
  (if-let [checked-val (when value "checked")]
    {:checked checked-val
     :value   checked-val}
    {}))

(defn requesting?
  ([_data _request-key] false)
  ([_data _request-search _request-key] false))

(defn select-all-text [_e]
  nil)

(defn scroll-href [anchor-id]
  {:href (str "#" anchor-id)})
