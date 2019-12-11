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
   :match-stylist.title/primary   "Welcome! Let’s match you with a top stylist."
   :match-stylist.title/secondary "If you don't love the install, we'll pay for your 2nd install. It's a win-win!"
   :match-stylist.button/id       "adventure-find-your-stylist"
   :match-stylist.button/label    "Next"
   :match-stylist.button/target   [events/navigate-adventure-find-your-stylist]})

(defcomponent template
  [{:keys [logo-header match-stylist cart]} _ _]
  [:div.bg-white.black.center.flex.flex-column
   stylist-matching.A/woman-in-yellow-background

   (header/mobile-nav-header
    {:class "border-bottom border-gray bg-white black"
     :style {:height "70px"}}
    [:a.block.black.ml2.p2.flex.justify-center.items-center
     (apply utils/route-to (:header.left/target logo-header))
     (svg/left-arrow {:width  "20"
                      :height "20"})]
    [:div.content-1.proxima.medium (:header/title logo-header)]
    [:div.mr2
     (ui/shopping-bag {:style     {:height "60px" :width "60px"}
                       :data-test "mobile-cart"}
                      cart)])

   (component/build match-stylist/organism match-stylist nil)])

(defn page
  [app-state]
  (component/build template
                   {:match-stylist match-stylist-query
                    :logo-header   logo-header-query
                    :cart          {:quantity (orders/product-quantity (get-in app-state keypaths/order))}}))
