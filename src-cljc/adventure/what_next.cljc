(ns adventure.what-next
  (:require [storefront.events :as events]
            [storefront.component :as component]
            [adventure.components.multi-prompt :as multi-prompt]))

(defn ^:private query [data]
  {:prompt       "What would you like to do next?"
   :prompt-image "http://placekitten.com/300/200"
   :data-test    "adventure-what-next-choice"
   :header-data  {:current-step 4 ;; Position in flow
                  :title        "Basic Info"
                  :back-link    events/navigate-adventure-get-in-contact
                  :subtitle     "Step 1 of 3"}
   :buttons      [{:text             "Match with stylist"
                   :data-test-suffix "match-stylist"
                   :value            {:flow "match-stylist"}
                   :target           events/navigate-adventure-match-stylist}
                  {:text             "Shop hair"
                   :data-test-suffix "shop-hair"
                   :value            {:flow "shop-hair"}
                   :target           events/navigate-adventure-shop-hair}]})


(defn built-component
  [data opts]
  (component/build multi-prompt/component (query data) opts))
