(ns stylist-matching.out-of-area
  (:require api.orders
            [storefront.accessors.experiments :as experiments]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.header :as header]
            [storefront.events :as events]
            [stylist-matching.ui.shopping-method-choice :as shopping-method-choice]))

(defn header-query
  [{:order.items/keys [quantity]}]
  {:header.cart/id                "mobile-cart"
   :header.cart/value             quantity
   :header.cart/color             "white"
   :header.title/id               "adventure-title"
   :header.title/primary          "Meet Your Stylist"
   :header.back-navigation/id     "adventure-back"
   :header.back-navigation/target [events/navigate-adventure-find-your-stylist]})

(defn shopping-method-choice-query [hide-bundle-sets?]
  {:shopping-method-choice.error-title/id        "stylist-matching-shopping-method-choice"
   :shopping-method-choice.error-title/primary   "We need some time to find you the perfect stylist!"
   :shopping-method-choice.error-title/secondary (str
                                                  "A Mayvenn representative will contact you soon "
                                                  "to help select a Certified Mayvenn Stylist. In the meantime…")
   :list/buttons (remove nil?
                         [{:shopping-method-choice.button/id       "button-looks"
                           :shopping-method-choice.button/label    "Shop by look"
                           :shopping-method-choice.button/target   [events/navigate-shop-by-look
                                                                    {:album-keyword :look}]
                           :shopping-method-choice.button/ucare-id "a9009728-efd3-4917-9541-b4514b8e4776"}
                          (when-not hide-bundle-sets?
                            {:shopping-method-choice.button/id       "button-bundle-sets"
                             :shopping-method-choice.button/label    "Pre-made bundle sets"
                             :shopping-method-choice.button/target   [events/navigate-shop-by-look
                                                                      {:album-keyword :all-bundle-sets}]
                             :shopping-method-choice.button/ucare-id "87b46db7-4c70-4d3a-8fd0-6e99e78d3c96"})
                          {:shopping-method-choice.button/id       "button-a-la-carte"
                           :shopping-method-choice.button/label    "Choose individual bundles"
                           :shopping-method-choice.button/target   [events/navigate-category
                                                                    {:page/slug           "mayvenn-install"
                                                                     :catalog/category-id "23"}]
                           :shopping-method-choice.button/ucare-id "6c39cd72-6fde-4ec2-823c-5e39412a6d54"}])})

(defcomponent template
  [{:keys [header shopping-method-choice]} _ _]
  [:div.bg-white.center.flex.flex-auto.flex-column
   (header/adventure-header (:header.back-navigation/target header)
                            (:header.title/primary header)
                            {:quantity (:header.cart/value header)})
   (component/build shopping-method-choice/organism shopping-method-choice nil)])

(defn page
  [app-state]
  (let [current-order (api.orders/current app-state)]
    (component/build template
                     {:shopping-method-choice
                      (shopping-method-choice-query
                       (experiments/hide-bundle-sets? app-state))
                      :header
                      (header-query current-order)})))
