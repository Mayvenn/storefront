(ns ui.product-card
  (:require catalog.keypaths
            [catalog.facets :as facets]
            [spice.selector :as selector]
            [storefront.accessors.skus :as skus]
            [storefront.components.money-formatters :as mf]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]))

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
         {:hair/length (:hair/length selections)}]))

(defn query
  [data product]
  (let [selections      (get-in data catalog.keypaths/category-selections)
        skus            (vals (select-keys (get-in data keypaths/v2-skus)
                                           (:selector/skus product)))
        facets          (get-in data keypaths/v2-facets)
        color-order-map (facets/color-order-map facets)
        in-stock-skus   (selector/match-all {}
                                            (assoc selections :inventory/in-stock? #{true})
                                            skus)

        skus-to-search            (or (not-empty in-stock-skus) skus)
        epitome                   (skus/determine-epitome color-order-map skus-to-search)
        product-detail-selections (get-in data catalog.keypaths/detailed-product-selections)
        lengths                   (->> skus
                                       (mapcat #(get % :hair/length))
                                       sort)
        length-facet-options      (->> facets (slug->facet :hair/length) :facet/options)
        image                     (->> epitome
                                       :selector/images
                                       (filter (comp #{"catalog"} :use-case))
                                       first)
        product-colors            (set (->> skus
                                            (mapcat #(get % :hair/color))))
        all-color-options         (->> facets
                                       (filter #(= :hair/color (:facet/slug %)))
                                       first
                                       :facet/options)
        product-color-swatch-urls (->> all-color-options
                                       (filter #(product-colors (:option/slug %)))
                                       (sort-by color-order-map)
                                       (map :option/circle-swatch))]
    {:product-card/product-id                          (:catalog/product-id product)
     :product-card/sku-id-matching-previous-selections (:catalog/sku-id (sku-best-matching-selections product-detail-selections
                                                                                                      skus
                                                                                                      color-order-map))
     :product-card/cheapest-sku-price                  (mf/as-money-without-cents (:sku/price (skus/determine-cheapest color-order-map skus-to-search) 0))
     :product-card/sold-out?                           (empty? in-stock-skus)
     :product-card/title                               (:copy/title product)
     :product-card/slug                                (:page/slug product)
     :length-range/shortest                            (->> length-facet-options
                                                            (slug->option (first lengths))
                                                            :option/name)
     :length-range/longest                             (->> length-facet-options
                                                            (slug->option (last lengths))
                                                            :option/name)
     :product-card-image/src                           (str (:url image) "-/format/auto/" (:filename image))
     :product-card-image/alt                           (:alt image)
     :color-swatches/urls                              product-color-swatch-urls}))

(defn component
  [{:product-card/keys       [cheapest-sku-price sku-id-matching-previous-selections sold-out? title slug product-id]
    :length-range/keys       [shortest longest]
    :product-card-image/keys [src alt]
    :color-swatches/keys     [urls]}]
  [:div.col.col-6.col-4-on-tb-dt.p1
   {:key slug}
   [:div
    {:style {:height "100%"}
     :class "border border-silver rounded"}
    [:a.inherit-color
     (assoc (utils/route-to events/navigate-product-details
                            {:catalog/product-id product-id
                             :page/slug          slug
                             :query-params       {:SKU sku-id-matching-previous-selections}})
            :data-test (str "product-" slug))
     [:div.center.relative
      ;; TODO: when adding aspect ratio, also use srcset/sizes to scale these images.
      [:img.block.col-12 {:style {:border-radius "5px 5px 0 0"}
                          :src   src
                          :alt   alt}]
      [:h2.h5.mt3.mb1.mx1.medium title]
      (if sold-out?
        [:p.h6.dark-gray "Out of stock"]
        [:div
         [:p.h6.dark-gray
          "in "
          (if (= shortest longest)
            shortest
            [:span shortest " - " longest])]
         [:div
          [:p.h6.dark-gray
           (for [color-url urls]
             (do (prn color-url)
                 [:img.mx1.border-light-gray
                  {:width  10
                   :height 10
                   :src    color-url}]))]]
         [:p.h6.mb4 "Starting at " cheapest-sku-price]])]]]])
