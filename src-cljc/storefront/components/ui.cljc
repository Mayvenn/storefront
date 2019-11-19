(ns storefront.components.ui
  (:require #?@(:cljs [[storefront.loader :as loader]
                       goog.events
                       [goog.object :as gobj]])
            [cemerick.url :as url]
            [clojure.string :as string]
            [storefront.assets :as assets]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.money-formatters :as mf]
            [storefront.components.svg :as svg]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.images :as images]
            [spice.core :as spice]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.platform.numbers :as numbers]))

(defn narrow-container
  "A container that is 480px wide on desktop and tablet, but squishes on mobile"
  ([content]
   (component/html
    [:div.container
     [:div.m-auto.col-8-on-tb.col-6-on-dt
      content]]))
  ;; Please don't use this form anymore, exists for backwards compatibility reasons
  ([main & content]
   (narrow-container [:div main (map-indexed (fn [[i v]]
                                               [:div {:key (str "i-" i)}
                                                v])
                                             content)])))

(defn forward-caret [{:keys [height width color]}]
  (svg/dropdown-arrow {:class  (if color (str "stroke-" color) "stroke-black")
                       :height height
                       :width  width
                       :style  {:transform "rotate(-90deg)"}}))

(defn back-caret
  [back-copy width]
  (component/html
   [:div.flex.items-center.stroke-navy
    ^:inline
    (svg/left-caret {:width  width
                     :height width})
    [:div back-copy]]))

(def spinner
  "Spinner that fills line, assuming line-height is 1.5em"
  (component/html
   [:div.img-spinner.bg-no-repeat.bg-center.bg-contain.col-12
    {:style {:height "1.5em"}}]))

(defn large-spinner [attrs]
  (component/html
   [:div.img-large-spinner.bg-center.bg-contain.bg-no-repeat.col-12
    (merge {:data-test "spinner"}
           attrs)]))

(defn built-loading-component [data opts]
  (large-spinner {:style {:height "6em"}}))

(defn lazy-load-component
  "Lazily loads a given component in a different google closure module. Server-side render is unchanged.

  Parameters:

   - module-name is the keyword of the module to load if the component is missing. See project.clj for modules.
   - fully-qualified-built-component-symbol is a symbol of the absolute namespace + function to use as the component when the module is loaded.
   - for-navigation-event is the nav event that the page is of to know if a re-triggering of navigate event is necessary (ie - b/c the module loaded,
     or the user navigated elsewhere).
  "
  [module-name fully-qualified-built-component-symbol for-navigation-event]
  #?(:cljs (if (loader/loaded? module-name)
             (let [obj  (str "window."
                             (munge-str (namespace fully-qualified-built-component-symbol))
                             "."
                             (munge-str (name fully-qualified-built-component-symbol)))
                   path (string/split obj #"\.")]
               (or (js/eval (string/join " && " (map (partial string/join ".") (rest (reductions conj [] path)))))
                   (when (and (.hasOwnProperty js/window "console") js/window.console.error)
                     (js/console.error (str "Failed to load component '" obj "' in module '" module-name "'")))))
             (do
               (loader/load module-name
                            (fn []
                              (handle-message events/module-loaded {:module-name          module-name
                                                                    :for-navigation-event for-navigation-event})))
               built-loading-component))
     :clj (do (require (symbol (namespace fully-qualified-built-component-symbol)))
              (resolve fully-qualified-built-component-symbol))))

(defn aspect-ratio
  "Refer to https://css-tricks.com/snippets/sass/maintain-aspect-ratio-mixin/. This is a slight modification, adapted from the wistia player."
  ([x y content]
   [:div.relative.overflow-hidden
    {:style {:padding-top (-> y (/ x) (* 100) float (str "%"))}}
    [:div.absolute.overlay content]])
  ([x y attrs content]
   [:div.relative.overflow-hidden
    {:style {:padding-top (-> y (/ x) (* 100) float (str "%"))}}
    [:div.absolute.overlay attrs content]]))

(defn button
  [{:keys [disabled? disabled-class spinning? navigation-message href]
    :as   opts}
   content]
  (let [shref   (str href)
        attrs   (cond-> opts
                  :always                                                   (dissoc :spinning? :disabled? :disabled-class :navigation-message)
                  navigation-message                                        (merge (apply utils/route-to navigation-message))
                  (and (string/starts-with? shref "#") (> (count shref) 1)) (merge (utils/scroll-href (subs href 1)))
                  (or disabled? spinning?)                                  (assoc :on-click utils/noop-callback)
                  disabled?                                                 (assoc :data-test-disabled "yes")
                  spinning?                                                 (assoc :data-test-spinning "yes")
                  disabled?                                                 (update :class str (str " btn-disabled " (or disabled-class "is-disabled"))))
        content (if spinning? [spinner] content)]
    [:a (merge {:href "#"} attrs)
     content]))

