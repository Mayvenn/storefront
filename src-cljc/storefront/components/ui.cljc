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
  [:div.gray
   (into [:div.p2.m-auto.overflow-hidden] content)])

(defn narrow-container [& content]
  (apply container {:class "md-up-col-8 lg-up-col-6"} content))

(def spinner
  "Spinner that fills line at current font size, assuming line-height is 1.2"
  (component/html
   [:div.img-spinner.bg-no-repeat.bg-center.bg-contain.col-12
    {:style {:height "1.2em"}}]))

(defn button
  [{:keys [disabled? spinning?]
    :as opts}
   & content]
  (let [opts    (cond-> opts
                  :always (update :on-click #(if (or disabled? spinning?)
                                               utils/noop-callback
                                               %))
                  :always (dissoc :spinning? :disabled?)
                  disabled? (update :class str " is-disabled "))
        content (if spinning? spinner content)]
    [:a (merge {:href "#"} opts) content]))

(defn ^:private button-colors [color-kw]
  (let [color (color-kw {:color/teal     "btn-primary bg-teal white"
                         :color/navy     "btn-primary bg-navy white"
                         :color/aqua     "btn-primary bg-aqua white"
                         :color/ghost    "btn-outline border-light-gray dark-gray"
                         :color/facebook "btn-primary bg-fb-blue white"})]
    (assert color (str "Button color " color-kw " has not been defined."))
    color))

(defn ^:private button-sizes [size-kw]
  (let [size (size-kw {:size/small "col-12 h4"
                       :size/large "col-12 h3 btn-large"})]
    (assert size (str "Button size " size-kw " has not been defined."))
    size))

(defn ^:private button-class [color-kw size-kw {:keys [class]}]
  (str/join " "
            ["btn"
             (button-sizes size-kw)
             (button-colors color-kw)
             class]))

(defn ^:private small-button [color-kw attrs & content]
  (button (assoc attrs
                 :class          (button-class color-kw :size/small attrs))
          (into [:div] content)))

(defn ^:private large-button [color-kw attrs & content]
  (button (assoc attrs
                 :class          (button-class color-kw :size/large attrs))
          (into [:div] content)))

(defn teal-button [attrs & content]
  (small-button :color/teal attrs content))

(defn large-teal-button [attrs & content]
  (large-button :color/teal attrs content))

(defn navy-button [attrs & content]
  (small-button :color/navy attrs content))

(defn large-navy-button [attrs & content]
  (large-button :color/navy attrs content))

(defn aqua-button [attrs & content]
  (small-button :color/aqua attrs content))

(defn large-aqua-button [attrs & content]
  (large-button :color/aqua attrs content))

(defn large-facebook-button [attrs & content]
  (large-button :color/facebook attrs content))

(defn ghost-button [attrs & content]
  (small-button :color/ghost attrs content))

(defn large-ghost-button [attrs & content]
  (large-button :color/ghost attrs content))

