(ns storefront.components.picker.picker
  (:require [catalog.products :as products]
            [catalog.keypaths :as catalog.keypaths]
            [storefront.component :as component]
            [storefront.keypaths :as keypaths]
            [storefront.components.money-formatters :as mf]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [catalog.facets :as facets]
            [storefront.platform.component-utils :as utils]))

(defn simple-selected-layer []
  [:div.absolute.border.border-width-3.rounded-0.border-light-teal.overlay.flex
   [:div.self-center.flex.items-center
    [:div {:style {:width "1em"}}]
    (ui/ucare-img {:width "30"
                   :retina-quality "better"
                   :default-quality "better"}
                  "ae0e9566-f688-4a6d-a0a9-378138308e48")]])

(defn simple-sold-out-layer [text]
  [:div.bg-darken-1.absolute.border.border-silver.rounded-0.overlay.flex.justify-end
   [:div.self-center.flex.items-center.mr4.dark-gray
    text]])

(defn simple-content-layer [content]
  [:div.flex.p4.rounded-0.absolute.overlay.bg-white.border.border-gray
   [:div.self-center.flex.items-center
    {:style {:margin-left "1.5em"}}
    [:div {:style {:width "1em"}}]]
   content])

(defn mobile-dropdown [label-html selected-value-html]
  [:div.flex.items-center.medium.h5
   {:style {:height "100%"}}
   label-html
   [:div.ml2.flex-auto selected-value-html]
   [:div.self-center (svg/dropdown-arrow {:height ".575em"
                                          :width  ".575em"
                                          :class  "stroke-teal"})]])

(defn field
  ([html-widget] (field nil html-widget))
  ([attrs html-widget]
   [:div.border-bottom.border-light-silver.border-width-2.px4
    (merge {:style {:height "75px"}}
           attrs)
    html-widget]))

(defn picker-rows
  "individual elements as in: https://app.zeplin.io/project/5a9f159069d48a4c15497a49/screen/5b21aa0352b1d5e31a32ac53"
  [{:keys [facets selections sku-quantity]}]
  (let [color  (get-in facets [:hair/color :facet/options (:hair/color selections)])
        length (get-in facets [:hair/length :facet/options (:hair/length selections)])]
    [:div.mxn2
     (field
      (utils/fake-href events/control-product-detail-picker-open {:facet-slug :hair/color})
       (mobile-dropdown
         [:img.border.border-gray.rounded-0
          {:height "33px"
           :width  "65px"
           :src    (:option/rectangle-swatch color)}]
         (:option/name color)))
     [:div.flex
      (field
        (merge
         {:class "border-right flex-grow-5"}
         (utils/fake-href events/control-product-detail-picker-open {:facet-slug :hair/length}))
        (mobile-dropdown
          [:div.h7 "Length:"]
          [:span.medium (:option/name length)]))
      [:div.flex-auto
       (field
        (utils/fake-href events/control-product-detail-picker-open {:facet-slug :item/quantity})
         (mobile-dropdown
           [:div.h7 "Qty:"]
           [:span.medium sku-quantity]))]]]))

(defn quantity-option [{:keys [key quantity primary-label checked? disabled?]}]
  (let [label-style (cond
                      disabled? "dark-gray"
                      checked?  "medium"
                      :else     nil)]
    (ui/option {:key      key
                :height   "4em"
                :on-click (utils/send-event-callback
                           events/control-product-detail-picker-option-quantity-select
                           {:value quantity})}
               (simple-content-layer
                [:div.col-2
                 (when label-style
                   {:class label-style})
                 primary-label])
               [:div
                (when disabled?
                  (simple-sold-out-layer ""))
                (when checked?
                  (simple-selected-layer))])))

(defn item-price [price]
  (when price
    [:span {:item-prop "price"} (mf/as-money-without-cents price)]))

(defn length-option [{:keys [item key primary-label secondary-label checked? disabled? selected-picker]}]
  (let [label-style (cond
                      disabled? "dark-gray"
                      checked? "medium"
                      :else     nil)]
    (ui/option {:key      key
                :height   "4em"
                :on-click (utils/send-event-callback
                            events/control-product-detail-picker-option-select
                            {:selection selected-picker
                             :value (:option/slug item)})}
               (simple-content-layer
                 (list
                   [:div.col-2
                    (when label-style
                      {:class label-style})
                    primary-label]
                   [:div.gray.flex-auto secondary-label]))
               [:div
                (when disabled?
                  (simple-sold-out-layer "Sold Out"))
                (when checked?
                  (simple-selected-layer))])))