(defn ^:private button-colors [color-kw]
  (let [color (color-kw {:color/teal        "btn-primary bg-teal white"
                         :color/navy        "btn-primary bg-navy white"
                         :color/aqua        "btn-primary bg-aqua white"
                         :color/white       "btn-primary btn-primary-teal-hover bg-white border-light-gray black"
                         :color/ghost       "btn-outline border-gray black"
                         :color/light-ghost "btn-outline border-white white"
                         :color/teal-ghost  "btn-outline border-teal teal"
                         :color/navy-ghost  "btn-outline border-navy navy"
                         :color/facebook    "btn-primary bg-fb-blue white"
                         :color/purple      "btn-primary bg-purple white"
                         :color/dark-gray   "btn-primary bg-dark-gray white"
                         :color/quadpay     "btn-primary bg-quadpay-blue white"})]
    (assert color (str "Button color " color-kw " has not been defined."))
    color))

(def ^:private predefined-height-classes
  {:small  "py1 h7 bold letter-spacing-half"
   :medium "py2"
   :large  "py3"})

(def ^:private predefined-width-classes
  {:small "px3"})

(defn button-class [color-kw {:keys [height-class width-class class]
                              :or   {width-class "col-12" height-class "py3"}}]
  (string/join " "
               ["btn h5"
                (predefined-width-classes width-class width-class)
                (predefined-height-classes height-class height-class)
                (button-colors color-kw)
                class]))

(defn color-button [color-kw attrs & content]
  (button (-> attrs
              (dissoc :width-class)
              (dissoc :height-class)
              (assoc :class (button-class color-kw attrs)))
          (into [:div] content)))

(defn teal-button [attrs & content]
  (color-button :color/teal attrs content))

(defn purple-button [attrs & content]
  (color-button :color/purple attrs content))

(defn white-button [attrs & content]
  (color-button :color/white attrs content))

(defn underline-button [attrs & content]
  (color-button :color/white attrs
                [:span.pxp3.border-bottom.border-teal.border-width-2 content]))

(defn dark-gray-button [attrs & content]
  (color-button :color/dark-gray attrs content))

(defn navy-button [attrs & content]
  (color-button :color/navy attrs content))

(defn aqua-button [attrs & content]
  (color-button :color/aqua attrs content))

(defn facebook-button [attrs & content]
  (color-button :color/facebook attrs content))

(defn ghost-button [attrs & content]
  (color-button :color/ghost attrs content))

(defn light-ghost-button [attrs & content]
  (color-button :color/light-ghost attrs content))

(defn teal-ghost-button [attrs & content]
  (color-button :color/teal-ghost attrs content))

(defn navy-ghost-button [attrs content]
  (color-button :color/navy-ghost attrs content))

(defn submit-button
  ([title] (submit-button title {}))
  ([title {:keys [spinning? disabled? data-test color-kw]
           :as attrs
           :or {color-kw :color/teal}}]
   (if spinning?
     (color-button color-kw attrs)
     [:input
      {:type "submit"
       :class (button-class color-kw attrs)
       :data-test data-test
       :value title
       :disabled (boolean disabled?)}])))

(def nbsp (component/html [:span {:dangerouslySetInnerHTML {:__html "&nbsp;"}}]))
(def rarr (component/html [:span {:dangerouslySetInnerHTML {:__html " &rarr;"}}]))
(def times (component/html [:span {:dangerouslySetInnerHTML {:__html " &times;"}}]))
(def new-flag
  (component/html
   [:div.right
    [:div.border.border-navy.navy.pp2.h8.line-height-1.medium "NEW"]]))

(defn- add-classes [attributes classes]
  (update attributes :class #(str %1 " " %2) classes))

#?(:cljs
   (defn selected-value [^js/Event evt]
     (let [elem (.-target evt)
           ^js/HTMLElement subelem (aget ^js/NodeList (.-options elem)
                                         (.-selectedIndex elem))]
       (.-value subelem))))

(defn field-error-message [error data-test]
  (when error
    [:div.error.my1.h6.center.medium
     {:data-test (str data-test "-error")}
     (or (:long-message error) nbsp)]))

(defn ^:private floating-label [label id {:keys [value?]}]
  (component/html
   [:div.absolute
    [:label.floating-label--label.col-12.h8.relative.gray.medium
     (cond-> {:for id}
       value? (add-classes "has-value"))
     label]]))

(defn ^:private field-wrapper-class [wrapper-class {:keys [error? focused?]}]
  (cond-> {:class wrapper-class}
    :always      (add-classes "rounded border x-group-item")
    focused?     (add-classes "glow")
    ;; .z1.relative keeps border between adjacent fields red if one of them is in error
    error?       (add-classes "border-error z1 relative")
    (not error?) (add-classes "border-gray")))

(defn ^:private field-class [{:as base :keys [label]} {:keys [error? value?]}]
  (cond-> base
    true                     (add-classes "floating-label--input rounded border-none")
    error?                   (add-classes "error")
    (and value? label) (add-classes "has-value")))

(defn text-input [{:keys [id type label keypath value] :as input-attributes}]
  (component/html
   [:input.h5.border-none.px2.bg-white.col-12.rounded.placeholder-dark-silver
    (merge
     {:style       {:height    "56px"
                    :font-size "16px"}
      :key         id
      :label       label
      :data-test   (str id "-input")
      :name        id
      :id          (str id "-input")
      :type        (or type "text")
      :value       (or value "")
      :placeholder label
      :on-change   #?(:clj (fn [_e] nil)
                      :cljs (fn [^js/Event e]
                              (handle-message events/control-change-state
                                              {:keypath keypath
                                               :value   (.. e -target -value)})))}
     (dissoc input-attributes :id :type :label :keypath :value))]))

