(ns storefront.platform.component-utils
  (:require [storefront.events :as events]
            [storefront.hooks.fastpass :as fastpass]
            [storefront.keypaths :as keypaths]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.routes :as routes]
            [storefront.app-routes :as app-routes]
            [storefront.utils.query :as query]))

(defn position [pred coll]
  (first (keep-indexed #(when (pred %2) %1)
                       coll)))

(defn noop-callback [e] (.preventDefault e))

(defn send-event-callback [event & [args]]
  (fn [e]
    (.preventDefault e)
    (handle-message event args)
    nil))

(defn expand-menu-callback [keypath]
  (send-event-callback events/control-menu-expand {:keypath keypath}))

(defn collapse-menus-callback [menus]
  (send-event-callback events/control-menu-collapse-all {:menus menus}))

(defn route-to [navigation-event & [args]]
  {:href (app-routes/path-for navigation-event args)
   :on-click
   (fn [e]
     (.preventDefault e)
     (handle-message events/control-menu-collapse-all)
     (routes/enqueue-navigate navigation-event args))})

(defn requesting?
  ([data request-key] (requesting? data :request-key request-key))
  ([data request-search request-key]
   (query/get
    {request-search request-key}
    (get-in data keypaths/api-requests))))

(defn suppress-return-key [e]
  (when (= 13 (.-keyCode e))
    (.preventDefault e)))

(defn fake-href [event & [args]]
  {:href "#"
   :on-click (send-event-callback event args)})

(defn toggle-checkbox [keypath value]
  (let [checked-val (if value "checked" "")]
    {:checked checked-val
     :value checked-val
     :on-change
     (fn [e]
       (handle-message events/control-change-state
                       {:keypath keypath
                        :value (.. e -target -checked)}))}))

(defn change-file [event]
  {:on-change (fn [e]
                (handle-message event
                                {:file (-> (.. e -target -files)
                                           array-seq
                                           first)}))})

(defn navigate-community
  "Can't be a def because (fastpass/community-url) is impure."
  []
  {:href (or (fastpass/community-url) "#")
   :on-click (send-event-callback events/external-redirect-community)})

(defn select-all-text [e]
  (let [el (.-target e)
        length (.-length (.-value el))]
    (cond
      (.-createTextRange el)
      (doto (.createTextRange el)
        (.collapse true)
        (.moveStart "character" 0)
        (.moveEnd "character" length)
        (.select))

      (.-setSelectionRange el)
      (.setSelectionRange el 0 length)

      :else
      (do
        (set! (.-selectionStart el) 0)
        (set! (.-selectionEnd el) length)))))
