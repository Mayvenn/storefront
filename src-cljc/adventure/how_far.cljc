(ns adventure.how-far
  (:require [storefront.events :as events]
            [storefront.component :as component]
            [storefront.platform.component-utils :as utils]
            [adventure.components.multi-prompt :as multi-prompt]))

(defn ^:private query [data]
  {:prompt       ["How far are you willing"
                  [:br]
                  " to travel to get your hair done?"]
   :prompt-image "//ucarecdn.com/3d071ed0-5d9c-4819-b117-84eb4cfc6ed7/-/format/auto/-/quality/normal/aladdinMatchingOverlayImagePurpleBR203Lm3x.png"
   :data-test    "adventure-how-far-choice"
   :header-data  {:title        "Find Your Stylist"
                  :current-step 7
                  :back-link    events/navigate-adventure-find-your-stylist
                  :subtitle     "Step 2 of 3"}
   :buttons      (mapv (fn [%] {:text             (str % " miles")
                                :data-test-suffix (str %)
                                :value            {:how-far %}
                                :target           events/navigate-adventure-matching-stylist-wait})
                       [10 25 50 100])
   :footer       [:div.h6.black.mt3
                  [:div "Not ready to find a stylist?"]
                  [:a.teal (utils/route-to events/navigate-adventure-shop-hair) "Shop hair"]]})

(defn built-component
  [data opts]
  (component/build multi-prompt/component (query data) opts))
