(ns storefront.components.ui
  (:require [storefront.platform.component-utils :as utils]
            [storefront.components.svg :as svg]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.platform.messages :refer [handle-message]]
            [storefront.platform.numbers :as numbers]
            [storefront.assets :as assets]
            [storefront.components.money-formatters :as mf]
            [clojure.string :as str]
            [storefront.platform.images :as images]))

(defn narrow-container
  "A container that is 480px wide on desktop and tablet, but squishes on mobile"
  [& content]
  [:div.container
   [:div.m-auto.col-8-on-tb.col-6-on-dt
    content]])

(def spinner
  "Spinner that fills line, assuming line-height is 1.5em"
  (component/html
   [:div.img-spinner.bg-no-repeat.bg-center.bg-contain.col-12
    {:style {:height "1.5em"}}]))

(defn aspect-ratio
  "Refer to https://css-tricks.com/snippets/sass/maintain-aspect-ratio-mixin/. This is a slight modification, adapted from the wistia player."
  [x y & content]
  [:div.relative.overflow-hidden
   {:style {:padding-top (-> y (/ x) (* 100) float (str "%")) }}
   (into [:div.absolute.overlay] content)])

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
  (let [color (color-kw {:color/teal        "btn-primary bg-teal white"
                         :color/navy        "btn-primary bg-navy white"
                         :color/aqua        "btn-primary bg-aqua white"
                         :color/ghost       "btn-outline border-dark-gray dark-gray"
                         :color/light-ghost "btn-outline border-white white"
                         :color/teal-ghost  "btn-outline border-teal teal"
                         :color/facebook    "btn-primary bg-fb-blue white"
                         :color/apple-pay   "btn-primary bg-black white"
                         :color/dark-gray   "btn-primary bg-dark-gray white"})]
    (assert color (str "Button color " color-kw " has not been defined."))
    color))

(defn ^:private button-class [color-kw {:keys [class]}]
  (str/join " "
            ["btn col-12 h5"
             (button-colors color-kw)
             class]))

(defn ^:private color-button [color-kw attrs & content]
  (button (assoc attrs :class (button-class color-kw attrs))
          (into [:div] content)))

(defn teal-button [attrs & content]
  (color-button :color/teal attrs content))

(defn dark-gray-button [attrs & content]
  (color-button :color/dark-gray attrs content))

(defn navy-button [attrs & content]
  (color-button :color/navy attrs content))

(defn aqua-button [attrs & content]
  (color-button :color/aqua attrs content))

(defn facebook-button [attrs & content]
  (color-button :color/facebook attrs content))

(defn apple-pay-button [attrs & content]
  (color-button :color/apple-pay attrs content))

(defn ghost-button [attrs & content]
  (color-button :color/ghost attrs content))

(defn light-ghost-button [attrs & content]
  (color-button :color/light-ghost attrs content))

(defn teal-ghost-button [attrs & content]
  (color-button :color/teal-ghost attrs content))

(defn submit-button
  ([title] (submit-button title {}))
  ([title {:keys [spinning? disabled? data-test] :as attrs}]
   (if spinning?
     (color-button :color/teal attrs)
     [:input
      {:type "submit"
       :class (button-class :color/teal attrs)
       :data-test data-test
       :value title
       :disabled (boolean disabled?)}])))

(def nbsp (component/html [:span {:dangerouslySetInnerHTML {:__html "&nbsp;"}}]))
(def rarr (component/html [:span {:dangerouslySetInnerHTML {:__html " &rarr;"}}]))
(def times (component/html [:span {:dangerouslySetInnerHTML {:__html " &times;"}}]))
(def new-flag
  (component/html
   [:div.right
    [:div.border.border-navy.navy.pp2.h7.line-height-1.medium "NEW"]]))

