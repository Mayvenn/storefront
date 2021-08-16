(ns storefront.components.picker.picker
  (:require api.catalog
            [catalog.facets :as facets]
            [catalog.keypaths :as catalog.keypaths]
            [catalog.products :as products]
            [spice.core :as spice]
            [storefront.accessors.experiments :as experiments]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.css-transitions :as css-transitions]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
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
  (svg/dropdown-arrow {:height "13px"
                       :width  "13px"}) )

(defn color-mobile-dropdown [label-html selected-value-html selected-color-swatch]
  [:div.flex.items-center.flex-auto
   {:style {:height "100%"}}
   label-html
   [:img.border.border-gray.ml4.mr1
    {:height "20px"
     :width  "21px"
     :src    selected-color-swatch}]
   [:div.ml2.flex-auto selected-value-html]
   [:div.self-center ^:inline picker-chevron]])

(defn color-desktop-dropdown [label-html selected-value-html select-html selected-color-swatch]
  [:div.flex.flex-column.relative.flex-auto {:style {:height "100%"}}
   [:div.flex.items-center
    {:style {:height "100%"}}
    label-html
    [:img.border.border-gray.ml4.mr1
     {:height "20px"
      :width  "21px"
      :src    selected-color-swatch}]
    [:div.ml2.flex-auto selected-value-html]
    [:div.self-center ^:inline picker-chevron]]
   select-html])

(defn base-material-mobile-dropdown [label-html selected-value-html]
  [:div.flex.items-center.flex-auto
   {:style {:height "100%"}}
   label-html
   [:div.ml2.flex-auto selected-value-html]
   [:div.self-center ^:inline picker-chevron]])

(defn base-material-desktop-dropdown [label-html selected-value-html select-html]
  [:div.flex.flex-column.relative.flex-auto {:style {:height "100%"}}
   [:div.flex.items-center
    {:style {:height "100%"}}
    label-html
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

(defn desktop-length-picker-rows
  [{:keys [product-sold-out-style selected-length selections options selected-auxiliary-lengths sku-quantity navigation-event]}]
  [:div.hide-on-mb.items-center.border-top.border-cool-gray.border-width-2
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
   (for [[idx auxiliary-selection] (zipmap (range) selected-auxiliary-lengths)]
     (field
      {:class "col-7"}
      (desktop-dropdown
       [:div.proxima.title-3.shout.bg-pink "Length"]
       [:span.medium
        product-sold-out-style
        (pr-str auxiliary-selection)] ; TODO thread in aux selected lengths
       (invisible-select
        {:on-change #(messages/handle-message events/control-product-detail-picker-option-auxiliary-select ; TODO make effect handler to save off the selection
                                              {:index            idx
                                               :value            (.-value (.-target %))})
         :value     (:hair/length auxiliary-selection)
         :options   (map (fn [option]
                           [:option {:value (:option/slug option)
                                     :key   (str "length-" (:option/slug option))}
                            (:option/name option)])
                         (:hair/length options))}))))])

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

;; TODO on second thought, split this into two parts
(defn mobile-length-picker-rows
  [{:keys [selected-length product-sold-out-style selected-auxiliary-lengths sku-quantity]}]
  [:div.hide-on-tb-dt.items-center.border-top.border-cool-gray.border-width-2
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
   (for [[idx auxiliary-selection] (zipmap (range) selected-auxiliary-lengths)]
     (field
      (merge
       {:class     " col-7 py2 bg-yellow"
        :data-test "picker-length"}
       (utils/fake-href events/control-product-detail-picker-open {:facet-slug :hair/length
                                                                   :index      idx}))
      (mobile-dropdown
       [:div.h8.proxima.title-3.shout "Length"]
       [:span.medium.canela.content-2
        (merge
         {:data-test (str "picker-selected-length-" (:option/slug auxiliary-selection))}
         product-sold-out-style)
        (:option/name auxiliary-selection)])))])

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
             {:data-test (str "picker-selected-color-" (facets/hacky-fix-of-bad-slugs-on-facets (:option/slug selected-color)))}
             product-sold-out-style)
      (:option/name selected-color)]
     (:option/rectangle-swatch selected-color)))])

