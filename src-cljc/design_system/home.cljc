(ns design-system.home
  (:require [design-system.adventure :as adventure]
            [design-system.classic :as classic]
            [design-system.ui :as common]
            [storefront.component :as component :refer [defcomponent]]
            #?@(:cljs
                [[storefront.components.tabs :as tabs]
                 [storefront.loader :as loader]])
            [storefront.components.ui :as ui]
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
  (let [{:keys [viewport-width file-width format quality]} (get-in data [:design-system :compression])
        focused?                                           (get-in data keypaths/ui-focus)]
    [:div
     (header "Compression")
     [:div.clearfix.mxn2
      [:div.col.p2
       [:div.mb2 (subheader "Viewport")]
       (ui/text-field
        {:type    "number"
         :label   "Width"
         :keypath [:design-system :compression :viewport-width]
         :focused focused?
         :value   viewport-width})]
      [:div.col.p2
       [:div.mb2 (subheader "File")]
       (ui/select-field {:id      "format"
                         :keypath [:design-system :compression :format]
                         :focused focused?
                         :label   "format"
                         :options [["as uploaded" ""]
                                   ["auto" "auto"]
                                   ["jpeg" "jpeg"]
                                   ["webp" "webp"]]
                         :value   format})
       (ui/select-field {:id      "quality"
                         :keypath [:design-system :compression :quality]
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
         :keypath [:design-system :compression :file-width]
         :focused focused?
         :value   file-width})]
      (for [file ["//ucarecdn.com/927f7594-e766-4985-98fa-3bc80e340947/"
                  "//ucarecdn.com/b307f889-6402-4ff2-801a-7cba0f43e8cf/"]]
        [:div.col {:key   file
                   :style (when (seq viewport-width) {:width (str viewport-width "px")})}
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
      [:li (section-link "Typography" events/navigate-design-system)]
      [:li (section-link "Color" events/navigate-design-system-color)]]]
    [:li [:h2.h5.my1 "Layout"]
     [:ul.list-reset.ml1
      [:li (section-link "Spacing" events/navigate-design-system-spacing)]]]
    [:li [:h2.h5.my1 "Components"]
     [:ul.list-reset.ml1
      [:li (section-link "Buttons" events/navigate-design-system-buttons)]
      [:li (section-link "Form Fields" events/navigate-design-system-form-fields)]
      [:li (section-link "Navigation" events/navigate-design-system-navigation)]
      [:li (section-link "Carousels" events/navigate-design-system-carousel)]]]]])

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
    (font-weight :bold "700")]])

