(ns storefront.components.home
  (:require [storefront.components.utils :as utils]
            [storefront.state :as state]
            [storefront.taxons :refer [taxon-path-for]]
            [om.core :as om]
            [clojure.string :as string]
            [sablono.core :refer-macros [html]]
            [storefront.events :as events]))

(defn category [data taxon]
  (let [taxon-name (taxon :name)
        taxon-path (taxon-path-for taxon)]
    [:a.hair-category-link (utils/route-to data
                                           events/navigate-category
                                           {:taxon-path taxon-path})
     [:div.hair-container.not-decorated.no-margin
      [:div.hair-taxon {:class taxon-path}
       [:p.hair-taxon-name (string/capitalize taxon-name)]]
      [:div.hair-image.image-cover {:class taxon-path}]]]))

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
      (map category (repeatedly (constantly data)) (get-in data state/taxons-path))
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
