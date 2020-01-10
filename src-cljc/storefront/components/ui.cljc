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

(defn forward-caret [opts]
  (svg/dropdown-arrow (merge {:height 14
                              :width  14
                              :style  {:transform "rotate(-90deg)"}}
                             opts)))

(defn back-caret [opts]
  (svg/dropdown-arrow (merge {:height 14
                              :width  14
                              :style  {:transform "rotate(90deg)"}}
                             opts)))

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
   (component/html
    [:div.relative.overflow-hidden
     {:style {:padding-top (-> y (/ x) (* 100) float (str "%"))}}
     [:div.absolute.overlay content]]))
  ([x y attrs content]
   (component/html
    [:div.relative.overflow-hidden
     {:style {:padding-top (-> y (/ x) (* 100) float (str "%"))}}
     [:div.absolute.overlay attrs content]])))

(defn button
  [additional-classes
   {:keys [disabled? disabled-class spinning? navigation-message href]
    :as   opts}
   content]
  (component/html
   (let [shref   (str href)
         attrs   (cond-> opts
                   :always                                                   (dissoc :spinning? :disabled? :disabled-class :navigation-message)
                   navigation-message                                        (merge (apply utils/route-to navigation-message))
                   (and (string/starts-with? shref "#") (> (count shref) 1)) (merge (utils/scroll-href (subs href 1)))
                   (or disabled? spinning?)                                  (assoc :on-click utils/noop-callback)
                   disabled?                                                 (assoc :data-test-disabled "yes")
                   spinning?                                                 (assoc :data-test-spinning "yes")
                   :always                                                   (update :class str " " additional-classes)
                   disabled?                                                 (update :class str (str " btn-gray btn-disabled " (or disabled-class "is-disabled"))))
         content (if spinning? [spinner] content)]
     [:a (merge {:href "#"} attrs)
      ;; FIXME: the button helper functions with & content force us to do this for consistency
      (if (seq? content)
        (into [:span] content)
        content)])))

(def ^:private button-large-primary-classes "btn-large btn-p-color button-font-1 shout")
(def ^:private disabled-button-large-primary-classes "btn-large btn-gray button-font-1 shout btn-disabled")
(defn button-large-primary              [attrs & content] (button button-large-primary-classes attrs content))
(defn button-large-secondary            [attrs & content] (button "btn-large btn-outline button-font-1 shout" attrs content))
(defn button-large-paypal               [attrs & content] (button "btn-large btn-paypal-color button-font-1 shout" attrs content))
(defn button-large-facebook-blue        [attrs & content] (button "btn-large btn-facebook-blue button-font-1 shout" attrs content))
(defn button-medium-primary             [attrs & content] (button "btn-medium btn-p-color button-font-1 shout" attrs content))
(defn button-medium-secondary           [attrs & content] (button "btn-medium btn-outline button-font-1 shout" attrs content))
(defn button-small-primary              [attrs & content] (button "btn-small btn-p-color button-font-4 shout" attrs content))
(defn button-small-secondary            [attrs & content] (button "btn-small btn-outline bg-white button-font-4 shout" attrs content))
(defn button-pill                       [attrs & content] (button "btn-pill btn-s-color button-font-3 shout" attrs content))
(defn button-medium-underline-primary   [attrs & content] (button "p-color button-font-2 shout" attrs [:span.border-bottom.border-width-2.border-p-color content]))
(defn button-medium-underline-secondary [attrs & content] (button "s-color button-font-2 shout" attrs [:span.border-bottom.border-width-2.border-s-color content]))
(defn button-medium-underline-black     [attrs & content] (button "black button-font-2 shout" attrs   [:span.border-bottom.border-width-2.border-black content]))
(defn button-small-underline-primary    [attrs & content] (button "p-color button-font-3 shout" attrs [:span.border-bottom.border-width-2.border-p-color content]))
(defn button-small-underline-secondary  [attrs & content] (button "s-color button-font-3 shout" attrs [:span.border-bottom.border-width-2.border-s-color content]))
(defn button-small-underline-black      [attrs & content] (button "black button-font-3 shout" attrs   [:span.border-bottom.border-width-2.border-black content]))

