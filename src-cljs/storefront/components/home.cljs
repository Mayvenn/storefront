(ns storefront.components.home
  (:require [storefront.components.utils :as utils]
            [storefront.hooks.analytics :as analytics]
            [storefront.keypaths :as keypaths]
            [storefront.accessors.taxons :refer [filter-nav-taxons taxon-path-for]]
            [om.core :as om]
            [clojure.string :as string]
            [sablono.core :refer-macros [html]]
            [storefront.events :as events]
            [storefront.hooks.experiments :as experiments]))

(defn category [data taxon index]
  (let [taxon-name (taxon :name)
        taxon-path (taxon-path-for taxon)]
    [:a.hair-category-link (utils/route-to data
                                           events/navigate-category
                                           {:taxon-path taxon-path})
     [:div.hair-container.not-decorated.no-margin
      (when (> index 5)
        {:class "extra-wide"})
      [:div.hair-taxon {:class taxon-path}]
      [:div.hair-image.image-cover {:class taxon-path}]]]))

(defn home-component [data owner]
  (om/component
   (html
    [:div#home-content
     [:div.home-large-image]
     [:div.home-actions-top
      [:a.guarantee
       (utils/route-to data events/navigate-guarantee)]
      [:a.free-shipping-action
       {:href "https://mayvenn.zendesk.com/hc/en-us/articles/205541565-Do-you-offer-free-shipping-" :target "_blank"}]
      [:div.shop-now
       [:a.full-link (utils/route-to data events/navigate-categories)]]
      [:a.home-30-day-guarantee
       (utils/route-to data events/navigate-guarantee)]
      [:a.home-free-shipping
       {:href "https://mayvenn.zendesk.com/hc/en-us/articles/205541565-Do-you-offer-free-shipping-" :target "_blank"}]]
     [:div.squashed-hair-categories
      (map (partial category data)
           (filter-nav-taxons (get-in data keypaths/taxons))
           (range))
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
