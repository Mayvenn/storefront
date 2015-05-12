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

(defn breadcrumb-link [title url last-element?]
  [:li {:item-scope "itemscope"
        :item-type "http://data-vocabulary.org/Breadcumb"}
   [:a {:href url
        :item-prop "url"}
    [:span {:item-prop "title"} title]]
   (if-not last-element?
     [:span {:dangerouslySetInnerHTML {:__html " &gt; "}}])])

(defn breadcrumbs [& links]
  (let [links (into [["Home" "/"]] links)
        last-index (dec (count links))]
    [:nav#breadcrumbs.sixteen.columns
     [:ul.inline
      (map-indexed
       (fn [index [title url]]
         (breadcrumb-link title url (= index last-index)))
       links)]]))