(defn submit-button
  ([title] (submit-button title {}))
  ([title {:keys [spinning? disabled? data-test]
           :as   attrs}]
   (component/html
    (if spinning?
      (button-large-primary attrs)
      [:input.col-12.bg-clear
       {:type      "submit"
        :class     (if disabled? disabled-button-large-primary-classes button-large-primary-classes)
        :data-test data-test
        :value     title
        :disabled  (boolean disabled?)}]))))

(def hyphen "‐") ; &hyphen;
(def shy (component/html [:span {:dangerouslySetInnerHTML {:__html "&shy;"}}]))
(def wbr (component/html [:wbr]))

(def nbsp (component/html [:span {:dangerouslySetInnerHTML {:__html "&nbsp;"}}]))
(def rarr (component/html [:span {:dangerouslySetInnerHTML {:__html " &rarr;"}}]))
(def times (component/html [:span {:dangerouslySetInnerHTML {:__html " &times;"}}]))

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
    [:div.error.my1.h6.medium
     {:data-test (str data-test "-error")}
     (or (some-> error :long-message string/capitalize) nbsp)]))

(defn ^:private floating-label [label id {:keys [value?]}]
  (component/html
   [:div.absolute
    [:label.floating-label--label.col-12.h8.relative.gray.medium
     (cond-> {:for id}
       value? (add-classes "has-value"))
     label]]))

(defn ^:private field-wrapper-class [{:as base :keys [wrapper-class large?]}
                                     {:as status :keys [error? focused?]}]
  (cond-> {:class wrapper-class}
    :always      (add-classes "border x-group-item")
    large?       (add-classes "border-black")
    (not large?) (add-classes "border-gray")
    focused?     (add-classes "glow")
    ;; .z1.relative keeps border between adjacent fields red if one of them is in error
    error?       (add-classes "bg-error z1 relative")))

(defn ^:private field-class [{:as base :keys [label large?]}
                             {:as status :keys [error? value?]}]
  (cond-> base
    :always            (add-classes "bg-clear border-none")
    (and value? label) (add-classes "has-value")
    (not large?)       (add-classes "floating-label--input")
    large?             (add-classes "floating-label-large--input")
    :always            (dissoc :large?)))

