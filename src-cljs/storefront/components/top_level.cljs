(ns storefront.components.top-level
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.state :as state]
            [storefront.events :as events]
            [storefront.components.utils :as utils]
            [storefront.components.slideout-nav :refer [slideout-nav-component]]
            [storefront.components.header :refer [header-component]]
            [storefront.components.footer :refer [footer-component]]
            [storefront.components.home :refer [home-component]]
            [storefront.components.category :refer [category-component]]
            [storefront.components.thirty-day-guarantee :refer [thirty-day-guarantee-component]]
            [storefront.components.help :refer [help-component]]
            [storefront.components.privacy :refer [privacy-component]]
            [storefront.components.tos :refer [tos-component]]
            [storefront.components.sign-in :refer [sign-in-component]]
            [storefront.components.sign-up :refer [sign-up-component]]
            [cljs.core.async :refer [put!]]))

(defn top-level-component [data owner]
  (om/component
   (html
    [:div
     (om/build slideout-nav-component data)
     [:div {:on-click (when (get-in data state/menu-expanded-path)
                        (utils/enqueue-event data events/control-menu-collapse))}
      [:div.page-wrap
       (om/build header-component data)
       [:main {:role "main"}
        [:div.container
         (om/build
          (condp = (get-in data state/navigation-event-path)
            events/navigate-home home-component
            events/navigate-category category-component
            events/navigate-guarantee thirty-day-guarantee-component
            events/navigate-help help-component
            events/navigate-privacy privacy-component
            events/navigate-tos tos-component
            events/navigate-sign-in sign-in-component
            events/navigate-sign-up sign-up-component)
          data)]]]
      (om/build footer-component data)]])))