(defn swatch-content-layer [{:option/keys [name rectangle-swatch]} sku-img checked?]
  [:div.flex.flex-column.bg-white
   [:div.flex
    [:div.rounded-top-left.bg-repeat-x
     {:style
      {:width "100%"
       :height "100px"
       :background-size "contain"
       :background-image (str "url(" rectangle-swatch ")")}}]
    (ui/ucare-img {:class "rounded-top-right" :height "100px"} sku-img)]

   [:div.py1.h6.ml3.self-start
    (when checked?
      {:class "bold"})
    name]])

(defn color-option [{:as stuff :keys [key color sku-image checked? disabled? selected-picker]}]
  (ui/option {:key      key
              :on-click (utils/send-event-callback
                         events/control-product-detail-picker-option-select
                         {:selection selected-picker
                          :value     (:option/slug color)})}
             (swatch-content-layer color sku-image checked?)
             [:div
              (when disabled?
                [:div.absolute.overlay.bg-lighten-3.flex.items-center.justify-center
                 [:div.dark-gray.self-center.flex.items-center.mr2
                  {:style {:margin-top "-30px"}}
                  "Sold Out"]])
              (when checked?
                [:div.absolute.border.border-width-3.rounded-0.border-light-teal.overlay.flex
                 [:div.self-center.flex.items-center
                  {:style {:margin-left "-2em"}}
                  [:div {:style {:width "1em"}}]
                  [:div.circle  ; checkmark circle
                   (ui/ucare-img {:width           "30"
                                  :retina-quality  "better"
                                  :default-quality "better"}
                                 "9e2a48b3-9811-46d2-840b-31c9f85670ad")]]])]))

(defn picker-dialog
  "picker dialog as in https://app.zeplin.io/project/5a9f159069d48a4c15497a49/screen/5b15c08f4819592903cb1348"
  [{:keys [title items cell-component-fn]}]
  [:div.hide-on-tb-dt.z4.fixed.overlay.overflow-auto.bg-light-silver

   [:div.p3.h5.bg-white.relative.border-bottom.border-gray
    {:style {:min-height "3em"}}
    [:div.absolute.overlay.flex.items-center.justify-center
     [:div.dark-gray title]]

    [:div.absolute.overlay.flex.items-center.justify-end
     [:a.teal.medium.p3
      (utils/fake-href events/control-product-detail-picker-close)
      "Done"]]]

   [:div.py3.px1 ;; body
    (map cell-component-fn items)]])

(defn component
  [{:keys [product selected-picker facets options sku-quantity] :as data} owner _]
  (component/create
   [:div
    (when (contains? (:catalog/department product) "hair")
      (condp = selected-picker
        :hair/color    (picker-dialog {:title             (get-in facets [selected-picker :facet/name])
                                       :items             (sort-by :option/order (get options selected-picker))
                                       :cell-component-fn (fn [item]
                                                            (color-option
                                                             {:key             (str "color-" (:option/name item))
                                                              :selected-picker selected-picker
                                                              :color           item
                                                              :sku-image       (:option/sku-swatch item)}))})
        :hair/length   (picker-dialog {:title             (get-in facets [selected-picker :facet/name])
                                       :items             (sort-by :option/order (get options selected-picker))
                                       :cell-component-fn (fn [item]
                                                            (length-option
                                                             {:key             (:option/name item)
                                                              :primary-label   (:option/name item)
                                                              :secondary-label (item-price (:price item))
                                                              :checked?        (:checked? item)
                                                              :selected-picker selected-picker
                                                              :item            item}))})
        :item/quantity (picker-dialog {:title             "Quantity"
                                       :items             (range 1 11)
                                       :cell-component-fn (fn [quantity]
                                                            (quantity-option
                                                             {:key           (str "quantity-" quantity)
                                                              :primary-label (str quantity)
                                                              :checked?      (= quantity sku-quantity)
                                                              :quantity      quantity}))})
        (picker-rows data)))]))


(defn query [data]
  {:product         (products/current-product data)
   :selected-picker (get-in data catalog.keypaths/detailed-product-selected-picker)
   :facets          (facets/by-slug data)
   :selections      (get-in data catalog.keypaths/detailed-product-selections)
   :options         (get-in data catalog.keypaths/detailed-product-options)
   :sku-quantity    (get-in data keypaths/browse-sku-quantity 1)})