(defn text-input [{:keys [id type label keypath value] :as input-attributes}]
  (component/html
   [:input.h5.border-none.px2.bg-white.col-12
    ^:attrs
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

(defn ^:private plain-text-field
  [label keypath value error? {:keys [type data-test wrapper-class wrapper-style id hint focused large?] :as input-attributes}]
  (component/html
   (let [input-attributes (dissoc input-attributes :wrapper-class :hint :focused :wrapper-style :large?)
         hint?            (seq hint)
         focused?         (= focused keypath)
         status           {:error?   error?
                           :focused? focused?
                           :hint?    hint?
                           :value?   (seq value)}]
     [:div ^:attrs (merge (field-wrapper-class {:wrapper-class wrapper-class
                                                :large?                large?} status)
                          {:style wrapper-style})
      [:div.pp1.col-12
       ^:inline (floating-label label id status)
       [:label
        [:input.col-12.line-height-1
         ^:attrs (field-class (merge {:key id
                                      :placeholder label
                                      :data-test   data-test
                                      :type        (or type "text")
                                      :label       label
                                      :name        id
                                      :value       (or value "")
                                      :large?      large?}
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
                        (-> input-attributes
                            (dissoc :label :keypath :value :errors)
                            (assoc :wrapper-class "content-2")
                            (assoc :large? false)))
      (field-error-message error data-test)])))

(defn text-field-large [{:keys [label keypath value errors data-test class] :as input-attributes :or {class "col-12"}}]
  (component/html
   (let [error (first errors)]
     [:div.mb2.stacking-context.bg-white {:class class}
      (plain-text-field label keypath value (not (nil? error))
                        (-> input-attributes
                            (dissoc :label :keypath :value :errors)
                            (assoc :wrapper-class "content-1")
                            (assoc :large? true)))
      (field-error-message error data-test)])))

(defn input-group [{:keys [label keypath value errors] :as text-input-attrs :or {class "col-12"}}
                   {:keys [args content] :as button-attrs}]
  (component/html
   (let [error (first errors)]
     [:div.mb2
      [:div.flex
       (plain-text-field label keypath value (not (nil? error))
                         (-> text-input-attrs
                             (dissoc :label :keypath :value :errors)
                             (update :wrapper-class str " x-group-item")
                             (assoc-in [:wrapper-style :border-right] "none")))
       (button-pill args content)]
      (field-error-message error "input-group")])))

(defn text-field-group
  "For grouping many fields on one line. Sets up columns
  and avoids doubling of borders between fields.

  column expects the css grid column, but as a vector:
  [''2fr'' ''1fr'']

  areas expects a vector of the field ids for their positions:
  [''field-id1'' ''field-id2'']"
  [& fields]
  {:pre [(zero? (rem 12 (count fields)))
         (every? (comp string? :id) fields)]}
  (component/html
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
               (plain-text-field
                label keypath value (seq errors)
                (-> field
                    (dissoc :label :keypath :value :errors)
                    (assoc :wrapper-style {:grid-area id}))))
             (for [{:keys [errors data-test error-style id]} fields]
               [:div {:style {:grid-area (str id "--error")}}
                (cond
                  (seq errors) (field-error-message (first errors) data-test)
                  some-errors? nbsp
                  :else        nil)])))])))

(def ^:private custom-select-dropdown
  (component/html
   [:div.absolute.floating-label--icon
    (svg/dropdown-arrow {:class "fill-gray"
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
        ^:attrs div-attrs
        (plain-select-field label keypath value options (not (nil? error))
                                     (dissoc select-attributes :label :keypath :value :options :errors :div-attrs))
        (field-error-message error data-test)]))))

(defn check-box [{:keys [label data-test errors keypath value label-classes disabled] :as attributes}]
  (component/html
   [:div.col-12.mb2
    [:label.flex.items-center
     ^:attrs (merge {:class label-classes}
                    (when data-test
                      {:data-test (str "label-" data-test)}))
     [:div.border.left.mr3.pp2
      (when disabled {:class "border-gray"})
      (if value
        (svg/x-sharp {:class  "block fill-p-color"
                      :width  "15px"
                      :height "15px"})
        [:div {:style {:width "15px" :height "15px"}}])]
     [:input.hide
      ^:attrs (merge (utils/toggle-checkbox keypath value)
                     (dissoc attributes :label :keypath :value :label-classes)
                     {:type "checkbox"})]
     [:span
      (when disabled {:class "gray"})
      label]]
    (when-let [error (first errors)]
      (field-error-message error data-test))]))

(defn radio-section
  [{:keys [checked key disabled data-test-id] :as radio-attrs} & content]
  (component/html
   (let [radio-attrs (dissoc radio-attrs :key :data-test-id)]
     [:label.flex.items-center.col-12.py1
      ^:attrs (cond-> {}
                key
                (assoc :key key)
                data-test-id
                (assoc :data-test-id data-test-id))
      [:div.left.mr3.pp2
       [:div.circle.bg-white.border.flex.items-center.justify-center
        ^:attrs (merge {:style {:height "22px" :width "22px"}}
                       (when disabled {:class "bg-cool-gray border-gray"}))
        (when checked
          [:div.circle.bg-p-color
           {:style {:height "10px" :width "10px"}}])]]
      [:input.hide.mx2.h2
       ^:attrs (merge {:type "radio"} radio-attrs)]
      (into [:div.clearfix.col-12] content)])))

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
   [:div.z6.fixed.overlay.bg-darken-4
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
    [:a.right (merge {:data-test data-test :title "Close"} close-attrs)
     (svg/x-sharp {:class (or class "block fill-dark-gray")
                   :width "15px"
                   :height "15px"})]]))

(defn big-x [{:keys [data-test attrs]}]
  (component/html
   [:div.relative.rotate-45.p2
    ^:attrs (merge  attrs
                    {:style     {:height "40px"}
                     :data-test data-test})
    [:div.absolute.border-right
     {:style {:width "25px" :height "50px"
              :border-width "2px"}}]
    [:div.absolute.border-bottom
     {:style {:width "50px" :height "25px"
              :border-width "2px"}}]]))

(defn square-image [{:keys [resizable-url]} size]
  (some-> resizable-url (str "-/scale_crop/" size "x" size "/center/")))

