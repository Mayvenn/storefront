(ns storefront.components.shop-by-look
  (:require [om.core :as om]
            [sablono.core :refer [html]]
            [storefront.accessors.pixlee :as pixlee]
            [storefront.components.ugc :as ugc]
            [storefront.keypaths :as keypaths]
            [storefront.accessors.experiments :as experiments]
            [storefront.components.ui :as ui]
            [storefront.config :as config]
            [storefront.accessors.experiments :as experiments]
            [spice.maps :as maps]))

(defn component [{:keys [looks deals? color-details copy spinning?]} owner opts]
  (om/component
   (html
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
       (om/build ugc/component
                 {:color-details color-details :looks looks}
                 {:opts {:copy copy}})]))))

(defn query [data]
  (let [ugc-content       (get-in data keypaths/ugc)
        selected-album-kw (get-in data keypaths/selected-album-keyword)
        actual-album-kw   (pixlee/determine-look-album data selected-album-kw)
        looks             (pixlee/images-in-album ugc-content actual-album-kw)]
    {:looks         looks
     :copy          (-> config/pixlee :copy actual-album-kw)
     :spinning?     (empty? looks)
     :deals?        (= selected-album-kw :deals)
     :color-details (->> (get-in data keypaths/v2-facets)
                         (filter #(= :hair/color (:facet/slug %)))
                         first
                         :facet/options
                         (maps/index-by :option/slug))}))

(defn built-component [data opts]
  (om/build component (query data) opts))
