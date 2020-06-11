(ns stylist-matching.match-stylist
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.components.header :as header]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.accessors.orders :as orders]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [stylist-matching.ui.atoms :as stylist-matching.A]
            [stylist-matching.ui.logo-header :as logo-header]
            [stylist-matching.ui.match-stylist :as match-stylist]))

(def logo-header-query
  {:header/title       "Welcome!"
   :header.left/target [events/navigate-home]})

(def match-stylist-query
  {:match-stylist.title/id        "stylist-matching-match-stylist"
   :match-stylist.title/primary   "Let’s match you with a top stylist."
   :match-stylist.title/secondary "If it’s not perfect, your service is covered under our 30-day guarantee. Win-win!"
   :match-stylist.button/id       "adventure-find-your-stylist"
   :match-stylist.button/label    "Next"
   :match-stylist.button/target   [events/navigate-adventure-find-your-stylist]})

(defcomponent template
  [{:keys [logo-header match-stylist cart]} _ _]
  [:div.bg-white.black.center.flex.flex-column
   (header/adventure-header (:header.left/target logo-header) (:header/title logo-header) cart)

   (component/build match-stylist/organism match-stylist nil)])

(defn page
  [app-state]
  (component/build template
                   {:match-stylist match-stylist-query
                    :logo-header   logo-header-query
                    :cart          {:quantity (orders/product-quantity (get-in app-state keypaths/order))}}))
