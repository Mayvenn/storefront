(ns catalog.product-card
  (:require
   #?(:cljs [storefront.component :as component]
      :clj  [storefront.component-shim :as component])
   [storefront.components.affirm :as affirm]
   [storefront.accessors.experiments :as experiments]
   [storefront.platform.component-utils :as utils]
   [storefront.components.money-formatters :as mf]
   [storefront.events :as events]
   [storefront.keypaths :as keypaths]
   [catalog.keypaths]
   [catalog.selector :as selector]
   [spice.core :as spice]))

(defn slug->facet [facet facets]
  (->> facets
       (filter (fn [{:keys [:facet/slug]}] (= slug facet)))
       first))

(defn slug->option [option options]
  (->> options
       (filter (fn [{:keys [:option/slug]}] (= slug option)))
       first))

(defmulti unconstrained-facet (fn [color-order-map product skus facets facet] facet))

(defmethod unconstrained-facet :hair/length
  [_color-order-map product skus facets facet]
  (let [lengths  (->> skus
                      (mapcat #(get % :hair/length))
                      sort)
        shortest (first lengths)
        longest  (last lengths)]
    [:p.h6.dark-gray
     "in "
     (->> facets
          (slug->facet :hair/length)
          :facet/options
          (slug->option shortest)
          :option/name)
     " - "
     (->> facets
          (slug->facet :hair/length)
          :facet/options
          (slug->option longest)
          :option/name)]))

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
  (let [sorted-product-colors (set (->> skus
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

(defn query [data product]
  (let [selections      (get-in data catalog.keypaths/category-selections)
        skus            (vals (select-keys (get-in data keypaths/v2-skus)
                                           (:selector/skus product)))
        facets          (get-in data keypaths/v2-facets)
        color-order-map (->> facets
                             (filter #(= (:facet/slug %) :hair/color))
                             first
                             :facet/options
                             (sort-by :filter/order)
                             (map :option/slug)
                             (map-indexed (fn [idx slug] [slug idx]))
                             (into {}))
        in-stock-skus   (selector/query skus selections {:inventory/in-stock? #{true}})
        ;; It is technically possible for the cheapest sku to not be the epitome
        cheapest-sku    (->> in-stock-skus
                             (sort-by :sku/price)
                             first)
        ;; Product definition of epitome is the "first" SKU on the product details page where
        ;; first is when the first of every facet is selected.
        ;;
        ;; We're being lazy and sort by color facet + sku price (which implies sort by hair/length)
        epitome         (->> in-stock-skus
                             (sort-by (juxt (comp color-order-map first :hair/color)
                                            :sku/price))
                             first)]
    {:product         product
     :skus            skus
     :epitome         epitome
     :cheapest-sku    cheapest-sku
     :color-order-map color-order-map
     :sold-out?       (nil? epitome)
     :title           (:copy/title product)
     :slug            (:page/slug product)
     :image           (->> epitome
                           :selector/images
                           (filter (comp #{"catalog"} :use-case))
                           (sort-by (comp color-order-map first :hair/color))
                           first)
     :facets          facets
     :selections      (get-in data catalog.keypaths/category-selections)}))

(defn component
  [{:keys [product skus cheapest-sku epitome sold-out? title slug image facets color-order-map]}]
  [:div.col.col-6.col-4-on-tb-dt.px1
   {:key slug}
   [:a.inherit-color
    (assoc (utils/route-to events/navigate-product-details
                           {:catalog/product-id (:catalog/product-id product)
                            :page/slug          slug
                            :query-params       {:SKU (:catalog/sku-id epitome)}})
           :data-test (str "product-" slug))
    [:div.center.relative
     ;; TODO: when adding aspect ratio, also use srcset/sizes to scale these images.
     [:img.block.col-12 {:src (str (:url image) "-/format/auto/" (:filename image))
                         :alt (:alt image)}]
     [:h2.h4.mt3.mb1 title]
     (if sold-out?
       [:p.h6.dark-gray "Out of stock"]
       [:div
        (for [selector (reverse (:selector/electives product))]
          [:div {:key selector}
           (unconstrained-facet color-order-map product skus facets selector)])
        [:p.h6 "Starting at " (mf/as-money-without-cents (:sku/price cheapest-sku 0))]])]]
   [:p.mb10.center
    [:div.h6.dark-gray
     (component/build affirm/as-low-as-component {:amount (:sku/price epitome)
                                                  :type   :text-only} {})]]])
