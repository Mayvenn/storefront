(ns storefront.components.shop-bundle-deals
  (:require [om.core :as om]
            [sablono.core :refer [html]]
            [storefront.accessors.pixlee :as pixlee]
            [storefront.components.ugc :as ugc]
            [storefront.keypaths :as keypaths]
            [storefront.accessors.black-friday :as black-friday]))

(defn component [{:keys [bundle-deals black-friday-stage]} owner opts]
  (om/component
   (html
    [:div
     [:div.center.bg-light-gray.py3
      [:h1.h2.navy (if (= :cyber-monday black-friday-stage)
                     "cyber monday deals"
                     "black friday deals")]
      [:div.img-shop-by-bundle-deal-icon.bg-no-repeat.bg-contain.mx-auto.my2
       {:style {:width "101px" :height "85px"}} ]
      [:p.dark-gray.col-10.col-6-on-tb-dt.mx-auto
       "Get 25% off everything on our site, and an extra 10% off purchases of 3 bundles or more. Save some time and shop our 3-bundle deals below!"]]
     (om/build ugc/component {:looks bundle-deals} {:opts {:copy {:back-copy   "back to bundle deals"
                                                                  :short-name  "deal"
                                                                  :button-copy "View this deal"}}})])))

(defn query [data]
  (let [bundle-deals (->> (pixlee/images-in-album (get-in data keypaths/ugc) :bundle-deals)
                          (remove (comp #{"video"} :content-type)))]
    {:bundle-deals       bundle-deals
     :black-friday-stage (black-friday/stage data)}))

(defn built-component [data opts]
  (om/build component (query data) opts))