(defn desktop-base-material-picker-row
  [{:keys [navigation-event selected-base-material selections options product-sold-out-style]}]
  [:div.hide-on-mb.border-top.border-cool-gray.border-width-2
   (field
    (base-material-desktop-dropdown
     [:span.proxima.title-3.shout
      "Base Material"]
     [:span product-sold-out-style (:option/name selected-base-material)]
     (invisible-select
      {:value     (:hair/base-material selections)
       :on-change #(messages/handle-message events/control-product-detail-picker-option-select
                                            {:selection        :hair/base-material
                                             :navigation-event navigation-event
                                             :value            (.-value (.-target %))})
       :options   (map (fn [option]
                         [:option {:value (:option/slug option)
                                   :key   (str "base-material-" (:option/slug option))}
                          (:option/name option)])
                       (:hair/base-material options))})))])

(defn mobile-base-material-picker-row
  [{:keys [selected-base-material product-sold-out-style]}]
  [:div.hide-on-tb-dt.border-top.border-cool-gray.border-width-2
   (field
    (merge {:data-test "picker-material"}
           (utils/fake-href events/control-product-detail-picker-open {:facet-slug :hair/base-material}))
    (base-material-mobile-dropdown
     [:span.proxima.title-3.shout
      "Base Material"]
     [:span (merge
             {:data-test (str "picker-selected-base-material-" (facets/hacky-fix-of-bad-slugs-on-facets (:option/slug selected-base-material)))}
             product-sold-out-style)
      (:option/name selected-base-material)]))])


;; The main panel, containing the faces of all pickers
(defn picker-rows
  "individual elements as in: https://app.zeplin.io/project/5a9f159069d48a4c15497a49/screen/5b21aa0352b1d5e31a32ac53"
  [{:as data :keys [multiple-lengths-pdp?]}]
  [:div.mxn2.mt2
   [:div.px3
    (mobile-color-picker-row data)
    (desktop-color-picker-row data)]
   (when (contains? (:options data) :hair/base-material)
     [:div.px3
      (mobile-base-material-picker-row data)
      (desktop-base-material-picker-row data)])
   [:div.px3
    (if multiple-lengths-pdp?
      (desktop-length-picker-rows data)
      (desktop-length-and-quantity-picker-rows data))
    (if multiple-lengths-pdp?
      (mobile-length-picker-rows data)
      (mobile-length-and-quantity-picker-rows data))]])

(defn select-and-close [close-event select-event options]
  (messages/handle-message select-event options)
  (messages/handle-later close-event nil))

