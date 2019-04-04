(ns adventure.stylist-matching.out-of-area
  (:require [adventure.components.header :as header]
            [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]
            [adventure.progress :as progress]))

(defn ^:private query
  [data]
  {:current-step 2
   :header-data  {:title                   "Your New Stylist"
                  :header-attrs            {:class "bg-light-lavender white"}
                  :progress                progress/out-of-area
                  :back-navigation-message [events/navigate-adventure-how-far]
                  :subtitle                "Step 2 of 3"}})

(defn ^:private component
  [{:keys [header-data]} _ _]
  (component/create
   [:div.center.flex-auto
    (header/built-component header-data nil)
    [:div.bg-white.flex.flex-column.justify-center
     {:style {:padding-top "75px"}}
     [:div.col-10.mx-auto.pt8
      [:div.h3.bold.purple.center.mx-auto.col-10
       "We need some time to find you the perfect stylist!"]
      [:div.py3.h5
       "A Mayvenn representative will contact you soon to help select a Certified Mayvenn Stylist with the following criteria:"]
      [:div.col-7.mx-auto.py2
       [:ul.h6.list-img-purple-checkmark.pl4.left-align
        [:li "Licensed Salon Stylist"]
        [:li "Mayvenn Certified"]
        [:li "In your area"]]]
      [:div.purple.bold.pt10
       "In the meantime..."]
      [:div.py2
       "Let's pick out your Mayvenn hair."]
      (ui/teal-button
       (merge {:data-test "out-of-area-select-hair"}
              (utils/route-to events/navigate-adventure-shop-hair))
       "Select hair")]]]))

(defn built-component
  [data opts]
  (component/build component (query data) opts))

