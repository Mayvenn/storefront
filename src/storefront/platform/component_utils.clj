(ns storefront.platform.component-utils
  (:require [storefront.routes :as routes]))

(defn route-to
  ([navigation-event args]
   {:href (routes/path-for navigation-event args)})
  ([navigation-event]
   (route-to navigation-event nil)))

(defn route-to-shop [navigation-event & [args]]
  ;; TODO(jeff): make it work on acceptance
  {:href (str "https://shop.mayvenn.com" (routes/path-for navigation-event args))})

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

(defn expand-menu-callback [_keypath]
  noop-callback)

(defn collapse-menus-callback [_menus]
  noop-callback)

(defn requesting?
  ([_data _request-key] false)
  ([_data _request-search _request-key] false))

(defn requesting-from-endpoint? [_data _request-key] false)

(defn select-all-text [_e]
  nil)



(defn scroll-href [anchor-id]
  {:href (str "#" anchor-id)})
