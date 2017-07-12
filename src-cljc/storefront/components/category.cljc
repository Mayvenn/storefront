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
  (get-in facets [facet option]))

(defmulti unconstrained-facet (fn [skus facets facet] facet))
(defmethod unconstrained-facet :length [skus facets facet]
  (let [lengths  (->> skus
                      (map #(get-in % [:attributes :length]))
                      sort)
        shortest (first lengths)
        longest  (last lengths)]
    [:p.h6.dark-gray
     (:name (facet-definition facets :length shortest))
     " - "
     (:name (facet-definition facets :length longest))
     " lengths available"]))

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
     [:div.p2
      [:p.h3.my4.max-580.mx-auto.center (-> category :copy :description)]
      [:div.flex.flex-wrap.mxn1
       (for [{:keys [slug representative-sku name skus] :as sku-set} sku-sets]
         (let [image (-> representative-sku :images :catalog)]
           [:div.col.col-6.col-3-on-tb-dt.p1.center {:key slug}
            ;; TODO: when adding aspect ratio, also use srcset/sizes to scale these images.
            [:img.block.col-12 {:src (str (:url image)
                                          (:filename image))
                                :alt (:alt image)}]
            [:h2.h4.medium name]
            ;; This is pretty specific to hair. Might be better to have a
            ;; sku-set know its "constrained" and "unconstrained" facets.
            (unconstrained-facet skus facets :length)
            (unconstrained-facet skus facets :color)
            [:p.h6 "Starting at " (mf/as-money-without-cents (:price representative-sku))]]))]]]]))

(defn hydrate-sku-set-with-skus [id->skus sku-set]
  (letfn [(sku-by-id [sku-id]
            (get id->skus sku-id))]
    (-> sku-set
        (update :skus #(map sku-by-id %))
        (update :representative-sku sku-by-id))))

(defn ^:private query [data]
  (let [facets      {:type     {"kits" {:name "kits"}},
                     :grade    {"6a" {:name "6a premier collection"},
                                "7a" {:name "7a deluxe collection"},
                                "8a" {:name "8a ultra collection"}},
                     :category {"hair"     {:name "hair"},
                                "closures" {:name "closures"},
                                "frontals" {:name "frontals"}},
                     :color    {"black"                  {:name      "Natural #1B",
                                                          :long-name "Natural Black (#1B)",
                                                          :image     "//d275k6vjijb2m1.cloudfront.net/cellar/colors/black_v1.png"},
                                "dark-blonde"            {:name      "Dark Blonde #27",
                                                          :long-name "Dark Blonde (#27)",
                                                          :image     "//d275k6vjijb2m1.cloudfront.net/cellar/colors/dark_blonde.png"},
                                "blonde"                 {:name      "Blonde #613",
                                                          :long-name "Blonde (#613)",
                                                          :image     "//d275k6vjijb2m1.cloudfront.net/cellar/colors/blonde.png"},
                                "dark-blonde-dark-roots" {:name      "Dark Blonde #27 with Dark Roots #1B",
                                                          :long-name "Dark Blonde (#27) with Dark Roots (#1B)",
                                                          :image     "//d275k6vjijb2m1.cloudfront.net/cellar/colors/dark_blonde_dark_roots.png"},
                                "blonde-dark-roots"      {:name      "Blonde #613 with Dark Roots #1B",
                                                          :long-name "Blonde (#613) with Dark Roots (#1B)",
                                                          :image     "//d275k6vjijb2m1.cloudfront.net/cellar/colors/blonde_dark_roots.png"}},
                     :style    {"straight"       {:name "Straight"},
                                "yaki-straight"  {:name "Yaki Straight"},
                                "kinky-straight" {:name "Kinky Straight"},
                                "body-wave"      {:name "Body Wave"},
                                "loose-wave"     {:name "Loose Wave"},
                                "water-wave"     {:name "Water Wave"},
                                "deep-wave"      {:name "Deep Wave"},
                                "curly"          {:name "Curly"}},
                     :origin   {"brazilian" {:name "Brazilian"},
                                "malaysian" {:name "Malaysian"},
                                "peruvian"  {:name "Peruvian"},
                                "indian"    {:name "Indian"}},
                     :material {"lace" {:name "Lace"}, "silk" {:name "Silk"}},
                     :length   {"22" {:name "22″"},
                                "26" {:name "26″"},
                                "28" {:name "28″"},
                                "14" {:name "14″"},
                                "20" {:name "20″"},
                                "18" {:name "18″"},
                                "12" {:name "12″"},
                                "24" {:name "24″"},
                                "16" {:name "16″"},
                                "10" {:name "10″"}}}]
    {:category (get-in data keypaths/current-category)
     :sku-sets (->> (get-in data keypaths/sku-sets)
                    vals
                    (map (partial hydrate-sku-set-with-skus (get-in data keypaths/skus))))

     :facets   facets}))

(defn built-component [data opts]
  (component/build component (query data) opts))

