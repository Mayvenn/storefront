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
        [:h1.h2.navy "shop by look"]
        [:div.img-shop-by-look-icon.bg-no-repeat.bg-contain.mx-auto.my2
         {:style {:width "101px" :height "85px"}} ]
        [:p.dark-gray.col-10.col-6-on-tb-dt.mx-auto "Get inspiration for your next hairstyle and shop your favorite looks from the #MayvennMade community."]]
       (om/build ugc/component {:looks looks} {:opts {:copy copy}})]))))

(defn query [data]
  (let [the-ville?         (experiments/the-ville? data)
        the-ville-control? (experiments/the-ville-control? data)
        album              (if the-ville? :free-install :mosaic)]
    {:looks     (->> (pixlee/images-in-album (get-in data keypaths/ugc) album)
                     (remove (comp #{"video"} :content-type)))
     :copy      (-> config/pixlee :copy :mosaic)
     :spinning? (not (or the-ville? the-ville-control?))}))

(defn built-component [data opts]
  (om/build component (query data) opts))
