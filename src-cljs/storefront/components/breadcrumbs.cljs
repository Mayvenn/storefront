(ns storefront.components.breadcrumbs
  (:require [storefront.components.utils :as utils]
            [storefront.events :as events]))

(defn breadcrumb-link [data title [event event-args] last-element?]
  [:li {:item-scope "itemscope"
        :item-type "http://data-vocabulary.org/Breadcumb"}
   [:a
    (merge
     (utils/route-to data event event-args)
     {:item-prop "url"})
    [:span {:item-prop "title"} title]]
   (if-not last-element?
     [:span {:dangerouslySetInnerHTML {:__html " &gt; "}}])])

(defn breadcrumbs [data & navigations]
  (let [navigations (into [["Home" [events/navigate-home]]] navigations)
        last-index (dec (count navigations))]
    [:nav#breadcrumbs.sixteen.columns
     [:ul.inline
      (map-indexed
       (fn [index [title navigation-msg]]
         (breadcrumb-link data title navigation-msg (= index last-index)))
       navigations)]]))
