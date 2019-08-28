(ns stylist-matching.match-success
  (:require [storefront.component :as component]
            [storefront.events :as events]
            adventure.keypaths
            [stylist-directory.stylists :as stylists]
            [stylist-matching.ui.atoms :as stylist-matching.A]
            [stylist-matching.ui.header :as header]
            [stylist-matching.ui.shopping-method-choice :as shopping-method-choice] ))

(defn query
  [servicing-stylist]
  {:shopping-method-choice.title/id        "stylist-matching-shopping-method-choice"
   :shopping-method-choice.title/primary   [:div "Congrats on matching with "
                                            [:span.bold (stylists/->display-name servicing-stylist)]
                                            "!"]
   :shopping-method-choice.title/secondary [:div
                                            [:div "Now for the fun part!"]
                                            [:div "How would you like to shop your hair?"]]

   :list/buttons
   [{:shopping-method-choice.button/id       "button-looks"
     :shopping-method-choice.button/label    "Shop by look"
     :shopping-method-choice.button/target   [events/navigate-shop-by-look
                                              {:album-keyword :look}]
     :shopping-method-choice.button/ucare-id "a9009728-efd3-4917-9541-b4514b8e4776"}
    {:shopping-method-choice.button/id       "button-a-la-carte"
     :shopping-method-choice.button/label    "Choose individual bundles"
     :shopping-method-choice.button/target   [events/navigate-category
                                              {:page/slug           "mayvenn-install"
                                               :catalog/category-id "23"}]
     :shopping-method-choice.button/ucare-id "6c39cd72-6fde-4ec2-823c-5e39412a6d54"}
    {:shopping-method-choice.button/id       "button-bundle-sets"
     :shopping-method-choice.button/label    "Pre-made bundle sets"
     :shopping-method-choice.button/target   [events/navigate-shop-by-look
                                              {:album-keyword :deals}]
     :shopping-method-choice.button/ucare-id "87b46db7-4c70-4d3a-8fd0-6e99e78d3c96"}]

   :header.cart/id                "adventure-cart"
   :header.title/id               "adventure-title"
   :header.title/primary          "Meet Your Certified Stylist"
   :header.back-navigation/id     "adventure-back"
   :header.back-navigation/target [events/navigate-adventure-stylist-results-pre-purchase]})

(defn template
  [data _ _]
  (component/create
   [:div.bg-lavender.white.center.flex.flex-auto.flex-column
    stylist-matching.A/bottom-right-party-background-atom
    (component/build header/organism data nil)
    (component/build shopping-method-choice/organism data nil)]))

(defn page
  [app-state]
  ;; TODO servicing-stylist api
  (let [servicing-stylist (get-in app-state adventure.keypaths/adventure-servicing-stylist)]
    (component/build template (query servicing-stylist))))
