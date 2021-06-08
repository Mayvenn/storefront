(ns storefront.components.picker.picker-two
  "Exists temporarily to allow us to refactor the interface for use in varying contexts"
  (:require [catalog.facets :as facets]
            [catalog.products :as products]
            [spice.core :as spice]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.css-transitions :as css-transitions]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]))

(def checkmark-circle
  [:div.circle.bg-s-color.flex.items-center.justify-center
   {:style {:height "24px" :width "24px"}}
   (svg/check-mark {:height "12px" :width "16px" :class "fill-white"})])

(defn simple-selected-layer []
  [:div.absolute.border.border-width-3.border-s-color.overlay.flex
   [:div.flex.items-center.justify-end.pr4.col-12
    checkmark-circle]])

(defn simple-sold-out-layer [text]
  [:div.bg-darken-1.absolute.overlay.flex.justify-end
   [:div.self-center.flex.items-center.mr4
    text]])

(defn simple-content-layer [content]
  [:div.flex.items-center.p4.absolute.overlay.bg-white
   [:div.ml1 content]])

(def picker-chevron
  (ui/forward-caret {:width  18
                     :height 18}) )

(defn picker-face
  [{:picker-face/keys [id title primary image-src]}]
  [:div.col-12
   [:div.proxima.title-3.shout title]
   [:div.flex.justify-between.bg-white.p3.items-center
    {:style {:height "100%"}}
    [:img.border.border-gray.mr1
     {:height "63px"
      :width  "63px"
      :src    image-src}]
    [:div.ml2.col-12
     [:div {:data-test id} primary]]
    [:div.self-center ^:inline picker-chevron]]])

(defn color-desktop-dropdown [label-html selected-value-html select-html selected-color]
  [:div.flex.flex-column.relative.flex-auto {:style {:height "100%"}}
   [:div.flex.items-center
    {:style {:height "100%"}}
    label-html
    [:img.border.border-gray.ml4.mr1
     {:height "20px"
      :width  "21px"
      :src    selected-color}]
    [:div.ml2.flex-auto selected-value-html]
    [:div.self-center ^:inline picker-chevron]]
   select-html])

(defn mobile-dropdown [label-html selected-value-html]
  [:div.flex.items-center.flex-auto
   {:style {:height "100%"}}
   label-html

   [:div.ml2.flex-auto selected-value-html]
   [:div.self-center ^:inline picker-chevron]])

(defn desktop-dropdown [label-html selected-value-html select-html]
  [:div.flex.flex-column.relative.flex-auto {:style {:height "100%"}}
   [:div.flex.items-center
    {:style {:height "100%"}}
    label-html
    [:div.ml2.flex-auto selected-value-html]
    [:div.self-center ^:inline picker-chevron]]
   select-html])

(defn field
  ([html-widget] (field nil html-widget))
  ([attrs html-widget]
   [:div.flex.items-center
    (merge {:style {:height "75px"}}
           attrs)
    html-widget]))

(defn invisible-select [{:keys [on-change options value]}]
  [:select.absolute.invisible-select.overlay
   {:on-change on-change
    :value     value
    :style     {:opacity 0
                :border  "none"
                :outline "none"
                :width   "100%"
                :height  "100%"}}
   options])

(def vertical-border
  [:div.border-right.border-cool-gray.mx3 {:style {:height "45px"}}])

