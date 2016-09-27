(ns storefront.components.style-guide
  (:require [storefront.component :as component]
            [storefront.components.tabs :as tabs]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [clojure.string :as string]))

(defn- header [name]
  [:h2.h3.my3.underline [:a {:name (string/lower-case name)} name]])

(defn- section-link [name navigation-event]
  [:a.h5 (utils/route-to navigation-event) name])

(def ^:private styles-menu
  [:nav.col.col-2
   [:div.border-bottom.border-dark-silver.p1
    [:div.img-logo.bg-no-repeat.bg-center.bg-contain {:style {:height "35px"}}]
    [:h1.hide "Mayvenn Styleguide"]]
   [:ul.list-reset.py2.col-8.mx-auto
    [:li [:h2.h5.my1 "Style"]
     [:ul.list-reset.ml1
      [:li (section-link "Typography" events/navigate-style-guide)]
      [:li (section-link "Color" events/navigate-style-guide-color)]]]
    [:li [:h2.h5.my1 "Layout"]
     [:ul.list-reset.ml1
      [:li (section-link "Spacing" events/navigate-style-guide-spacing)]]]
    [:li [:h2.h5.my1 "Components"]
     [:ul.list-reset.ml1
      [:li (section-link "Buttons" events/navigate-style-guide-buttons)]
      [:li (section-link "Form Fields" events/navigate-style-guide-form-fields)]
      [:li (section-link "Navigation" events/navigate-style-guide-navigation)]
      [:li (section-link "Progress" events/navigate-style-guide-progress)]]]]])

(def ^:private typography
  [:section
   (header "Typography")

   [:div.flex.flex-wrap
    [:div.col-2.h1 ".h1"]
    [:div.col-4.h1 "3.3rem"]
    [:div.col-6.h1 "40px or 53px"]
    [:div.col-2.h2.light ".h2.light"]
    [:div.col-10.h2.light.mb2.gray " for subtitles of .h1"]

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
    [:div.col-4.h6 ".875rem"]
    [:div.col-6.h6.mb2 "10.5px or 12px"]]

   [:p.h4.my3 "The rem based fonts scale to large screens, but for designs that need the same font size on all screens, use the " [:code ".fN"] " helpers:"]

   [:div.flex.flex-wrap
    [:div.col-2.f1 ".f1"]
    [:div.col-10.f1.mb2 "40px"]

    [:div.col-2.f2 ".f2"]
    [:div.col-10.f2.mb2 "24px"]

    [:div.col-2.f3 ".f3"]
    [:div.col-10.f3.mb2 "18px"]

    [:div.col-2.f4 ".f4"]
    [:div.col-10.f4.mb2 "14px"]

    [:div.col-2.f5 ".f5"]
    [:div.col-10.f5.mb2 "12px"]

    [:div.col-2.f6 ".f6"]
    [:div.col-10.f6.mb2 "10.5px"]]])

(def ^:private buttons
  [:section
   (header "Buttons")
   [:h3 "Normal"]
   [:div.flex.flex-wrap.mxn1
    [:div.col-4.p1.mb1 (ui/teal-button {} "ui/teal-button")]
    [:div.col-4.p1.mb1 (ui/navy-button {} "ui/navy-button")]
    [:div.col-4.p1.mb1 (ui/ghost-button {} "ui/ghost-button")]
    [:div.col-4.p1.mb1 (ui/teal-button {:disabled? true} "(ui/teal-button {:disabled? true})")]
    [:div.col-4.p1.mb1 (ui/aqua-button {} "ui/aqua-button")]]

   [:h3.mt4 "Large"]
   [:div.flex.flex-wrap.mxn1
    [:div.col-6.p1.mb1 (ui/large-teal-button {} "ui/large-teal-button")]
    [:div.col-6.p1.mb1 (ui/large-navy-button {} "ui/large-navy-button")]
    [:div.col-6.p1.mb1 (ui/large-teal-button {:disabled? true} "(ui/large-teal-button {:disabled? true})")]
    [:div.col-6.p1.mb1 (ui/large-aqua-button {} "ui/large-aqua-button")]
    [:div.col-6.p1.mb1 (ui/large-ghost-button {} "ui/large-ghost-button")]]])

