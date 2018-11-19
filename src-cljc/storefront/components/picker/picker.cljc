(ns storefront.components.picker.picker
  (:require [catalog.facets :as facets]
            [catalog.keypaths :as catalog.keypaths]
            [catalog.products :as products]
            [storefront.component :as component]
            [storefront.components.money-formatters :as mf]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [storefront.css-transitions :as css-transitions]))

(defn simple-selected-layer []
  [:div.absolute.border.border-width-3.rounded-0.border-light-teal.overlay.flex
   [:div.self-center.flex.items-center
    [:div {:style {:width "1em"}}]
    (ui/ucare-img {:width           "30"
                   :retina-quality  "better"
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
  [:div.flex.items-center.medium.h5.flex-auto
   {:style {:height "100%"}}
   label-html
   [:div.ml2.flex-auto selected-value-html]
   [:div.self-center (svg/dropdown-arrow {:height ".575em"
                                          :width  ".575em"
                                          :class  "stroke-teal"})]])

(defn desktop-dropdown [label-html selected-value-html select-html]
  [:div.flex.flex-column.relative.flex-auto {:style {:height "100%"}}
   [:div.flex.items-center.medium.h5
    {:style {:height "100%"}}
    label-html
    [:div.ml2.flex-auto selected-value-html]
    [:div.self-center (svg/dropdown-arrow {:height ".575em"
                                           :width  ".575em"
                                           :class  "stroke-teal"})]]
   select-html])

(defn field
  ([html-widget] (field nil html-widget))
  ([attrs html-widget]
   [:div.border-bottom.border-light-silver.border-width-2.px4.flex.items-center
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
  (clojure.string/replace (str slug) #"#" ""))

(defn picker-rows
  "individual elements as in: https://app.zeplin.io/project/5a9f159069d48a4c15497a49/screen/5b21aa0352b1d5e31a32ac53"
  [{:as data :keys [product-sold-out? options facets selections sku-quantity]}]
  (let [color                  (get-in facets [:hair/color :facet/options (:hair/color selections)])
        length                 (get-in facets [:hair/length :facet/options (:hair/length selections)])
        product-sold-out-style (when product-sold-out? {:class "gray"})]
    [:div.mxn2
     [:div
      [:div.hide-on-dt
       (field
        (merge {:data-test "picker-color"}
               (utils/fake-href events/control-product-detail-picker-open {:facet-slug :hair/color}))
        (mobile-dropdown
         [:img.border.border-gray.rounded-0
          {:height "33px"
           :width  "65px"
           :src    (:option/rectangle-swatch color)}]
         [:span (merge
                 {:data-test (str "picker-selected-color-" (hacky-fix-of-bad-slugs-on-facets (:option/slug color)))}
                 product-sold-out-style)
          (:option/name color)]))]
      [:div.hide-on-mb-tb
       (field
        (desktop-dropdown
         [:img.border.border-gray.rounded-0
          {:height "33px"
           :width  "65px"
           :src    (:option/rectangle-swatch color)}]
         [:span product-sold-out-style (:option/name color)]
         (invisible-select
          {:value     (:hair/color selections)
           :on-change #(messages/handle-message events/control-product-detail-picker-option-select
                                                {:selection :hair/color
                                                 :value     (.-value (.-target %))})
           :options   (map (fn [option]
                             [:option {:value (:option/slug option)
                                       :key   (str "color-" (:option/slug option))}
                              (:option/name option)])
                           (:hair/color options))})))]]
     [:div
      [:div.flex.hide-on-mb-tb
       (field
        {:class "border-right flex-grow-5"}
        (desktop-dropdown
         [:div.h7 "Length:"]
         [:span.medium product-sold-out-style (:option/name length)]
         (invisible-select
          {:on-change #(messages/handle-message events/control-product-detail-picker-option-select
                                                {:selection :hair/length
                                                 :value     (.-value (.-target %))})
           :value     (:hair/length selections)
           :options   (map (fn [option]
                             [:option {:value (:option/slug option)
                                       :key   (str "length-" (:option/slug option))}
                              (str (:option/name option) " - " (mf/as-money-without-cents (:price option)))])
                           (:hair/length options))})))
       [:div.flex-auto
        (field
         (desktop-dropdown
          [:div.h7 "Qty:"]
          [:span.medium product-sold-out-style sku-quantity]
          (invisible-select
           {:on-change #(messages/handle-message events/control-product-detail-picker-option-quantity-select
                                                 {:value (spice.core/parse-int (.-value (.-target %)))})
            :value     sku-quantity
            :options   (map (fn [quantity]
                              [:option {:value quantity
                                        :key   (str "quantity-" quantity)}
                               quantity])
                            (range 1 11))})))]]
      [:div.flex.hide-on-dt
         (field
          (merge
           {:class     "border-right flex-grow-5"
            :data-test "picker-length"}
           (utils/fake-href events/control-product-detail-picker-open {:facet-slug :hair/length}))
          (mobile-dropdown
           [:div.h7 "Length:"]
           [:span.medium
            (merge
             {:data-test (str "picker-selected-length-" (:option/slug length))}
             product-sold-out-style)
            (:option/name length)]))
         [:div.flex-auto
          (field
           (merge {:data-test "picker-quantity"}
                  (utils/fake-href events/control-product-detail-picker-open {:facet-slug :item/quantity}))
           (mobile-dropdown
            [:div.h7 "Qty:"]
            [:span.medium (merge
                           {:data-test (str "picker-selected-quantity-" sku-quantity)}
                           product-sold-out-style)
             sku-quantity]))]]]]))

(defn select-and-close [event-key options]
  (messages/handle-message event-key options)
  (messages/handle-later events/control-product-detail-picker-close nil))

(defn quantity-option [{:keys [key quantity primary-label checked? disabled?]}]
  (let [label-style (cond
                      disabled? "dark-gray"
                      checked?  "medium"
                      :else     nil)]
    [:div {:data-test (str "picker-quantity-" quantity)}
     (ui/option {:key      key
                 :height   "4em"
                 :on-click #(select-and-close
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
                   (simple-selected-layer))])]))

(defn item-price [price]
  (when price
    [:span {:item-prop "price"} (mf/as-money-without-cents price)]))

(defn length-option [{:keys [item key primary-label secondary-label checked? disabled? selected-picker]}]
  (let [label-style (cond
                      disabled? "dark-gray"
                      checked?  "medium"
                      :else     nil)]
    [:div {:data-test (str "picker-length-" (:option/slug item))}
     (ui/option {:key      key
                 :height   "4em"
                 :on-click #(select-and-close
                             events/control-product-detail-picker-option-select
                             {:selection selected-picker
                              :value     (:option/slug item)})}
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
                   (simple-selected-layer))])]))

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
  [{:keys [key color sku-image checked? disabled? selected-picker]}]
  [:div {:data-test (str "picker-color-" (:option/slug color))}
   (ui/option {:key      key
               :on-click #(select-and-close
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
                                  "9e2a48b3-9811-46d2-840b-31c9f85670ad")]]])])])
