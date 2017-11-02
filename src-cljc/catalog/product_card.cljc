(ns catalog.product-card
  (:require
   #?(:cljs [storefront.component :as component]
      :clj  [storefront.component-shim :as component])
   [storefront.components.affirm :as affirm]
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
  [{:keys [matching-skus criteria/essential] :as product} facets facet]
  [:div
   (let [colors (->> matching-skus
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
   (let [origin (some-> essential :hair/origin first)
         family (some-> essential :hair/family first)]
     (when (and (#{"brazilian" "malaysian"} origin)
                (not (#{"lace-front-wigs" "360-wigs"} family)))
       [:p.h6.teal "Bestseller!"]))])

(defn component
  [{:keys [page/slug
           representative-sku
           sku-set/name
           sold-out?] :as product}
   facets
   affirm?]
  (let [image (->> representative-sku :images (filter (comp #{"catalog"} :use-case)) first)]
    [:div.col.col-6.col-4-on-tb-dt.px1
     {:key slug}
     [:a.inherit-color
      ;; TODO: use the representative sku to preselect the options on product details
      (assoc (utils/route-to events/navigate-product-details
                             {:catalog/product-id (:catalog/product-id product)
                              :page/slug          slug
                              :query-params       {:SKU (:sku representative-sku)}})
             :data-test (str "product-" slug))
      [:div.center
       ;; TODO: when adding aspect ratio, also use srcset/sizes to scale these images.
       [:img.block.col-12 {:src (str (:url image) "-/format/auto/" (:filename image))
                           :alt (:alt image)}]
       [:h2.h4.mt3.mb1 name]
       (if sold-out?
         [:p.h6.dark-gray "Out of stock"]
         [:div
          (for [selector (reverse (:criteria/selectors product))]
            [:div {:key selector}
             (unconstrained-facet product
                                  facets
                                  (keyword selector))])
          [:p.h6 "Starting at " (mf/as-money-without-cents (:price representative-sku))]])]]
     [:p.mb10.center
      (when affirm?
        [:div.h6.dark-gray
         (component/build affirm/as-low-as-component {:amount (:price representative-sku)
                                                      :type   :text-only} {})])]]))
