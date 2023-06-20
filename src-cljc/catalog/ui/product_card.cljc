(ns catalog.ui.product-card
  (:require catalog.keypaths
            [catalog.facets :as facets]
            [spice.selector :as selector]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.skus :as skus]
            [storefront.accessors.images :as images]
            [storefront.components.money-formatters :as mf]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.ui :as ui] ))

(defn- slug->facet [facet facets]
  (->> facets
       (filter (fn [{:keys [:facet/slug]}] (= slug facet)))
       first))

(defn- slug->option [option options]
  (->> options
       (filter (fn [{:keys [:option/slug]}] (= slug option)))
       first))

(defn sku-best-matching-selections
  "Find the sku best matching selectors, falling back to trying one facet at a time"
  [selections skus color-order-map]
  (some (fn [criteria]
          (skus/determine-epitome color-order-map
                                  (selector/match-all {:selector/complete? true} criteria skus)))
        [selections
         {:hair/color (:hair/color selections)}
         {:hair/length (:hair/length selections)}
         {}]))

(defn product-options
  [facets skus facet-slug]
  (let [facet-options (->> facets (slug->facet facet-slug) :facet/options)]
    (->> skus
         (into #{} (mapcat #(get % facet-slug)))
         (mapv #(slug->option % facet-options))
         (sort-by (juxt :option/order :option/slug)))))

(defn query-new ;; TODO: rename to query
  [data product]
  (let [images-catalog            (get-in data keypaths/v2-images)
        skus                      (vals (select-keys (get-in data keypaths/v2-skus)
                                                     (:selector/skus product)))
        facets                    (get-in data keypaths/v2-facets)
        category-selections       (get-in data catalog.keypaths/category-selections)

        color-order-map           (facets/color-order-map facets)
        in-stock-skus             (selector/match-all {}
                                                      (assoc category-selections :inventory/in-stock? #{true})
                                                      skus)
        skus-to-search            (or (not-empty in-stock-skus) skus)
        product-detail-selections (get-in data catalog.keypaths/detailed-product-selections)

        product-slug              (:page/slug product)
        product-colors            (product-options facets skus :hair/color)
        [shortest longest]        (->> (product-options facets skus :hair/length)
                                       ((juxt first last))
                                       (mapv :option/name))
        [lightest heaviest]       (->> (product-options facets skus :hair/weight)
                                       ((juxt first last))
                                       (mapv :option/name))
        cheapest-sku              (skus/determine-cheapest color-order-map skus-to-search)
        epitome                   (skus/determine-epitome color-order-map skus-to-search)

        image                     (->> epitome
                                       (images/for-skuer images-catalog)
                                       (filter (comp #{"catalog"} :use-case))
                                       first)]
    {:new?                                  true
     :sort/value                            (:sku/price cheapest-sku)
     :card/type                             :product
     :react/key                             (str "product-" product-slug "-" (:catalog/sku-id cheapest-sku))
     :product-card-title/id                 (some->> product-slug (str "product-card-title-")) ;; TODO: better display-decision id
     :product-card-title/primary            (str (:copy/title product)
                                                 (when (= 1 (count product-colors))
                                                   (->> product-colors
                                                        first
                                                        :option/name
                                                        (str " - "))))
     :product-card/id                       (str "product-" product-slug (->> product-colors
                                                                              first
                                                                              :option/slug
                                                                              (str "-")))
     :product-card/target                   [events/navigate-product-details
                                             {:catalog/product-id (:catalog/product-id product)
                                              :page/slug          product-slug
                                              :query-params       {:SKU (:catalog/sku-id
                                                                         (sku-best-matching-selections
                                                                          (merge product-detail-selections category-selections)
                                                                          skus
                                                                          color-order-map))}}]
     :product-card-details/id               (str "product-card-details-" product-slug)
     :product-card-details/specs            (str (if (= shortest longest)
                                                   shortest
                                                   (str shortest " - " longest))
                                                 (when lightest
                                                   (str " | "
                                                        (if (= lightest heaviest)
                                                          lightest
                                                          (str lightest " - " heaviest)))))
     :product-card-details/colors           (when (< 1 (count product-colors))
                                              product-colors)
     :product-card-details/price            (:sku/price cheapest-sku)
     :product-card-details/discounted-price (when (->> cheapest-sku (and (seq in-stock-skus)) :promo.clearance/eligible first)
                                              (* 0.65 (:sku/price cheapest-sku)))
     :card-image/src                        (str (:url image) "-/format/auto/" (:filename image))
     :card-image/alt                        (:alt image)}))

(defcomponent card-image-molecule
  [{:keys [card-image/src card-image/alt]} _ _]
  ;; TODO: when adding aspect ratio, also use srcset/sizes to scale these images.
  (ui/aspect-ratio 
   17 20
   (ui/defer-ucare-img
    {:class    "block col-12 container-height"
     :alt      alt
     :max-size 640
     :crop     "17:20"
     :retina?  false
     :placeholder-attrs {:class "col-12 container-height" :style {:height "100%"}}}
    src)))

(defn product-card-title-molecule
  [{:product-card-title/keys [id primary]}]
  (when id
    (component/html
     [:div.proxima.text-sm.pb2 primary])))

(defcomponent product-card-details-molecule
  [{:product-card-details/keys [id specs colors discounted-price price]} _ _]
  (when id
    (component/html
     [:div.proxima.text-xs
      (when specs
        [:div.my1.gray-700 {:key (str id "-specs")} "in " specs])
      (when colors
        [:div.my1 {:key (str id "-color-count")}
         (str "+ " (count colors) " colors")])
      (when price
        [:div.my1.proxima.text-base
         (if discounted-price
           [:span "from " [:span.strike (mf/as-money price)] " "
            [:span.warning-red (mf/as-money discounted-price)]]
           [:span "from " [:span.content-2.proxima (mf/as-money price)]])])])))

(defn organism-new
  [{:as                data
    react-key          :react/key
    :product-card/keys [id target]}] 
  (component/html
   [:a.inherit-color.col.col-6.col-4-on-tb-dt.p1
    (merge (apply utils/route-to target)
           {:key       react-key
            :data-test id})
    [:div.border.border-cool-gray.container-height.flex.flex-column.justify-between
     (ui/screen-aware card-image-molecule data)
     [:div.p2.flex-auto.flex.flex-column.justify-between
      (product-card-title-molecule data)
      (ui/screen-aware product-card-details-molecule data)]]]))

;; GROT all below 

(defn query-old
  [data product]
  (let [images-catalog      (get-in data keypaths/v2-images)
        skus                (vals (select-keys (get-in data keypaths/v2-skus)
                                               (:selector/skus product)))
        facets              (get-in data keypaths/v2-facets)
        category-selections (get-in data catalog.keypaths/category-selections)

        color-order-map           (facets/color-order-map facets)
        in-stock-skus             (selector/match-all {}
                                                      (assoc category-selections :inventory/in-stock? #{true})
                                                      skus)
        skus-to-search            (or (not-empty in-stock-skus) skus)
        product-detail-selections (get-in data catalog.keypaths/detailed-product-selections)

        product-slug       (:page/slug product)
        product-colors     (product-options facets skus :hair/color)
        [shortest longest] (->> (product-options facets skus :hair/length)
                                ((juxt first last))
                                (mapv :option/name))

        cheapest-sku  (skus/determine-cheapest color-order-map skus-to-search)
        epitome       (skus/determine-epitome color-order-map skus-to-search)

        image (->> epitome
                   (images/for-skuer images-catalog)
                   (filter (comp #{"catalog"} :use-case))
                   first)]
    {:sort/value                   (:sku/price cheapest-sku)
     :card/type                    :product
     :react/key                    (str "product-" product-slug "-" (:catalog/sku-id cheapest-sku))
     :product-card-title/id        (some->> product-slug (str "product-card-title-")) ;; TODO: better display-decision id
     :product-card-title/primary   (:copy/title product)
     :product-card/id              (str "product-" product-slug)
     :product-card/target          [events/navigate-product-details
                                    {:catalog/product-id (:catalog/product-id product)
                                     :page/slug          product-slug
                                     :query-params       {:SKU (:catalog/sku-id
                                                                (sku-best-matching-selections
                                                                 (merge product-detail-selections category-selections)
                                                                 skus
                                                                 color-order-map))}}]
     :product-card-details/id      (str "product-card-details-" product-slug)
     :product-card-details/content [[:text (str "in "
                                                (if (= shortest longest)
                                                  shortest
                                                  (str shortest " - " longest)))]
                                    (if (= 1 (count product-colors))
                                      [:text (:option/name (first product-colors))]
                                      [:swatches [product-slug product-colors]])
                                    (if (empty? in-stock-skus)
                                      [:text "Out of stock"]
                                      [:pricing [(:sku/price cheapest-sku)
                                                 (when (-> cheapest-sku :promo.clearance/eligible first)
                                                   (* 0.65 (:sku/price cheapest-sku)))]])]
     :card-image/src               (str (:url image) "-/format/auto/" (:filename image))
     :card-image/alt               (:alt image)}))


(defcomponent card-image-molecule-old
  [{:keys [card-image/src card-image/alt screen/seen?]} _ _]
  ;; TODO: when adding aspect ratio, also use srcset/sizes to scale these images.
  (ui/aspect-ratio
   640 580
   (ui/defer-ucare-img
    {:class   "block col-12 container-height"
     :alt     alt
     :width   640
     :retina? false
     :placeholder-attrs {:class "col-12 container-height" :style {:height "100%"}}}
    src)))

(defn product-card-title-molecule-old
  [{:product-card-title/keys [id primary]}]
  (when id
    (component/html
     [:h3.mt3.mx1.content-2.proxima
      primary])))

(defcomponent product-card-details-molecule-old
  [{:product-card-details/keys [id content]
    :screen/keys               [seen?]}
   _ _]
  (when id
    (component/html
     [:div.mb4.content-3.proxima
      (for [[idx [kind item]] (map-indexed vector content)]
        [:div.py1 {:key (str id "-" idx)}
         (case kind
           :text     (str item)
           :pricing  (let [[price discounted-price] item]
                       (if discounted-price
                         [:span.black "Starting at " [:span.strike (mf/as-money price)] " "
                          [:span.warning-red (mf/as-money discounted-price)]]
                         [:span.black "Starting at " [:span.content-2.proxima (mf/as-money price)]]))
           :swatches [:div.flex.col-10.mx-auto.justify-center
                      {:style {:height "9px"}}
                      (when seen?
                        (let [[product-slug options] item]
                          (for [{option-slug  :option/slug
                                 option-name  :option/name
                                 :option/keys [rectangle-swatch]} options]
                            [:div.mx1.overflow-hidden
                             {:style {:transform "rotate(45deg)"
                                      :width     "9px"
                                      :height    "9px"
                                      :padding   "0"}
                              :key   option-slug}
                             [:img
                              {:key   (str "product-card-details-" product-slug "-" option-slug)
                               :style {:transform "rotate(-45deg) translateY(-3px)"
                                       ;; :margin     "5px 5px"
                                       :width     "13px"
                                       :height    "13px"}
                               :alt   option-name
                               :src   (str "https://ucarecdn.com/" (ui/ucare-img-id rectangle-swatch) "/-/format/auto/-/resize/13x/")}]])))])])])))


(defn organism-old
  [{:as data react-key :react/key :product-card/keys [id target]}]
  (component/html
   [:a.inherit-color.col.col-6.col-4-on-tb-dt.p1
    (merge (apply utils/route-to target)
           {:key       react-key
            :data-test id})
    [:div.border.border-cool-gray.container-height.center
     (ui/screen-aware card-image-molecule-old data)
     (product-card-title-molecule-old data)
     (ui/screen-aware product-card-details-molecule-old data)]]))

(defn query [app-state product]
  (if (experiments/product-card-update? app-state)
    (query-new app-state product)
    (query-old app-state product)))

(defn organism
  [{:as data :keys [new?]}]
  (if new?
    (organism-new data) 
    (organism-old data)))
