(ns storefront.components.home
  (:require [storefront.components.utils :as utils]
            [storefront.hooks.analytics :as analytics]
            [storefront.keypaths :as keypaths]
            [storefront.accessors.taxons :refer [taxon-path-for default-taxon-path]]
            [om.core :as om]
            [clojure.string :as string]
            [sablono.core :refer-macros [html]]
            [storefront.events :as events]
            [storefront.hooks.experiments :as experiments]))

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
      [:a.guarantee
       (when (experiments/display-variation data "home-page-links")
         (utils/route-to data events/navigate-guarantee))]
      [:a.free-shipping-action
       (when (experiments/display-variation data "home-page-links")
         {:href "https://mayvenn.zendesk.com/hc/en-us/articles/205541565-Do-you-offer-free-shipping-" :target "_blank"})]
      [:div.shop-now
       (when-let [path (default-taxon-path data)]
         [:a.full-link (utils/route-to data events/navigate-category
                                       {:taxon-path path})])]
      [:a.home-30-day-guarantee
       (when (experiments/display-variation data "home-page-links")
         (utils/route-to data events/navigate-guarantee))]
      [:a.home-free-shipping
       (when (experiments/display-variation data "home-page-links")
         {:href "https://mayvenn.zendesk.com/hc/en-us/articles/205541565-Do-you-offer-free-shipping-" :target "_blank"})]]
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