(defn submit-button
  ([title] (submit-button title {}))
  ([title {:keys [spinning? disabled? data-test] :as attrs}]
   (if spinning?
     (large-button :color/teal attrs)
     [:input
      {:type "submit"
       :class (button-class :color/teal :size/large attrs)
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

(def ^:private field-error-icon
  [:div.absolute {:style {:right "1rem" :top "0.75rem"}}
   [:div.img-error-icon.bg-no-repeat.bg-contain.bg-center
    {:style {:width "1.5rem" :height "1.5rem"}}]])

(defn ^:private field-error-message [error data-test]
  [:div.orange.mtp2.mb1.bold
   (when error {:data-test (str data-test "-error")})
   (or (:long-message error) nbsp)])

(defn- add-classes [attributes classes]
  (update attributes :class #(str %1 " " %2) classes))

(defn ^:private text-field-without-error-message [label keypath value error? input-attributes]
  (let [wrapper-class    (:wrapper-class input-attributes)
        input-attributes (dissoc input-attributes :wrapper-class)]
    [:div.clearfix.rounded.border.pp1
     (cond-> {:class wrapper-class}
       ;; .z1.relative is for adjacent text-fields with left in error and right
       ;; not in error; keeps the left field's right border/inset 2px;
       error?       (add-classes "z1 relative border-orange inset-orange x-group-item-2")
       (not error?) (add-classes "border-dark-silver x-group-item"))
     ;; z3 puts the icon above the field, even when it has focus
     [:div.right.relative.z3 (when error? field-error-icon)]
     [:div.absolute
      [:label.floating-label--label.col-12.h6.relative
       (cond-> {:for (:id input-attributes)}
         (seq value)       (add-classes "has-value")
         error?            (add-classes "orange")
         (not error?)      (add-classes "light-gray"))
       label]]
     [:input.floating-label--input.col-12.h4.glow.border-none
      (cond-> (merge {:key label
                      :placeholder label
                      :value (or value "")
                      :on-change
                      (fn [e]
                        (handle-message events/control-change-state
                                        {:keypath keypath
                                         :value (.. e -target -value)}))}
                     input-attributes)
        error?                         (add-classes "field-is-error pr4")
        (and error? (not (seq value))) (add-classes "orange")
        (seq value)                    (add-classes "has-value bold"))]]))

(defn text-field [label keypath value {:keys [errors data-test] :as input-attributes}]
  (let [error (first errors)]
    [:div.col-12.mb1
     (text-field-without-error-message label keypath value (not (nil? error))
                                       (dissoc input-attributes :errors))
     (field-error-message error data-test)]))

(defn text-field-group
  "For grouping many fields on one line. Sets up columns, rounding of
  first and last fields, and avoids doubling of borders between fields."
  [& fields]
  {:pre [(zero? (rem 12 (count fields)))]}
  (let [col-size (str "col col-" (/ 12 (count fields)))]
    [:div.mb1
     (into [:div.clearfix]
           (concat
            (for [[idx {:keys [label keypath value errors] :as field}]
                  (map-indexed vector fields)

                  :let [first? (zero? idx)
                        last? (= idx (dec (count fields)))]]
              (let [wrapper-class (str col-size
                                       (when first? " rounded-left")
                                       (when last? " rounded-right"))]
                (text-field-without-error-message
                 label keypath value (seq errors)
                 (-> field
                     (dissoc :label :keypath :value :errors)
                     (assoc :wrapper-class wrapper-class)))))
            (for [{:keys [errors data-test]} fields]
              [:div {:class col-size}
               (field-error-message (first errors) data-test)])))]))

(defn selected-value [evt]
  (let [elem (.-target evt)]
    (.-value
     (aget (.-options elem)
           (.-selectedIndex elem)))))

(defn select-field [label keypath value options select-attributes]
  ;; TODO: This is almost an exact copy of the floating label implementation
  ;; from text-field. Unify.
  (let [option-text   first
        option-value  (comp str second)
        selected-text (->> options
                           (filter (comp #{(str value)} option-value))
                           first
                           option-text)
        error         (first (:errors select-attributes))
        error?        (not (nil? error))]
    [:div.col-12.mb1
     [:div.clearfix.rounded.border.pp1
      (cond-> {}
        ;; .z1.relative is for adjacent text-fields with left in error and right
        ;; not in error; keeps the left field's right border/inset 2px;
        error?       (add-classes "z1 relative border-orange inset-orange x-group-item-2")
        (not error?) (add-classes "border-dark-silver x-group-item"))
      ;; z3 puts the icon above the field, even when it has focus
      [:div.right.relative.z3 (when error? field-error-icon)]
      [:div.absolute
       [:label.floating-label--label.col-12.h6.relative
        (cond-> {:for (:id select-attributes)}
          (seq selected-text) (add-classes "has-value")
          error?              (add-classes "orange")
          (not error?)        (add-classes "light-gray"))
        label]]
      [:select.floating-label--input.col-12.h4.glow.border-none.bg-clear
       (cond-> (merge {:key         label
                       :value       (or value "")
                       :on-change   #(handle-message events/control-change-state
                                                     {:keypath keypath
                                                      :value   (selected-value %)})}
                      (dissoc select-attributes :errors))
         error?                                 (add-classes "field-is-error pr4")
         (and error? (not (seq selected-text))) (add-classes "orange")
         (seq selected-text)                    (add-classes "has-value bold"))
       (when-let [placeholder (:placeholder select-attributes)]
         [:option {:value "" :disabled "disabled"} placeholder])
       (for [option options]
         [:option
          {:key   (option-value option)
           :value (option-value option)}
          (option-text option)])]]
     (field-error-message error (:data-test select-attributes))]))

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

(defn modal [{:keys [on-close bg-class col-class] :or {col-class "col-11 md-up-col-7 lg-up-col-5"}} & body]
  ;; Inspired by https://css-tricks.com/considerations-styling-modal/
  [:div
   ;; The scrim, a sibling to the modal
   [:div.z3.fixed.overlay.bg-darken-4
    {:on-click on-close}
    ;; Set bg-class to override or darken the scrim
    [:div.fixed.overlay {:class bg-class}]]
   ;; The modal itself
   ;; - is above the scrim (z)
   ;; - centers the contents in the viewport (fixed, translate-center)
   ;; - stays within the bounds of the screen and scrolls when necessary (col-12, max-height, overflow)
   [:div.z4.fixed.translate-center.col-12.overflow-auto {:style    {:max-height "100%"}
                                                         :on-click on-close}
    ;; The inner wrapper
    ;; - provides a place to set width of the modal content (col-class)
    ;;   - should be a percentage based width; will be centered with mx-auto
    ;; Because the contents are centered with auto margin, normally clicks in
    ;; these margins would not close the modal. This is remedied with the
    ;; on-click handlers on the modal and on the wrapper, which collaborate to
    ;; close the modal on click in the margin, but not on click in the contents
    (into [:div.mx-auto {:class    col-class
                         :on-click utils/stop-propagation}]
          body)]])

(defn modal-close [{:keys [data-test on-close bg-class]}]
  [:div.clearfix
   {:data-scrollable "not-a-modal"}
   [:a.pointer.h3.right.rotate-45 {:href "#" :on-click on-close :data-test data-test}
    [:div {:alt "Close"
           :class (or bg-class "fill-light-gray")}
     svg/counter-inc]]])

(defn circle-picture
  ([src] (circle-picture {} src))
  ([{:keys [width] :as attrs :or {width "4em"}} src]
   [:div.circle.bg-light-silver.overflow-hidden
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
  [:div.fill-dark-silver
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
