(ns storefront.components.ui
  (:require [storefront.components.utils :as utils]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.messages :refer [handle-message]]
            [sablono.core :refer-macros [html]]))

(defn text-field [label keypath value input-attributes]
  [:.col-10.floating-label.mb2.mx-auto
   [:.absolute
    [:label.col-12.h6.teal.relative
     (when (seq value) {:class "has-value"})
     label]]
   [:input.col-12.h3.border.border-width-1.border-light-gray.border-teal-gradient.col-10.rounded-1.glow.floating-input
    (merge {:key label}
           (utils/change-text keypath value)
           (when (seq value) {:class "has-value"})
           {:placeholder label}
           input-attributes)]])

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