(defn plain-text-field
  [label keypath value error? {:keys [wrapper-class wrapper-style id hint focused] :as input-attributes}]
  (component/html
   (let [input-attributes (dissoc input-attributes :wrapper-class :hint :focused :wrapper-style)
         hint?            (seq hint)
         focused?         (= focused keypath)
         status           {:error?   error?
                           :focused? focused?
                           :hint?    hint?
                           :value?   (seq value)}]
     [:div (merge (field-wrapper-class wrapper-class status)
                  {:style wrapper-style})
      [:div.pp1.col-12
       (floating-label label id status)
       [:label
        [:input.col-12.h4.line-height-1
         (field-class (merge {:key         id
                              :placeholder label
                              :label       label
                              :value       (or value "")}
                             #?(:cljs
                                {:on-focus
                                 (fn [^js/Event e]
                                   (handle-message events/control-focus {:keypath keypath}))
                                 :on-blur
                                 (fn [^js/Event e]
                                   (handle-message events/control-blur {:keypath keypath}))
                                 :on-change
                                 (fn [^js/Event e]
                                   (handle-message events/control-change-state
                                                   {:keypath keypath
                                                    :value   (.. e -target -value)}))})
                             input-attributes)
                      status)]
        (when hint? [:div.py1.px2
                     (when error? {:class "error"})
                     hint])]]])))

(defn hidden-field
  [{:keys [keypath type disabled? checked?] :as attributes}]
  (component/html
   (let [args    (dissoc attributes :keypath)
         handler (utils/send-event-callback keypath args)]
     [:input.hide
      (cond-> {:type type :on-change handler}
        disabled?
        (assoc :disabled true)
        checked?
        (assoc :checked true))])))

(defn text-field [{:keys [label keypath value errors data-test class] :as input-attributes :or {class "col-12"}}]
  (component/html
   (let [error (first errors)]
     [:div.mb2.stacking-context {:class class}
      (plain-text-field label keypath value (not (nil? error))
                        (dissoc input-attributes :label :keypath :value :errors))
      (field-error-message error data-test)])))

(defn input-group [{:keys [label keypath value errors data-test class] :as input-attributes :or {class "col-12"}}
                   {:keys [ui-element args content]}]
  (component/html
   (let [error (first errors)]
     [:div.mb2.stacking-context
      [:div.flex.justify-around
       (plain-text-field label keypath value (not (nil? error))
                         (-> input-attributes
                             (dissoc :label :keypath :value :errors)
                             (update :wrapper-class str " rounded-left x-group-item")))
       (ui-element (update args :class str " rounded-right x-group-item") content)]
      (field-error-message error data-test)])))

(defn pill-group [{:keys [label keypath value errors data-test class] :as input-attributes :or {class "col-12"}}
                  {:keys [ui-element args content]}]
  (component/html
   (let [error (first errors)]
     [:div.mb2.stacking-context
      [:div.flex.justify-around
       (plain-text-field label keypath value (not (nil? error))
                         (-> input-attributes
                             (dissoc :label :keypath :value :errors)
                             (update :wrapper-class str " x-group-item")))
       (ui-element (update args :class str " x-group-item") content)]
      (field-error-message error data-test)])))

