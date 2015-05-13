(ns storefront.components.breadcrumbs)

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
