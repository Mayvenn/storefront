(ns storefront.components.picker.picker
  (:require [catalog.facets :as facets]
            [catalog.keypaths :as catalog.keypaths]
            [catalog.products :as products]
            [clojure.string :as string]
            [spice.core :as spice]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.css-transitions :as css-transitions]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [storefront.accessors.experiments :as experiments]))

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
  (svg/dropdown-arrow {:height "13px"
                       :width  "13px"}) )

(defn color-mobile-dropdown [label-html selected-value-html selected-color]
  [:div.flex.items-center.flex-auto
   {:style {:height "100%"}}
   label-html
   [:img.border.border-gray.ml4.mr1
    {:height "20px"
     :width  "21px"
     :src    selected-color}]
   [:div.ml2.flex-auto selected-value-html]
   [:div.self-center ^:inline picker-chevron]])

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

(defn- hacky-fix-of-bad-slugs-on-facets [slug]
  (string/replace (str slug) #"#" ""))

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

(defn mobile-color-picker-row
  [{:keys [selected-color product-sold-out-style]}]
  [:div.hide-on-tb-dt.border-top.border-cool-gray.border-width-2
   (field
    (merge {:data-test "picker-color"}
           (utils/fake-href events/control-product-detail-picker-open {:facet-slug :hair/color}))
    (color-mobile-dropdown
     [:span.proxima.title-3.shout
      "Color"]
     [:span (merge
             {:data-test (str "picker-selected-color-" (hacky-fix-of-bad-slugs-on-facets (:option/slug selected-color)))}
             product-sold-out-style)
      (:option/name selected-color)]
     (:option/rectangle-swatch selected-color)))])

(defn picker-rows
  "individual elements as in: https://app.zeplin.io/project/5a9f159069d48a4c15497a49/screen/5b21aa0352b1d5e31a32ac53"
  [data]
  [:div.mxn2
   [:div.px3
    (mobile-color-picker-row data)
    (desktop-color-picker-row data)]
   [:div.px3
    (desktop-length-and-quantity-picker-rows data)
    (mobile-length-and-quantity-picker-rows data)]])

(defn select-and-close [event-key options]
  (messages/handle-message event-key options)
  (messages/handle-later events/control-product-detail-picker-close nil))

(defn quantity-option [{:keys [key quantity primary-label checked?]}]
  (let [label-style (cond
                      checked?  "medium"
                      :else     nil)]
    [:div {:key       key
           :data-test (str "picker-quantity-" quantity)}
     (ui/option {:height   "4em"
                 :key      (str key "-option")
                 :on-click #(select-and-close
                             events/control-product-detail-picker-option-quantity-select
                             {:value quantity})}
                (simple-content-layer
                 [:div.col-2
                  (when label-style
                    {:class label-style})
                  primary-label])
                [:div
                 (when checked?
                   (simple-selected-layer))])]))

(defn length-option
  [{:keys [item key primary-label secondary-label checked? selected-picker navigation-event]}]
  [:div {:key       key
         :data-test (str "picker-length-" (:option/slug item))}
   (ui/option {:height   "4em"
               :key      (str key "-option")
               :href     "#"
               :on-click #(select-and-close
                           events/control-product-detail-picker-option-select
                           {:selection        selected-picker
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
  [{:keys [key color sku-image checked? selected-picker navigation-event]}]
  [:div {:key       key
         :data-test (str "picker-color-" (hacky-fix-of-bad-slugs-on-facets (:option/slug color)))}
   (ui/option {:key      (str key "-option")
               :on-click #(select-and-close
                           events/control-product-detail-picker-option-select
                           {:selection        selected-picker
                            :navigation-event navigation-event
                            :value            (:option/slug color)})}
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

(defn swatch-content-layer-new [{:option/keys [name rectangle-swatch]}]
  [:div.flex.flex-column.bg-white
   [:div.flex
    [:div.bg-repeat-x
     {:style
      {:width            "100%"
       :height           "165px"
       :background-size  "cover"
       :background-image (str "url(" rectangle-swatch ")")}}]]
   [:div.py3.ml3.self-start.content-3.proxima
    name]])

(defn color-option-new
  [{:keys [key color checked? selected-picker navigation-event]}]
  [:div.col-6.flex-wrap
   {:key       key
    :data-test (str "picker-color-" (hacky-fix-of-bad-slugs-on-facets (:option/slug color)))}
   (ui/option {:key      (str key "-option")
               :on-click #(select-and-close
                           events/control-product-detail-picker-option-select
                           {:selection        selected-picker
                            :navigation-event navigation-event
                            :value            (:option/slug color)})}
              (swatch-content-layer-new color)
              [:div
               (when checked?
                 [:div.absolute.border.border-width-3.border-s-color.overlay.flex
                  [:div.col-12.flex.justify-end.m2
                   checkmark-circle]])])])

(defn- slide-animate [content]
  (css-transitions/transition-group
   {:classNames "picker"
    :key        "picker"
    :in         (boolean (not-empty content))
    :timeout    250}
   (component/html (or content [:div]))))

(defn picker-dialog
  "picker dialog as in https://app.zeplin.io/project/5a9f159069d48a4c15497a49/screen/5b15c08f4819592903cb1348"
  [{:keys [title items cell-component-fn wrap?]}]
  [:div.hide-on-tb-dt.z6.fixed.overlay.overflow-auto.bg-cool-gray
   {:key (str "picker-dialog-" title) :data-test "picker-dialog"}
   [:div.p3.content-1.proxima.bg-white.relative.border-bottom.border-gray
    {:style {:height "70px"}}
    [:div.absolute.overlay.flex.items-center.justify-center
     title]

    [:div.absolute.overlay.flex.items-center.justify-end
     [:a.flex.items-center.justify-center.medium.p3
      (merge {:style     {:height "70px" :width "70px"}
              :data-test "picker-close"}
             (utils/fake-href events/control-product-detail-picker-close))
      (svg/x-sharp {:height "14px" :width "14px"})]]]
   [:div.py3.px1 ;; body
    (when wrap?
      {:class "flex flex-wrap"})
    (mapv cell-component-fn items)]])

(defcomponent component
  [{:keys [navigation-event
           selected-picker
           facets
           selections
           options
           sku-quantity
           color-picker-redesign?]
    :as   data} owner _]
  (let [color-option-fn (if color-picker-redesign? color-option-new color-option)]
    [:div
     (slide-animate
      (when (seq options)
        (condp = selected-picker
          :hair/color    (picker-dialog {:title               (get-in facets [selected-picker :facet/name])
                                         :items               (sort-by :option/order (get options selected-picker))
                                         :wrap?               color-picker-redesign?
                                         :cell-component-fn   (fn [item]
                                                                (color-option-fn
                                                                 {:key              (str "color-" (:option/name item))
                                                                  :selected-picker  selected-picker
                                                                  :navigation-event navigation-event
                                                                  :color            item
                                                                  :checked?         (= (:hair/color selections)
                                                                                       (:option/slug item))
                                                                  :sku-image        (:option/sku-swatch item)}))})
          :hair/length   (picker-dialog {:title               (get-in facets [selected-picker :facet/name])
                                         :items               (sort-by :option/order (get options selected-picker))
                                         :cell-component-fn   (fn [item]
                                                                (length-option
                                                                 {:key              (str "length-" (:option/name item))
                                                                  :primary-label    (:option/name item)
                                                                  :navigation-event navigation-event
                                                                  :checked?         (= (:hair/length selections)
                                                                                       (:option/slug item))
                                                                  :selected-picker  selected-picker
                                                                  :item             item}))})
          :item/quantity (picker-dialog {:title               "Quantity"
                                         :items               (range 1 11)
                                         :cell-component-fn   (fn [quantity]
                                                                (quantity-option
                                                                 {:key           (str "quantity-" quantity)
                                                                  :primary-label (str quantity)
                                                                  :checked?      (= quantity sku-quantity)
                                                                  :quantity      quantity}))})
          nil)))
     (when (seq options)
       (picker-rows data))]))

(defn query
  [data]
  (let [family            (:hair/family (get-in data catalog.keypaths/detailed-product-selected-sku))
        options           (get-in data catalog.keypaths/detailed-product-options)
        selected-picker   (get-in data catalog.keypaths/detailed-product-selected-picker)
        product-skus      (products/extract-product-skus data (products/current-product data))
        product-sold-out? (every? (comp not :inventory/in-stock?) product-skus)
        facets            (facets/by-slug data)
        selections        (get-in data catalog.keypaths/detailed-product-selections)]
    {:selected-picker        selected-picker
     :color-picker-redesign? (experiments/color-picker-redesign? data)
     :facets                 facets
     :selections             (get-in data catalog.keypaths/detailed-product-selections)
     :options                options
     :selected-color         (get-in facets [:hair/color :facet/options (:hair/color selections)])
     :selected-length        (get-in facets [:hair/length :facet/options (:hair/length selections)])
     :product-sold-out-style (when product-sold-out? {:class "gray"})
     :sku-quantity           (get-in data keypaths/browse-sku-quantity 1)
     :navigation-event       events/navigate-product-details}))