(def ^:private increment->size
  {1 ".5rem"
   2 "1rem"
   3 "2rem"
   4 "4rem"})

(def ^:private spacing
  (let [box (fn [class px]
             [:div {:key (str class "-" px)}
              [:div.m1
               [:div.border-dashed.border-light-gray.inline-block.center
                [:div.border.border-teal.inline-block {:class class}
                 [:div.border.border-light-gray.inline-block.bg-silver
                  [:div
                   [:p.h6 (str "." class)]
                   [:p.h6 px]]]]]]])
        subsection (fn [text body]
                     [:div.my2
                      [:div.center.h3.light.my1 text]
                      body])]
    [:section
     (header "Spacing")
     [:div.h4.light.mb2.gray "Margin puts space between elements. Padding puts space within elements."]

     [:h3 "Key"]
     (subsection
      ""
      [:div.border-dashed.border-light-gray.inline-block.p1.center.h6
       "Margin"
       [:div.border.border-teal.p1
        "Padding"
        [:div.border.border-light-gray.p1.bg-silver
         "Content"]]])

     [:h3 "Margin"]
     (subsection
      "in rems"
      [:div.mxn1
       (for [direction ["" "y" "x" "t" "b" "l" "r"]]
         [:div.clearfix
          (for [increment (range 1 5)
                :let [class (str "m" direction increment)]]
            [:div.col.col-3
             (box class (increment->size increment))])])])

     (subsection
      "in pixels"
      [:div.mxn1
       (for [direction ["" "t" "b" "l" "r"]]
         [:div.clearfix
          (for [increment (range 1 7)
                :let [class (str "m" direction "p" increment)
                      text (str "." class)]]
            [:div.col.col-2
             (box class (str increment "px"))])])])

     [:h3 "Padding"]

     [:p.mt1.light-gray "Backgrounds and borders are usually symmetrical around their content. For example, buttons look best when their content is equidistant from their edges. Therefore, padding is usually symmetrical too."]
     [:p.mt1.light-gray "So, " [:code.gray ".pl1"] ", " [:code.gray ".pl2"] ", etc. exist, but are discouraged and are not show here."]

     (subsection
      "in rems"
      [:div
       [:div.mxn1
        (for [direction ["" "y" "x"]]
          [:div.clearfix
           (for [increment (range 1 5)
                 :let [class (str "p" direction increment)]]
             [:div.col.col-3
              (box class (increment->size increment))])])]])

     (subsection
      "in pixels"
      [:div.mxn1
       (for [direction ["" "y" "x"]]
         [:div.clearfix
          (for [increment (range 1 7)
                :let [class (str "p" direction "p" increment)]]
            [:div.col.col-2
             (box class (str increment "px"))])])])]))

(defn color-swatch [color-class hex]
 [:div.flex.items-center
  [:div
   [:div.circle.p4.m2.border.border-light-silver
    {:class (str "bg-" color-class)}]]
  [:div.flex
   [:div.mt2.flex-column.bold.underline.shout.dark-gray
    [:div.my1 "Class"]
    [:div.my1 "HEX"]]
   [:div.mt2.ml2.flex-column
    [:div.my1 (str "." color-class)]
    [:div.my1 "#" hex]]]])

(def ^:private colors
 [:section
  (header "Color")
  [:div.col.col-6
   [:h3 "Primary"]
   (color-swatch "teal" "40CBAC")
   (color-swatch "navy" "175674")]
  [:div.col.col-6
   [:h3 "Secondary"]
   (color-swatch "aqua" "49BBF0")
   (color-swatch "orange" "E8A50C")]
  [:h3 "Neutrals"]
  [:div.col.col-6
   (color-swatch "white" "FFFFFF")
   (color-swatch "light-silver" "F8F8F8")
   (color-swatch "silver" "EBEBEB")
   (color-swatch "dark-silver" "DADADA")]
  [:div.col.col-6
   (color-swatch "light-gray" "B4B4B4")
   (color-swatch "gray" "666666")
   (color-swatch "dark-gray" "333333")
   (color-swatch "black" "000000")]])

