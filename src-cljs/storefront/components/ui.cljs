(ns storefront.components.ui
  (:require [storefront.components.utils :as utils]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.messages :refer [handle-message]]
            [clojure.string :as str]
            [sablono.core :refer-macros [html]]))

(def container :.flex.flex-column.items-center.col-10.md-col-8.lg-col-5.m-auto.mt1.py2.black.sans-serif)

(def ^:private large-button :.my2.btn.btn-large.btn-primary.btn-teal-gradient.col-12)
(def ^:private large-button-text :.h3.p1.letter-spacing-1)

(defn submit-button
  ([title]
   (submit-button title {}))
  ([title {:keys [spinning? disabled?]}]
   (if spinning?
     [large-button
      [:.img-spinner.bg-no-repeat.bg-center
       {:style {:height "2.1em"}}]]
     [:input.reset.border.btn-large.btn-primary.btn-teal-gradient.col-12.h3.p1.letter-spacing-1
      {:type "submit"
       :value title
       :disabled (boolean disabled?)
       :style {:height "3.25rem"}}])))

(def button-classes
  ["reset"
   "border"
   "btn-large"
   "btn-primary"
   "col-12"
   "h3"
   "p1"
   "letter-spacing-1"])

(defn button
  ([title event]
   (button title event {}))
  ([title event {:keys [show-spinner? disabled? color]}]
   [:div.flex.items-center.justify-center
    {:style {:height "3.25rem"}
     :class (conj button-classes
                  (or color "btn-teal-gradient")
                  (when disabled? "is-disabled"))
     :on-click (if disabled?
                 utils/noop-callback
                 (utils/send-event-callback event))}
    (if show-spinner?
      [:.img-spinner.bg-no-repeat.bg-center
       {:style {:height "2.1em"}}]
      title)]))

(def nbsp [:span {:dangerouslySetInnerHTML {:__html " &nbsp;"}}])
(def rarr [:span {:dangerouslySetInnerHTML {:__html " &rarr;"}}])
(def new-flag
  (html
   [:.pyp1.right
    [:.inline-block.border.border-gray.gray.pp2
     [:div {:style {:margin-bottom "-2px" :font-size "7px"}} "NEW"]]]))

(defn text-field [label keypath value input-attributes]
  [:.col-12.floating-label.mb2
   [:.absolute
    [:label.floated-label.col-12.h6.teal.relative
     (when (seq value) {:class "has-value"})
     label]]
   [:input.col-12.h3.border.border-width-1.border-light-gray.border-teal-gradient.glow.floating-input
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
   [:.bg-pure-white.border.border-width-1.border-light-gray.border-teal-gradient.rounded-1.p1
    [:label.col-12.h6.teal.relative
     (merge
      {:for "shipping-state"}
      (when (seq value) {:class "has-value"}))
     label]
    [:.h3.black.relative
     (or (:name (first (filter (comp (partial = value) :abbr) options)))
         nbsp)]]])

(defn spinner
  ([] (spinner {:width "100%" :height "32px"}))
  ([style] [:.img-spinner.bg-no-repeat.bg-center {:style style}]))

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

(def ^:private counter-button
  :a.col.flex.border.border-silver.circle.items-center.justify-center.black.h1)

(defn ^:private counter-button* [spinning? f label]
  [counter-button
   {:href "#"
    :disabled spinning?
    :on-click (if-not spinning? f utils/noop-callback)
    :style {:height "1em" :width "1em"}} [:div label]]
  )

(defn counter [value spinning? dec-fn inc-fn]
  [:div.flex.items-center
   (counter-button* spinning? dec-fn "-")
   [:div.center.h2.mx1
    {:class (when spinning? "img-spinner bg-no-repeat bg-center bg-contain")
     :style {:height "1.0em"
             :width "1.0em"}}
    (when-not spinning? value)]
   (counter-button* spinning? inc-fn "+")])
