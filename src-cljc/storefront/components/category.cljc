(ns storefront.components.category
  (:require [storefront.platform.component-utils :as utils]
            [storefront.components.money-formatters :refer [as-money-without-cents as-money]]
            [storefront.accessors.promos :as promos]
            [storefront.accessors.named-searches :as named-searches]
            [storefront.accessors.categories :as categories]
            [storefront.accessors.products :as products]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.bundle-builder :as bundle-builder]
            [storefront.platform.reviews :as reviews]
            [storefront.platform.ugc :as ugc]
            [storefront.components.ui :as ui]
            [clojure.string :as string]
            [clojure.set :as set]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.assets :as assets]
            [storefront.request-keys :as request-keys]
            [storefront.platform.carousel :as carousel]
            [storefront.components.money-formatters :as mf]))

(defn facet-definition [facets facet option]
  (->> facets
       (filter (fn [{:keys [step]}] (= step facet)))
       first
       :options
       (filter (fn [{:keys [:option/slug]}] (= slug option)))
       first))

(defmulti unconstrained-facet (fn [skus facets facet] facet))
(defmethod unconstrained-facet :length [skus facets facet]
  (let [lengths  (->> skus
                      (map #(get-in % [:attributes :length]))
                      sort)
        shortest (first lengths)
        longest  (last lengths)]
    [:p.h6.dark-gray
     "in "
     (:name (facet-definition facets :length shortest))
     " - "
     (:name (facet-definition facets :length longest))]))

(defmethod unconstrained-facet :color [skus facets facet]
  (let [colors (->> skus
                    (map #(get-in % [:attributes :color]))
                    distinct)]
    (when (> (count colors) 1)
      [:p.h6.dark-gray "+ more colors available"])))

(defn ^:private component [{:keys [category sku-sets facets]} owner opts]
  (component/create
   [:div
    [:h1
     (let [{:keys [mobile-url file-name desktop-url alt]} (:hero (:images category))]
       [:picture
        [:source {:media   "(min-width: 750px)"
                  :src-set (str desktop-url "-/format/auto/" file-name " 1x")}]
        [:img.block.col-12 {:src (str mobile-url "-/format/auto/" file-name)
                            :alt alt}]])]
    [:div.container
     [:div.px2-on-mb
      [:div.py6 [:p.my6.max-580.mx-auto.center (-> category :copy :description)]]
      [:div.flex.flex-wrap.mxn1
       (for [{:keys [slug representative-sku name skus sold-out?] :as sku-set} sku-sets]
         (let [image (->> representative-sku :images (filter (comp #{"catalog"} :use-case)) first)]
           [:div.col.col-6.col-4-on-tb-dt.px1 {:key slug}
            [:div.mb10.center
             ;; TODO: when adding aspect ratio, also use srcset/sizes to scale these images.
             [:img.block.col-12 {:src (str (:url image)
                                           (:filename image))
                                 :alt (:alt image)}]
             [:h2.h4.mt3.mb1 name]
             (if sold-out?
               [:p.h6.dark-gray "Out of stock"]
               [:div
                ;; This is pretty specific to hair. Might be better to have a
                ;; sku-set know its "constrained" and "unconstrained" facets.
                (unconstrained-facet skus facets :length)
                (unconstrained-facet skus facets :color)
                [:p.h6 "Starting at " (mf/as-money-without-cents (:price representative-sku))]])]]))]]]]))

(defn hydrate-sku-set-with-skus [id->skus sku-set]
  (letfn [(sku-by-id [sku-id]
            (get id->skus sku-id))]
    (-> sku-set
        (update :skus #(map sku-by-id %))
        (update :representative-sku sku-by-id))))

(defn by-launched-at-price-name [x y]
  ;; launched-at is desc
  (compare [(:launched-at y) (:price (:representative-sku x)) (:name x)]
           [(:launched-at x) (:price (:representative-sku y)) (:name y)]))

(defn ^:private query [data]
  (let [category    (categories/current-category data)
        sku-sets    (->> category
                         :sku-set-ids
                         (keep #(get-in data (conj keypaths/sku-sets %)))
                         (map (partial hydrate-sku-set-with-skus (get-in data keypaths/skus)))
                         (sort by-launched-at-price-name))]
    {:category category
     :sku-sets sku-sets
     :facets   (get-in data keypaths/facets)}))

(defn built-component [data opts]
  (component/build component (query data) opts))

