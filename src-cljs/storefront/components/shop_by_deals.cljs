(ns storefront.components.shop-by-deals
  (:require [om.core :as om]
            [sablono.core :refer [html]]
            [storefront.accessors.pixlee :as pixlee]
            [storefront.components.ugc :as ugc]
            [storefront.keypaths :as keypaths]
            [storefront.config :as config]))

(defn component [{:keys [deals copy]} owner opts]
  (om/component
   (html
    [:div
     [:div.center.bg-light-gray.py3
      [:h1.h2.navy "shop deals"]
      [:div.img-shop-by-bundle-deal-icon.bg-no-repeat.bg-contain.mx-auto.my2
       {:style {:width "101px" :height "85px"}} ]
      [:p.dark-gray.col-10.col-6-on-tb-dt.mx-auto
       "Save more when you bundle up! We wrapped our most popular textures into packaged bundle deals so you can shop with ease."]]
     (om/build ugc/component {:looks deals} {:opts {:copy copy}})])))

(defn query [data]
  {:deals (->> (pixlee/images-in-album (get-in data keypaths/ugc) :deals)
               (remove (comp #{"video"} :content-type)))
   :copy (-> config/pixlee :copy :deals)})

(defn built-component [data opts]
  (om/build component (query data) opts))
