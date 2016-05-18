(ns storefront.components.ui
  (:require [storefront.components.utils :as utils]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.messages :refer [handle-message]]
            [clojure.string :as str]
            [sablono.core :refer-macros [html]]))

(defn container [& content]
  [:.bg-light-white.light-black.sans-serif
   (into [:.p2.m-auto] content)])

(defn narrow-container [& content]
  (apply container {:class [:md-col-8 :lg-col-6]} content))

(defn spinner
  ([] (spinner {:width "100%" :height "32px"}))
  ([style] [:.img-spinner.bg-no-repeat.bg-center.bg-contain {:style style}]))

(defn button
  ([content event]
   (button content event {}))
  ([content event {:keys [show-spinner? disabled? color on-click] :or {color "bg-green"}}]
   [:.reset.border.btn-large.btn-primary.col-12.h3.px1.py2.letter-spacing-1
    {:class (conj [color]
                  (when disabled? "is-disabled"))
     :on-click (or on-click
                   (if (or disabled? show-spinner?)
                     utils/noop-callback
                     (utils/send-event-callback event)))}
    [:.flex.items-center.justify-center
     (if show-spinner? (spinner {:height "1.2em" :width "1.2em"}) content)]]))

(defn submit-button
  ([title] (submit-button title {}))
  ([title {:keys [spinning? disabled?]}]
   (if spinning?
     (button nil nil {:show-spinner? true})
     [:input.reset.border.btn-large.btn-primary.bg-green.col-12.h3.py2.px1.letter-spacing-1
      {:type "submit"
       :value title
       :disabled (boolean disabled?)}])))

(def nbsp (html [:span {:dangerouslySetInnerHTML {:__html " &nbsp;"}}]))
(def rarr (html [:span {:dangerouslySetInnerHTML {:__html " &rarr;"}}]))
(def new-flag
  (html
   [:.pyp1.right
    [:.inline-block.border.border-navy.navy.pp2.medium
     [:div {:style {:margin-bottom "-2px" :font-size "7px"}} "NEW"]]]))

(defn text-field [label keypath value input-attributes]
  [:.col-12.floating-label.mb2
   [:.absolute
    [:label.floated-label.col-12.h6.navy.relative
     (when (seq value) {:class "has-value"})
     label]]
   [:input.col-12.h3.border.border-width-1.border-light-silver.glow.floating-input
    (cond-> (merge {:key label
                    :class "rounded-1"
                    :placeholder label}
                   (utils/change-text keypath value)
                   input-attributes)
      (seq value) (update :class #(str %1 " " %2) "has-value"))]])

(defn selected-value [evt]
  (let [elem (.-target evt)]
    (.-value
     (aget (.-options elem)
           (.-selectedIndex elem)))))

(defn select-field [label keypath value options select-attributes]
  [:.col-12.floating-label.mb2.mx-auto
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
   [:.bg-pure-white.border.border-width-1.border-light-silver.rounded-1.p1
    [:label.col-12.h6.navy.relative
     (merge
      {:for "shipping-state"}
      (when (seq value) {:class "has-value"}))
     label]
    [:.h3.black.relative
     (or (:name (first (filter (comp (partial = value) :abbr) options)))
         nbsp)]]])

(defn drop-down [expanded? menu-keypath [link-tag & link-contents] menu]
  [:div
   (into [link-tag
          (utils/fake-href events/control-menu-expand {:keypath menu-keypath})]
         link-contents)
   (when expanded?
     [:.relative.z2
      {:on-click #(handle-message events/control-menu-collapse-all)}
      [:.fixed.overlay]
      menu])])

(defn circle-picture
  ([src] (circle-picture {} src))
  ([{:keys [width] :as attrs :or {width "4em"}} src]
   [:.circle.bg-silver.overflow-hidden
    (merge {:style {:width width :height width}} attrs)
    [:img {:style {:width width :height width :object-fit "cover"} :src src}]]))

(defn ^:private counter-button [spinning? f label]
  [:.circle.bg-gray
   [:a.col.flex.items-center.justify-center.bg-lighten-3.white.h1.extra-light
    {:href "#"
     :disabled spinning?
     :on-click (if-not spinning? f utils/noop-callback)
     :style {:height ".93em" :width ".93em"}} [:div label]]])

(defn counter [value spinning? dec-fn inc-fn]
  [:div.flex.items-center
   (counter-button spinning? dec-fn "–")
   [:.center.h2.mx1 {:style {:width "1.2em"}}
    (if spinning? (spinner {:height "1.2em"}) value)]
   (counter-button spinning? inc-fn "+")])

(defn note-box [color contents]
  [:.border.rounded-1
   {:class (str "bg-" color " border-" color)}
   [:.bg-lighten-4.rounded-1
    contents]])
