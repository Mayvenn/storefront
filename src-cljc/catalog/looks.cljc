(ns catalog.looks
  "Shopping by Looks: index page of 'looks' for an 'album'"
  (:require [spice.maps :as maps]
            [storefront.accessors.contentful :as contentful]
            [storefront.accessors.experiments :as experiments]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.ugc :as component-ugc]
            [storefront.components.ui :as ui]
            [storefront.keypaths :as keypaths]
            [storefront.ugc :as ugc]
            [storefront.accessors.sites :as sites]))

(defcomponent component [{:keys [looks copy spinning?]} owner opts]
  (if spinning?
    (ui/large-spinner {:style {:height "4em"}})
    [:div.bg-warm-gray
     [:div.center.py6
      [:h1.title-1.canela.py3 (:title copy)]
      [:p.col-10.col-6-on-tb-dt.mx-auto.proxima.content-2 (:description copy)]
      [:p.col-10.col-6-on-tb-dt.mx-auto.proxima.content-2 (:secondary-description copy)]]
     [:div.flex.flex-wrap.mbn2.justify-center.justify-start-on-tb-dt.bg-cool-gray.py2-on-tb-dt.px1-on-tb-dt
      (map-indexed
       (fn [idx look]
         (ui/screen-aware component-ugc/social-image-card-component
                          (assoc look :hack/above-the-fold? (zero? idx))
                          {:opts               {:copy copy}
                           :child-handles-ref? true
                           :key                (str (:id look))}))
       looks)]]))

(defn query [data]
  (let [selected-album-kw (get-in data keypaths/selected-album-keyword)
        actual-album-kw   (ugc/determine-look-album data selected-album-kw)
        looks             (-> data (get-in keypaths/cms-ugc-collection) actual-album-kw :looks)
        color-details     (->> (get-in data keypaths/v2-facets)
                               (filter #(= :hair/color (:facet/slug %)))
                               first
                               :facet/options
                               (maps/index-by :option/slug))]
    {:looks     (mapv (partial contentful/look->social-card
                               selected-album-kw
                               color-details)
                      looks)
     :copy      (actual-album-kw ugc/album-copy)
     :spinning? (empty? looks)}))

(def default-copy
  {:title       "Shop by Look"
   :description "Get 3 or more hair items and receive a service for FREE"
   :secondary-description "#MayvennMade"
   :button-copy "Shop Look"
   :short-name  "look"
   :seo-title   "Shop by Look | Mayvenn"
   :og-title    "Shop by Look - Find and Buy your favorite Mayvenn bundles!"})

(defn sbl-query
  [data]
  (let [selected-album-kw (get-in data keypaths/selected-album-keyword)
        actual-album-kw   (ugc/determine-look-album data selected-album-kw)
        looks             (-> data (get-in keypaths/cms-ugc-collection) actual-album-kw :looks)
        color-details     (->> (get-in data keypaths/v2-facets)
                               (filter #(= :hair/color (:facet/slug %)))
                               first
                               :facet/options
                               (maps/index-by :option/slug))]
    {:looks     (mapv (partial contentful/look->social-card
                               selected-album-kw
                               color-details)
                      looks)
     :copy      default-copy
     :spinning? (empty? looks)}))

(defn ^:export built-component [data opts]
  (let [album-kw (ugc/determine-look-album data (get-in data keypaths/selected-album-keyword))]
    (component/build component (if (and (experiments/sbl-update? data)        ;; featured
                                        (= :shop (sites/determine-site data)) ;; dtc, shop
                                        (= :aladdin-free-install album-kw))   ;; main look page
                                 (sbl-query data)
                                 (query data)) opts)))
