(ns stylist-matching.match-stylist
  (:require api.orders
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.header :as header]
            [storefront.events :as events]
            [stylist-matching.ui.match-stylist :as match-stylist]))

(defn header-query [{:order.items/keys [quantity]}]
  {:header.title/id               "adventure-title"
   :header.title/primary          "Welcome!"
   :header.back-navigation/id     "adventure-back"
   :header.back-navigation/target [events/navigate-home]
   :header.cart/id                "mobile-cart"
   :header.cart/value             quantity
   :header.cart/color             "white"})

(def match-stylist-query
  {:match-stylist.title/id        "stylist-matching-match-stylist"
   :match-stylist.title/primary   "Let’s match you with a top stylist."
   :match-stylist.title/secondary "If it’s not perfect, your service is covered under our 30-day guarantee. Win-win!"
   :match-stylist.button/id       "adventure-find-your-stylist"
   :match-stylist.button/label    "Next"
   :match-stylist.button/target   [events/navigate-adventure-find-your-stylist]})

(defcomponent template
  [{:keys [header-query match-stylist cart]} _ _]
  [:div.bg-white.black.center.flex.flex-column
   (header/adventure-header header-query)

   (component/build match-stylist/organism match-stylist nil)])

(defn page
  [app-state]
  (component/build template
                   {:match-stylist match-stylist-query
                    :header-query  (header-query (api.orders/current app-state))}))
