(ns storefront.components.shop-by-look
  (:require [om.core :as om]
            [sablono.core :refer [html]]
            [storefront.accessors.pixlee :as pixlee]
            [storefront.components.ugc :as ugc]
            [storefront.keypaths :as keypaths]
            [storefront.accessors.experiments :as experiments]
            [storefront.components.ui :as ui]
            [storefront.config :as config]
            [storefront.accessors.experiments :as experiments]))

(defn component [{:keys [looks copy spinning? shop-by-look?]} owner opts]
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
       (if shop-by-look?
         (om/build ugc/shop-by-look-experiment-component {:looks looks} {:opts {:copy copy}})
         (om/build ugc/component {:looks looks} {:opts {:copy copy}}))]))))

(defn query [data]
  (let [ugc-content       (get-in data keypaths/ugc)
        selected-album-kw (get-in data keypaths/selected-album-keyword)
        actual-album-kw   (pixlee/determine-look-album data selected-album-kw)
        looks             (pixlee/images-in-album ugc-content actual-album-kw)]
    {:looks         looks
     :copy          (-> config/pixlee :copy actual-album-kw)
     :spinning?     (empty? looks)
     :shop-by-look? (experiments/shop-by-look? data)}))

(defn built-component [data opts]
  (om/build component (query data) opts))