(declare color-swatch)
(defn ^:private buttons [data]
  (let [toggle-state (fn [keypath e]
                       (handle-message events/control-change-state
                                       {:keypath keypath
                                        :value   (.. e -target -checked)}))
        set-state    (fn [keypath value e]
                       (.preventDefault e)
                       (handle-message events/control-change-state {:keypath keypath :value value}))
        button-attrs (get-in data [:design-system :buttons])]
    [:section
     (header "Buttons")
     [:div.h6.flex.flex-column
      [:div "Backgrounds: "]
      [:a.black {:href "#" :on-click (partial set-state [:design-system :buttons-bg] "bg-white")}
       (color-swatch "white" "ffffff")]
      [:a.black {:href "#" :on-click (partial set-state [:design-system :buttons-bg] "bg-cool-gray")}
       (color-swatch "cool-gray" "eeefef")]]
     [:div.h6
      "Button States: "
      [:pre
       "{"
       [:br]
       [:label
        [:input.mr1.ml2 {:type      "checkbox"
                         :checked   (get-in data [:design-system :buttons :spinning?])
                         :on-change (partial toggle-state [:design-system :buttons :spinning?])}]
        ":spinner? "
        (if (get-in data [:design-system :buttons :spinning?]) "true" "false")]
       [:br]
       (if (get-in data [:design-system :buttons :spinning?]) "true" "false")]
      [:label
       [:input.mr1.ml2 {:type      "checkbox"
                        :checked   (get-in data [:design-system :buttons :disabled?])
                        :on-change (partial toggle-state [:design-system :buttons :disabled?])}]
       ":disabled? "
       (if (get-in data [:design-system :buttons :disabled?]) "true" "false")]
      "}"]
     [:div.h6
      "Not every button state is used everywhere (eg - spinners are only used if the action may take some time and we would like feedback. Eg - a button to an external site will not spin)"]
     [:div.clearfix.mxn1.flex.flex-wrap
      (when-let [bg (get-in data [:design-system :buttons-bg])]
        {:class (str bg)})
      [:div.col-4
       [:div.p1 (ui/button-large-primary button-attrs "primary large")]
       [:div.p1 (ui/button-large-secondary button-attrs "secondary large")]
       [:div.p1 (ui/button-large-paypal button-attrs "paypal large")]]
      [:div.col-4
       [:div.p1 (ui/button-medium-primary button-attrs "primary medium")]
       [:div.p1 (ui/button-medium-secondary button-attrs "secondary medium")]]
      [:div.col-4
       [:div.p1 (ui/button-small-primary button-attrs "primary small")]
       [:div.p1 (ui/button-small-secondary button-attrs "secondary small")]]
      [:div.col.col-12.p1 (ui/button-large-primary button-attrs
                                                   [:div "col-12 with styled "
                                                    [:span.medium.italic.underline "SPAN™"]
                                                    " and svg "
                                                    ^:inline (svg/dropdown-arrow {:class  "fill-white"
                                                                                  :width  "16px"
                                                                                  :height "16px"
                                                                                  :style  {:transform "rotate(-90deg)"}})])]
      [:div.col.col-12.p1 (ui/input-group
                           {:type          "text"
                            :wrapper-class "col-7 pl3 flex items-center bg-white"
                            :placeholder   "Text"
                            :focused       false}
                           {:ui-element ui/button-large-primary
                            :content    "Button"
                            :args       (merge button-attrs
                                               {:class "flex justify-center items-center"})})]]]))

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
                 [:div.border.border-p-color.inline-block {:class class}
                  [:div.border.border-gray.inline-block.bg-cool-gray
                   [:div
                    [:p.h6 (str "." class)]
                    [:p.h6 px]]]]]]])
        subsection (fn [text body]
                     [:div.my2
                      [:div.center.h3.light.my1 text]
                      body])]
    [:section
     (header "Spacing")
     [:div.h4.light.mb2 "Margin puts space between elements. Padding puts space within elements."]

     [:h3 "Key"]
     (subsection
      ""
      [:div.border-dashed.border-gray.inline-block.p1.center.h6
       "Margin"
       [:div.border.border-p-color.p1
        "Padding"
        [:div.border.border-gray.p1.bg-cool-gray
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
     [:p.mt1.gray "So, " [:code ".pl1"] ", " [:code ".pl2"] ", etc. exist, but are discouraged and are not show here."]

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
                  (when (#{"black" "quadpay-blue" "fb-blue" "p-color"} color-class) " white")
                  (when (string/ends-with? color-class "white") " border border-gray"))
      :style {:height "8em"}}
     [:div.mt4
      [:div color-class]
      [:div "#" hex]]]]])

(defn color-gradient-swatch [color-class light-hex dark-hex]
  [:div.col-6.col-4-on-tb.col-2-on-dt
   [:div.p1
    [:div.p4
     {:style {:background (str "linear-gradient(#" light-hex ", #" dark-hex ")")
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
    (color-swatch "p-color" "4427c1")
    (color-swatch "s-color" "6bc8ad")
    (color-swatch "cool-gray" "eeefef")]

   (subheader "Grays")
   [:div.flex.flex-wrap.mxn1.mb4
    (color-swatch "black" "000000")
    (color-swatch "gray" "cccccc")
    (color-swatch "white" "ffffff")]

   (subheader "Success dialog and error handling")
   [:div.flex.flex-wrap.mxn1.mb4
    (color-swatch "s-color" "6bc8ad")
    (color-swatch "error" "ff004f")]

   (subheader "Flourishes")
   [:div.flex.flex-wrap.mxn1.mb4
    (color-swatch "s-color" "ffc520")
    (color-swatch "pale-purple" "dedbe5")]

   (subheader "Third party")
   [:div.flex.flex-wrap.mxn1.mb4
    (color-swatch "fb-blue" "3b5998")
    (color-swatch "quadpay-blue" "1d73ec")
    (color-swatch "twitter-blue" "00aced")
    (color-swatch "sms-green" "1fcc23")]])

(defn ^:private form [data]
  [:div
   [:div.h6.mb2 "Type " [:pre.inline "wrong"] " in input field to show its failed validation state."]
   (ui/text-field-group
    {:type    "text"
     :label   "First Name"
     :id      "first-name"
     :keypath [:design-system :form :first-name]
     :focused (get-in data keypaths/ui-focus)
     :value   (get-in data [:design-system :form :first-name])
     :errors  (if (= "wrong" (get-in data [:design-system :form :first-name]))
                [{:long-message "wrong"}]
                [])}
    {:type    "text"
     :label   "Last Name"
     :id      "last-name"
     :keypath [:design-system :form :last-name]
     :focused (get-in data keypaths/ui-focus)
     :value   (get-in data [:design-system :form :last-name])
     :errors  (if (= "wrong" (get-in data [:design-system :form :last-name]))
                [{:long-message "wrong"}]
                [])})
   (ui/text-field
    {:type     "text"
     :label    "Mobile Phone"
     :keypath  [:design-system :form :phone]
     :focused  (get-in data keypaths/ui-focus)
     :value    (get-in data [:design-system :form :phone])
     :errors   (if (= "wrong" (get-in data [:design-system :form :phone]))
                 [{:long-message "wrong"}]
                 [])
     :required true})
   (ui/text-field
    {:type     "password"
     :label    "Password"
     :id       "password"
     :keypath  [:design-system :form :password]
     :focused  (get-in data keypaths/ui-focus)
     :value    (get-in data [:design-system :form :password])
     :errors   (if (= "wrong" (get-in data [:design-system :form :password]))
                 [{:long-message "Incorrect"}]
                 [])
     :hint     (get-in data [:design-system :form :password])
     :required true})
   (ui/select-field {:errors      (if (= "jacob" (get-in data [:design-system :form :besty]))
                                    [{:long-message "wrong"}]
                                    [])
                     :id          "id-is-required"
                     :keypath     [:design-system :form :besty]
                     :focused     (get-in data keypaths/ui-focus)
                     :label       "Besty"
                     :options     [["Corey" "corey"] ["Jacob" "jacob"]]
                     :placeholder "Besty"
                     :required    true
                     :value       (get-in data [:design-system :form :besty])})
   (ui/text-field
    {:type    "text"
     :label   "Always wrong textfield"
     :id      "first-name-always-wrong"
     :keypath [:design-system :form :always-wrong]
     :focused (get-in data keypaths/ui-focus)
     :value   (get-in data [:design-system :form :always-wrong])
     :errors  [{:long-message "your answer is always incorrect"}]})
   (ui/select-field {:errors      [{:long-message "your answer is always incorrect"}]
                     :id          "id-is-required"
                     :keypath     [:design-system :form :wrong-choices]
                     :focused     (get-in data keypaths/ui-focus)
                     :label       "Wrong Choices select field"
                     :options     [["Corey" "corey"] ["Jacob" "jacob"]]
                     :placeholder "Wrong Choices"
                     :required    true
                     :value       (get-in data [:design-system :form :wrong-choices])})

   (ui/input-group
    {:keypath       [:design-system :form :pill-phone]
     :id            "other-thing"
     :wrapper-class "col-8"
     :name          "phone"
     :focused       (get-in data keypaths/ui-focus)
     :placeholder   "(xxx) xxx - xxxx"
     :type          "tel"
     :value         (get-in data [:design-system :form :pill-phone])
     :errors        (if (= (get-in data [:design-system :form :pill-phone]) "wrong")
                      [{:long-message "wrong"}]
                      [])}
    {:content "Get Survey"
     :args    {:data-test ""
               :class     "col-4"}})
   (ui/input-group
    {:keypath       [:design-system :form :pill-phone-2]
     :wrapper-class "col-8"
     :name          "phone"
     :focused       (get-in data keypaths/ui-focus)
     :placeholder   "With disabled button"
     :type          "tel"
     :value         (get-in data [:design-system :form :pill-phone-2])
     :errors        (if (= (get-in data [:design-system :form :pill-phone-2]) "wrong")
                      [{:long-message "wrong"}]
                      [])}
    {:content "Get Survey"
     :args    {:data-test ""
               :class     "col-4"
               :disabled? true}})
   (ui/input-group
    {:keypath       [:design-system :form :arrow]
     :wrapper-class "col-11"
     :name          "arrow"
     :focused       (get-in data keypaths/ui-focus)
     :placeholder   "arrowed"
     :value         (get-in data [:design-system :form :arrow])
     :errors        (if (= (get-in data [:design-system :form :arrow]) "wrong")
                      [{:long-message "wrong"}]
                      [])}
    {:content (svg/forward-arrow {:width  "12px"
                                  :height "12px"})
     :args    {:data-test ""
               :class     "col-1"}})
   (ui/input-group
    {:keypath       [:design-system :form :disabled-arrow]
     :wrapper-class "col-11"
     :name          "disabled arrow"
     :id            "disabled arrow"
     :focused       (get-in data keypaths/ui-focus)
     :placeholder   "disabled arrowed"
     :value         (get-in data [:design-system :form :disabled-arrow])
     :errors        (if (= (get-in data [:design-system :form :disabled-arrow]) "wrong")
                      [{:long-message "wrong"}]
                      [])}
    {:content (svg/forward-arrow {:width  "12px"
                                  :height "12px"})
     :args    {:data-test ""
               :disabled? true
               :class     "col-1"}})
   (let [keypath [:design-system :form :text-field-large]]
     (ui/text-field-large
      {:type    "text"
       :label   "placeholder"
       :keypath keypath
       :focused (get-in data keypaths/ui-focus)
       :value   (get-in data keypath)
       :errors  (if (= "wrong" (get-in data keypath))
                  [{:long-message "wrong"}]
                  [])}))
   (let [keypath [:design-system :form :large-input-group]
         value   (get-in data keypath)]
     (ui/input-group
      {:keypath       keypath
       :wrapper-class "col-10 content-1"
       :large?        true
       :name          "disabled arrow"
       :focused       (get-in data keypaths/ui-focus)
       :placeholder   "large input with button"
       :value         (get-in data keypath)
       :errors        (if (= value "wrong")
                        [{:long-message "wrong"}]
                        [])}
      {:content "Apply"
       :args    {:data-test ""
                 :class     "border-black col-2"}}))])

(defn radio-buttons [data]
  [:div.flex
   (ui/radio-section
    (merge {:key       "toggled-button"
            :name      "items"
            :id        "item-a"
            :data-test "item-a"
            :checked   "checked"})
    "Selected")
   (ui/radio-section
    (merge {:key       "untoggled-button"
            :name      "items"
            :id        "item-b"
            :data-test "item-b"})
    "Unselected")
   (ui/radio-section
    (merge {:key       "disabled-button"
            :name      "items"
            :id        "item-c"
            :disabled  true
            :data-test "item-c"})
    "Disabled")])

(defn input-toggles [data]
  [:div.flex
   (ui/check-box
    (let [keypath [:design-system :form :checkbox :checked-box]
          value   (get-in data keypath)]
      {:label         "Checked"
       :data-test     "checked-box"
       :errors        (if-not value [{:long-message "must be checked"}] [])
       :keypath       keypath
       :value         value
       :label-classes {}
       :disabled      false}))
   (ui/check-box
    (let [keypath [:design-system :form :checkbox :unchecked-box]
          value   (get-in data keypath)]
      {:label         "Unchecked"
       :data-test     "unchecked-check-box"
       :errors        (if value [{:long-message "Must be unchecked"}] [])
       :keypath       keypath
       :value         value
       :label-classes {}
       :disabled     false}))
   (ui/check-box
    (let [keypath [:design-system :form :checkbox :disabled]
          value   (get-in data keypath)]
      {:label         "Disabled"
       :data-test     "disabled-check-box"
       :errors        []
       :keypath       keypath
       :value         value
       :label-classes {}
       :disabled      true}))])

(defn ^:private form-fields [data]
  [:section
   (header "Form Fields")
   (form data)
   (header "Input toggles")
   (input-toggles data)
   (radio-buttons data)])

(defcomponent ^:private navigation [data _ _]
  [:section
   (header "Navigation")
   [:div
    [:div.bg-cool-gray
     [:div.col-6-on-tb-dt.mx-auto
      #?(:cljs
         (component/build tabs/component
                          {:selected-tab (get-in data keypaths/navigation-event)}
                          {:opts {:tab-refs ["one" "two" "three"]
                                  :labels   ["One" "Two" "Three"]
                                  :tabs     [events/navigate-design-system-navigation-tab1
                                             events/navigate-design-system-navigation
                                             events/navigate-design-system-navigation-tab3]}}))]]]])

(defcomponent ^:private carousel [data _ _]
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
                     {})]])

