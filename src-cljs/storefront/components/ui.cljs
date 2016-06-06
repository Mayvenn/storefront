(ns storefront.components.ui
  (:require [storefront.components.utils :as utils]
            [storefront.components.svg :as svg]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.messages :refer [handle-message]]
            [clojure.string :as str]
            [sablono.core :refer-macros [html]]))

;; TODO: when the old CSS is gone, perhaps this can go on body
(defn container [& content]
  [:.bg-light-white.light-black.sans-serif
   (into [:.p2.m-auto] content)])

(defn narrow-container [& content]
  (apply container {:class [:md-col-8 :lg-col-6]} content))

(def spinner
  "Spinner that fills line at current font size, assuming line-height is 1.2"
  (html
   [:.img-spinner.bg-no-repeat.bg-center.bg-contain
    {:style {:height "1.2em" :width "100%"}}]))

(defn button
  [content {:keys [show-spinner? disabled? color btn-type border text-color on-click data-test]
            :or {color "bg-green"
                 text-color ""
                 btn-type "btn-primary"
                 on-click utils/noop-callback}}]
  [:.btn.col-12.h3.px1.py2.letter-spacing-1
   {:class (conj [color text-color btn-type border]
                 (when disabled? "is-disabled"))
    :data-test data-test
    :on-click (if (or disabled? show-spinner?)
                utils/noop-callback
                on-click)}
   [:.flex.items-center.justify-center
    (if show-spinner? spinner content)]])

(defn submit-button
  ([title] (submit-button title {}))
  ([title {:keys [spinning? disabled? data-test]}]
   (if spinning?
     (button nil {:show-spinner? true})
     [:input.btn.btn-primary.col-12.h3.px1.py2.letter-spacing-1
      {:type "submit"
       :data-test data-test
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
   [:input.col-12.h3.border.border-light-silver.glow.floating-input
    (cond-> (merge {:key label
                    :class "rounded"
                    :placeholder label}
                   (utils/change-text keypath value)
                   input-attributes)
      (seq value) (update :class #(str %1 " " %2) "has-value"))]])

(defn selected-value [evt]
  (let [elem (.-target evt)]
    (.-value
     (aget (.-options elem)
           (.-selectedIndex elem)))))

(defn select-field [label value options select-attributes]
  [:.col-12.floating-label.mb2.mx-auto
   [:.relative.z1
    [:select.col-12.h2.glow.floating-input.absolute.border-none
     (merge {:key         label
             :style       {:height "3.75rem" :color "transparent" :background-color "transparent"}
             :placeholder label
             :value       value}
            (when (seq value) {:class "has-value"})
            select-attributes)
     [:option ""]
     (for [{name :name val :abbr} options]
       [:option {:key val :value val}
        (str name)])]]
   [:.bg-pure-white.border.border-light-silver.rounded.p1
    [:label.col-12.h6.navy.relative
     (merge
      {:for (name (:id select-attributes))}
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

(defn ^:private counter-button [spinning? f content]
  [:a.col
   {:href "#"
    :disabled spinning?
    :on-click (if spinning? utils/noop-callback f)}
   content])

(defn ^:private counter-value [spinning? value]
  [:.left.center.mx1 {:style {:width "1.2em"}
                      :data-test "line-item-quantity"}
   (if spinning? spinner value)])

(defn counter [value spinning? dec-fn inc-fn]
  [:div
   (counter-button spinning? dec-fn svg/counter-dec)
   (counter-value spinning? value)
   (counter-button spinning? inc-fn svg/counter-inc)])

(defn note-box [{:keys [color data-test]} contents]
  [:.border.rounded
   {:class (str "bg-" color " border-" color)
    :data-test data-test}
   [:.bg-lighten-4.rounded
    contents]])

