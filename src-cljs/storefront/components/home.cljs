(ns storefront.components.home
  (:require [storefront.components.utils :as utils]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.events :as events]))

(defn home-component [data owner]
  (om/component
   (html
    [:div#home-content
     [:div.home-large-image]
     [:div.home-actions-top
      [:div.guarantee]
      [:div.free-shipping-action]
      [:div.shop-now
       [:a {:href "#FIXME"}]]
      [:img.home-free-shipping {:src "/images/30_day_ship_combo.png"}]]
     [:div.squashed-hair-categories
      [:a.hair-category-link {:href "#FIXME"}
       [:div.hair-container.not-decorated.no-margin
        [:div.hair-taxon
         [:p.hair-taxon-name "Straight#FIXME"]]
        [:div.hair-image.image-cover {:class "#FIXME"}]]]
      [:div {:style {:clear "both"}}]]
     [:div.featured-product-content
      [:figure.featured-new]
      [:figure.featured-product-image]
      [:p.featured-product-banner "Introducing DELUXE and ULTRA hair"]]
     [:div.home-sessions-container
      [:p.home-sessions-description
       [:a {:href "#FIXME"} "Sign In"]
       " or "
       [:a {:href "#FIXME"} "sign up"]
       " for an account for exclusive promotions, order tracking, and big hair love."]
      [:a.session-button {:href "#FIXME"} "Sign In"]
      [:a.session-button {:href "#FIXME"} "Sign Up"]
      [:div {:style {:clear "both"}}]]])))
