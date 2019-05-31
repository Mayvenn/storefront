(ns storefront.components.shop-by-look
  (:require [storefront.component :as component]
            [storefront.accessors.pixlee :as pixlee]
            [storefront.accessors.contentful :as contentful]
            [storefront.accessors.experiments :as experiments]
            [storefront.components.ugc :as ugc]
            [storefront.keypaths :as keypaths]
            [storefront.components.ui :as ui]
            [storefront.config :as config]
            [spice.maps :as maps]))

(defn component [{:keys [looks deals? copy spinning?]} owner opts]
  (component/create
   (if spinning?
     (ui/large-spinner {:style {:height "4em"}})
     [:div
      [:div.center.bg-light-gray.py3
       [:h1.h2.navy (:title copy)]
       [:div.bg-no-repeat.bg-contain.mx-auto
        (if deals?
          {:class "img-shop-by-bundle-deal-icon"
           :style {:width "110px" :height "110px"}}
          {:class "img-shop-by-look-icon my2"
           :style {:width "101px" :height "85px"}})]
       [:p.dark-gray.col-10.col-6-on-tb-dt.mx-auto (:description copy)]]
      [:div.flex.flex-wrap.mtn2.py4.px2.justify-center.justify-start-on-tb-dt
       (for [look looks]
         (component/build ugc/social-image-card-component look {:opts {:copy copy}}))]])))

(defn query [data]
  (let [ugc-content           (get-in data keypaths/ugc)
        navigation-event      (get-in data keypaths/navigation-event)
        selected-album-kw     (get-in data keypaths/selected-album-keyword)
        actual-album-kw       (pixlee/determine-look-album data selected-album-kw)
        pixlee-to-contentful? (experiments/pixlee-to-contentful? data)
        looks                 (if pixlee-to-contentful?
                                (-> data (get-in keypaths/cms-ugc-collection) actual-album-kw :looks)
                                (pixlee/images-in-album ugc-content actual-album-kw))
        color-details         (->> (get-in data keypaths/v2-facets)
                                   (filter #(= :hair/color (:facet/slug %)))
                                   first
                                   :facet/options
                                   (maps/index-by :option/slug))
        look-converter        (if pixlee-to-contentful?
                                (partial contentful/look->social-card
                                         navigation-event
                                         selected-album-kw
                                         color-details)
                                (partial ugc/pixlee-look->social-card color-details))]
    {:looks     (mapv look-converter looks)
     :copy      (-> config/pixlee :copy actual-album-kw)
     :spinning? (empty? looks)
     :deals?    (= selected-album-kw :deals)}))

(defn built-component [data opts]
  (component/build component (query data) opts))
