(ns storefront.components.stylist.cash-out-success
  (:require [storefront.events :as events]
            [storefront.platform.component-utils :as utils]
            [storefront.components.ui :as ui]
            [storefront.keypaths :as keypaths]
            [storefront.components.svg :as svg]
            [storefront.component :as component :refer [defcomponent]]))

(defn query [data]
  {:balance-transfer-id (get-in data keypaths/stylist-cash-out-balance-transfer-id)})

(defcomponent component [data owner opts]
  [:div.container.p4.center
   ^:inline (svg/circled-check {:class "stroke-teal"
                                :style {:width "100px" :height "100px"}})
   [:h2.my3 "Cha-Ching!"]
   [:p.my4 "You have successfully cashed out your earnings. View your transfer by clicking the button below."]
   (ui/teal-button (merge (utils/route-to events/navigate-stylist-dashboard-balance-transfer-details
                                          {:balance-transfer-id (:balance-transfer-id data)})
                          {:data-test "see-transfer-button"})
                   "See Transfer")])

(defn ^:export built-component [data opts]
  (component/build component (query data) opts))

