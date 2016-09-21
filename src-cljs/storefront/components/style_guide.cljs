(ns storefront.components.style-guide
  (:require [storefront.component :as component]
            [storefront.components.ui :as ui]
            [clojure.string :as string]))

(defn- header [name]
  [:h2.h3.my3.underline [:a {:name (string/lower-case name)} name]])

(defn- section-link [name]
  [:a.h5 {:href (str "#" (string/lower-case name))} name])

(def ^:private styles-menu
  [:nav.col.col-2
   [:div.border-bottom.border-dark-silver.p1
    [:div.img-logo.bg-no-repeat.bg-center.bg-contain {:style {:height "35px"}}]]
   [:ul.list-reset.py2.col-6.mx-auto
    [:li [:h2.h5.mb1 "Style"]
     [:ul.list-reset.ml1
      [:li (section-link "Typography")]]]
    [:li [:h2.h5.mb1 "Components"]
     [:ul.list-reset.ml1
      [:li (section-link "Buttons")]]]]])

(def ^:private typography
  [:section
   (header "Typography")

   [:div.flex.flex-wrap
    [:div.col-2.h1 ".h1"]
    [:div.col-4.h1 "3.3rem"]
    [:div.col-6.h1 "40px or 53px"]
    [:div.col-2.h2.light ".h2.light"]
    [:div.col-10.h2.light.mb2.gray " for subtitles of .h2"]

    [:div.col-2.h2 ".h2"]
    [:div.col-4.h2 "2rem"]
    [:div.col-6.h2 "24px or 32px"]
    [:div.col-2.h3.light ".h3.light"]
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
    [:div.col-6.h6.mb2 "9px or 12px"]]

   [:div.flex.flex-wrap
    [:div.col-2.f1 ".f1"]
    [:div.col-10.f1 "40px"]
    [:div.col-2.f2.light ".f2.light"]
    [:div.col-10.f2.light.mb2.gray " for subtitles of .f2"]

    [:div.col-2.f2 ".f2"]
    [:div.col-10.f2 "24px"]
    [:div.col-2.f3.light ".f3.light"]
    [:div.col-10.f3.light.mb2.gray " for subtitles of .f2"]

    [:div.col-2.f3 ".f3"]
    [:div.col-10.f3.mb2 "18px"]

    [:div.col-2.f4 ".f4"]
    [:div.col-10.f4.mb2 "14px"]

    [:div.col-2.f5 ".f5"]
    [:div.col-10.f5.mb2 "12px"]

    [:div.col-2.f6 ".f6"]
    [:div.col-10.f6.mb2 "9px"]]])

(def ^:private buttons
  [:section
   (header "Buttons")
   ;; Normal Buttons, Active States
   [:div.flex.flex-wrap.mxn1
    [:div.col-4.p1
     [:div.mb1.bold "Primary"]
     [:div.mb1 (ui/teal-button {} "ui/teal-button")]]
    [:div.col-4.p1
     [:div.mb1.bold "Secondary"]
     [:div.mb1 (ui/navy-button {} "ui/navy-button")]]
    [:div.col-4.p1
     [:div.mb1.bold "Ghost"]
     [:div.mb1 (ui/ghost-button {} "ui/ghost-button")]]
    [:div.col-4.p1
     [:div.mb1.bold "Disabled"]
     [:div.mb1 (ui/teal-button {:disabled? true} "(ui/teal-button {:disabled? true})")]]
    [:div.col-4.p1
     [:div.mb1.bold "Branded"]
     [:div.mb1 (ui/aqua-button {} "ui/aqua-button")]]]

   ;; Large Buttons, Active States
   [:div.flex.flex-wrap.mxn1
    [:div.col-6.p1
     [:div.mb1.bold "Primary (Large)"]
     [:div.mb1 (ui/large-teal-button {} "ui/large-teal-button")]]
    [:div.col-6.p1
     [:div.mb1.bold "Secondary (Large)"]
     [:div.mb1 (ui/large-navy-button {} "ui/large-navy-button")]]
    [:div.col-6.p1
     [:div.mb1.bold "Disabled (Large)"]
     [:div.mb1 (ui/large-teal-button {:disabled? true} "(ui/large-teal-button {:disabled? true})")]]
    [:div.col-6.p1
     [:div.mb1.bold "Branded (Large)"]
     [:div.mb1 (ui/large-aqua-button {} "ui/large-aqua-button")]]
    [:div.col-6.p1
     [:div.mb1.bold "Ghost (Large)"]
     [:div.mb1 (ui/large-ghost-button {} "ui/large-ghost-button")]]]])

(defn component [data owner opts]
  (component/create
   [:div.col-12.bg-white.clearfix
    styles-menu

    [:div.col.col-10.px3.py3.border-left.border-dark-silver
     typography
     buttons]]))

(defn query [data]
  {})

(defn built-component [data opts]
  (component/build component (query data) opts))
