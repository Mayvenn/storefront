(ns catalog.ui.product-card
  (:require catalog.keypaths
            [catalog.facets :as facets]
            [spice.selector :as selector]
            [storefront.accessors.skus :as skus]
            [storefront.components.money-formatters :as mf]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.ui :as ui]))

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
         (mapcat #(get % facet-slug))
         set
         (map #(slug->option % facet-options))
         (sort-by (juxt :option/order :option/slug)))))

(defn query
  [data product]
  (let [skus                (vals (select-keys (get-in data keypaths/v2-skus)
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

        cheapest-sku (skus/determine-cheapest color-order-map skus-to-search)
        epitome      (skus/determine-epitome color-order-map skus-to-search)

        image (->> epitome
                   :selector/images
                   (filter (comp #{"catalog"} :use-case))
                   first)]
    {:sort/value                   (:sku/price cheapest-sku)
     :card/type                    :product
     :react/key                    (str "product-" product-slug)
     :product-card-title/id        (some->> product-slug (str "product-card-title-")) ;; TODO: better display-decision id
     :product-card-title/primary   (:copy/title product)
     :product-card/target          [events/navigate-product-details
                                    {:catalog/product-id (:catalog/product-id product)
                                     :page/slug          product-slug
                                     :query-params       {:SKU (:catalog/sku-id
                                                                (sku-best-matching-selections
                                                                 (merge product-detail-selections category-selections)
                                                                 skus
                                                                 color-order-map))}}]
     :product-card-details/id      (str "product-card-details-" product-slug)
     :product-card-details/content (if (empty? in-stock-skus)
                                     [[:text "Out of stock"]]
                                     [[:text (str "in "
                                                  (if (= shortest longest)
                                                    shortest
                                                    (str shortest " - " longest)))]
                                      (if (= 1 (count product-colors))
                                        [:text (:option/name (first product-colors))]
                                        [:swatches {:product-slug   product-slug
                                                    :product-colors product-colors}])
                                      [:starting-price (:sku/price cheapest-sku)]])
     :card-image/src               (str (:url image) "-/format/auto/" (:filename image))
     :card-image/alt               (:alt image)}))

(defcomponent card-image-molecule
  [{:keys [card-image/src card-image/alt screen/seen?]} _ _]
  ;; TODO: when adding aspect ratio, also use srcset/sizes to scale these images.
  (cond
    (nil? seen?) [:noscript
                  (ui/aspect-ratio
                   640 580
                   (ui/ucare-img
                    {:class "block col-12 container-height"
                     :style {:border-radius "5px 5px 0 0"}
                     :alt   alt}
                    src))]
    seen?        (ui/aspect-ratio
                  640 580
                  (ui/ucare-img
                   {:class "block col-12 container-height"
                    :style {:border-radius "5px 5px 0 0"}
                    :alt   alt}
                   src))
    :else        (ui/aspect-ratio
                  640 580
                  [:div.col-12.container-height
                   {:style {:height "100%"}}])))

(defn product-card-title-molecule
  [{:product-card-title/keys [id primary]}]
  (component/html
   (when id
     (component/html
      [:h2.mt3.mx1.content-2.proxima
       primary]))))

(defcomponent swatches
  [{:keys        [product-slug product-colors]
    :screen/keys [seen?]} _ _]
  [:div.flex.col-10.mx-auto.justify-center
   (when seen? ;; explicitly ignoring SSR to minimize number of dom nodes
     (for [{option-slug  :option/slug
            :option/keys [rectangle-swatch]} product-colors]
       [:div.mx1.overflow-hidden
        {:style {:transform "rotate(45deg)"
                 :width     "9px"
                 :height    "9px"
                 :padding   "0"}
         :key option-slug}
        [:img
         {:key   (str "product-card-details-" product-slug "-" option-slug)
          :style {:transform "rotate(-45deg) translateY(-3px)"
                  ;; :margin     "5px 5px"
                  :width     "13px"
                  :height    "13px"}
          :src   rectangle-swatch}]]))])

(defn product-card-details-molecule
  [{:product-card-details/keys [id content]}]
  (component/html
   (when id
     [:div.mb4.content-3.proxima
      (for [[idx [type item]] (map-indexed vector content)]
        [:div.py1 {:key (str id "-" idx)}
         (case type
           :text item
           :swatches (ui/screen-aware swatches item)
           :starting-price [:span.black
                            "Starting at "
                            [:span.content-2.proxima (mf/as-money item)]]
           item)])])))

(defn organism
  [{:as data react-key :react/key :product-card/keys [target]}]
  (component/html
   [:a.inherit-color.col.col-6.col-4-on-tb-dt.p1
    (merge (apply utils/route-to target)
           {:key       react-key
            :data-test react-key})
    [:div.border.border-cool-gray.rounded.container-height.center
     ^:inline (ui/screen-aware card-image-molecule data)
     ^:inline (product-card-title-molecule data)
     ^:inline (product-card-details-molecule data)]]))
