(ns adventure.components.product-card
  (:require catalog.keypaths
            [catalog.selector :as selector]
            [catalog.facets :as facets]
            [spice.selector :as spice-selector]
            [storefront.accessors.skus :as skus]
            [storefront.accessors.experiments :as experiments]
            [storefront.component :as component]
            [storefront.components.money-formatters :as mf]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            adventure.keypaths
            [storefront.platform.component-utils :as utils]
            [adventure.keypaths :as adventure.keypaths]))

(defn slug->facet [facet facets]
  (->> facets
       (filter (fn [{:keys [:facet/slug]}] (= slug facet)))
       first))

(defn slug->option [option options]
  (->> options
       (filter (fn [{:keys [:option/slug]}] (= slug option)))
       first))

(defmulti unconstrained-facet
  (fn [color-order-map product skus facets facet]
    facet))

(defmethod unconstrained-facet :default
  [color-order-map product skus facets facet]
  [:div])

(defmethod unconstrained-facet :hair/length
  [color-order-map product skus facets facet]
  (let [lengths              (->> skus
                                  (mapcat #(get % :hair/length))
                                  sort)
        length-facet-options (->> facets (slug->facet :hair/length) :facet/options)
        shortest             (->> length-facet-options
                                  (slug->option (first lengths))
                                  :option/name)
        longest              (->> length-facet-options
                                  (slug->option (last lengths))
                                  :option/name)]
    [:p.h6.dark-gray
     "in "
     (if (= shortest longest)
       shortest
       [:span shortest " - " longest])]))

(defn facet-image
  [facets facet option]
  (->> facets
       (filter #(= facet (:facet/slug %)))
       first
       :facet/options
       (filter #(= option (:option/slug %)))
       (map :option/image)))

(defmethod unconstrained-facet :hair/color
  [color-order-map product skus facets facet-slug]
  (let [sorted-product-colors (distinct (->> skus
                                             (mapcat #(get % :hair/color))
                                             (sort-by color-order-map)))]
    [:div
     (when (> (count sorted-product-colors) 1)
       [:p.h6.dark-gray
        (for [color-url (map #(facet-image facets facet-slug %)
                             sorted-product-colors)]
          [:img.mx1.border-light-gray
           {:width  10
            :height 10
            :src    (first color-url)}])])]))

(defn sku-best-matching-selections [selections skus]
  (->> skus
       (spice-selector/match-all {:selector/complete? true}
                                 {:hair/length (:hair/length selections)})
       first))

(defn query [data product]
  (let [skus-matching-color       (get-in data adventure.keypaths/adventure-matching-skus-color)
        facets                    (get-in data keypaths/v2-facets)
        color-order-map           (facets/color-order-map facets)
        in-stock-skus             (selector/query skus-matching-color {} {:inventory/in-stock? #{true}})
        ;; in order to fill the product card, we should always have a sku to use for
        ;; the cheapest-sku and epitome
        skus-to-search            (->> (or (not-empty in-stock-skus)
                                           skus-matching-color)
                                       (filter #(= (set (:hair/family %))
                                                   (:hair/family product))))
        ;; It is technically possible for the cheapest sku to not be the epitome
        cheapest-sku              (->> skus-to-search
                                       (sort-by :sku/price)
                                       first)
        ;; Product definition of epitome is the "first" SKU on the product details page where
        ;; first is when the first of every facet is selected.
        ;;
        ;; We're being lazy and sort by color facet + sku price (which implies sort by hair/length)
        epitome                   (skus/determine-epitome color-order-map skus-to-search)]
    {:product                          product
     :skus                             skus-matching-color
     :epitome                          epitome
     :cheapest-sku                     cheapest-sku
     :color-order-map                  color-order-map
     :sold-out?                        (empty? in-stock-skus)
     :title                            (:copy/title product)
     :slug                             (:page/slug product)
     :image                            (->> epitome
                                            :selector/images
                                            (filter (comp #{"catalog"} :use-case))
                                            (sort-by (comp color-order-map first :hair/color))
                                            first)
     :facets                           facets}))

(defn component
  [{:keys [product skus cheapest-sku epitome sku-matching-previous-selections sold-out? title slug image facets color-order-map]}]
  (component/create
   [:a.block.col-6.col-4-on-tb-dt.p1.my2.black.flex.flex-stretch
    (assoc (utils/route-to events/navigate-product-details
                           {:catalog/product-id (:catalog/product-id product)
                            :page/slug          slug
                            :query-params       {:SKU (:catalog/sku-id epitome)}})
           :data-test (str "product-" slug)
           :key slug)
    [:div.bg-white.border-light-gray.border
     ;; TODO: when adding aspect ratio, also use srcset/sizes to scale these images.
     [:img.block.col-12 {:src (str (:url image) "-/format/auto/" (:filename image))
                         :alt (:alt image)}]
     [:div.p1
      [:h2.h4.mt3.mb1 title]
      (if sold-out?
        [:p.h6.dark-gray "Out of stock"]
        [:div
         (for [selector (reverse (:selector/electives product))]
           [:div {:key selector}
            (unconstrained-facet color-order-map product skus facets (keyword selector))])
         [:p.h6.mb4 "Starting at " (mf/as-money-without-cents (:sku/price cheapest-sku 0))]])]]]))
