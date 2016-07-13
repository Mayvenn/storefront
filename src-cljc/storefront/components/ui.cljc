(ns storefront.components.ui
  (:require [storefront.platform.component-utils :as utils]
            [storefront.components.svg :as svg]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.platform.messages :refer [handle-message]]
            [storefront.components.money-formatters :as mf]
            [clojure.string :as str]))

(defn container [& content]
  [:div.bg-light-white.light-black.sans-serif
   (into [:div.p2.m-auto.overflow-hidden] content)])

(defn narrow-container [& content]
  (apply container {:class "md-up-col-8 lg-up-col-6"} content))

(def spinner
  "Spinner that fills line at current font size, assuming line-height is 1.2"
  (component/html
   [:div.img-spinner.bg-no-repeat.bg-center.bg-contain
    {:style {:height "1.2em" :width "100%"}}]))

(defn button
  [content {:keys [show-spinner?
                   disabled?
                   color
                   btn-type
                   border
                   text-color
                   on-click
                   data-test
                   href
                   padding]
            :or   {color      "bg-green"
                   text-color ""
                   href       "#"
                   padding    "px1 py2"
                   btn-type   "btn-primary"
                   on-click   utils/noop-callback}}]
  [:div.btn.col-12.h3.letter-spacing-1
   {:class     (str/join " " (conj [padding color text-color btn-type border]
                                   (when disabled? "is-disabled")))
    :data-test data-test
    :href      href
    :on-click  (if (or disabled? show-spinner?)
                 utils/noop-callback
                 on-click)}
   [:div.flex.items-center.justify-center
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

(def nbsp (component/html [:span {:dangerouslySetInnerHTML {:__html " &nbsp;"}}]))
(def rarr (component/html [:span {:dangerouslySetInnerHTML {:__html " &rarr;"}}]))
(def new-flag
  (component/html
   [:div.pyp1.right
    [:div.inline-block.border.border-navy.navy.pp2.medium
     [:div {:style {:margin-bottom "-2px" :font-size "7px"}} "NEW"]]]))

(defn text-field [label keypath value {:keys [errors data-test] :as input-attributes}]
  (let [error (first errors)]
    [:div.col-12.mb1
     [:div.right.relative
      (when error
        [:div.absolute
         {:style {:right "1rem" :top "0.8725rem" :bottom "0"}}
         [:div.img-error-icon.bg-no-repeat.bg-contain.bg-center
          {:style {:width "2.25rem" :height "2.25rem"}}]])]
     [:div.absolute
      [:label.floating-label--label.col-12.h6.navy.relative
       {:class (str/join " " (map str
                                  [(when (seq value)
                                     "has-value")
                                   (when error
                                     "orange")]))}
       label]]
     [:input.floating-label--input.col-12.h3.border
      (cond-> (merge {:key label
                      :class "rounded"
                      :placeholder label}
                     (utils/change-text keypath value)
                     input-attributes)
        ;; TODO: needs refactor
        (nil? error) (update :class #(str %1 " " %2) "border-light-silver glow-green")
        error (update :class #(str %1 " " %2) "border-orange border-width-2 pr4 glow-orange")
        (seq value) (update :class #(str %1 " " %2) "has-value"))]
     [:div.orange.mtp2.mb1
      {:data-test (str data-test "-error")}
      (or (:long-message error) nbsp)]]))

(defn selected-value [evt]
  (let [elem (.-target evt)]
    (.-value
     (aget (.-options elem)
           (.-selectedIndex elem)))))

(defn select-field [label keypath value options select-attributes]
  (let [option-text  first
        option-value (comp str second)]
    [:div.col-12.mb2.mx-auto
     [:div.relative.z1
      [:select.col-12.h2.glow-green.absolute.border-none
       (merge {:key         label
               :style       {:height "3.75rem" :color "transparent" :background-color "transparent"}
               :placeholder label
               :value       value
               :on-change   #(handle-message events/control-change-state
                                             {:keypath keypath
                                              :value   (selected-value %)})}
              select-attributes)
       (when-let [placeholder (:placeholder select-attributes)]
         [:option {:disabled "disabled"} placeholder])
       (for [option options]
         [:option
          {:key   (option-value option)
           :value (option-value option)}
          (option-text option)])]]
     [:div.bg-pure-white.border.border-light-silver.rounded.p1
      [:label.col-12.h6.navy.relative
       {:for (name (:id select-attributes))}
       label]
      [:div.h3.black.relative
       (or (->> options (filter (comp #{(str value)} option-value)) first option-text)
           nbsp)]]]))

(defn drop-down [expanded? menu-keypath [link-tag & link-contents] menu]
  [:div
   (into [link-tag
          (utils/fake-href events/control-menu-expand {:keypath menu-keypath})]
         link-contents)
   (when expanded?
     [:div.relative.z2
      {:on-click #(handle-message events/control-menu-collapse-all)}
      [:div.fixed.overlay]
      menu])])

(defn modal [{:keys [on-close bg-class] :as attrs} & body]
  [:div
   [:div.fixed.overlay.bg-darken-4.z3
    {:on-click on-close}
    [:div.fixed.overlay {:class bg-class}]]
   (into [:div.fixed.z3.left-0.right-0.mx-auto.col-11.md-up-col-7.lg-up-col-5.overflow-auto {:style {:max-height "100%"}
                                                                                             :data-snap-to "top"}]
         body)])

(defn modal-close [{:keys [data-test on-close bg-class]}]
  [:div.clearfix
   {:data-scrollable "not-a-modal"}
   [:a.pointer.h2.right.rotate-45 {:href "#" :on-click on-close :data-test data-test}
    [:div
     {:alt "Close"
      :class (or bg-class "fill-dark-silver")}
     svg/counter-inc]]])

(defn circle-picture
  ([src] (circle-picture {} src))
  ([{:keys [width] :as attrs :or {width "4em"}} src]
   [:div.circle.bg-white.overflow-hidden
    (merge {:style {:width width :height width}} attrs)
    (if src
      [:img {:style {:width width :height width :object-fit "cover"} :src src}]
      (svg/missing-profile-picture {:width width :height width}))]))

(defn ^:private counter-button [spinning? data-test f content]
  [:a.col
   {:href "#"
    :data-test data-test
    :disabled spinning?
    :on-click (if spinning? utils/noop-callback f)}
   content])

(defn ^:private counter-value [spinning? value]
  [:div.left.center.mx1 {:style {:width "1.2em"}
                      :data-test "line-item-quantity"}
   (if spinning? spinner value)])

(defn counter [value spinning? dec-fn inc-fn]
  [:div.fill-light-silver
   (counter-button spinning? "quantity-dec" dec-fn svg/counter-dec)
   (counter-value spinning? value)
   (counter-button spinning? "quantity-inc" inc-fn svg/counter-inc)])

(defn note-box [{:keys [color data-test]} contents]
  [:div.border.rounded
   {:class (str "bg-" color " border-" color)
    :data-test data-test}
   [:div.bg-lighten-4.rounded
    contents]])

(defn big-money [amount]
  [:div.flex.justify-center.line-height-1
   (mf/as-money-without-cents amount)
   [:span.h5 {:style {:margin "5px 3px"}} (mf/as-money-cents-only amount)]])
