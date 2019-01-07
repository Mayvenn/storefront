(ns adventure.shop-hair
  (:require [adventure.components.basic-prompt :as basic-prompt]
            [storefront.component :as component]
            [storefront.events :as events]))

(defn ^:private query [data]
  {:prompt       "Shop for hair"
   :background-image "http://placekitten.com/300/200"
   :data-test    "adventure-match-shop-hair"
   :header-data  {:current-step 5 ;; Position in flow
                  :title        "Basic Info"
                  :back-link    events/navigate-adventure-what-next
                  :subtitle     "Step 1 of 3"} ;; TODO: Derive from state
   :button       {:text   "Complete shopping" ;; Goes somewhere based on previous choices
                  :value  nil
                  :target nil}})


(defn built-component
  [data opts]
  (component/build basic-prompt/component (query data) opts))