(defn ucare-img-id [url-or-image-id]
  (if (string/includes? (str url-or-image-id) "ucarecdn.com")
    (last (butlast (string/split url-or-image-id #"/" 5)))
    url-or-image-id))

(defn ucare-img
  [{:as   img-attrs
    :keys [width retina-quality default-quality picture-classes]
    :or   {retina-quality  "lightest"
           default-quality "normal"}}
   image-id]
  {:pre [(or (spice.core/parse-int width) (nil? width))]}
  (component/html
   (let [width       (spice/parse-int width)
         image-id    (ucare-img-id image-id)
         retina-url  (cond-> (str "//ucarecdn.com/" image-id "/-/format/auto/-/quality/" retina-quality "/")
                       width (str "-/resize/" (* 2 width) "x/"))
         default-url (cond-> (str "//ucarecdn.com/" image-id "/-/format/auto/-/quality/" default-quality "/")
                       width (str "-/resize/" width "x/"))]
     [:picture ^:attrs (merge {:key image-id}
                              (when picture-classes
                                {:class picture-classes}))
      [:source {:src-set (str retina-url " 2x," default-url " 1x")}]
      [:img ^:attrs (-> img-attrs
                        (dissoc :width :retina-quality :default-quality :picture-classses)
                        (assoc :src default-url))]])))

(defn ucare-gif2video
  [{:as   attrs
    :keys [width]}
   uuid]
  {:pre [(or (spice.core/parse-int width) (nil? width))]}
  (component/html
   [:video.hide-chromecast-icon
    ^:attrs (merge {:autoPlay              true
                    :loop                  true
                    :muted                 true
                    :webkitplaysinline     "true"
                    :playsInline           true} attrs)
    (for [format ["mp4", "webm"]]
      [:source {:src  (str "https://ucarecdn.com/" uuid "/gif2video/-/format/" format "/")
                :type (str "video/" format)}])]))

(defn circle-ucare-img
  [{:keys [width] :as attrs :or {width "4em"}} image-id]
  (component/html
   (let [width (spice/parse-int width)
         size {:style {:height (str width "px") :width (str width "px")}}]
     [:div.circle.bg-cool-gray.overflow-hidden.relative
      ^:attrs (merge size
                     (dissoc attrs :width))
      (if image-id
        (ucare-img attrs image-id)
        (svg/missing-portrait size))])))

(defn circle-picture
  ([src] (circle-picture {} src))
  ([{:keys [width overlay-copy] :as attrs :or {width "4em"}} src]
   (component/html
    [:div.circle.bg-cool-gray.overflow-hidden.relative
     ^:attrs (merge {:style {:width width :height width}}
                    (dissoc attrs :width :overlay-copy))
     (if src
       [:img {:style {:width width :height width} :src src}]
       (svg/missing-portrait {:width width :height width}))
     (when overlay-copy
       [:div.absolute.overlay.bg-darken-2
        [:div.absolute.m-auto.overlay {:style {:height "50%"}} overlay-copy]])])))

(defn img-attrs [img size]
  {:src (get img (keyword (str (name size) "_url")))
   :alt (:alt img)})

;; GROT: Will be rendered useless post consolidated-cart
(defn ^:private counter-button [spinning? data-test f content]
  (component/html
   [:a.col.inherit-color
    {:href "#"
     :data-test data-test
     :style {:border-color "gray"}
     :disabled spinning?
     :on-click (if spinning? utils/noop-callback f)}
    content]))

(defn ^:private counter-value [spinning? value]
  (component/html
   [:div.left.center.mx1.proxima.title-2 {:style     {:width "1.2em"}
                                          :data-test "line-item-quantity"}
    (if spinning? spinner value)]))

(defn counter [{:keys [data-test spinning?]} value dec-fn inc-fn]
  (component/html
   [:div
    (counter-button spinning? (str "quantity-dec-" data-test) dec-fn (svg/counter-dec {:title "Decrement cart item count"}))
    (counter-value spinning? value)
    (counter-button spinning? (str "quantity-inc-" data-test) inc-fn (svg/counter-inc {:title "Increment cart item count"}))]))

;; GROT: Will be rendered useless post consolidated-cart
(defn auto-complete-counter [{:keys [data-test spinning?]} value dec-fn inc-fn]
  (component/html
   [:div
    (counter-button spinning? (str "quantity-dec-" data-test) dec-fn
                    (svg/minus-sign {:height "18px"
                                     :width  "18px"}))
    (counter-value spinning? value)
    (counter-button spinning? (str "quantity-inc-" data-test) inc-fn
                    (svg/plus-sign {:height "18px"
                                    :width  "18px"}))]))

(defn ^:private consolidated-cart-counter-button [spinning? data-test f content]
  (component/html
   [:a.inherit-color.flex.items-center
    {:href "#"
     :data-test data-test
     :disabled spinning?
     :on-click (if spinning? utils/noop-callback f)}
    content]))

(defn consolidated-cart-auto-complete-counter [{:keys [data-test spinning?]} value dec-fn inc-fn]
  (component/html
   [:div.medium.flex.items-center
    [:div.border.border-pale-purple
     (consolidated-cart-counter-button spinning? (str "quantity-dec-" data-test) dec-fn
                                       (svg/minus-sign {:height "18px"
                                                        :width  "18px"}))]
    (counter-value spinning? value)
    [:div.border.border-pale-purple
     (consolidated-cart-counter-button spinning? (str "quantity-inc-" data-test) inc-fn
                                       (svg/plus-sign {:height "18px"
                                                       :width  "18px"}))]]))

(defn note-box [{:keys [color data-test]} contents]
  (component/html
   [:div.border
    {:class (str "bg-" color " border-" color)
     :data-test data-test}
    [:div.bg-lighten-4
     contents]]))

(defn big-money [amount]
  (component/html
   [:div.flex.justify-center.line-height-1
    (mf/as-money-without-cents amount)
    [:span.h5 {:style {:margin "5px 3px"}} (mf/as-money-cents-only amount)]]))

(defn shopping-bag
  "TODO(corey,stella) fix fonts after new fonts land
   TODO(corey,stella) fix colors after colors land"
  [opts {:keys [quantity]}]
  (component/html
   [:a.relative.pointer.block.p-color.mtn1
    ^:attrs (merge (utils/route-to events/navigate-cart)
                   opts)
    [:div.relative.container-size.flex.items-center.justify-center
     (svg/shopping-bag {:width  "33px"
                        :height "44px"})
     [:div.absolute.bold.flex.items-center.justify-center.content-3.mtp4
      {:style {:font "900 14px/17px 'Proxima Nova'"}}
      quantity]]]))

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
  (component/html
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
      [:noscript [:img.col-12.absolute.overlay {:src hq-url :alt alt}]]])))

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
   [:source ^:attrs (merge attrs
                           {:key     (str media ":" url ":" type)
                            :src-set (build-src-set url type src-set)
                            :type    (contentful-type->mime type)})]))

