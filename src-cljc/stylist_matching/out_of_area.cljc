(ns stylist-matching.out-of-area
  (:require [storefront.component :as component]
            [storefront.events :as events]
            api.orders
            [stylist-matching.ui.header :as header]
            [stylist-matching.ui.shopping-method-choice :as shopping-method-choice]))

(defn header-query
  [{:order.items/keys [quantity]}]
  {:header.cart/id                "adventure-cart"
   :header.cart/value             quantity
   :header.cart/color             "white"
   :header.title/id               "adventure-title"
   :header.title/primary          "Meet Your Certified Stylist"
   :header.back-navigation/id     "adventure-back"
   :header.back-navigation/target [events/navigate-adventure-stylist-results-pre-purchase]})

(def shopping-method-choice-query
  {:shopping-method-choice.title/id        "stylist-matching-shopping-method-choice"
   :shopping-method-choice.title/primary   "We need some time to find you the perfect stylist!"
   :shopping-method-choice.title/secondary "A Mayvenn representative will contact you soon to help select a Certified Mayvenn Stylist. In the meantime…"
   :list/buttons
   [{:shopping-method-choice.button/id       "button-looks"
     :shopping-method-choice.button/label    "Shop by look"
     :shopping-method-choice.button/target   [events/navigate-shop-by-look
                                              {:album-keyword :look}]
     :shopping-method-choice.button/ucare-id "a9009728-efd3-4917-9541-b4514b8e4776"}
    {:shopping-method-choice.button/id       "button-bundle-sets"
     :shopping-method-choice.button/label    "Pre-made bundle sets"
     :shopping-method-choice.button/target   [events/navigate-shop-by-look
                                              {:album-keyword :all-bundle-sets}]
     :shopping-method-choice.button/ucare-id "87b46db7-4c70-4d3a-8fd0-6e99e78d3c96"}
    {:shopping-method-choice.button/id       "button-a-la-carte"
     :shopping-method-choice.button/label    "Choose individual bundles"
     :shopping-method-choice.button/target   [events/navigate-category
                                              {:page/slug           "mayvenn-install"
                                               :catalog/category-id "23"}]
     :shopping-method-choice.button/ucare-id "6c39cd72-6fde-4ec2-823c-5e39412a6d54"}]})

(defn template
  [{:keys [header shopping-method-choice]} _ _]
  (component/create
   [:div.dark-gray.bg-white.center.flex.flex-auto.flex-column
    (component/build header/organism header nil)
    (component/build shopping-method-choice/organism shopping-method-choice nil)]))

(defn page
  [app-state]
  (let [current-order (api.orders/current app-state)]
    (component/build template
                     {:shopping-method-choice
                      shopping-method-choice-query
                      :header
                      (header-query current-order)})))
