(ns storefront.components.shop-by-look
  (:require [storefront.component :as component]
            [storefront.accessors.contentful :as contentful]
            [storefront.components.ugc :as component-ugc]
            [storefront.keypaths :as keypaths]
            [storefront.components.ui :as ui]
            [spice.maps :as maps]
            [storefront.ugc :as ugc]))

(defn component [{:keys [looks deals? copy spinning?]} owner opts]
  (component/create
   (if spinning?
     (ui/large-spinner {:style {:height "4em"}})
     [:div
      [:div.center.bg-white.py3
       [:h1.h2.navy (:title copy)]
       [:div.bg-no-repeat.bg-contain.mx-auto
        (if deals?
          {:class "img-shop-by-bundle-deal-icon"
           :style {:width "110px" :height "110px"}}
          {:class "img-shop-by-look-icon my2"
           :style {:width "101px" :height "85px"}})]
       [:p.dark-gray.col-10.col-6-on-tb-dt.mx-auto (:description copy)]]
      [:div.flex.flex-wrap.mbn2.justify-center.justify-start-on-tb-dt.bg-light-gray.py2-on-tb-dt.px1-on-tb-dt
       (for [look looks]
         (component/build component-ugc/social-image-card-component look {:opts {:copy copy}}))]])))

(defn query [data]
  (let [navigation-event  (get-in data keypaths/navigation-event)
        selected-album-kw (get-in data keypaths/selected-album-keyword)
        actual-album-kw   (ugc/determine-look-album data selected-album-kw)
        looks             (-> data (get-in keypaths/cms-ugc-collection) actual-album-kw :looks)
        color-details     (->> (get-in data keypaths/v2-facets)
                               (filter #(= :hair/color (:facet/slug %)))
                               first
                               :facet/options
                               (maps/index-by :option/slug))]
    {:looks     (mapv (partial contentful/look->social-card
                               navigation-event
                               selected-album-kw
                               color-details)
                      looks)
     :copy      (actual-album-kw ugc/album-copy)
     :spinning? (empty? looks)
     :deals?    (= selected-album-kw :deals)}))

(defn built-component [data opts]
  (component/build component (query data) opts))
