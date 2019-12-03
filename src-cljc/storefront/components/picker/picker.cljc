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

(defn simple-selected-layer []
  [:div.absolute.border.border-width-3.rounded-0.border-cool-gray.overlay.flex
   [:div.self-center.flex.items-center
    [:div {:style {:width "1em"}}]
    (ui/ucare-img {:width           "30"
                   :retina-quality  "better"
                   :default-quality "better"}
                  "ae0e9566-f688-4a6d-a0a9-378138308e48")]])

(defn simple-sold-out-layer [text]
  [:div.bg-darken-1.absolute.border.border-cool-gray.rounded-0.overlay.flex.justify-end
   [:div.self-center.flex.items-center.mr4
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
   [:div.self-center ^:inline (svg/dropdown-arrow {:height ".575em"
                                                   :width  ".575em"
                                                   :class  "stroke-p-color"})]])

(defn desktop-dropdown [label-html selected-value-html select-html]
  [:div.flex.flex-column.relative.flex-auto {:style {:height "100%"}}
   [:div.flex.items-center.medium.h5
    {:style {:height "100%"}}
    label-html
    [:div.ml2.flex-auto selected-value-html]
    [:div.self-center ^:inline (svg/dropdown-arrow {:height ".575em"
                                                    :width  ".575em"
                                                    :class  "stroke-p-color"})]]
   select-html])

(defn field
  ([html-widget] (field nil html-widget))
  ([attrs html-widget]
   [:div.border-bottom.border-cool-gray.border-width-2.px4.flex.items-center
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

(defn desktop-length-and-quantity-picker-rows
  [{:keys [product-sold-out-style selected-length selections options sku-quantity navigation-event]}]
  [:div.flex.hide-on-mb
   (field
    {:class "border-right flex-grow-5"}
    (desktop-dropdown
     [:div.h8 "Length:"]
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
   [:div.flex-auto
    (field
     (desktop-dropdown
      [:div.h8 "Qty:"]
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
  [:div.flex.hide-on-tb-dt
   (field
    (merge
     {:class     "border-right flex-grow-5"
      :data-test "picker-length"}
     (utils/fake-href events/control-product-detail-picker-open {:facet-slug :hair/length}))
    (mobile-dropdown
     [:div.h8 "Length:"]
     [:span.medium
      (merge
       {:data-test (str "picker-selected-length-" (:option/slug selected-length))}
       product-sold-out-style)
      (:option/name selected-length)]))
   [:div.flex-auto
    (field
     (merge {:data-test "picker-quantity"}
            (utils/fake-href events/control-product-detail-picker-open {:facet-slug :item/quantity}))
     (mobile-dropdown
      [:div.h8 "Qty:"]
      [:span.medium
       (merge
        {:data-test (str "picker-selected-quantity-" sku-quantity)}
        product-sold-out-style)
       sku-quantity]))]])

(defn desktop-color-picker-row
  [{:keys [navigation-event selected-color selections options product-sold-out-style]}]
  [:div.hide-on-mb
   (field
    (desktop-dropdown
     [:img.border.border-gray.rounded-0
      {:height "33px"
       :width  "65px"
       :src    (:option/rectangle-swatch selected-color)}]
     [:span product-sold-out-style (:option/name selected-color)]
     (invisible-select
      {:value     (:hair/color selections)
       :on-change #(messages/handle-message events/control-product-detail-picker-option-select
                                            {:selection :hair/color
                                             :value     (.-value (.-target %))})
       :options   (map (fn [option]
                         [:option {:value (:option/slug option)
                                   :key   (str "color-" (:option/slug option))}
                          (:option/name option)])
                       (:hair/color options))})))])

(defn mobile-color-picker-row [{:keys [selected-color product-sold-out-style]}]
  [:div.hide-on-tb-dt
   (field
    (merge {:data-test "picker-color"}
           (utils/fake-href events/control-product-detail-picker-open {:facet-slug :hair/color}))
    (mobile-dropdown
     [:img.border.border-gray.rounded-0
      {:height "33px"
       :width  "65px"
       :src    (:option/rectangle-swatch selected-color)}]
     [:span (merge
             {:data-test (str "picker-selected-color-" (hacky-fix-of-bad-slugs-on-facets (:option/slug selected-color)))}
             product-sold-out-style)
      (:option/name selected-color)]))])

(defn picker-rows
  "individual elements as in: https://app.zeplin.io/project/5a9f159069d48a4c15497a49/screen/5b21aa0352b1d5e31a32ac53"
  [data]
  [:div.mxn2
   [:div
    (mobile-color-picker-row data)
    (desktop-color-picker-row data)]
   [:div
    (desktop-length-and-quantity-picker-rows data)
    (mobile-length-and-quantity-picker-rows data)]])

(defn select-and-close [event-key options]
  (messages/handle-message event-key options)
  (messages/handle-later events/control-product-detail-picker-close nil))

(defn quantity-option [{:keys [key quantity primary-label checked? disabled?]}]
  (let [label-style (cond
                      disabled? "dark-gray"
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
                 (when disabled?
                   (simple-sold-out-layer ""))
                 (when checked?
                   (simple-selected-layer))])]))

(defn length-option
  [{:keys [item key primary-label secondary-label checked? disabled? selected-picker navigation-event]}]
  (let [label-style (cond
                      disabled? "dark-gray"
                      checked?  "medium"
                      :else     nil)]
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
                   {:key "primary-label"
                    :class (str label-style)}
                   primary-label]
                  [:div.gray.flex-auto
                   {:key "secondary-label"}
                   secondary-label]))
                [:div
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
  [{:keys [key color sku-image checked? disabled? selected-picker navigation-event]}]
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
               (when disabled?
                 [:div.absolute.overlay.bg-lighten-3.flex.items-center.justify-center
                  [:div.dark-gray.self-center.flex.items-center.mr2
                   {:style {:margin-top "-30px"}}
                   "Sold Out"]])
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

(defn- slide-animate [content]
  (css-transitions/transition-group
   {:classNames "picker"
    :key        "picker"
    :in         (boolean (not-empty content))
    :timeout    250}
   (component/html content)))

(defn picker-dialog
  "picker dialog as in https://app.zeplin.io/project/5a9f159069d48a4c15497a49/screen/5b15c08f4819592903cb1348"
  [{:keys [title items cell-component-fn product-alternative]}]
  [:div.hide-on-tb-dt.z4.fixed.overlay.overflow-auto.bg-cool-gray
   {:key (str "picker-dialog-" title) :data-test "picker-dialog"}
   [:div.p3.h5.bg-white.relative.border-bottom.border-gray
    {:style {:min-height "3em"}}
    [:div.absolute.overlay.flex.items-center.justify-center
     title]

    [:div.absolute.overlay.flex.items-center.justify-end
     [:a.p-color.medium.p3
      (merge {:data-test "picker-close"}
             (utils/fake-href events/control-product-detail-picker-close))
      "Done"]]]
   [:div.py3.px1 ;; body
    (mapv cell-component-fn items)]
   (when-let [{:keys [link-attrs link-text lead-in]} product-alternative]
     [:div.center.mt6
      {:data-test "picker-product-alternative"}
      [:div.h6 lead-in]
      [:div.h4.medium.mt2 [:a.p-color.underline link-attrs link-text]]])])

(defcomponent component
  [{:keys [navigation-event
           selected-picker
           facets
           selections
           options
           sku-quantity
           product-alternative]
    :as   data} owner _]
  [:div
   (slide-animate
    (when (seq options)
      (condp = selected-picker
        :hair/color    (picker-dialog {:title               (get-in facets [selected-picker :facet/name])
                                       :items               (sort-by :option/order (get options selected-picker))
                                       :cell-component-fn   (fn [item]
                                                              (color-option
                                                               {:key              (str "color-" (:option/name item))
                                                                :selected-picker  selected-picker
                                                                :navigation-event navigation-event
                                                                :color            item
                                                                :checked?         (= (:hair/color selections)
                                                                                     (:option/slug item))
                                                                :sku-image        (:option/sku-swatch item)}))
                                       :product-alternative product-alternative})
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
                                                                :item             item}))
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
     (picker-rows data))])

(defn query
  [data]
  (let [family            (:hair/family (get-in data catalog.keypaths/detailed-product-selected-sku))
        options           (get-in data catalog.keypaths/detailed-product-options)
        selected-picker   (get-in data catalog.keypaths/detailed-product-selected-picker)
        product-skus      (products/extract-product-skus data (products/current-product data))
        product-sold-out? (every? (comp not :inventory/in-stock?) product-skus)
        facets            (facets/by-slug data)
        selections        (get-in data catalog.keypaths/detailed-product-selections)
        navigation-event  (if (= "freeinstall" (get-in data keypaths/store-slug))
                            events/navigate-adventure-product-details
                            events/navigate-product-details)]
    {:selected-picker        selected-picker
     :facets                 facets
     :selections             (get-in data catalog.keypaths/detailed-product-selections)
     :options                options
     :selected-color         (get-in facets [:hair/color :facet/options (:hair/color selections)])
     :selected-length        (get-in facets [:hair/length :facet/options (:hair/length selections)])
     :product-sold-out-style (when product-sold-out? {:class "gray"})
     :sku-quantity           (get-in data keypaths/browse-sku-quantity 1)
     :product-alternative    (when (and
                                    (not= "freeinstall" (get-in data keypaths/store-slug))
                                    (= 1 (count (:hair/color options)))
                                    (= selected-picker :hair/color)
                                    (some family ["frontals" "bundles" "closures" "360-frontals"]))
                               {:lead-in    "Want more color?"
                                :link-text  "Browse Dyed Virgin"
                                :link-attrs (utils/route-to events/navigate-category
                                                            {:page/slug           "dyed-virgin-hair"
                                                             :catalog/category-id "16"})})
     :navigation-event       navigation-event}))