(defn text-field-group
  "For grouping many fields on one line. Sets up columns, rounding of
  first and last fields, and avoids doubling of borders between fields.

  column expects the css grid column, but as a vector:
  [''2fr'' ''1fr'']

  areas expects a vector of the field ids for their positions:
  [''field-id1'' ''field-id2'']"
  [& fields]
  {:pre [(zero? (rem 12 (count fields)))
         (every? (comp string? :id) fields)]}
  (let [areas        (map :id fields)
        columns      (map (fn [field] (get field :column-size "1fr")) fields)
        some-errors? (some (comp seq :errors) fields)]
    [:div.mb2
     (into [:div.clearfix.stacking-context
            {:style {:display               :grid
                     :grid-template-columns (string/join " " columns)
                     :grid-template-areas   (str "'"
                                                 (string/join " " areas)
                                                 "' '"
                                                 (string/join " " (map (fn [c] (str c "--error")) areas))
                                                 "'")}}]
           (concat
            (for [[idx {:keys [label keypath value errors id] :as field}]
                  (map-indexed vector fields)]
              (let [first?        (zero? idx)
                    last?         (= idx (dec (count fields)))
                    wrapper-class (str (when first? " rounded-left")
                                       (when last? " rounded-right"))]
                (plain-text-field
                 label keypath value (seq errors)
                 (-> field
                     (dissoc :label :keypath :value :errors)
                     (assoc :wrapper-class wrapper-class)
                     (assoc :wrapper-style {:grid-area id})))))
            (for [{:keys [errors data-test error-style id]} fields]
              [:div {:style {:grid-area (str id "--error")}}
               (cond
                 (seq errors) (field-error-message (first errors) data-test)
                 some-errors? nbsp
                 :else        nil)])))]))

(def ^:private custom-select-dropdown
  (component/html
   [:div.absolute.floating-label--icon
    ^:inline (svg/dropdown-arrow {:class "stroke-gray"
                                  :style {:width "1.2em" :height "1em"}})]))

(defn ^:private plain-select-field
  [label keypath value options error? {:keys [id placeholder] :as select-attributes}]
  (component/html
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
                            :label       ""
                            :value       (or value "")}
                           #?(:clj nil
                              :cljs {:on-change #(handle-message events/control-change-state
                                                                 {:keypath keypath
                                                                  :value   (selected-value %)})})
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
      custom-select-dropdown])))

(defn select-field [{:keys [label keypath value options errors data-test div-attrs] :as select-attributes}]
  (component/html
   (when (seq options) ;; Hacky fix to get around React not invalidating the element if only options change
     (let [error (first errors)]
       [:div.col-12.mb2.stacking-context
        div-attrs
        (plain-select-field label keypath value options (not (nil? error))
                            (dissoc select-attributes :label :keypath :value :options :errors :div-attrs))
        (field-error-message error data-test)]))))

(defn check-box [{:keys [label data-test errors keypath value label-classes disabled] :as attributes}]
  (component/html
   [:div.col-12.mb2
    [:label.flex.items-center
     (merge {:class label-classes}
            (when data-test
              {:data-test (str "label-" data-test)}))
     ;; 15px svg + 2*2px padding + 2*1px border = 21px
     [:div.border.left.mr3.pp2
      (when disabled
        {:class "border-gray"})
      (if value
        (svg/simple-x {:class        "block teal"
                       :width        "15px"
                       :height       "15px"})
        [:div {:style {:width "15px" :height "15px"}}])]
     [:input.hide
      (merge (utils/toggle-checkbox keypath value)
             (dissoc attributes :label :keypath :value :label-classes)
             {:type "checkbox"})]
     [:span
      (when disabled {:class "gray"})
      label]]
    (when-let [error (first errors)]
      (field-error-message error data-test))]))

(defn radio-section [radio-attrs & content]
  (component/html
   (let [k (:key radio-attrs)
         radio-attrs (dissoc radio-attrs :key)]
     [:label.flex.items-center.col-12.py1
      (when k {:key k})
      [:input.mx2.h2
       (merge {:type "radio"}
              radio-attrs)]
      (into [:div.clearfix.col-12]
            content)])))

(defn radio-group [{:keys [group-name keypath checked-value] :as attributes} options]
  (component/html
   [:div
    (for [{:keys [id label value] :as option} options]
      [:label
       {:key (str group-name id)}
       [:input.mx2.h2
        (merge {:type         "radio"
                :name         group-name
                :data-test    group-name
                :data-test-id id
                :id           (str group-name id)
                :on-change    #(handle-message events/control-change-state
                                               {:keypath keypath
                                                :value   value})}
               (when (= checked-value value)
                 {:checked (= checked-value value)})
               (dissoc attributes
                       :group-name :keypath :checked-value
                       :id :checked :data-test-id))]
       [:span label]])]))

(defn drop-down [expanded? menu-keypath [link-tag & link-contents] menu]
  (component/html
   [:div.pointer
    (into [link-tag
           (utils/fake-href events/control-menu-expand {:keypath menu-keypath})]
          link-contents)
    (when expanded?
      [:div.relative.z4
       {:on-click #(handle-message events/control-menu-collapse-all)}
       [:div.fixed.overlay]
       menu])]))

(defn modal [{:keys [close-attrs bg-class col-class] :or {col-class "col-11 col-7-on-tb col-5-on-dt"}} & body]
  (component/html
   ;; The scrim
   [:div.z5.fixed.overlay.bg-darken-4
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
             body)]]]]))

