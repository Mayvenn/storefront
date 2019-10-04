(ns stylist-matching.match-stylist
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.events :as events]
            [stylist-matching.ui.atoms :as stylist-matching.A]
            [stylist-matching.ui.logo-header :as logo-header]
            [stylist-matching.ui.match-stylist :as match-stylist]
            
            
            [storefront.component :as component :refer [defcomponent]]
            [storefront.component :as component :refer [defcomponent]]))

(def logo-header-query
  {:logo-header.logo/id "adventure-logo"})

(def match-stylist-query
  {:match-stylist.title/id        "stylist-matching-match-stylist"
   :match-stylist.title/primary   "Welcome! Let’s match you with a top stylist."
   :match-stylist.title/secondary "If you don’t love the install, we’ll pay for you to get it taken down and redone. It’s a win-win!"
   :match-stylist.button/id       "adventure-find-your-stylist"
   :match-stylist.button/label    "Next"
   :match-stylist.button/target   [events/navigate-adventure-find-your-stylist]})

(defcomponent template
  [{:keys [logo-header match-stylist]} _ _]
  [:div.bg-lavender.white.center.flex.flex-column
    stylist-matching.A/woman-in-yellow-background
    (component/build logo-header/organism logo-header nil)
    (component/build match-stylist/organism match-stylist nil)])

(defn page
  [app-state]
  (component/build template
                   {:match-stylist match-stylist-query
                    :logo-header   logo-header-query}))