(defn- slide-animate [& content]
  (css-transitions/transition-group
   {:transitionName          "picker"}
   content))

(defn picker-dialog
  "picker dialog as in https://app.zeplin.io/project/5a9f159069d48a4c15497a49/screen/5b15c08f4819592903cb1348"
  [{:keys [title items cell-component-fn product-alternative]}]
  [:div.hide-on-tb-dt.z4.fixed.overlay.overflow-auto.bg-light-silver

   [:div.p3.h5.bg-white.relative.border-bottom.border-gray
    {:style {:min-height "3em"}}
    [:div.absolute.overlay.flex.items-center.justify-center
     [:div.dark-gray title]]

    [:div.absolute.overlay.flex.items-center.justify-end
     [:a.teal.medium.p3
      (merge {:data-test "picker-close"}
             (utils/fake-href events/control-product-detail-picker-close))
      "Done"]]]
   [:div.py3.px1 ;; body
    (mapv cell-component-fn items)]
   (when-let [{:keys [link-attrs link-text lead-in]} product-alternative]
     [:div.center.mt6
      {:data-test "picker-product-alternative"}
      [:div.h6 lead-in]
      [:div.h4.medium.mt2 [:a.teal.underline link-attrs link-text]]])])

(defn component
  [{:keys [selected-picker facets selections options sku-quantity product-alternative] :as data} owner _]
  (component/create
   [:div
    (slide-animate
     (when (seq options)
       (condp = selected-picker
         :hair/color    (picker-dialog {:title               (get-in facets [selected-picker :facet/name])
                                        :items               (sort-by :option/order (get options selected-picker))
                                        :cell-component-fn   (fn [item]
                                                               (color-option
                                                                {:key             (str "color-" (:option/name item))
                                                                 :selected-picker selected-picker
                                                                 :color           item
                                                                 :checked?        (= (:hair/color selections)
                                                                                     (:option/slug item))
                                                                 :sku-image       (:option/sku-swatch item)}))
                                        :product-alternative product-alternative})
         :hair/length   (picker-dialog {:title               (get-in facets [selected-picker :facet/name])
                                        :items               (sort-by :option/order (get options selected-picker))
                                        :cell-component-fn   (fn [item]
                                                               (length-option
                                                                {:key             (str "length-" (:option/name item))
                                                                 :primary-label   (:option/name item)
                                                                 :secondary-label (item-price (:price item))
                                                                 :checked?        (= (:hair/length selections)
                                                                                     (:option/slug item))
                                                                 :selected-picker selected-picker
                                                                 :item            item}))
                                        :product-alternative product-alternative})
         :item/quantity (picker-dialog {:title               "Quantity"
                                        :items               (range 1 11)
                                        :cell-component-fn   (fn [quantity]
                                                               (quantity-option
                                                                {:key           (str "quantity-" quantity)
                                                                 :primary-label (str quantity)
                                                                 :checked?      (= quantity sku-quantity)
                                                                 :quantity      quantity}))
                                        :product-alternative product-alternative})
         nil)))
    (when (seq options)
      (picker-rows data))]))

(defn query
  [data]
  (let [family          (:hair/family (get-in data catalog.keypaths/detailed-product-selected-sku))
        options         (get-in data catalog.keypaths/detailed-product-options)
        selected-picker (get-in data catalog.keypaths/detailed-product-selected-picker)
        product-skus    (products/extract-product-skus data (products/current-product data))]
    {:selected-picker     selected-picker
     :facets              (facets/by-slug data)
     :selections          (get-in data catalog.keypaths/detailed-product-selections)
     :options             options
     :sku-quantity        (get-in data keypaths/browse-sku-quantity 1)
     :product-sold-out?   (every? (comp not :inventory/in-stock?) product-skus)
     :product-alternative (when (and (= 1 (count (:hair/color options)))
                                     (= selected-picker :hair/color)
                                     (some family ["frontals" "bundles" "closures" "360-frontals"]))
                            {:lead-in    "Want more color?"
                             :link-text  "Browse Dyed Virgin"
                             :link-attrs (utils/route-to events/navigate-category
                                                         {:page/slug           "dyed-virgin-hair"
                                                          :catalog/category-id "16"})})}))