(defn desktop-length-and-quantity-picker-rows
  [{:keys [product-sold-out-style selected-length selections options sku-quantity navigation-event]}]
  [:div.flex.hide-on-mb.items-center.border-top.border-cool-gray.border-width-2
   (field
    {:class "col-7"}
    (desktop-dropdown
     [:div.proxima.title-3.shout "Length"]
     [:span.medium
      product-sold-out-style
      (:option/name selected-length)]
     (invisible-select
      {:on-change #(messages/handle-message events/control-product-detail-picker-option-select
                                            {:selection        :hair/length
                                             :navigation-event navigation-event
                                             :value            (.-value (.-target %))})
       :value     (:hair/length selections)
       :options   (map (fn [option]
                         [:option {:value (:option/slug option)
                                   :key   (str "length-" (:option/slug option))}
                          (:option/name option)])
                       (:hair/length options))})))
   vertical-border
   [:div.flex-auto
    (field
     (desktop-dropdown
      [:div.proxima.title-3.shout "Qty"]
      [:span.medium product-sold-out-style sku-quantity]
      (invisible-select
       {:on-change #(messages/handle-message events/control-product-detail-picker-option-quantity-select
                                             {:value (spice/parse-int (.-value (.-target %)))})
        :value     sku-quantity
        :options   (map (fn [quantity]
                          [:option {:value quantity
                                    :key   (str "quantity-" quantity)}
                           quantity])
                        (range 1 11))})))]])

(defn mobile-length-and-quantity-picker-rows
  [{:keys [selected-length product-sold-out-style sku-quantity]}]
  [:div.flex.hide-on-tb-dt.items-center.border-top.border-cool-gray.border-width-2
   (field
    (merge
     {:class     " col-7 py2"
      :data-test "picker-length"}
     (utils/fake-href events/control-product-detail-picker-open {:facet-slug :hair/length}))
    (mobile-dropdown
     [:div.h8.proxima.title-3.shout "Length"]
     [:span.medium.canela.content-2
      (merge
       {:data-test (str "picker-selected-length-" (:option/slug selected-length))}
       product-sold-out-style)
      (:option/name selected-length)]))
   vertical-border
   [:div.flex-auto
    (field
     (merge {:data-test "picker-quantity"}
            (utils/fake-href events/control-product-detail-picker-open {:facet-slug :item/quantity}))
     (mobile-dropdown
      [:div.h8.proxima.title-3.shout "Qty"]
      [:span.medium
       (merge
        {:data-test (str "picker-selected-quantity-" sku-quantity)}
        product-sold-out-style)
       sku-quantity]))]])

(defn desktop-color-picker-row
  [{:keys [navigation-event selected-color selections options product-sold-out-style]}]
  [:div.hide-on-mb.border-top.border-cool-gray.border-width-2
   (field
    (color-desktop-dropdown
     [:span.proxima.title-3.shout
      "Color"]
     [:span product-sold-out-style (:option/name selected-color)]
     (invisible-select
      {:value     (:hair/color selections)
       :on-change #(messages/handle-message events/control-product-detail-picker-option-select
                                            {:selection        :hair/color
                                             :navigation-event navigation-event
                                             :value            (.-value (.-target %))})
       :options   (map (fn [option]
                         [:option {:value (:option/slug option)
                                   :key   (str "color-" (:option/slug option))}
                          (:option/name option)])
                       (:hair/color options))})
     (:option/rectangle-swatch selected-color)))])

(defn color-picker-face
  [{:keys                            [selected-color open-target options]
    [selection-event selection-args] :selection-target}]
  (let [face-data #:picker-face{:id        (str "picker-selected-color-" (facets/hacky-fix-of-bad-slugs-on-facets (:option/slug selected-color)))
                                :title     "Color"
                                :image-src (:option/rectangle-swatch selected-color)
                                :primary   (:option/name selected-color)}]
    [:div
     [:div.hide-on-tb-dt
      (field
       (merge {:data-test "picker-color"} (apply utils/fake-href open-target))
       (picker-face face-data))]
     [:div.hide-on-mb.relative.col-12
      (invisible-select
       {:value     (:option/slug selected-color)
        :on-change #(messages/handle-message selection-event
                                             (assoc selection-args :value (.-value (.-target %))))
        :options   (map (fn [option]
                          [:option {:value (:option/slug option)
                                    :key   (str "color-" (:option/slug option))}
                           (:option/name option)])
                        options)})
      (picker-face (update face-data :id str "-desktop"))]]))

