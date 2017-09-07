(ns catalog.product-card
  (:require
   [storefront.platform.component-utils :as utils]
   [storefront.components.money-formatters :as mf]
   [storefront.events :as events]))

(defn slug->facet [facet facets]
  (->> facets
       (filter (fn [{:keys [:facet/slug]}] (= slug facet)))
       first))

(defn slug->option [option options]
  (->> options
       (filter (fn [{:keys [:option/slug]}] (= slug option)))
       first))

(defmulti unconstrained-facet (fn [product facets facet] facet))

(defmethod unconstrained-facet :hair/length
  [{:keys [matching-skus]} facets facet]
  (let [lengths  (->> matching-skus
                      (map #(get-in % [:attributes :hair/length]))
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

(defmethod unconstrained-facet :hair/color
  [{:keys [matching-skus criteria/essential] :as product} facets facet]
  (let [origin (some-> essential :hair/origin first)]
    (if (#{"brazilian" "malaysian"} origin)
      [:p.h6.teal "Bestseller!"]
      (let [colors (->> matching-skus
                        (map #(get-in % [:attributes :hair/color]))
                        distinct)]
        (when (> (count colors) 1)
          [:p.h6.dark-gray "+ more colors available"])))))

(defn component
  [{:keys [sku-set/slug
           representative-sku
           sku-set/name
           sold-out?] :as product}
   facets]
  (let [image (->> representative-sku :images (filter (comp #{"catalog"} :use-case)) first)]
    [:div.col.col-6.col-4-on-tb-dt.px1
     {:key slug}
     [:a.inherit-color
      ;; TODO: use the representative sku to preselect the options on product details
      (utils/route-to events/navigate-product-details {:id   (:sku-set/id product)
                                                       :slug (:sku-set/slug product)})
      [:div.mb10.center
       ;; TODO: when adding aspect ratio, also use srcset/sizes to scale these images.
       [:img.block.col-12 {:src (str (:url image) "-/format/auto/" (:filename image))
                           :alt (:alt image)}]
       [:h2.h4.mt3.mb1 name]
       (if sold-out?
         [:p.h6.dark-gray "Out of stock"]
         [:div
          (for [selector (:criteria/selectors product)]
            (unconstrained-facet product facets (keyword selector)))
          [:p.h6 "Starting at " (mf/as-money-without-cents (:price representative-sku))]])]]]))
