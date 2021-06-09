(ns storefront.components.picker.picker-two
  "Exists temporarily to allow us to refactor the interface for use in varying contexts"
  (:require [storefront.component :as component :refer [defcomponent]]
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

(defn picker-face*
  [{:picker-face/keys [id primary image-src]}]
  [:div.col-12.flex.justify-between.items-center
   {:style {:height "100%"}}
   (ui/img
    {:src         image-src
     :width       "63"
     :square-size 63
     :max-size    100})
   [:div.ml2.col-12
    [:div {:data-test id} primary]]
   [:div.self-center ^:inline picker-chevron]])

(defn picker-face
  [{:keys
    [id
     value-id
     primary
     image-src
     selected-value
     open-target
     options]
    [selection-event selection-args] :selection-target}]
  (let [face-data #:picker-face{:id        value-id
                                :image-src image-src
                                :primary   primary}]
    [:div.bg-white.my2.p3
     {:key id}
     [:div.hide-on-tb-dt
      (merge {:data-test id}
             (apply utils/fake-href open-target))
      (picker-face* face-data)]
     [:div.hide-on-mb.relative.col-12
      (invisible-select
       {:value     selected-value
        :on-change #(messages/handle-message selection-event
                                             (assoc selection-args :value (.-value (.-target %))))
        :options   (map (fn [option]
                          [:option {:value (:option/slug option)
                                    :key   (str id "-" (:option/slug option))}
                           (:option/name option)])
                        options)})
      (picker-face* (update face-data :id str "-desktop"))]]))

(defn ^:private length-option
  [{:keys [id
           key
           primary-label
           secondary-label
           checked?
           select-target]}]
  [:div {:key       id
         :data-test id}
   (ui/option {:height   "4em"
               :key      (str key "-option")
               :href     "#"
               :on-click (when select-target (apply utils/send-event-callback select-target))}
              (simple-content-layer
               (list
                [:div.col-2
                 {:key "primary-label"}
                 primary-label]
                [:div.gray.flex-auto
                 {:key "secondary-label"}
                 secondary-label]))
              [:div
               (when checked?
                 (simple-selected-layer))])])

(defn ^:private swatch-content-layer [{:option/keys [name rectangle-swatch]} sku-img checked?]
  [:div.flex.flex-column.bg-white
   [:div.flex
    [:div.rounded-top-left.bg-repeat-x
     {:style
      {:width            "100%"
       :height           "100px"
       :background-size  "contain"
       :background-image (str "url(" rectangle-swatch ")")}}]
    (ui/img {:class    "rounded-top-right"
             :height   "100px"
             :width    "110"
             :src      sku-img
             :max-size 110})]

   [:div.py1.h6.ml3.self-start
    (when checked?
      {:class "bold"})
    name]])

(defn ^:private color-option
  [{:keys
    [id
     color ;; {:option/keys [name rectangle-swatch slug]}
     sku-image
     checked?
     select-target]}]
  [:div {:key       id
         :data-test id}
   (ui/option {:on-click (when select-target (apply utils/send-event-callback select-target))}
              (swatch-content-layer color sku-image checked?)
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

(defn ^:private picker-dialog
  "picker dialog as in https://app.zeplin.io/project/5a9f159069d48a4c15497a49/screen/5b15c08f4819592903cb1348"
  [{:keys [title items cell-component-fn wrap? close-target]
    :as data}]
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
    (mapv cell-component-fn items)]])

(defcomponent modal
  [{:picker-modal/keys
    [options
     visible?
     title
     close-target]
    picker-type :picker-modal/type} owner _]
  [:div
   (slide-animate
    visible?
    (condp = picker-type
      :hair/color
      (picker-dialog {:title             title
                      :items             (sort-by :option/order options)
                      :wrap?             false
                      :close-target      close-target
                      :cell-component-fn (fn [option]
                                           (color-option
                                            {:id              (:id option)
                                             :selected-picker picker-type
                                             :color           option
                                             :checked?        (:checked? option)
                                             :sku-image       (:option/sku-swatch option)
                                             :close-target    close-target
                                             :select-target   (:selection-target option)}))})

      :hair/length (picker-dialog {:title             title
                                   :close-target      close-target
                                   :items             (sort-by :option/order options)
                                   :cell-component-fn (fn [option]
                                                        (length-option
                                                         {:id              (:id option)
                                                          :primary-label   (:option/name option)
                                                          :checked?        (:checked? option)
                                                          :selected-picker picker-type
                                                          :close-target    close-target
                                                          :select-target   (:selection-target option)}))})

      nil))])