(defn select-and-close [close-target select-event options]
  (messages/handle-message select-event options)
  (messages/handle-later close-target nil))

(defn quantity-option [{:keys [key quantity primary-label checked? close-target select-event]}]
  (let [label-style (cond
                      checked?  "medium"
                      :else     nil)]
    [:div {:key       key
           :data-test (str "picker-quantity-" quantity)}
     (ui/option {:height   "4em"
                 :key      (str key "-option")
                 :on-click #(select-and-close close-target select-event {:value quantity})}
                (simple-content-layer
                 [:div.col-2
                  (when label-style
                    {:class label-style})
                  primary-label])
                [:div
                 (when checked?
                   (simple-selected-layer))])]))

(defn length-option
  [{:keys [item key primary-label secondary-label checked? selected-picker navigation-event close-target select-event]}]
  [:div {:key       key
         :data-test (str "picker-length-" (:option/slug item))}
   (ui/option {:height   "4em"
               :key      (str key "-option")
               :href     "#"
               :on-click #(select-and-close close-target select-event {:selection        selected-picker ;; <yeah it's the length picker BUT which length picker? 0 1 2
                                                                       :navigation-event navigation-event
                                                                       :value            (:option/slug item)})}
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

(defn swatch-content-layer [{:option/keys [name rectangle-swatch]} sku-img checked?]
  [:div.flex.flex-column.bg-white
   [:div.flex
    [:div.rounded-top-left.bg-repeat-x
     {:style
      {:width            "100%"
       :height           "100px"
       :background-size  "contain"
       :background-image (str "url(" rectangle-swatch ")")}}]
    (ui/ucare-img {:class "rounded-top-right" :height "100px"} sku-img)]

   [:div.py1.h6.ml3.self-start
    (when checked?
      {:class "bold"})
    name]])

(defn color-option
  [{:keys
    [id
     color ;; {:option/keys [name rectangle-swatch slug]}
     sku-image
     checked?
     select-target]}]
  [:div {:key       id
         :data-test id}
   (ui/option {:on-click (apply utils/send-event-callback select-target)}
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

(defn slide-animate [enabled? content]
  (css-transitions/transition-group
   {:classNames    "picker"
    :key           "picker"
    :in            (boolean enabled?)
    :unmountOnExit true
    :timeout       1000}
   (component/html (or content [:div]))))

(defn title-cta [{:title-cta/keys
                  [primary target id]}]
  (when id
    (ui/button-small-underline-primary
     (assoc
      (apply utils/fake-href target)
      :data-test id)
     primary)))

(defn picker-dialog
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

(defcomponent open-picker-component
  [{:keys [picker-type
           options
           picker-visible?
           title
           close-target]
    :as   data} owner _]
  [:div
   (slide-animate
    picker-visible?
    ;; TODO pass in the cell component?
    (condp = picker-type
      :hair/color
      (picker-dialog {:title             title
                      :items             (sort-by :option/order options)
                      :wrap?             false
                      :close-target      close-target
                      :cell-component-fn (fn [option]
                                           (color-option
                                            {:id               (:id option)
                                             :selected-picker  picker-type
                                             :color            option
                                             :checked?         (:checked? option)
                                             :sku-image        (:option/sku-swatch option)
                                             :close-target     close-target
                                             :select-target    (:selection-target option)}))}) ;;
      nil))])

(defn open-picker-query
  [{:keys [data
           options
           open?
           picker-type
           close-target]}]
  (let [product-skus      (products/extract-product-skus data (products/current-product data))
        product-sold-out? (every? (comp not :inventory/in-stock?) product-skus) ;; TODO...
        facets            (facets/by-slug data)]
    {:picker-visible?        open?
     :picker-type            picker-type
     :options                options
     :product-sold-out-style (when product-sold-out? {:class "gray"}) ;; TODO: unavailable & sold out...
     :close-target           close-target
     :title                  (get-in facets [picker-type :facet/name])}))