(defn quantity-option [{:keys [key quantity primary-label checked? close-event select-event]}]
  (let [label-style (cond
                      checked?  "medium"
                      :else     nil)]
    [:div {:key       key
           :data-test (str "picker-quantity-" quantity)}
     (ui/option {:height   "4em"
                 :key      (str key "-option")
                 :on-click #(select-and-close close-event select-event {:value quantity})}
                (simple-content-layer
                 [:div.col-2
                  (when label-style
                    {:class label-style})
                  primary-label])
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

(defn color-option-drop-down-item
  [{:keys [key color sku-image checked? selected-picker navigation-event close-event select-event]}]
  [:div {:key       key
         :data-test (str "picker-color-" (facets/hacky-fix-of-bad-slugs-on-facets (:option/slug color)))}
   (ui/option {:key      (str key "-option")
               :on-click #(select-and-close close-event select-event {:selection        selected-picker
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

(defn simple-option-drop-down-item
  [{:keys [item key data-test primary-label secondary-label checked? selected-picker navigation-event close-event select-event]}]
  [:div {:key       key
         :data-test data-test}
   (ui/option {:height   "4em"
               :key      (str key "-option")
               :href     "#"
               :on-click #(select-and-close close-event select-event {:selection        selected-picker
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
  [{:keys [title items cell-component-fn wrap?]
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
             (utils/fake-href events/control-product-detail-picker-close))
      (svg/x-sharp {:height "12px" :width "12px"})]]]
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
           picker-visible?
           length-guide-image]
    :as   data} owner _]
  [:div
   (slide-animate
    picker-visible?
    (case selected-picker
      :hair/color         (picker-dialog {:title             (get-in facets [selected-picker :facet/name])
                                          :items             (sort-by :option/order (get options selected-picker))
                                          :wrap?             false
                                          :cell-component-fn (fn [item]
                                                               (color-option-drop-down-item
                                                                {:key              (str "color-" (:option/name item))
                                                                 :selected-picker  selected-picker
                                                                 :navigation-event navigation-event
                                                                 :color            item
                                                                 :checked?         (= (:hair/color selections)
                                                                                      (:option/slug item))
                                                                 :sku-image        (:option/sku-swatch item)
                                                                 :close-event      events/control-product-detail-picker-close
                                                                 :select-event     events/control-product-detail-picker-option-select}))})
      :hair/base-material (picker-dialog (merge
                                          {:title             (get-in facets [selected-picker :facet/name])
                                           :items             (sort-by :option/order (get options selected-picker))
                                           :cell-component-fn (fn [item]
                                                                (simple-option-drop-down-item
                                                                 {:key              (str "base-material-" (:option/name item))
                                                                  :data-test        (str "picker-base-material-" (:option/slug item))
                                                                  :primary-label    (:option/name item)
                                                                  :navigation-event navigation-event
                                                                  :checked?         (= (:hair/base-material selections)
                                                                                       (:option/slug item))
                                                                  :selected-picker  selected-picker
                                                                  :item             item
                                                                  :close-event      events/control-product-detail-picker-close
                                                                  :select-event     events/control-product-detail-picker-option-select}))}

                                          (when length-guide-image
                                            {:title-cta/id      "length-picker-length-guide"
                                             :title-cta/target  [events/popup-show-length-guide {:length-guide-image length-guide-image
                                                                                                 :location           "length-picker"}]
                                             :title-cta/primary "Length Guide"})))
      :hair/length (picker-dialog (merge
                                   {:title             (get-in facets [selected-picker :facet/name])
                                    :items             (sort-by :option/order (get options selected-picker))
                                    :cell-component-fn (fn [item]
                                                         (simple-option-drop-down-item
                                                          {:key              (str "length-" (:option/name item))
                                                           :data-test        (str "picker-length-" (:option/slug item))
                                                           :primary-label    (:option/name item)
                                                           :navigation-event navigation-event
                                                           :checked?         (= (:hair/length selections)
                                                                                (:option/slug item))
                                                           :selected-picker  selected-picker
                                                           :item             item
                                                           :close-event      events/control-product-detail-picker-close
                                                           :select-event     events/control-product-detail-picker-option-select}))}

                                          (when length-guide-image
                                            {:title-cta/id      "length-picker-length-guide"
                                             :title-cta/target  [events/popup-show-length-guide {:length-guide-image length-guide-image
                                                                                                 :location           "length-picker"}]
                                             :title-cta/primary "Length Guide"})))
      :item/quantity (picker-dialog {:title             "Quantity"
                                     :items             (range 1 11)
                                     :cell-component-fn (fn [quantity]
                                                          (quantity-option
                                                           {:key           (str "quantity-" quantity)
                                                            :primary-label (str quantity)
                                                            :checked?      (= quantity sku-quantity)
                                                            :quantity      quantity
                                                            :close-event   events/control-product-detail-picker-close
                                                            :select-event  events/control-product-detail-picker-option-quantity-select}))})
      nil))
   (if (seq options)
     (picker-rows data)
     [:div.py2])])

(defn query
  [data length-guide-image]
  (let [options              (get-in data catalog.keypaths/detailed-product-options)
        selected-picker      (get-in data catalog.keypaths/detailed-product-selected-picker)
        picker-visible?      (get-in data catalog.keypaths/detailed-product-picker-visible?)
        current-product      (products/current-product data)
        product-skus         (products/extract-product-skus data current-product)
        product-sold-out?    (every? (comp not :inventory/in-stock?) product-skus)
        facets               (facets/by-slug data)
        selections           (get-in data catalog.keypaths/detailed-product-selections)
        auxiliary-selections (get-in data catalog.keypaths/detailed-product-auxiliary-selections)]
    {:selected-picker            selected-picker
     :picker-visible?            (and options selected-picker picker-visible?)
     :facets                     facets
     :selections                 selections
     :options                    options
     :selected-color             (get-in facets [:hair/color :facet/options (:hair/color selections)])
     :selected-length            (get-in facets [:hair/length :facet/options (:hair/length selections)])
     :selected-auxiliary-lengths (map #(get-in facets [:hair/length :facet/options (:hair/length %)]) auxiliary-selections)
     :selected-base-material     (get-in facets [:hair/base-material :facet/options (:hair/base-material selections)])
     :product-sold-out-style     (when product-sold-out? {:class "gray"})
     :sku-quantity               (get-in data keypaths/browse-sku-quantity 1)
     :navigation-event           events/navigate-product-details
     :length-guide-image         length-guide-image
     :multiple-lengths-pdp?      (and (-> current-product :hair/family first (= "bundles"))
                                      (experiments/multiple-lengths-pdp? data))}))
