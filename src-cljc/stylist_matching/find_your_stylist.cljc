(ns stylist-matching.find-your-stylist
  (:require [storefront.component :as component]
            [storefront.events :as events]
            adventure.keypaths
            storefront.keypaths
            api.orders
            [stylist-matching.ui.header :as header]
            [stylist-matching.ui.stylist-search :as stylist-search]
            [stylist-matching.ui.spinner :as spinner]))

(defn spinner-query
  [app-state]
  (when-not (boolean (get-in app-state storefront.keypaths/loaded-google-maps))
    {:spinner/id "loading-google-maps"}))

(defn stylist-search-query
  [app-state] ;; TODO ui model/api?
  (let [input-location     (get-in app-state adventure.keypaths/adventure-stylist-match-address)
        selected-location? (boolean
                            (get-in app-state adventure.keypaths/adventure-stylist-match-location))]
    {:stylist-search.title/id      "find-your-stylist-stylist-search-title"
     :stylist-search.title/primary "Where do you want to get your hair done?"

     :stylist-search.location-search-box/id          "stylist-match-address"
     :stylist-search.location-search-box/placeholder "Enter city or street address"
     :stylist-search.location-search-box/value       (str input-location)
     :stylist-search.location-search-box/clear?      selected-location?

     :stylist-search.button/id        "stylist-match-address-submit"
     :stylist-search.button/disabled? (not selected-location?)
     :stylist-search.button/target    [events/control-adventure-location-submit {}]
     :stylist-search.button/label     "Search"}))

(defn header-query
  [{:order.items/keys [quantity]}]
  {:header.cart/id                "adventure-cart"
   :header.cart/value             quantity
   :header.cart/color             "white"
   :header.title/id               "adventure-title"
   :header.title/primary          "Meet Your Certified Stylist"
   :header.back-navigation/id     "adventure-back"
   :header.back-navigation/target [events/navigate-adventure-match-stylist]})

(defn template
  [{:keys [header stylist-search spinner]} _ _]
  (component/create
   [:div.bg-lavender.white.center.flex.flex-auto.flex-column
    (component/build header/organism header nil)
    (if (seq spinner)
      (component/build spinner/organism spinner nil)
      (component/build stylist-search/organism stylist-search nil))]))

(defn page
  [app-state]
  (let [current-order (api.orders/current app-state)]
    (component/build template
                     {:stylist-search (stylist-search-query app-state)
                      :spinner        (spinner-query app-state)
                      :header         (header-query current-order)})))
