(ns storefront.components.style-guide
  (:require [storefront.component :as component]
            [storefront.components.tabs :as tabs]
            [storefront.components.ui :as ui]
            [storefront.loader :as loader]
            [storefront.platform.carousel :as carousel]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [clojure.string :as string]
            [storefront.components.svg :as svg]
            [storefront.components.picker.picker :as picker]))

(defn- header [name]
  [:h2.h3.py1.my3.shout.medium.border-bottom [:a {:name (string/lower-case name)} name]])

(defn subheader [& copy]
  (into [:div.shout.medium.gray] copy))

(defn- section-link [name navigation-event]
  [:a.h5 (utils/route-to navigation-event) name])

(defn compression [data]
  (let [{:keys [viewport-width file-width format quality]} (get-in data [:style-guide :compression])
        focused? (get-in data keypaths/ui-focus)]
    [:div
     (header "Compression")
     [:div.clearfix.mxn2
      [:div.col.p2
       [:div.mb2 (subheader "Viewport")]
       (ui/text-field
        {:type    "number"
         :label   "Width"
         :keypath [:style-guide :compression :viewport-width]
         :focused focused?
         :value   viewport-width})]
      [:div.col.p2
       [:div.mb2 (subheader "File")]
       (ui/select-field {:id      "format"
                         :keypath [:style-guide :compression :format]
                         :focused focused?
                         :label   "format"
                         :options [["as uploaded" ""]
                                   ["auto" "auto"]
                                   ["jpeg" "jpeg"]
                                   ["webp" "webp"]]
                         :value   format})
       (ui/select-field {:id      "quality"
                         :keypath [:style-guide :compression :quality]
                         :focused focused?
                         :label   "quality"
                         :options [["no setting" ""]
                                   ["best" "best"]
                                   ["better" "better"]
                                   ["normal" "normal"]
                                   ["lighter" "lighter"]
                                   ["lightest" "lightest"]]
                         :value   quality})
       (ui/text-field
        {:type    "number"
         :label   "Resize width"
         :keypath [:style-guide :compression :file-width]
         :focused focused?
         :value   file-width})]
      (for [file ["//ucarecdn.com/927f7594-e766-4985-98fa-3bc80e340947/"
                  "//ucarecdn.com/b307f889-6402-4ff2-801a-7cba0f43e8cf/"]]
        [:div.col {:style (when (seq viewport-width) {:width (str viewport-width "px")})}
         [:img.block.col-12 {:src (str file
                                       (when (seq format) (str "-/format/" format "/"))
                                       (when (seq quality) (str "-/quality/" quality "/"))
                                       (when (seq file-width) (str "-/resize/" file-width "x/")))}]])]]))

(def ^:private styles-menu
  [:nav.col.col-2
   [:div.border-bottom.border-gray.p1
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
      [:li (section-link "Progress" events/navigate-style-guide-progress)]
      [:li (section-link "Carousels" events/navigate-style-guide-carousel)]]]]])

(def lorem "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Etiam euismod justo ut metus blandit commodo. Quisque iaculis odio non sem suscipit porta. Donec id bibendum tellus. Proin eu malesuada massa, mattis vestibulum orci.")

(defn font-size [font-class sizes]
  [:div
   [:div.mb2
    (subheader
     [:div (str sizes "px")])]

   [:div.mb4 {:class font-class} lorem]])

(defn font-weight [font-class weight-name]
  [:div
   (subheader weight-name)

   [:div.mb4 {:class font-class} lorem]])

