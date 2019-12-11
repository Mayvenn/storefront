(ns stylist-matching.find-your-stylist
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.events :as events]
            adventure.keypaths
            storefront.keypaths
            api.orders
            [stylist-matching.ui.header :as header]
            [stylist-matching.ui.stylist-search :as stylist-search]
            [stylist-matching.ui.spinner :as spinner]
            [storefront.components.flash :as flash]))

(defn spinner-query
  [app-state]
  (when-not (boolean (get-in app-state storefront.keypaths/loaded-google-maps))
    {:spinner/id "loading-google-maps"}))

(defn stylist-search-query
  [app-state] ;; TODO ui model/api?
  (let [input-location     (get-in app-state adventure.keypaths/adventure-stylist-match-address)
        selected-location  (get-in app-state adventure.keypaths/adventure-stylist-match-location)
        selected-location? (boolean selected-location)]
    {:stylist-search.title/id      "find-your-stylist-stylist-search-title"
     :stylist-search.title/primary "Where do you want to get your hair done?"

     :stylist-search.location-search-box/id          "stylist-match-address"
     :stylist-search.location-search-box/placeholder "Enter city or street address"
     :stylist-search.location-search-box/value       (str input-location)
     :stylist-search.location-search-box/clear?      selected-location?

     :stylist-search.button/id        "stylist-match-address-submit"

     :stylist-search.button/disabled? (not (and selected-location?
                                                input-location))
     :stylist-search.button/target    [events/control-adventure-location-submit {:query-params {:long (:longitude selected-location)
                                                                                                :lat  (:latitude selected-location)}}]
     :stylist-search.button/label     "Search"}))

(defn header-query
  [{:order.items/keys [quantity]}]
  {:header.cart/id                "mobile-cart"
   :header.cart/value             quantity
   :header.cart/color             "white"
   :header.title/id               "adventure-title"
   :header.title/primary          "Meet Your Certified Stylist"
   :header.back-navigation/id     "adventure-back"
   :header.back-navigation/target [events/navigate-adventure-match-stylist]})

(defcomponent template
  [{:keys [flash header stylist-search spinner]} _ _]
  [:div.center.flex.flex-auto.flex-column
   (component/build header/organism header nil)
   (component/build flash/component flash nil)
   (if (seq spinner)
     (component/build spinner/organism spinner nil)
     [:div.px2.mt8.pt4
      (component/build stylist-search/organism stylist-search nil)])])

(defn page
  [app-state]
  (let [current-order (api.orders/current app-state)]
    (component/build template
                     {:stylist-search (stylist-search-query app-state)
                      :flash          (flash/query app-state)
                      :spinner        (spinner-query app-state)
                      :header         (header-query current-order)})))
