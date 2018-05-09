(ns storefront.components.shop-by-look
  (:require [om.core :as om]
            [sablono.core :refer [html]]
            [storefront.accessors.pixlee :as pixlee]
            [storefront.components.ugc :as ugc]
            [storefront.keypaths :as keypaths]
            [storefront.accessors.experiments :as experiments]
            [storefront.components.ui :as ui]
            [storefront.config :as config]))

(defn component [{:keys [looks copy spinning?]} owner opts]
  (om/component
   (html
    (if spinning?
      (ui/large-spinner {:style {:height "4em"}})
      [:div
       [:div.center.bg-light-gray.py3
        [:h1.h2.navy (:title copy)]
        [:div.img-shop-by-look-icon.bg-no-repeat.bg-contain.mx-auto.my2
         {:style {:width "101px" :height "85px"}} ]
        [:p.dark-gray.col-10.col-6-on-tb-dt.mx-auto (:description copy)]]
       (om/build ugc/component {:looks looks} {:opts {:copy copy}})]))))

(defn query [data]
  (let [ugc-content         (get-in data keypaths/ugc)
        selected-album-slug (get-in data keypaths/selected-album-slug)
        actual-album-slug   (pixlee/determine-look-album data selected-album-slug)
        looks               (pixlee/images-in-album ugc-content actual-album-slug)]
    {:looks     looks
     :copy      (-> config/pixlee :copy actual-album-slug)
     :spinning? (empty? looks)}))

(defn built-component [data opts]
  (om/build component (query data) opts))
