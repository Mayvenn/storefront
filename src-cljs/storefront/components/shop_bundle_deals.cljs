(ns storefront.components.shop-bundle-deals
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.accessors.pixlee :as pixlee]
            [storefront.components.ugc :as ugc]
            [storefront.components.ui :as ui]
            [storefront.keypaths :as keypaths]))

(defn component [{:keys [bundle-deals]} owner opts]
  (om/component
   (html
    [:div
     [:div.center.bg-light-gray.py3
      [:h1.h2.navy "shop bundle deals"]
      [:div.img-shop-by-bundle-deal-icon.bg-no-repeat.bg-contain.mx-auto.my2
       {:style {:width "101px" :height "85px"}} ]
      [:p.dark-gray.col-10.col-6-on-tb-dt.mx-auto "Get inspired by #MayvennMade community. Find your favorite bundle-deal and click it to easily add it to your bag!"]]
     [:div.container.clearfix.mtn2
      (for [{:keys [id imgs] :as bundle-deal} bundle-deals]
        [:div
         {:key id}
         [:div.py2.col-12.col.hide-on-tb-dt {:key (str "small-" id)}
          (ugc/image-thumbnail (:medium imgs))
          (ugc/image-attribution bundle-deal)]
         [:div.p2.col.col-4.hide-on-mb {:key (str "large-" id)}
          (ui/aspect-ratio
           1 1
           {:class "hoverable"}
           (ugc/image-thumbnail (:medium imgs))
           [:div.absolute.bottom-0.col-12.show-on-hover
            (ugc/image-attribution bundle-deal)])]])]])))

(defn query [data]
  (let [bundle-deals (->> (pixlee/images-in-album (get-in data keypaths/ugc) :bundle-deals)
                   (remove (comp #{"video"} :content-type)))]
    {:bundle-deals bundle-deals}))

(defn built-component [data opts]
  (om/build component (query data) opts))