(def model-img
  "http://ucarecdn.com/1a3ce0a2-d8a4-4c72-b20b-62b5ff445096/-/format/auto/-/resize/110x/")

(def swatch-img
  "http://ucarecdn.com/cf2e6d44-4e93-4792-801b-1e2aacdac408/-/format/auto/swatchnatural.png")

(defn simple-option
  [{:keys [on-click primary-label secondary-label selected? sold-out?]}]
  (let [label-style (cond
                      sold-out? "gray"
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

(defcomponent component [data owner opts]
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
         events/navigate-design-system                 typography
         events/navigate-design-system-color           colors
         events/navigate-design-system-buttons         buttons
         events/navigate-design-system-spacing         spacing
         events/navigate-design-system-form-fields     (form-fields data)
         events/navigate-design-system-navigation      (component/build navigation data opts)
         events/navigate-design-system-navigation-tab1 (component/build navigation data opts)
         events/navigate-design-system-navigation-tab3 (component/build navigation data opts)
         events/navigate-design-system-carousel        (component/build carousel data opts))]]])

(defn ^:export built-component [data opts]
  (component/build component data opts))

(defcomponent top-level
  [data owner opts]
  (built-component data opts))

(defn ^:export built-top-level
  [data opts]
  (component/build top-level data opts))

#?(:cljs (loader/set-loaded! :design-system))
