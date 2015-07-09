(ns storefront.components.home
  (:require [storefront.components.utils :as utils]
            [storefront.analytics :as analytics]
            [storefront.keypaths :as keypaths]
            [storefront.taxons :refer [taxon-path-for default-taxon-path]]
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
       (when-let [path (default-taxon-path data)]
         [:a.full-link (utils/route-to data events/navigate-category
                                       {:taxon-path path})])]
      [:div.home-free-shipping]]
     [:div.squashed-hair-categories
      (map (partial category data)
           (get-in data keypaths/taxons))
      [:div {:style {:clear "both"}}]]
     [:div.featured-product-content
      {:on-click (fn [_] (analytics/track-event "Banner"
                                                "Click"
                                                "deluxeUltraBannerHome"
                                                1
                                                false))}
      [:figure.featured-new]
      [:figure.featured-product-image]
      [:p.featured-product-banner "Introducing DELUXE and ULTRA hair"]]
     [:div.home-sessions-container
      [:p.home-sessions-description
       [:a (utils/route-to data events/navigate-sign-in) "Sign In"]
       " or "
       [:a (utils/route-to data events/navigate-sign-up) "sign up"]
       " for an account for exclusive promotions, order tracking, and big hair love."]
      [:a.session-button (utils/route-to data events/navigate-sign-in) "Sign In"]
      [:a.session-button (utils/route-to data events/navigate-sign-up) "Sign Up"]
      [:div {:style {:clear "both"}}]]])))