(def ^:private typography
  [:section
   (header "Typography")
   (subheader "Font sizes are specific to browser breakpoint."
              [:div "Current Breakpoint: "
               [:span.hide-on-tb-dt "mobile"]
               [:span.hide-on-mb.hide-on-dt "tablet"]
               [:span.hide-on-mb-tb "desktop"]])
   [:div.col-8-on-tb-dt.my3
    [:div.mb2 (subheader "font-size/line-height")]

    (font-size :h1 "28/36")
    (font-size :h2 "24/32")
    (font-size :h3 "20/28")
    (font-size :h4 "18/26")
    (font-size :h5 "16/24")
    (font-size :h6 "14/22")
    (font-size :h7 "12/20")
    (font-size :h8 "10/18")

    [:div.mb2 (subheader "font-weight")]

    (font-weight :light "300")
    (font-weight :medium "400")
    (font-weight :bold "700")]

   #_(header "Text Links")
   #_[:p [:a.navy {:href "https://developer.mozilla.org/en-US/docs/Web/HTML/Element/a"} "Learn more"]]])

(declare color-swatch)
(defn ^:private buttons [data]
  (let [toggle-state (fn [keypath e]
                       (handle-message events/control-change-state
                                       {:keypath keypath
                                        :value   (.. e -target -checked)}))
        set-state(fn [keypath value e]
                   (.preventDefault e)
                   (prn keypath value)
                   (handle-message events/control-change-state {:keypath keypath :value value}))
        button-attrs (get-in data [:style-guide :buttons])]
    [:section
     (header "Buttons")
     [:div.h6.flex.flex-column
      [:div "Backgrounds: "]
      [:a.black {:href "#" :on-click (partial set-state [:style-guide :buttons-bg] "bg-white")}
       (color-swatch "white" "ffffff")]
      [:a.black {:href "#" :on-click (partial set-state [:style-guide :buttons-bg] "bg-teal")}
       (color-swatch "teal" "40cbac")]]
     [:div.h6
      "Button States: "
      [:pre.inline
       "{"
       [:label
        [:input.mr1.ml2 {:type      "checkbox"
                         :checked   (get-in data [:style-guide :buttons :spinning?])
                         :on-change (partial toggle-state [:style-guide :buttons :spinning?])}]
        ":spinner? "
        (if (get-in data [:style-guide :buttons :spinning?]) "true" "false")]
       [:label
        [:input.mr1.ml2 {:type      "checkbox"
                         :checked   (get-in data [:style-guide :buttons :disabled?])
                         :on-change (partial toggle-state [:style-guide :buttons :disabled?])}]
        ":disabled? "
        (if (get-in data [:style-guide :buttons :disabled?]) "true" "false")]
       "}"]]
     [:div.h6.dark-gray
      "Not every button state is used everywhere (eg - spinners are only used if the action may take some time and we would like feedback. Eg - a button to an external site will not spin)"]
     [:div.clearfix.mxn1
      (when-let [bg (get-in data [:style-guide :buttons-bg])]
        {:class (str bg)})
      [:div.col.col-6.p1 (ui/teal-button button-attrs "ui/teal-button")]
      [:div.col.col-6.p1 (ui/navy-button button-attrs "ui/navy-button")]
      [:div.col.col-6.p1 (ui/aqua-button button-attrs "ui/aqua-button")]
      [:div.col.col-6.p1 (ui/dark-gray-button button-attrs "ui/dark-gray-button")]
      [:div.col.col-6.p1 (ui/facebook-button button-attrs "ui/facebook-button")]

      [:div.col.col-6.p1 (ui/ghost-button button-attrs "ui/ghost-button")]
      [:div.col.col-6.p1 (ui/navy-ghost-button button-attrs "ui/navy-ghost-button")]
      [:div.col.col-6.p1 (ui/light-ghost-button button-attrs "ui/light-ghost-button")]
      [:div.col.col-6.p1 (ui/teal-ghost-button button-attrs "ui/teal-ghost-button")]

      [:div.col.col-6.p1 (ui/underline-button button-attrs "ui/underline-button")]
      [:div.col.col-6.p1 (ui/teal-button (merge button-attrs {:height-class "py2" :title "Used for adding promo codes to cart"}) "height-class py2")]
      [:div.col.col-12.p1 (ui/teal-button {:spinning? false
                                           :disabled? false}
                                          [:div "col-12 with styled "
                                           [:span.medium.italic.underline "SPAN™"]
                                           " and svg "
                                           (svg/dropdown-arrow {:class  "stroke-white"
                                                                :width  "12px"
                                                                :height "10px"
                                                                :style  {:transform "rotate(-90deg)"}})])]
      [:div.col.col-12.p1 (ui/input-group
                           {:type          "text"
                            :wrapper-class "col-7 pl3 flex items-center bg-white circled-item"
                            :placeholder   "Text"
                            :focused       false}
                           {:ui-element ui/teal-button
                            :content    "Button"
                            :args       (merge button-attrs
                                               {:class      "flex justify-center items-center circled-item"
                                                :size-class "col-5"})})]]]))

(def ^:private increment->size
  {1 "5px"
   2 "10px"
   3 "15px"
   4 "20px"})

(def ^:private spacing
  (let [box (fn [class px]
             [:div {:key (str class "-" px)}
              [:div.m1
               [:div.border-dashed.border-gray.inline-block.center
                [:div.border.border-teal.inline-block {:class class}
                 [:div.border.border-gray.inline-block.bg-light-gray
                  [:div
                   [:p.h6 (str "." class)]
                   [:p.h6 px]]]]]]])
        subsection (fn [text body]
                     [:div.my2
                      [:div.center.h3.light.my1 text]
                      body])]
    [:section
     (header "Spacing")
     [:div.h4.light.mb2.dark-gray "Margin puts space between elements. Padding puts space within elements."]

     [:h3 "Key"]
     (subsection
      ""
      [:div.border-dashed.border-gray.inline-block.p1.center.h6
       "Margin"
       [:div.border.border-teal.p1
        "Padding"
        [:div.border.border-gray.p1.bg-light-gray
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
                :let [class (str "m" direction "p" increment)]]
            [:div.col.col-2
             (box class (str increment "px"))])])])

     [:h3 "Padding"]

     [:p.mt1.gray "Backgrounds and borders are usually symmetrical around their content. For example, buttons look best when their content is equidistant from their edges. Therefore, padding is usually symmetrical too."]
     [:p.mt1.gray "So, " [:code.dark-gray ".pl1"] ", " [:code.dark-gray ".pl2"] ", etc. exist, but are discouraged and are not show here."]

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
  [:div.col-6.col-4-on-tb.col-2-on-dt
   [:div.p1
    [:div.p4
     {:class (str "bg-" color-class
                  (when (#{"purple" "black" "quadpay-blue" "fb-blue" "dark-gray"} color-class) " white")
                  (when (string/ends-with? color-class "white") " border border-gray"))
      :style {:height "8em"}}
     [:div.mt4
      [:div color-class]
      [:div "#" hex]]]]])

(defn color-gradient-swatch [color-class light-hex dark-hex]
  [:div.col-6.col-4-on-tb.col-2-on-dt
   [:div.p1
    [:div.p4
     {
      :style {:background (str "linear-gradient(#" light-hex ", #" dark-hex ")")
              :height "8em"}}
     [:div.mt4
      [:div color-class]
      [:div "#" light-hex]
      [:div "#" dark-hex]]]]])

(def ^:private colors
 [:section
  (header "Palette")

  (subheader "Primary")
  [:div.flex.flex-wrap.mxn1.mb4
   (color-swatch "too-light-teal" "f5fcfb")
   (color-swatch "light-teal" "9fe5d5")
   (color-swatch "teal" "40cbac")]

  (subheader "Grays")
  [:div.flex.flex-wrap.mxn1.mb4
   (color-swatch "black" "000000")
   (color-swatch "dark-gray" "666666")
   (color-swatch "gray" "cccccc")
   (color-swatch "dark-silver" "dadada")
   (color-swatch "light-gray" "ebebeb")
   (color-swatch "fate-white" "f8f8f8")
   (color-swatch "white" "ffffff")]

  (subheader "Success dialog and error handling")
  [:div.flex.flex-wrap.mxn1.mb4
   (color-swatch "green" "00cc00")
   (color-swatch "red" "ff0000")]

  (subheader "Flourishes")
  [:div.flex.flex-wrap.mxn1.mb4
   (color-swatch "light-orange" "fff8e5")
   (color-swatch "orange" "ffc520")
   (color-swatch "transparent-light-teal" "9fe5d5cc")
   (color-swatch "hover-only-teal-gray" "e5eeec")
   (color-swatch "purple" "7e006d")]

  (subheader "Adventure Flow")
  [:div.flex.flex-wrap.mxn1.mb4
   (color-swatch "lavender" "af9fc4")
   (color-swatch "light-lavender" "cab6d7")
   (color-gradient-swatch "Lavender-gradient" "cdB8d9" "9a8fb4")
   (color-swatch "lavender-gradient-light" "cdB8d9")
   (color-swatch "lavender-gradient-dark" "9a8fb4")]

  (subheader "Third party")
  [:div.flex.flex-wrap.mxn1.mb4
   (color-swatch "fb-blue" "3b5998")
   (color-swatch "quadpay-blue" "1d73ec")
   (color-swatch "twitter-blue" "00aced")
   (color-swatch "sms-green" "1fcc23")]])

(defn ^:private form [data]
  [:div
   [:p.h6.mb2 "Type " [:pre.inline "wrong"] " in input field to show it's failed validation state."]
   (ui/text-field-group
    {:type    "text"
     :label   "First Name"
     :id      "first-name"
     :keypath [:style-guide :form :first-name]
     :focused (get-in data keypaths/ui-focus)
     :value   (get-in data [:style-guide :form :first-name])
     :errors  (if (= "wrong" (get-in data [:style-guide :form :first-name]))
                [{:long-message "wrong"}]
                [])}
    {:type    "text"
     :label   "Last Name"
     :id      "last-name"
     :keypath [:style-guide :form :last-name]
     :focused (get-in data keypaths/ui-focus)
     :value   (get-in data [:style-guide :form :last-name])
     :errors  (if (= "wrong" (get-in data [:style-guide :form :last-name]))
                [{:long-message "wrong"}]
                [])})
   (ui/text-field
    {:type     "text"
     :label    "Mobile Phone"
     :keypath  [:style-guide :form :phone]
     :focused  (get-in data keypaths/ui-focus)
     :value    (get-in data [:style-guide :form :phone])
     :errors   (if (= "wrong" (get-in data [:style-guide :form :phone]))
                 [{:long-message "wrong"}]
                 [])
     :required true})
   (ui/text-field
    {:type     "password"
     :label    "Password"
     :id       "password"
     :keypath  [:style-guide :form :password]
     :focused  (get-in data keypaths/ui-focus)
     :value    (get-in data [:style-guide :form :password])
     :errors   (if (= "wrong" (get-in data [:style-guide :form :password]))
                 [{:long-message "Incorrect"}]
                 [])
     :hint     (get-in data [:style-guide :form :password])
     :required true})
   (ui/select-field {:errors      (if (= "jacob" (get-in data [:style-guide :form :besty]))
                                    [{:long-message "wrong"}]
                                    [])
                     :id          "id-is-required"
                     :keypath     [:style-guide :form :besty]
                     :focused     (get-in data keypaths/ui-focus)
                     :label       "Besty"
                     :options     [["Corey" "corey"] ["Jacob" "jacob"]]
                     :placeholder "Besty"
                     :required    true
                     :value       (get-in data [:style-guide :form :besty])})
   (ui/text-field
    {:type    "text"
     :label   "Always wrong textfield"
     :id      "first-name-always-wrong"
     :keypath [:style-guide :form :always-wrong]
     :focused (get-in data keypaths/ui-focus)
     :value   (get-in data [:style-guide :form :always-wrong])
     :errors  [{:long-message "your answer is always incorrect"}]})
   (ui/select-field {:errors      [{:long-message "your answer is always incorrect"}]
                     :id          "id-is-required"
                     :keypath     [:style-guide :form :wrong-choices]
                     :focused     (get-in data keypaths/ui-focus)
                     :label       "Wrong Choices select field"
                     :options     [["Corey" "corey"] ["Jacob" "jacob"]]
                     :placeholder "Wrong Choices"
                     :required    true
                     :value       (get-in data [:style-guide :form :wrong-choices])})

   (ui/input-group
    {:keypath       [:style-guide :form :pill-phone]
     :wrapper-class "col-8 flex bg-white pl3 items-center circled-item"
     :class         ""
     :name          "phone"
     :focused       (get-in data keypaths/ui-focus)
     :placeholder   "(xxx) xxx - xxxx"
     :type          "tel"
     :value         (get-in data [:style-guide :form :pill-phone])
     :errors        (if (= (get-in data [:style-guide :form :pill-phone]) "wrong")
                      [{:long-message "wrong"}]
                      [])} 
    {:ui-element ui/teal-button
     :content    "Get Survey"
     :args       {:class          "flex justify-center medium items-center circled-item"
                  :size-class     "col-4"
                  :height-class   "py2"
                  :data-test      ""
                  :disabled-class "disabled bg-teal"}})
   (ui/input-group
    {:keypath       [:style-guide :form :pill-phone-2]
     :wrapper-class "col-8 flex bg-white pl3 items-center circled-item"
     :class         ""
     :name          "phone"
     :focused       (get-in data keypaths/ui-focus)
     :placeholder   "With disabled button"
     :type          "tel"
     :value         (get-in data [:style-guide :form :pill-phone-2])
     :errors        (if (= (get-in data [:style-guide :form :pill-phone-2]) "wrong")
                      [{:long-message "wrong"}]
                      [])} 
    {:ui-element ui/teal-button
     :content    "Get Survey"
     :args       {:class          "flex justify-center medium items-center circled-item"
                  :size-class     "col-4"
                  :height-class   "py2"
                  :data-test      ""
                  :disabled?      true}})])

(defn ^:private form-fields [data]
  [:section
   (header "Form Fields")
   (form data)])

(defn ^:private navigation [data _ _]
  (component/create
   [:section
    (header "Navigation")
    [:.dark-gray
     [:div.bg-light-gray
      [:div.col-6-on-tb-dt.mx-auto
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
      [:div.col-6-on-tb-dt.mx-auto
       (ui/progress-indicator {:value 0 :maximum 100})
       (ui/progress-indicator {:value 50 :maximum 100})
       (ui/progress-indicator {:value 5 :maximum 7})
       (ui/progress-indicator {:value 100 :maximum 100})]]]]))

(defn ^:private carousel [data _ _]
  (component/create
   [:section
    (header "Carousels")
    [:div.flex.items-center.col-12
     [:div.col-4
      (component/build carousel/component
                       {:slides [[:img.mx-auto {:src "http://lorempixel.com/200/200/cats/1"}]
                                 [:img.mx-auto {:src "http://lorempixel.com/200/200/cats/2"}]
                                 [:img.mx-auto {:src "http://lorempixel.com/200/200/cats/3"}]
                                 [:img.mx-auto {:src "http://lorempixel.com/200/200/cats/4"}]]
                        :settings {:swipe true
                                   :arrows true
                                   :dots true}}
                       {})]

     [:div.col-4
      (component/build carousel/component
                       {:slides [[:img.mx-auto {:src "http://lorempixel.com/200/200/cats/5"}]
                                 [:img.mx-auto {:src "http://lorempixel.com/200/200/cats/6"}]
                                 [:img.mx-auto {:src "http://lorempixel.com/200/200/cats/7"}]
                                 [:img.mx-auto {:src "http://lorempixel.com/200/200/cats/8"}]]
                        :settings {:swipe true
                                   :arrows true}}
                       {})]

     [:div.col-4
      (component/build carousel/component
                       {:slides [[:img.mx-auto {:src "http://lorempixel.com/200/200/cats/9"}]]
                        :settings {:swipe true
                                   :arrows false}}
                       {})]]

    [:div.mt1
     (component/build carousel/component
                      {:slides (for [i (range 12)]
                                 [:img.mx-auto {:src (str "http://lorempixel.com/200/200/animals/" i)}])
                       :settings {:swipe true
                                  :slidesToShow 3
                                  :arrows true
                                  :dots true}}
                      {})]]))

(def model-img
  "http://ucarecdn.com/1a3ce0a2-d8a4-4c72-b20b-62b5ff445096/-/format/auto/-/resize/110x/")

(def swatch-img
  "http://ucarecdn.com/cf2e6d44-4e93-4792-801b-1e2aacdac408/-/format/auto/swatchnatural.png")

(defn simple-option
  [{:keys [on-click primary-label secondary-label selected? sold-out?]}]
  (let [label-style (cond
                      sold-out? "dark-gray"
                      selected? "medium"
                      :else     nil)]
    (ui/option {:key      primary-label
                :height   "4em"
                :on-click on-click}
               (picker/simple-content-layer
                (list
                 [:div.col-2
                  (when label-style
                    {:class label-style})
                  primary-label]
                 [:div.gray.flex-auto secondary-label]))
               (cond
                 sold-out? (picker/simple-sold-out-layer "Sold Out")
                 selected? (picker/simple-selected-layer)
                 :else     nil))))

(def simple-custom-options
  [:section.flex.flex-column
   [:div.col-5
    (simple-option
     {:primary-label  "10″"
      :secondary-labe "$100"
      :selected?      true
      :sold-out?      false})]

   [:div.col-5
    (simple-option
     {:primary-label  "10″"
      :secondary-labe "$100"
      :selected?      false
      :sold-out?      false})]

   [:div.col-5
    (simple-option
     {:primary-label  "10″"
      :secondary-labe "$100"
      :selected?      false
      :sold-out?      true})]
   [:div.col-5
    (simple-option
     {:primary-label "1"
      :selected?     false})]

   [:div.col-5
    (simple-option
     {:primary-label "2"
      :selected?     true})]])

(def swatch-custom-options
  [:section.flex.flex-column
   [:div.col-5
    (picker/color-option
     {:color     {:option/name             "Natural Black"
                  :option/rectangle-swatch swatch-img}
      :model-img model-img
      :selected? true
      :sold-out? false})]

   [:div.col-5
    (picker/color-option
     {:color     {:option/name             "Natural Black"
                  :option/rectangle-swatch swatch-img}
      :model-img model-img
      :selected? false
      :sold-out? false})]

   [:div.col-5
    (picker/color-option
     {:color
      {:option/name             "Natural Black"
       :option/rectangle-swatch swatch-img}
      :model-img model-img
      :selected? false
      :sold-out? true})]])

(defn component [data owner opts]
  (component/create
   [:div
    [:div.mx3
     [:div.container
      [:div {:style {:margin "50px 0"}}
       [:h1.mb4 "Mayvenn Style Guide"]
       (subheader
        [:span.hide-on-tb-dt "mobile"]
        [:span.hide-on-mb.hide-on-dt "tablet"]
        [:span.hide-on-mb-tb "desktop"]
        " breakpoint")]

      (compression data)
      colors
      typography
      (form-fields data)
      (buttons data)

      #_simple-custom-options
      #_swatch-custom-options

      #_(condp = (get-in data keypaths/navigation-event)
          events/navigate-style-guide                 typography
          events/navigate-style-guide-color           colors
          events/navigate-style-guide-buttons         buttons
          events/navigate-style-guide-spacing         spacing
          events/navigate-style-guide-form-fields     (form-fields data)
          events/navigate-style-guide-navigation      (component/build navigation data opts)
          events/navigate-style-guide-navigation-tab1 (component/build navigation data opts)
          events/navigate-style-guide-navigation-tab3 (component/build navigation data opts)
          events/navigate-style-guide-progress        (component/build progress data opts)
          events/navigate-style-guide-carousel        (component/build carousel data opts))]]]))

(defn built-component [data opts]
  (component/build component data opts))

(loader/set-load! :style-guide)