(defn option
  [{:keys [key disabled? height on-click]} & content]
  (component/html
   [:a.block.relative.my1.mx2
    ^:attrs (merge
             {:key      key
              :style    {:height height}
              :on-click (if disabled? utils/noop-callback on-click)}
             (when disabled?
               {:data-test-disabled "disabled"}))
    [:div.absolute.overlay.border.border-gray]
    (for [[i c] (map-indexed vector content)]
      [:div {:key (str i)} c])]))

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
  (component/html
   [:div.rotate-180
    (ucare-img (merge {:width "24"}
                      attrs)
               dark-forward-arrow-uuid)]))

(def light-forward-arrow-uuid
  "f6d9ea1b-5a27-4582-a422-f25a1e5ba22e")

(defn forward-arrow [attrs]
  (ucare-img (merge {:width "24"}
                    (dissoc attrs :disabled?))
             (if (:disabled? attrs)
               dark-forward-arrow-uuid
               light-forward-arrow-uuid)))

(defn clickable-logo [{:as attrs :keys [height event]}]
  [:a.block.img-logo.bg-no-repeat.bg-center.bg-contain.p-color
   (merge {:style     {:height height}
           :title     "Mayvenn"}
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
  (component/html
   [:span.mrp1
    {:key (str (name type) "-" index)}
    (case type
      :whole         (svg/whole-star {:height "13px" :width "13px"})
      :three-quarter (svg/three-quarter-star {:height "13px" :width "13px"})
      :half          (svg/half-star {:height "13px" :width "13px"})
      :empty         (svg/empty-star {:height "13px" :width "13px"})
      nil)]))

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
  (component/html
   (let [{:keys [whole-stars partial-star empty-stars]} (rating->stars rating)]
     [:div.flex.items-center
      whole-stars
      partial-star
      empty-stars
      [:span.mlp2 rating]])))

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
  [{:share-icon/keys [target icon]}]
  #?(:clj nil
     :cljs
     (when js/navigator.share
       [:a
        (when target
          (apply utils/fake-href target))
        icon])))
