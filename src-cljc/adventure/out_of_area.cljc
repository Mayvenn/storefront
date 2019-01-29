(ns adventure.out-of-area
  (:require [adventure.components.header :as header]
            [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.events :as events]))

(defn ^:private query
  [data]
  {:header-data {:title        "Your New Stylist"
                 :header-attrs {:class "bg-light-lavender white"}
                 :current-step 7 ;; TODO
                 :back-link    events/navigate-adventure-how-far
                 :subtitle     "Step 2 of 3"}})

(defn ^:private component
  [{:keys [header-data]} _ _]
  (component/create
   [:div.center.flex-auto
    (header/built-component header-data nil)
    [:div.bg-white.flex.flex-column.justify-center
     {:style {:padding-top "75px"}}
     [:div.col-10.mx-auto.pt8
      [:div.h3.bold.purple.center.mx-auto.col-8
       "You'll be matched with a stylist in 24 hours."]
      [:div.py2 "The stylist will meet the following criteria:"]
      [:div.col-7.mx-auto.py2
       [:ul.h6.purple-checkmark.pl4.left-align
        [:li "Licensed Salon Stylist"]
        [:li "Mayvenn Certified"]
        [:li "In your area"]]]
      [:div.purple.bold.pt10
       "In the meantime..."]
      [:div.py2
       "Let's pick out your Mayvenn hair."]
      (ui/teal-button {} "Select hair")]]]))

(defn built-component
  [data opts]
  (component/build component (query data) opts))

