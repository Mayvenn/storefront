(ns storefront.components.home
  (:require [storefront.components.utils :as utils]
            [storefront.hooks.analytics :as analytics]
            [storefront.hooks.experiments :as experiments]
            [storefront.keypaths :as keypaths]
            [storefront.accessors.taxons :refer [filter-nav-taxons]]
            [storefront.accessors.navigation :as navigation]
            [om.core :as om]
            [clojure.string :as string]
            [sablono.core :refer-macros [html]]
            [storefront.events :as events]))

(defn categories-component [{:keys [taxons]} owner]
  (om/component
   (html
    [:div
     (let [height 9
           rem #(str % "rem")]
       (for [[index {:keys [slug]}] (map-indexed vector taxons)]
         [:a.col (merge {:key slug
                         :data-test (str "taxon-" slug)}
                        (utils/route-to events/navigate-category
                                        {:taxon-slug slug})
                        (if (> index 5)
                          {:class "col-6"}
                          {:class "col-4"}))
          [:.mp1.flex.items-center.justify-center.bg-cover.bg-no-repeat.bg-center
           {:class (str "img-" slug)
            :style (if (> index 5)
                     {:height (rem (/ height 2))}
                     {:height (rem height)})}
           ;; duplicate element to emulate old styles without custom special-cased classes
           [:.bg-contain.bg-no-repeat.bg-center.col-10.md-up-hide
            {:class (str "img-text-" slug)
             :style {:height "1.33333rem"}}]
           [:.bg-contain.bg-no-repeat.bg-center.col-10.to-md-hide
            {:class (str "img-text-" slug)
             :style {:height "25px"}}]]]))])))

(defn home-component [data owner]
  (om/component
   (html
    (let [taxons (filter-nav-taxons (get-in data keypaths/taxons))]
      [:.home-container.m-auto
       [:a.lg-up-hide.img-md-home-banner.bg-no-repeat.bg-full.bg-center.col-12.block.banner-container
        (apply utils/route-to (navigation/shop-now-navigation-message data))]
       [:a.to-lg-hide.img-lg-home-banner.bg-no-repeat.bg-full.bg-center.col-12.block.banner-container
        (merge {:style {:margin-bottom "33px"}}
               (apply utils/route-to (navigation/shop-now-navigation-message data)))]
       [:.text-free-shipping-banner
        [:p "Free Shipping + 30 Day Money Back Guarantee"]]

       [:.col-12.lg-col-6 [:.h3.center.black.mb1 "Pick your style"]]
       [:.col.col-12.lg-col-6
        (om/build categories-component {:taxons taxons})
        [:.clearfix]]
       [:.col.col-6.to-lg-hide
        [:a.block.img-featured.col-12.bg-no-repeat.bg-center.bg-cover.mtp1
         (merge {:style {:height "300px"}}
                (apply utils/route-to (navigation/shop-now-navigation-message data)))]
        [:p.bg-pink-gradient.col-12.white.italic.flex.items-center.justify-center.mtn1
         {:style {:height "72px"}}
         "Introducing Peruvian In All Textures"]]

       [:.clearfix]]))))
