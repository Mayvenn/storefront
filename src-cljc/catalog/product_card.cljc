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

(defmulti unconstrained-facet (fn [product skus facets facet] facet))

(defmethod unconstrained-facet :hair/length
  [product skus facets facet]
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
  [product skus facets facet]
  [:div
   (let [colors (->> skus
                     (map #(get % :hair/color))
                     distinct)]
     (when (> (count colors) 1)
       [:p.h6.dark-gray
        (for [color-url (map #(facet-image facets facet %)
                             colors)]
          [:img.mx1.border-light-gray
           {:width  10
            :height 10
            :src    (first color-url)}])]))
   #_
   (let [origin (some-> product :hair/origin first)
         family (some-> product :hair/family first)]
     (when (and (#{"brazilian" "malaysian"} origin)
                (not (#{"lace-front-wigs" "360-wigs"} family)))
       [:p.h6.teal "Bestseller!"]))])

(defn query [data product]
  (let [selections (get-in data catalog.keypaths/category-selections)
        skus       (vals (select-keys (get-in data keypaths/skus)
                                      (:selector/skus product)))
        epitome    (first (sort-by :price (selector/query skus
                                                          selections
                                                          {:in-stock? #{true}})))]
    {:product   product
     :skus      skus
     :epitome   epitome
     :sold-out? (nil? epitome)
     :title     (:sku-set/title product)
     :slug      (:page/slug product)
     :image     (->> epitome :images (filter (comp #{"catalog"} :use-case)) first)
     :facets    (get-in data keypaths/facets)
     :affirm?   (experiments/affirm? data)
     :selections (get-in data catalog.keypaths/category-selections)}))

(defn component
  [{:keys [product skus epitome sold-out? title slug image facets affirm?]}]
  [:div.col.col-6.col-4-on-tb-dt.px1
   {:key slug}
   [:a.inherit-color
    (assoc (utils/route-to events/navigate-product-details
                           {:catalog/product-id (:catalog/product-id product)
                            :page/slug          slug
                            :query-params       {:SKU (:sku epitome)}})
           :data-test (str "product-" slug))
    [:div.center
     ;; TODO: when adding aspect ratio, also use srcset/sizes to scale these images.
     [:img.block.col-12 {:src (str (:url image) "-/format/auto/" (:filename image))
                         :alt (:alt image)}]
     [:h2.h4.mt3.mb1 title]
     (if sold-out?
       [:p.h6.dark-gray "Out of stock"]
       [:div
        (for [selector (reverse (:selector/electives product))]
          [:div {:key selector}
           (unconstrained-facet product skus facets selector)])
        [:p.h6 "Starting at " (mf/as-money-without-cents (:price epitome 0))]])]]
   [:p.mb10.center
    (when affirm?
      [:div.h6.dark-gray
       (component/build affirm/as-low-as-component {:amount (:price epitome)
                                                    :type   :text-only} {})])]])
