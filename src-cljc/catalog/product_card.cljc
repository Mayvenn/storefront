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
   [catalog.selector :as selector]))

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
                      (map #(get % :hair/length))
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
                                        (map #(get % :hair/color))
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
        skus            (vals (select-keys (get-in data keypaths/skus)
                                           (:selector/skus product)))
        facets          (get-in data keypaths/facets)
        color-order-map (->> facets
                             (filter #(= (:facet/slug %) :hair/color))
                             first
                             :facet/options
                             (sort-by :filter/order)
                             (map :option/slug)
                             (map-indexed (fn [idx slug] [slug idx]))
                             (into {}))
        epitome         (->> (selector/query skus selections {:in-stock? #{true}})
                             (group-by :price)
                             (sort-by first)
                             vals
                             first
                             (sort-by (comp color-order-map :hair/color))
                             first)]
    {:product         product
     :skus            skus
     :epitome         epitome
     :color-order-map color-order-map
     :sold-out?       (nil? epitome)
     :title           (:sku-set/title product)
     :slug            (:page/slug product)
     :image           (->> epitome :images (filter (comp #{"catalog"} :use-case)) first)
     :facets          facets
     :selections      (get-in data catalog.keypaths/category-selections)
     :bestseller?     (experiments/bestseller? data)}))

(def best-seller-badge
  [:div.circle.absolute.top-0.right-0.bg-teal.flex.justify-center.mp6
   {:style {:width "45px" :height "45px"}}
   [:span.h7.white.letter-spacing-0.bold.self-center
    {:style {:line-height "11px"}}
    "Best" [:br] "Seller"]])

(defn component
  [{:keys [product skus epitome sold-out? title slug image facets color-order-map bestseller?]}]
  [:div.col.col-6.col-4-on-tb-dt.px1
   {:key slug}
   [:a.inherit-color
    (assoc (utils/route-to events/navigate-product-details
                           {:catalog/product-id (:catalog/product-id product)
                            :page/slug          slug
                            :query-params       {:SKU (:sku epitome)}})
           :data-test (str "product-" slug))
    [:div.center.relative
     ;; TODO: when adding aspect ratio, also use srcset/sizes to scale these images.
     [:img.block.col-12 {:src (str (:url image) "-/format/auto/" (:filename image))
                         :alt (:alt image)}]
     (let [origin (some-> product :hair/origin first)]
       (when (and bestseller? (#{"brazilian" "malaysian"} origin)) best-seller-badge))
     [:h2.h4.mt3.mb1 title]
     (if sold-out?
       [:p.h6.dark-gray "Out of stock"]
       [:div
        (for [selector (reverse (:selector/electives product))]
          [:div {:key selector}
           (unconstrained-facet color-order-map product skus facets selector)])
        [:p.h6 "Starting at " (mf/as-money-without-cents (:price epitome 0))]])]]
   [:p.mb10.center
    [:div.h6.dark-gray
     (component/build affirm/as-low-as-component {:amount (:price epitome)
                                                  :type   :text-only} {})]]])
