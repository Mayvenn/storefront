(ns storefront.components.ui
  (:require [storefront.components.utils :as utils]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.messages :refer [handle-message]]
            [sablono.core :refer-macros [html]]))

(defn text-field [label keypath value input-attributes]
  [:.col-10.floating-label.mb2.mx-auto
   [:.absolute
    [:label.floated-label.col-12.h6.teal.relative
     (when (seq value) {:class "has-value"})
     label]]
   [:input.col-12.h3.border.border-width-1.border-light-gray.border-teal-gradient.col-10.rounded-1.glow.floating-input
    (merge {:key label
            :placeholder label}
           (utils/change-text keypath value)
           (when (seq value) {:class "has-value"})
           input-attributes)]])

(defn selected-value [evt]
  (let [elem (.-target evt)]
    (.-value
     (aget (.-options elem)
           (.-selectedIndex elem)))))

(defn select-field [label keypath value options select-attributes]
  [:.col-10.floating-label.mb2.mx-auto
   [:.relative.z1
    [:select.col-12.h2.glow.floating-input.absolute.border-none
     (merge {:key label
             :style {:height "3.75rem" :color "transparent" :background-color "transparent"}
             :placeholder label
             :value value
             :on-change #(handle-message events/control-change-state
                                         {:keypath keypath
                                          :value   (selected-value %)})}
            (when (seq value) {:class "has-value"})
            select-attributes)
     [:option ""]
     (map (fn [{name :name val :abbr}]
            [:option {:key val :value val}
             (str name)])
          options)]]
   [:.bg-pure-white.border.border-width-1.border-light-gray.border-teal-gradient.rounded-1.p1
    [:label.col-12.h6.teal.relative
     (merge
      {:for "shipping-state"}
      (when (seq value) {:class "has-value"}))
     label]
    [:.h3.black.relative
     (:name (first (filter (comp (partial = value) :abbr) options)))]]])


(def large-button :.my2.btn.btn-large.btn-primary.btn-teal-gradient.col-10)
(def large-button-text :.h3.p1.letter-spacing-1)

(def nbsp [:span {:dangerouslySetInnerHTML {:__html " &nbsp;"}}])
(def rarr [:span {:dangerouslySetInnerHTML {:__html " &rarr;"}}])
(def new-flag
  (html
   [:.pyp1.right
    [:.inline-block.border.border-gray.gray.pp2
     [:div {:style {:margin-bottom "-2px" :font-size "7px"}} "NEW"]]]))

(defn spinner
  ([] (spinner {:width "100%" :height "32px"}))
  ([style] [:.img-spinner.bg-no-repeat.bg-center {:style style}]))

(defn drop-down [expanded? menu-keypath [link-tag & link-contents] menu]
  [:div
   (into [link-tag
          (utils/fake-href events/control-menu-expand {:keypath menu-keypath})]
         link-contents)
   (when expanded?
     [:.relative.z1
      {:on-click #(handle-message events/control-menu-collapse-all)}
      [:.fixed.overlay]
      menu])])

(defn circle-picture
  ([src] (circle-picture {} src))
  ([{:keys [width] :as attrs :or {width "4em"}} src]
   [:.circle.bg-silver.overflow-hidden
    (merge {:style {:width width :height width}} attrs)
    [:img {:style {:width width :height width :object-fit "cover"} :src src}]]))
