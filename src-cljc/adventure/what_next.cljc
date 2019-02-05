(ns adventure.what-next
  (:require [storefront.events :as events]
            [storefront.component :as component]
            [adventure.components.multi-prompt :as multi-prompt]))

(defn ^:private query [data]
  {:prompt       [:div.h3 "Choose your next step."]
   :prompt-image "//ucarecdn.com/232d0b42-e889-4b2b-823f-86f9be0e2a0d/-/format/auto/-/quality/normal/"
   :data-test    "adventure-what-next-choice"
   :current-step 1
   :header-data  {:progress  4
                  :title     "Basic Info"
                  :back-link events/navigate-adventure-budget
                  :subtitle  "Step 1 of 3"}
   :buttons      [{:text             "Match me with a certified stylist"
                   :data-test-suffix "match-stylist"
                   :value            {:flow "match-stylist"}
                   :target           events/navigate-adventure-match-stylist}
                  {:text             "Show me high quality hair"
                   :data-test-suffix "shop-hair"
                   :value            {:flow "shop-hair"}
                   :target           events/navigate-adventure-shop-hair}]})


(defn built-component
  [data opts]
  (component/build multi-prompt/component (query data) opts))