(defn- add-classes [attributes classes]
  (update attributes :class #(str %1 " " %2) classes))

(defn selected-value [evt]
  (let [elem (.-target evt)]
    (.-value
     (aget (.-options elem)
           (.-selectedIndex elem)))))

(defn ^:private field-error-message [error data-test]
  (when error
    [:div.red.my1.h6.center.medium
     {:data-test (str data-test "-error")}
     (or (:long-message error) nbsp)]))

(defn ^:private floating-label [label id {:keys [error? value?]}]
  [:div.absolute
   [:label.floating-label--label.col-12.h7.relative.gray.medium
    (cond-> {:for id}
      value? (add-classes "has-value"))
    label]])

(defn ^:private field-wrapper-class [wrapper-class {:keys [error? focused?]}]
  (cond-> {:class wrapper-class}
    true         (add-classes "rounded border pp1 x-group-item")
    focused?     (add-classes "glow")
    ;; .z1.relative keeps border between adjacent fields red if one of them is in error
    error?       (add-classes "border-red z1 relative")
    (not error?) (add-classes "border-gray")))

(defn ^:private field-class [base {:keys [error? value?]}]
  (cond-> base
    true                      (add-classes "floating-label--input rounded border-none")
    error?                    (add-classes "red")
    value?                    (add-classes "has-value")))

(defn ^:private plain-text-field
  [label keypath value error? {:keys [wrapper-class id hint focused] :as input-attributes}]
  (let [input-attributes (dissoc input-attributes :wrapper-class :hint :focused)
        hint?            (seq hint)
        focused?         (= focused keypath)
        status           {:error?   error?
                          :focused? focused?
                          :hint?    hint?
                          :value?   (seq value)}]
    [:div.clearfix (field-wrapper-class wrapper-class status)
     (floating-label label id status)
     [:label
      [:input.col-12.h4.line-height-1
       (field-class (merge {:key         label
                            :placeholder label
                            :value       (or value "")
                            :on-focus
                            (fn [e]
                              (handle-message events/control-focus {:keypath keypath}))
                            :on-blur
                            (fn [e]
                              (handle-message events/control-blur {:keypath keypath}))
                            :on-change
                            (fn [e]
                              (handle-message events/control-change-state
                                              {:keypath keypath
                                               :value   (.. e -target -value)}))}
                           input-attributes)
                    status)]
      (when hint? [:div.py1.px2
                   (when error? {:class "red"})
                   hint])]]))

(defn text-field [{:keys [label keypath value errors data-test] :as input-attributes}]
  (let [error (first errors)]
    [:div.col-12.mb2.stacking-context
     (plain-text-field label keypath value (not (nil? error))
                       (dissoc input-attributes :label :keypath :value :errors))
     (field-error-message error data-test)]))

(defn text-field-group
  "For grouping many fields on one line. Sets up columns, rounding of
  first and last fields, and avoids doubling of borders between fields."
  [& fields]
  {:pre [(zero? (rem 12 (count fields)))]}
  (let [col-size (str "col col-" (/ 12 (count fields)))
        some-errors? (some (comp seq :errors) fields)]
    [:div.mb2
     (into [:div.clearfix.stacking-context]
           (concat
            (for [[idx {:keys [label keypath value errors] :as field}]
                  (map-indexed vector fields)

                  :let [first? (zero? idx)
                        last? (= idx (dec (count fields)))]]
              (let [wrapper-class (str col-size
                                       (when first? " rounded-left")
                                       (when last? " rounded-right"))]
                (plain-text-field
                 label keypath value (seq errors)
                 (-> field
                     (dissoc :label :keypath :value :errors)
                     (assoc :wrapper-class wrapper-class)))))
            (for [{:keys [errors data-test]} fields]
              [:div {:class col-size}
               (cond
                 (seq errors) (field-error-message (first errors) data-test)
                 some-errors? nbsp
                 :else nil)])))]))

(def ^:private custom-select-dropdown
  (component/html
   [:div.absolute.floating-label--icon
    (svg/dropdown-arrow {:class "stroke-gray"
                         :style {:width "1.2em" :height "1em"}})]))

(defn ^:private plain-select-field
  [label keypath value options error? {:keys [id placeholder] :as select-attributes}]
  (let [option-text   first
        option-value  (comp str second)
        selected-text (->> options
                           (filter (comp #{(str value)} option-value))
                           first
                           option-text)
        status        {:error? error?
                       :value? (seq selected-text)}]
    [:div.clearfix.relative
     (field-wrapper-class "" status)
     (floating-label label id status)
     [:select.col-12.bg-clear
      (field-class (merge {:key         label
                           :value       (or value "")
                           :on-change   #(handle-message events/control-change-state
                                                         {:keypath keypath
                                                          :value   (selected-value %)})}
                          (dissoc select-attributes :focused)
                          (when-not (seq selected-text) {:style {:opacity 0.5}}))
                   status)
      (when placeholder
        [:option {:value "" :disabled "disabled"} placeholder])
      (for [option options]
        [:option
         {:key   (option-value option)
          :value (option-value option)}
         (option-text option)])]
     custom-select-dropdown]))

(defn select-field [{:keys [label keypath value options errors data-test] :as select-attributes}]
  (let [error (first errors)]
    [:div.col-12.mb2.stacking-context
     (plain-select-field label keypath value options (not (nil? error))
                         (dissoc select-attributes :label :keypath :value :options :errors))
     (field-error-message error data-test)]))

(defn check-box [{:keys [label keypath value label-classes disabled] :as attributes}]
  [:div.col-12.mb2
   [:label.flex.items-center {:class label-classes}
    ;; 15px svg + 2*2px padding + 2*1px border = 21px
    [:div.border.left.mr3.pp2
     (when disabled
       {:class "border-gray"})
     (if value
       (svg/simple-x {:class "block stroke-teal" :width "15px" :height "15px"})
       [:div {:style {:width "15px" :height "15px"}}])]
    [:input.hide
     (merge (utils/toggle-checkbox keypath value)
            (dissoc attributes :label :keypath :value :label-classes)
            {:type "checkbox"})]
    [:span
     (when disabled {:class "gray"})
     label]]])

(defn drop-down [expanded? menu-keypath [link-tag & link-contents] menu]
  [:div
   (into [link-tag
          (utils/fake-href events/control-menu-expand {:keypath menu-keypath})]
         link-contents)
   (when expanded?
     [:div.relative.z4
      {:on-click #(handle-message events/control-menu-collapse-all)}
      [:div.fixed.overlay]
      menu])])

(defn modal [{:keys [close-attrs bg-class col-class] :or {col-class "col-11 col-7-on-tb col-5-on-dt"}} & body]
  ;; The scrim
  [:div.z4.fixed.overlay.bg-darken-4
   ;; Set bg-class to override or darken the scrim
   [:div.absolute.overlay {:class bg-class}]
   ;; The modal itself, centered with https://www.smashingmagazine.com/2013/08/absolute-horizontal-vertical-centering-css/2/#table-cell
   ;; This method was chosen because it is widely supported and avoids bluriness.
   ;; Flex may also work, with less markup, but we couldn't find a way to use
   ;; it and have overflow scroll vertically
   [:div.absolute.overlay.overflow-auto
    [:div.table.container-size
     {:style {:table-layout "fixed"}}
     [:div.table-cell.align-middle
      {:on-click (:on-click close-attrs)}
      ;; The inner wrapper
      ;; - provides a place to adjust the width of the modal content (col-class)
      ;;   - should be a percentage based width; will be centered with mx-auto
      ;; - collaborates with its wrapper to ensure that clicks around the modal
      ;;   close it, but clicks within it do not
      (into [:div.mx-auto {:class col-class
                           :on-click utils/stop-propagation}]
            body)]]]])

(defn modal-close [{:keys [class data-test close-attrs]}]
  [:div.clearfix
   {:data-scrollable "not-a-modal"}
   [:a.h3.right (merge {:data-test data-test :title "Close"} close-attrs)
    (svg/close-x {:class (or class "stroke-white fill-gray")})]])

(defn square-image [{:keys [resizable_url]} size]
  (some-> resizable_url (str "-/scale_crop/" size "x" size "/center/")))

(defn circle-picture
  ([src] (circle-picture {} src))
  ([{:keys [width overlay-copy] :as attrs :or {width "4em"}} src]
   [:div.circle.bg-light-gray.overflow-hidden.relative
    (merge {:style {:width width :height width}}
           (dissoc attrs :width :overlay-copy))
    (if src
      [:img {:style {:width width :height width} :src src}]
      (svg/missing-portrait {:width width :height width}))
    (when overlay-copy
      [:div.absolute.overlay.bg-darken-2
       [:div.absolute.m-auto.overlay {:style {:height "50%"}} overlay-copy]])]))

(defn img-attrs [img size]
  {:src (get img (keyword (str (name size) "_url")))
   :alt (:alt img)})

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
  [:div
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

(defn progress-indicator [{:keys [value maximum]}]
  (let [bar-value (-> value (/ maximum) (* 100.0) (min 100))
        bar-width (str (numbers/round bar-value) "%")
        bar-style {:padding-top "0.3em" :padding-bottom "0.15em" :height "20px"}]
    [:div.flex.items-center
     [:div.my2.border.border-dark-gray.capped.h4.flex-auto
      (cond
        (zero? value) [:div.px2.capped {:style bar-style}]
        (= value maximum) [:div.bg-teal.px2.capped {:style bar-style}]
        :else [:div.bg-teal.px2.capped {:style (merge bar-style {:width bar-width})}])]
     (if (= value maximum)
       (svg/circled-check {:class "stroke-teal"
                           :style {:width "3rem" :height "3rem"
                                   :margin-left "0.4rem"
                                   :margin-right "-0.2rem"}})
       [:div.border.circle.border-dark-gray.h6.center.ml1
        {:style {:height "2.66667rem" :width "2.66667rem" :line-height "2.666667rem"}}
        bar-width])]))

(defn lqip
  "Generates a Low Quality Image Placeholder.
  http://www.guypo.com/introducing-lqip-low-quality-image-placeholders/
  https://jmperezperez.com/more-progressive-image-loading/
  https://qz.com/894001/theres-a-wrong-and-a-right-way-to-talk-to-your-dog-according-to-science/

  This technique initially shows a blurry (and small, so fast-to-load) image,
  then replaces it with a high-quality image which fades in. It relies on having
  a CDN which supports image operations.

  The `lq-width` and `lq-height` define the width and height the small
  low-quality image, which will be generated by the CDN from the large
  high-quality image. These values also define the aspect ratio for both images.

  The values can be changed as long as the ratio stays the same. Setting higher
  values makes the low-quality image look better in large spaces, at the expense
  of loading slower.

  For example, suppose you have a 4:3 image. In a small space, the low-quality
  image may look good when it is 40 by 30 or 48 by 36. In large spaces it may
  need to be 96 by 72. Experiment with different values, trying to keep the
  low-quality image less than 3kb.

  Within the `image` hashmap, the `resizable_url` will be used with the CDN
  image operations. The `resizable_filename` will be appended to all image URLs,
  for SEO. And the `alt` will be put on all img tags."
  [lq-width lq-height image]
  (let [{:keys [resizable_url resizable_filename alt]} image

        lq-url (str resizable_url "-/resize/" lq-width "x" lq-height "/-/quality/lighter/" resizable_filename)
        hq-url (str resizable_url resizable_filename)]
    [:picture.overflow-hidden.bg-cover.block.relative
     {:style {:background-image (assets/css-url lq-url)
              :padding-top      (-> lq-height (/ lq-width) (* 100) float (str "%"))}}
     [:img.col-12.absolute.overlay {:src   lq-url
                                    :alt   alt
                                    :style {:filter "blur(10px)"}}]
     (images/platform-hq-image {:src hq-url :alt alt})
     [:noscript [:img.col-12.absolute.overlay {:src hq-url :alt alt}]]]))

(def header-image-size 36)

(defn expand-icon [expanded?]
  [:img {:style {:width "8px"}
         :src   (if expanded?
                  (assets/path "/images/icons/collapse.png")
                  (assets/path "/images/icons/expand.png"))}])
