(ns adventure.stylist-matching.stylist-profile
  (:require [adventure.components.header :as header]
            [adventure.components.profile-card :as profile-card]
            [adventure.keypaths :as keypaths]
            [storefront.accessors.stylists :as stylists]
            [storefront.component :as component]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.platform.carousel :as carousel]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]))


(defn component [{:keys []} owner opts]
  (component/create
   [:div.col-12
    [:div.white]
    [:div {:style {:height "75px"}}]]))

(defn built-component
  [data opts]
  (component/build component {} {}))