(defn ^:private form [data errors]
  [:div
   (ui/text-field-group
    {:type    "text"
     :label   "First Name"
     :id      (str key "-" :first-name)
     :keypath [:style-guide :form :first-name]
     :value   (get-in data [:style-guide :form :first-name])
     :errors  (:first-name errors)}
    {:type    "text"
     :label   "Last Name"
     :id      "last-name"
     :keypath [:style-guide :form :last-name]
     :value   (get-in data [:style-guide :form :last-name])
     :errors  (:last-name errors)})
   (ui/text-field
    {:type     "text"
     :label    "Mobile Phone"
     :keypath  [:style-guide :form :phone]
     :value    (get-in data [:style-guide :form :phone])
     :errors   (:phone errors)
     :required true})
   (ui/text-field
    {:type     "password"
     :label    "Password"
     :keypath  [:style-guide :form :password]
     :value    (get-in data [:style-guide :form :password])
     :errors   (:password errors)
     :hint     (get-in data [:style-guide :form :password])
     :required true})
   (ui/select-field {:errors      (:besty errors)
                     :id          "id-is-required"
                     :keypath     [:style-guide :form :besty]
                     :label       "Besty"
                     :options     [["Corey" "corey"] ["Jacob" "jacob"]]
                     :placeholder "Besty"
                     :required    true
                     :value       (get-in data [:style-guide :form :besty])})])

(defn ^:private form-fields [data]
  [:section
   (header "Form Fields")
   [:div
    [:h3.mb1 "Active"] (form data {:first-name []
                                   :last-name []
                                   :phone []
                                   :besty []})]
   [:div
    [:h3.mb1 "Errors"] (form data {:first-name [{:long-message "Wrong"}]
                                   :last-name [{:long-message "wrong"}]
                                   :phone [{:long-message "Wrong"}]
                                   :password [{:long-message "Incorrect"}]
                                   :besty [{:long-message "wrong"}]})]])

(defn ^:private navigation [data _ _]
  (component/create
   [:section
    (header "Navigation")
    [:.dark-gray
     [:div.bg-light-silver
      [:div.md-up-col-6.mx-auto
       (component/build tabs/component
                        {:selected-tab (get-in data keypaths/navigation-event)}
                        {:opts {:tab-refs ["one" "two" "three"]
                                :labels   ["One" "Two" "Three"]
                                :tabs     [events/navigate-style-guide-navigation-tab1
                                           events/navigate-style-guide-navigation
                                           events/navigate-style-guide-navigation-tab3]}})]]]]))

(defn ^:private progress [data _ _]
  (component/create
   [:section
    (header "Progress Indicator")
    [:.dark-gray
     [:div.bg-white
      [:div.md-up-col-6.mx-auto
       (ui/progress-indicator {:value 0 :maximum 100})
       (ui/progress-indicator {:value 50 :maximum 100})
       (ui/progress-indicator {:value 5 :maximum 7})
       (ui/progress-indicator {:value 100 :maximum 100})]]]]))

(defn component [data owner opts]
  (component/create
   [:div.col-12.bg-white.clearfix
    styles-menu

    [:div.col.col-10.px3.py3.border-left.border-dark-silver
     (condp = (get-in data keypaths/navigation-event)
       events/navigate-style-guide typography
       events/navigate-style-guide-color colors
       events/navigate-style-guide-buttons buttons
       events/navigate-style-guide-spacing spacing
       events/navigate-style-guide-form-fields (form-fields data)
       events/navigate-style-guide-navigation (component/build navigation data opts)
       events/navigate-style-guide-navigation-tab1 (component/build navigation data opts)
       events/navigate-style-guide-navigation-tab3 (component/build navigation data opts)
       events/navigate-style-guide-progress (component/build progress data opts))]]))

(defn built-component [data opts]
  (component/build component data opts))
