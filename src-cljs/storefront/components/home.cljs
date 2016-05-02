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
     (for [[index {:keys [slug]}] (map-indexed vector taxons)]
       [:a.hair-category-link
        (merge {:key slug
                :data-test (str "taxon-" slug)}
               (utils/route-to events/navigate-category
                               {:taxon-slug slug}))
        [:div.hair-container.not-decorated.no-margin
         (when (> index 5)
           {:class "half-wide"})
         [:div.hair-image.image-cover {:class slug}]
         [:div.hair-taxon {:class slug}]]])])))

(defn home-component [data owner]
  (om/component
   (html
    (let [taxons (filter-nav-taxons (get-in data keypaths/taxons))]
      [:div#home-content
       [:a.home-large-image
        (apply utils/route-to (navigation/shop-now-navigation-message data))]
       [:div.text-free-shipping-banner
        [:p "Free Shipping + 30 Day Money Back Guarantee"]]
       [:div.squashed-hair-categories
        [:h3.pick-style "Pick your style"]
        (om/build categories-component {:taxons taxons})
        [:div {:style {:clear "both"}}]]
       [:div.featured-product-content.mobile-hidden
        [:figure.featured-new]
        [:a.featured-product-image
         (apply utils/route-to (navigation/shop-now-navigation-message data))]
        [:p.featured-product-banner "Introducing Peruvian In All Textures"]]
       [:div {:style {:clear "both"}}]]))))
