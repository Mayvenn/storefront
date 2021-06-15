(ns storefront.components.picker.picker-two
  "Exists temporarily to allow us to refactor the interface for use in varying contexts"
  (:require #?@(:cljs [[storefront.browser.scroll :as scroll]])
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.css-transitions :as css-transitions]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]))

(def ^:private checkmark-circle
  [:div.circle.bg-s-color.flex.items-center.justify-center
   {:style {:height "24px" :width "24px"}}
   (svg/check-mark {:height "12px" :width "16px" :class "fill-white"})])

(defn ^:private simple-selected-layer []
  [:div.absolute.border.border-width-3.border-s-color.overlay.flex
   [:div.flex.items-center.justify-end.pr4.col-12
    checkmark-circle]])

(defn ^:private simple-sold-out-layer [text]
  [:div.bg-darken-1.absolute.overlay.flex.justify-end
   [:div.self-center.flex.items-center.mr4
    text]])

(defn ^:private simple-content-layer [content]
  [:div.flex.items-center.p4.absolute.overlay.bg-white
   [:div.ml1 content]])

(def ^:private picker-chevron
  (ui/forward-caret {:width  18
                     :height 18}))

(defn ^:private invisible-select [{:keys [on-change options value]}]
  [:select.absolute.invisible-select.overlay
   {:on-change on-change
    :value     value
    :style     {:opacity 0
                :border  "none"
                :outline "none"
                :width   "100%"
                :height  "100%"}}
   options])

(defn ^:private picker-face
  [{:picker-face/keys [id primary-attrs image-attrs label image-src]}]
  [:div.col-12.flex.justify-between.items-center
   (merge primary-attrs {:style {:height "100%"}})
   (ui/img
    (merge image-attrs
           {:src         image-src
            :width       "63"
            :square-size 63
            :max-size    100}))
   [:div.ml2.col-12 {:data-test id} label]
   [:div.self-center ^:inline picker-chevron]])

(defn component
  [{:keys
    [id
     value-id
     primary
     image-src
     selected-value
     open-target
     options
     primary-attrs
     image-attrs]
    [selection-event selection-args] :selection-target}]
  (let [face-data #:picker-face{:id            value-id
                                :primary-attrs primary-attrs
                                :image-attrs   image-attrs
                                :image-src     image-src
                                :label         primary}]
    [:div.bg-white.my2.p3
     {:key id}
     [:div.hide-on-tb-dt
      (merge {:data-test id}
             (apply utils/fake-href open-target))
      (picker-face face-data)]
     [:div.hide-on-mb.relative.col-12
      (invisible-select
       {:value     selected-value
        :on-change #(messages/handle-message selection-event
                                             (assoc selection-args :value (.-value (.-target %))))
        :options   (map (fn [{:option/keys [value label available?]}]
                          [:option (merge {:value value
                                           :label label
                                           :key   (str id "-" value)}
                                          (when-not available?
                                            {:disabled true}))])
                        options)})
      (picker-face (update face-data :id str "-desktop"))]]))

(defn ^:private length-option
  [{:option/keys
    [id
     label
     checked?
     selection-target
     label-attrs]}]
  [:div {:key       id
         :data-test id}
   (ui/option (merge
               {:height   "4em"
                :key      (str id "-option")
                :href     "#"}
               (when selection-target
                 {:on-click (apply utils/send-event-callback selection-target)}))
              (simple-content-layer
               [:div
                (merge {:key "primary-label"}
                       label-attrs)
                label])
              [:div
               (when checked?
                 (simple-selected-layer))])])

(defn ^:private swatch-content-layer-two
  [{:option/keys [bg-image-src image-src label checked?]}]
  [:div.flex.flex-column.bg-white
   [:div.flex
    [:div.rounded-top-left.bg-repeat-x
     {:style
      {:width            "100%"
       :height           "100px"
       :background-size  "contain"
       :background-image (str "url(" bg-image-src ")")}}]
    (ui/img {:class    "rounded-top-right"
             :height   "100px"
             :width    "110"
             :src      image-src
             :max-size 110})]
   [:div.py1.h6.ml3.self-start
    (when checked? {:class "bold"}) label]])

(defn ^:private color-option
  [{:option/keys
    [id
     checked?
     selection-target]
    :as option}]
  [:div {:key       id
         :data-test id}
   (ui/option {:on-click (apply utils/send-event-callback selection-target)}
              (swatch-content-layer-two option)
              [:div
               (when checked?
                 [:div.absolute.border.border-width-3.rounded-0.border-cool-gray.overlay.flex
                  [:div.self-center.flex.items-center
                   {:style {:margin-left "-2em"}}
                   [:div {:style {:width "1em"}}]
                   [:div.circle  ; checkmark circle
                    (ui/ucare-img {:width           "30"
                                   :retina-quality  "better"
                                   :default-quality "better"}
                                  "9e2a48b3-9811-46d2-840b-31c9f85670ad")]]])])])

(defn ^:private slide-animate [enabled? content]
  (css-transitions/transition-group
   {:classNames    "picker"
    :key           "picker"
    :in            (boolean enabled?)
    :unmountOnExit true
    :timeout       1000}
   (component/html (or content [:div]))))

(defn ^:private title-cta [{:title-cta/keys
                  [primary target id]}]
  (when id
    (ui/button-small-underline-primary
     (assoc
      (apply utils/fake-href target)
      :data-test id)
     primary)))


"picker dialog as in https://app.zeplin.io/project/5a9f159069d48a4c15497a49/screen/5b15c08f4819592903cb1348"
(component/defdynamic-component ^:private picker-dialog
  (did-mount [_]
    #?(:cljs (scroll/disable-body-scrolling)))
  (will-unmount [_]
    #?(:cljs (scroll/enable-body-scrolling)))
  (render [this]
          (let [{:keys [title items wrap? close-target]
                 :as data}
                (component/get-props this)
                {:keys [cell-component-fn]} (component/get-opts this)]
            (component/html
             [:div.hide-on-tb-dt.z5.fixed.overlay.overflow-auto.bg-cool-gray
              {:key (str "picker-dialog-" title) :data-test "picker-dialog"}
              [:div.py3.pl3.pr1.content-2.proxima.bg-white.relative.border-bottom.border-gray.flex.justify-between.items-center
               {:style {:height "55px"}}
               [:div.self-start
                {:style {:min-width "96px"}}
                (title-cta data)]
               [:div.self-center.flex-grow-1.center title]

               [:div.flex.justify-end
                {:style {:min-width "96px"}}
                [:a.flex.items-center.justify-center.medium
                 (merge {:style     {:height "55px" :width "55px"}
                         :data-test "picker-close"}
                        (apply utils/fake-href close-target))
                 (svg/x-sharp {:height "12px" :width "12px"})]]]
              [:div.py3.px1 ;; body
               (when wrap?
                 {:class "flex flex-wrap"})
               (mapv cell-component-fn items)]]))))

(defcomponent modal
  [{:picker-modal/keys
    [options
     visible?
     title
     close-target]
    picker-type :picker-modal/type} _ _]
  [:div
   (slide-animate
    visible?
    (condp = picker-type
      :hair/color
      (component/build
       picker-dialog {:title             title
                      :items             options
                      :wrap?             false
                      :close-target      close-target}
       {:opts {:cell-component-fn color-option}})


      :hair/length
      (component/build
       picker-dialog {:title             title
                      :close-target      close-target
                      :items             options}
       {:opts {:cell-component-fn length-option}})

      nil))])
