(ns storefront.components.style-guide
  (:require [storefront.component :as component]))

(defn component [data owner opts]
  (component/create
   [:div.col-12.bg-pure-white.clearfix
    [:nav.col.col-2
     [:h1.border-bottom.border-silver.center "Mayvenn"]
     [:ul.list-reset.py2.col-6.mx-auto
      [:li [:h2.h5.mb1 "Style"]
       [:ul.list-reset.ml1
        [:li [:a.h5 {:href "#typography"} "Typography"]]]]]]

    [:div.col.col-10.px3.py3.border-left.border-silver
     [:section
      [:h1.mb1 [:a {:name "typography"} "Typography"]]

      [:div.flex.flex-wrap

       [:div.col-4.h1 ".h1"]
       [:div.col-4.h1 "3.3rem"]
       [:div.col-4.h1 "40px or 53px"]
       [:div.col-4.h1sub ".h1sub"]
       [:div.col-4.h1sub "2.3rem"]
       [:div.col-4.h1sub.mb2 "27px or 37px"]

       [:div.col-4.h2 ".h2"]
       [:div.col-4.h2 "2rem"]
       [:div.col-4.h2 "24px or 32px"]
       [:div.col-4.h2sub ".h2sub"]
       [:div.col-4.h2sub "1.5rem"]
       [:div.col-4.h2sub.mb2 "18px or 24px"]

       [:div.col-4.h3 ".h3"]
       [:div.col-4.h3 "1.5rem"]
       [:div.col-4.h3.mb2 "18px or 24px"]

       [:div.col-4.h4 ".h4"]
       [:div.col-4.h4 "1.2rem"]
       [:div.col-4.h4.mb2 "14px or 19px"]

       [:div.col-4.h5 ".h5"]
       [:div.col-4.h5 "1rem"]
       [:div.col-4.h5.mb2 "12px or 16px"]

       [:div.col-4.p "p"]
       [:div.col-4.p "1rem"]
       [:div.col-4.p.mb2 "12px or 16px"]

       [:div.col-4.sub ".sub"]
       [:div.col-4.sub ".75rem"]
       [:div.col-4.sub.mb2 "9px"]
       ]]]]))

(defn query [data]
  {})

(defn built-component [data opts]
  (component/build component (query data) opts))