(defn modal-close [{:keys [class data-test close-attrs]}]
  (component/html
   [:div.clearfix
    {:data-scrollable "not-a-modal"}
    [:a.h3.right (merge {:data-test data-test :title "Close"} close-attrs)
     (svg/close-x {:class (or class "stroke-white fill-gray")})]]))

;; TODO(ellie) Replace with svg version
(defn big-x [{:keys [data-test attrs]}]
  (component/html
   [:div {:style {:width "70px"}}
    [:div.relative.rotate-45.p2 (merge  {:style     {:height "70px"}
                                         :data-test data-test}
                                        attrs)
     [:div.absolute.border-right.border-dark-gray {:style {:width "25px" :height "50px"}}]
     [:div.absolute.border-bottom.border-dark-gray {:style {:width "50px" :height "25px"}}]]]))

(defn square-image [{:keys [resizable-url]} size]
  (some-> resizable-url (str "-/scale_crop/" size "x" size "/center/")))

(defn ucare-img-id [url-or-image-id]
  (if (string/includes? (str url-or-image-id) "ucarecdn.com")
    (last (butlast (string/split url-or-image-id #"/" 5)))
    url-or-image-id))

(defn ucare-img
  [{:as   img-attrs
    :keys [width retina-quality default-quality]
    :or   {retina-quality  "lightest"
           default-quality "normal"}}
   image-id]
  {:pre [(or (spice.core/parse-int width) (nil? width))]}
  (let [width       (spice/parse-int width)
        image-id    (ucare-img-id image-id)
        retina-url  (cond-> (str "//ucarecdn.com/" image-id "/-/format/auto/-/quality/" retina-quality "/")
                      width (str "-/resize/" (* 2 width) "x/"))
        default-url (cond-> (str "//ucarecdn.com/" image-id "/-/format/auto/-/quality/" default-quality "/")
                      width (str "-/resize/" width "x/"))]
    [:picture {:key image-id}
     [:source {:src-set (str retina-url " 2x,"
                             default-url " 1x")}]
     [:img (-> img-attrs
               (dissoc :width :retina-quality :default-quality)
               (assoc :src default-url))]]))

(defn circle-ucare-img
  [{:keys [width] :as attrs :or {width "4em"}} image-id]
  (let [width (spice/parse-int width)
        size {:style {:height (str width "px") :width (str width "px")}}]
    [:div.circle.bg-light-gray.overflow-hidden.relative
     (merge size
            (dissoc attrs :width))
     (if image-id
       (ucare-img attrs image-id)
       (svg/missing-portrait size))]))

(defn circle-picture
  ([src] (circle-picture {} src))
  ([{:keys [width overlay-copy] :as attrs :or {width "4em"}} src]
   [:div.circle.bg-light-gray.overflow-hidden.relative
    (merge {:style {:width width :height width}}
           (dissoc attrs :width :overlay-copy))
    (if src
      [:img {:style {:width width :height width} :src src}]
      ^:inline (svg/missing-portrait {:width width :height width}))
    (when overlay-copy
      [:div.absolute.overlay.bg-darken-2
       [:div.absolute.m-auto.overlay {:style {:height "50%"}} overlay-copy]])]))

(defn img-attrs [img size]
  {:src (get img (keyword (str (name size) "_url")))
   :alt (:alt img)})

;; GROT: Will be rendered useless post consolidated-cart
(defn ^:private counter-button [spinning? data-test f content]
  [:a.col.inherit-color
   {:href "#"
    :data-test data-test
    :disabled spinning?
    :on-click (if spinning? utils/noop-callback f)}
   content])

(defn ^:private counter-value [spinning? value]
  [:div.left.center.mx1 {:style {:width "1.2em"}
                         :data-test "line-item-quantity"}
   (if spinning? spinner value)])

(defn counter [{:keys [data-test spinning?]} value dec-fn inc-fn]
  [:div
   (counter-button spinning? (str "quantity-dec-" data-test) dec-fn (svg/counter-dec {:title "Decrement cart item count"}))
   (counter-value spinning? value)
   (counter-button spinning? (str "quantity-inc-" data-test) inc-fn (svg/counter-inc {:title "Increment cart item count"}))])

;; GROT: Will be rendered useless post consolidated-cart
(defn auto-complete-counter [{:keys [data-test spinning?]} value dec-fn inc-fn]
  [:div.medium
   (counter-button spinning? (str "quantity-dec-" data-test) dec-fn
                   (svg/minus-sign {:height "28px"
                                    :width  "30px"}))
   (counter-value spinning? value)
   (counter-button spinning? (str "quantity-inc-" data-test) inc-fn
                   (svg/plus-sign {:height "28px"
                                   :width  "30px"}))])

(defn ^:private consolidated-cart-counter-button [spinning? data-test f content]
  [:a.col.inherit-color.flex.items-center
   {:href "#"
    :data-test data-test
    :disabled spinning?
    :on-click (if spinning? utils/noop-callback f)}
   content])

(defn consolidated-cart-auto-complete-counter [{:keys [data-test spinning?]} value dec-fn inc-fn]
  [:div.medium.flex.items-center
   (consolidated-cart-counter-button spinning? (str "quantity-dec-" data-test) dec-fn
                                     (svg/minus-sign {:height "18px"
                                                      :width  "20px"}))
   (counter-value spinning? value)
   (consolidated-cart-counter-button spinning? (str "quantity-inc-" data-test) inc-fn
                                     (svg/plus-sign {:height "18px"
                                                     :width  "20px"}))])

(defn note-box [{:keys [color data-test]} contents]
  [:div.border.rounded
   {:class (str "bg-" color " border-" color)
    :data-test data-test}
   [:div.bg-lighten-4.rounded
    contents]])

(defn big-money [amount]
  (component/html
   [:div.flex.justify-center.line-height-1
    (mf/as-money-without-cents amount)
    [:span.h5 {:style {:margin "5px 3px"}} (mf/as-money-cents-only amount)]]))

;; To be deprecated
(defn progress-indicator [{:keys [value maximum]}]
  (component/html
   (let [bar-value (-> value (/ maximum) (* 100.0) (min 100))
         bar-width (str (numbers/round bar-value) "%")
         bar-style {:padding-top "0.3em" :padding-bottom "0.15em" :height "20px"}]
     [:div.flex.items-center
      [:div.my2.border.border-dark-gray.capped.h4.flex-auto
       (cond
         (zero? value)     [:div.px2.capped {:style bar-style}]
         (= value maximum) [:div.bg-teal.px2.capped {:style bar-style}]
         :else             [:div.bg-teal.px2.capped {:style (merge bar-style {:width bar-width})}])]
      (if (= value maximum)
        ^:inline (svg/circled-check {:class "stroke-teal"
                                     :style {:width        "3rem" :height "3rem"
                                             :margin-left  "0.4rem"
                                             :margin-right "-0.2rem"}})
        [:div.border.circle.border-dark-gray.h6.center.ml1
         {:style {:height "2.66667rem" :width "2.66667rem" :line-height "2.666667rem"}}
         bar-width])])))

(defn shopping-bag [opts {:keys [quantity]}]
  (component/html
   [:a.relative.pointer.block (merge (utils/route-to events/navigate-cart)
                                     opts)
    ^:inline (svg/bag {:class (str "absolute overlay m-auto "
                                   (if (pos? quantity) "fill-navy" "fill-black"))})
    (when (pos? quantity)
      [:div.absolute.overlay.m-auto {:style {:height "9px"}}
       [:div.center.navy.h6.line-height-1 {:data-test (:data-test opts)} quantity]])]))

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

  Within the `image` hashmap, the `resizable-url` will be used with the CDN
  image operations. The `resizable-filename` will be appended to all image URLs,
  for SEO. And the `alt` will be put on all img tags."
  [lq-width lq-height image]
  (let [{:keys [resizable-url resizable-filename alt]} image

        lq-url (str resizable-url "-/resize/" lq-width "x" lq-height "/-/quality/lighter/" resizable-filename)
        hq-url (str resizable-url resizable-filename)]
    [:picture.overflow-hidden.bg-cover.block.relative
     {:style {:background-image (assets/css-url lq-url)
              :padding-top      (-> lq-height (/ lq-width) (* 100) float (str "%"))}}
     [:img.col-12.absolute.overlay {:src   lq-url
                                    :alt   alt
                                    :style {:filter "blur(10px)"}}]
     (images/platform-hq-image {:src hq-url :alt alt})
     [:noscript [:img.col-12.absolute.overlay {:src hq-url :alt alt}]]]))

(def ^:private contentful-type->mime
  {"jpg" "image/jpeg"
   "webp" "image/webp"})

(defn ^:private build-src-set-url [url format [rule query]]
  (-> (url/url (str "https:" url))
      (assoc :query query)
      (assoc-in [:query :fm] format)
      (str " " rule)))

(defn ^:private build-src-set [url format src-set]
  (->> (map (partial build-src-set-url url format) src-set)
       (string/join ", ")))

;; TODO(jeff): GROT
(defn source
  "For contentful only."
  [url {:as attrs :keys [src-set type media]}]
  (assert (contains? contentful-type->mime type)
          (str "[ui/source] Invalid contentful format:" type
               "; must be one of:" (keys contentful-type->mime)))
  (component/html
   [:source (merge attrs
                   {:key     (str media ":" url ":" type)
                    :src-set (build-src-set url type src-set)
                    :type    (contentful-type->mime type)})]))

(defn option
  [{:keys [key disabled? height on-click]} & content]
  [:a.block.relative.my1.mx2
   (merge
    {:key      key
     :style    {:height height}
     :on-click (if disabled? utils/noop-callback on-click)}
    (when disabled?
      {:data-test-disabled "disabled"}))
   [:div.absolute.overlay.rounded-0.border.border-gray]
   (for [[i c] (map-indexed vector content)]
     [:div {:key (str i)} c])])

(def header-image-size 36)

(defn expand-icon [expanded?]
  (component/html
   [:img {:style {:width "8px"}
          :src   (if expanded?
                   "//ucarecdn.com/6cfa1b3c-ed89-4e71-b702-b6fdfba72c0a/-/format/auto/collapse"
                   "//ucarecdn.com/dbdcce35-e6da-4247-be57-22991d086fc1/-/format/auto/expand")}]))

(defn back-arrow [attrs]
  (ucare-img (merge {:width "24"}
                    attrs)
             "2dba0ec5-f62b-4aad-b122-68fdf3eba9dc"))

(def dark-forward-arrow-uuid
  "e4a70b53-905f-449b-86f1-e8d5c327a6df")

(defn dark-back-arrow [attrs]
  [:div.rotate-180
   (ucare-img (merge {:width "24"}
                     attrs)
              dark-forward-arrow-uuid)])

(def light-forward-arrow-uuid
  "f6d9ea1b-5a27-4582-a422-f25a1e5ba22e")

(defn forward-arrow [attrs]
  (ucare-img (merge {:width "24"}
                    (dissoc attrs :disabled?))
             (if (:disabled? attrs)
               dark-forward-arrow-uuid
               light-forward-arrow-uuid)))

(defn clickable-logo [{:as attrs :keys [height event]}]
  [:a.block.img-logo.bg-no-repeat.bg-center.bg-contain.teal
   (merge {:style     {:height height}
           :title     "Mayvenn"
           :content   (assets/path "/images/header_logo.svg")}
          (when event (utils/route-to event))
          (dissoc attrs :height :event))])

(defn pluralize-with-amount
  ([cnt singular] (pluralize-with-amount cnt singular (str singular "s")))
  ([cnt singular plural]
   (str cnt " " (if (= 1 (max cnt (- cnt))) singular plural))))

(defn pluralize
  ([cnt singular] (pluralize cnt singular (str singular "s")))
  ([cnt singular plural]
   (if (= 1 (max cnt (- cnt))) singular plural)))

(defn phone-url [tel-num]
  (str "tel://+" (numbers/digits-only tel-num)))

(defn email-url [email]
  (str "mailto:" email))

(defn email-link [tag attrs & body]
  [tag (assoc attrs :href (email-url (last body))) (apply str body)])

(defn phone-link [tag attrs & body]
  [tag (assoc attrs :href (phone-url (last body))) body])

(defn sms-url [tel-num]
  ;; Android cannot detect shortcodes properly if you're using sms://+34649 or sms://34649
  ;; But does work if you only have sms:34649.
  (str "sms:" (numbers/digits-only tel-num)))

(defn sms-link [tag attrs & body]
  [tag (assoc attrs :href (sms-url (last body))) (apply str body)])

(defn youtube-responsive
  "Explanation: https://www.ostraining.com/blog/coding/responsive-videos/"
  [url]
  (component/html
   [:div.col-12.relative
    {:style {:height         "0"
             :padding-bottom "56.25%"}}
    [:iframe.col-12.absolute.left-0.top-0
     {:style           {:height "100%"}
      :src             url
      :frameBorder     0
      :allowFullScreen true}]]))

(defmulti link (fn [link-type & _] link-type))
(defmethod link :link/email [link-type tag attrs & body]
  (apply email-link tag attrs body))
(defmethod link :link/phone [link-type tag attrs & body]
  (apply phone-link tag attrs body))
(defmethod link :link/sms [link-type tag attrs & body]
  (apply sms-link tag attrs body))

(defn ^:private star [type index]
  [:span.mrp1
   {:key (str (name type) "-" index)}
   (ucare-img
    {:width "13"}
    (case type
      :whole         "6eaf883d-2cc7-4f52-aee7-292330944c67"
      :three-quarter "e7b3d754-a0c4-4ff2-969d-6eac684ce16f"
      :half          "44815567-e66c-4375-a46b-e5624a122646"
      :empty         "9f04c257-1b27-4039-8d53-1dd63c44653c"
      nil))])

(defn rating->stars [rating]
  (let [remainder-rating (mod rating 1)
        whole-stars      (map (partial star :whole) (range (int rating)))
        partial-star     (cond
                           (<= remainder-rating 0.2)
                           nil

                           (< 0.2 remainder-rating 0.7)
                           (star :half "half")

                           (<= 0.7 remainder-rating)
                           (star :three-quarter "three-quarter")

                           :else
                           nil)
        empty-stars (map
                     (partial star :empty)
                     (range
                      (- 5
                         (count whole-stars)
                         (if partial-star 1 0))))]
    ;; TODO: vec?
    {:whole-stars  whole-stars
     :partial-star partial-star
     :empty-stars  empty-stars}))

(defn star-rating
  [rating]
  (let [{:keys [whole-stars partial-star empty-stars]} (rating->stars rating)]
    [:div.flex.items-center
     whole-stars
     partial-star
     empty-stars
     [:span.mlp2 rating]]))

(def ^:private screen-aware-component
  #?(:clj (fn [data owner {:keys [embed opts]}]
            (component/create [:div
                               (component/build embed
                                                (assoc data
                                                       :screen/seen? nil
                                                       :screen/visible? nil)
                                                opts)]))
     :cljs (component/create-dynamic
            "screen-aware-component"
            (constructor [this props]
                         (component/create-ref! this "trigger")
                         {:seen?    false
                          :visible? nil})
            (did-mount [this]
                       (if (.hasOwnProperty js/window "IntersectionObserver")
                         (when-let [ref (component/get-ref this "trigger")]
                           (let [{:screen/keys [root-margin]} (component/get-opts this)
                                 observer                     (js/IntersectionObserver.
                                                               (fn [entries observer]
                                                                 (doseq [entry entries]
                                                                   (when (= (.-target entry)
                                                                            ref)
                                                                     (if (.-isIntersecting entry)
                                                                       (do
                                                                         (component/set-state! this
                                                                                               :seen? true
                                                                                               #_#_:visible? true))
                                                                       #_(component/set-state! this :visible? false)))))
                                                               #js {:delay      100
                                                                    :rootMargin (or root-margin "25px")})]
                             (gobj/set this "observer" observer)
                             (.observe observer ref)))
                         (do
                           ;; No API, lie about it
                           (component/set-state! this
                                                 :seen? true
                                                 :visible? true))))
            (will-unmount [this]
                          (let [observer (gobj/get this "observer")
                                element  (component/get-ref this "trigger")]
                            (when (and observer element)
                              (.unobserve observer element)
                              (.disconnect observer))))
            (render [this]
                    (let [trigger                            (component/use-ref this "trigger")
                          {:keys [seen? visible?] :as state} (component/get-state this)
                          data                               (component/get-props this)
                          {:keys [embed opts]}               (component/get-opts this)]
                      (component/html
                       [:div {:ref trigger}
                        (when-not seen? nbsp)  ; When the content has no height, isIntersecting is always false.
                        (component/build embed
                                         (assoc data
                                                :screen/seen? seen?
                                                :screen/visible? visible?)
                                         (merge {:key "embed"} opts))]))))))

(defn screen-aware
  "A decorator around component/build that sets the screen information data to
  the underlying component.

  Extra keys associated into the data:

   :screen/seen? (bool) if the user's screen has this element on the screen at
      least once

  If :screen/seen? and :screen/visible? is nil, then the component is under a
  server-side render.

  Note that this is best-effort and not a guarantee of accuracy. This requires
  browsers to support the IntersectionObserver APIs. A browser that does not support
  this API will have :screen/seen? and :screen/visible? both sets to true.
  "
  ([component] (screen-aware component nil nil))
  ([component data] (screen-aware component data nil))
  ([component data opts]
   (component/build
    screen-aware-component
    data
    (merge
     (when (:key opts)
       {:key (:key opts)})
     {:opts {:embed component
             :opts  opts}}))))

(defcomponent ^:private defer-ucare-img-component [{:screen/keys [seen?] :keys [id attrs]} owner opts]
  (let [placeholder-attrs (select-keys attrs [:class :width :height])]
    (cond
      seen? (ucare-img attrs id)
      :else [:div placeholder-attrs])))

(defn defer-ucare-img
  "A particular instance of screen-aware that only loads the image when the
  screen can render (or almost render) this content.

  Server side renders this via noscript.
  "
  [{:as   img-attrs
    :keys [width retina-quality default-quality]
    :or   {retina-quality  "lightest"
           default-quality "normal"}}
   image-id]
  (screen-aware defer-ucare-img-component {:attrs img-attrs
                                           :id    image-id}
                {:key image-id}))

(defn navigator-share
  [title text url]
  #?(:clj nil
     :cljs
     (when js/navigator.share
       [:a
        {:onClick #(try
                     (doto (js/navigator.share (clj->js {:title title
                                                         :text  text
                                                         :url   url}))
                       (.then  (fn [v] (prn "success!" v)))
                       (.catch (fn [v] (prn "fail!"    v))))
                     (catch js/Error error
                       (js/alert "we caught an error")))}
        (svg/share-icon {:height "19px"
                         :width  "18px"})])))
