(ns storefront.components.shop-by-deals
  (:require [om.core :as om]
            [sablono.core :refer [html]]
            [storefront.accessors.pixlee :as pixlee]
            [storefront.components.ugc :as ugc]
            [storefront.keypaths :as keypaths]))

(defn component [{:keys [deals]} owner opts]
  (om/component
   (html
    [:div
     [:div.center.bg-light-gray.py3
      [:h1.h2.navy "shop deals"]
      [:div.img-shop-by-bundle-deal-icon.bg-no-repeat.bg-contain.mx-auto.my2
       {:style {:width "101px" :height "85px"}} ]
      [:p.dark-gray.col-10.col-6-on-tb-dt.mx-auto
       "Save more when you bundle up! We wrapped our most popular textures into packaged bundle deals so you can shop with ease."]]
     (om/build ugc/component {:looks deals} {:opts {:copy {:back-copy   "back to deals"
                                                           :short-name  "deal"
                                                           :button-copy "View this deal"}}})])))

(defn query [data]
  (let [deals (->> (pixlee/images-in-album (get-in data keypaths/ugc) :deals)
                   (remove (comp #{"video"} :content-type)))]
    {:deals deals}))

(defn built-component [data opts]
  (om/build component (query data) opts))
