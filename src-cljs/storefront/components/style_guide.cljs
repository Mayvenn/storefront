(ns storefront.components.style-guide
  (:require [storefront.component :as component]
            [clojure.string :as string]))

(defn- header [name]
  [:h2.h3.mb3.underline [:a {:name (string/lower-case name)} name]])

(defn- section-link [name]
  [:a.h5 {:href (str "#" (string/lower-case name))} name])

(defn component [data owner opts]
  (component/create
   [:div.col-12.bg-pure-white.clearfix
    [:nav.col.col-2
     [:div.border-bottom.border-light-silver.p1
      [:div.img-logo.bg-no-repeat.bg-center.bg-contain {:style {:height "35px"}}]]
     [:ul.list-reset.py2.col-6.mx-auto
      [:li [:h2.h5.mb1 "Style"]
       [:ul.list-reset.ml1
        [:li (section-link "Typography")]]]]]

    [:div.col.col-10.px3.py3.border-left.border-light-silver
     [:section
      (header "Typography")

      [:div.flex.flex-wrap
       [:div.col-2.h2 ".h2"]
       [:div.col-4.h2 "3.3rem"]
       [:div.col-6.h2 "40px or 53px"]
       [:div.col-2.h2.light ".h2.thin"]
       [:div.col-10.h2.light.mb2.gray " for subtitles of .h2"]

       [:div.col-2.h2 ".h2"]
       [:div.col-4.h2 "2rem"]
       [:div.col-6.h2 "24px or 32px"]
       [:div.col-2.h3.light ".h3.thin"]
       [:div.col-10.h3.light.mb2.gray " for subtitles of .h2"]

       [:div.col-2.h3 ".h3"]
       [:div.col-4.h3 "1.5rem"]
       [:div.col-6.h3.mb2 "18px or 24px"]

       [:div.col-2.h4 ".h4"]
       [:div.col-4.h4 "1.2rem"]
       [:div.col-6.h4.mb2 "14px or 19px"]

       [:div.col-2.h5 ".h5"]
       [:div.col-4.h5 "1rem"]
       [:div.col-6.h5.mb2 "12px or 16px"]

       [:div.col-2.p "p"]
       [:div.col-4.p "1rem"]
       [:div.col-6.p.mb2 "12px or 16px"]

       [:div.col-2.h6 ".h6"]
       [:div.col-4.h6 ".75rem"]
       [:div.col-6.h6.mb2 "9px"]]

      [:div.flex.flex-wrap
       [:div.col-2.f2 ".f2"]
       [:div.col-10.f2 "40px or 53px"]
       [:div.col-2.f2.light ".f2.thin"]
       [:div.col-10.f2.light.mb2.gray " for subtitles of .f2"]

       [:div.col-2.f2 ".f2"]
       [:div.col-10.f2 "24px or 32px"]
       [:div.col-2.f3.light ".f3.thin"]
       [:div.col-10.f3.light.mb2.gray " for subtitles of .f2"]

       [:div.col-2.f3 ".f3"]
       [:div.col-10.f3.mb2 "18px or 24px"]

       [:div.col-2.f4 ".f4"]
       [:div.col-10.f4.mb2 "14px or 19px"]

       [:div.col-2.f5 ".f5"]
       [:div.col-10.f5.mb2 "12px or 16px"]

       [:div.col-2.f6 ".f6"]
       [:div.col-10.f6.mb2 "9px"]]]]]))

(defn query [data]
  {})

(defn built-component [data opts]
  (component/build component (query data) opts))
