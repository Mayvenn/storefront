(ns adventure.shop-hair
  (:require [adventure.components.basic-prompt :as basic-prompt]
            [adventure.keypaths :as keypaths]
            [storefront.component :as component]
            [storefront.events :as events]))

(defn background-png-with-gradient [{:keys [ucare-uuid position size gradient-direction start-color end-color]}]
  {:background
   (str "url(//ucarecdn.com/" ucare-uuid "/-/format/auto/bg.png) center "
        position "/" size
        " no-repeat, linear-gradient(" gradient-direction ", " start-color ", " end-color ")")})

(defn ^:private query [data]
  (let [adventure-choices (get-in data keypaths/adventure-choices)
        hair-flow?        (-> adventure-choices :flow #{"shop-hair"})]
    {:prompt               [:div.pb2.line-height-2 "It’s time to pick out some amazing virgin hair."]
     :mini-prompt          [:div.pt2.line-height-2 "Wear it, cut it, style it. If you don’t love your hair, we’ll exchange it for free."]
     :background-overrides {:style
                            (background-png-with-gradient
                             {:ucare-uuid         "20636e0b-60ff-405a-88a4-94c9a3bb3e8a"
                              :position           "bottom"
                              :size               "90%"
                              :gradient-direction "180deg"
                              :start-color        "#CDB8D9"
                              :end-color          "#9A8FB4"})}
     :data-test       "adventure-match-with-stylist"
     :header-data          {:current-step 1
                            :subtitle     (str "Welcome to Step " (if hair-flow? 2 3))
                            :back-link    events/navigate-adventure-what-next}
     :button               {:text   "Next"
                            :data-test "shop-hair-button"
                            :target events/navigate-adventure-match-stylist}}))

(defn built-component
  [data opts]
  (component/build basic-prompt/component (query data) opts))
