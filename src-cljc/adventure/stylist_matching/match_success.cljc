(ns adventure.stylist-matching.match-success
  (:require [storefront.accessors.stylists :as stylists]
            [storefront.events :as events]
            [storefront.accessors.orders :as orders]
            [storefront.component :as component]
            [adventure.components.basic-prompt :as basic-prompt]
            [storefront.keypaths :as storefront-keypaths]
            [adventure.keypaths :as keypaths]
            [storefront.effects :as effects]
            [storefront.transitions :as transitions]
            #?(:cljs [storefront.history :as history])
            [adventure.progress :as progress]
            [storefront.accessors.experiments :as experiments]))

(defn ^:private query [data]
  (let [servicing-stylist     (get-in data keypaths/adventure-servicing-stylist)
        order                 (get-in data storefront-keypaths/order)
        adv-on-shop?          (experiments/adventure-on-shop? data)
        cart-has-hair?        (> (orders/product-quantity order) 0)
        button-text           (if cart-has-hair? "Continue to Cart" "Show me hair")
        button-target-message (if (or cart-has-hair? adv-on-shop?) [events/navigate-cart] [events/navigate-adventure-shop-hair])]
    {:prompt               (str "Congrats on matching with " (stylists/->display-name servicing-stylist) "!")
     :mini-prompt          (if cart-has-hair?
                             "We'll connect you with your stylist after your place your order!"
                             "We'll connect you with your stylist shortly. But first, pick out your hair!")
     :show-logo?           false
     :background-overrides {:style
                            {:background-size     "200px"
                             :background-position "right 15px bottom 150px"
                             :background-image
                             "url(//ucarecdn.com/8a87f86f-948f-48da-b59d-3ca4d8c6d5a0/-/format/png/-/quality/normal/)"}}
     :current-step         2
     :header-data          {:progress                (if adv-on-shop? 6 progress/match-success)
                            :back-navigation-message [events/navigate-adventure-stylist-results-pre-purchase]}
     :button               {:text           button-text
                            :data-test      "adventure-match-success-choice-show-hair"
                            :color          :white
                            :target-message button-target-message}}))

(defn built-component
  [data opts]
  (component/build basic-prompt/component (query data) opts))

(defmethod transitions/transition-state events/api-success-assign-servicing-stylist-pre-purchase
  [_ _ {:keys [order]} app-state]
  (assoc-in app-state storefront-keypaths/order order))

(defmethod transitions/transition-state events/api-success-assign-servicing-stylist-post-purchase
  [_ _ {:keys [order]} app-state]
  (assoc-in app-state storefront-keypaths/completed-order order))

(defmethod transitions/transition-state events/api-success-assign-servicing-stylist
  [_ _ {:keys [servicing-stylist]} app-state]
  (assoc-in app-state adventure.keypaths/adventure-servicing-stylist servicing-stylist))

#?(:cljs
   (defmethod effects/perform-effects events/api-success-assign-servicing-stylist-pre-purchase [_ _ _ _ app-state]
     (history/enqueue-navigate events/navigate-adventure-match-success-pre-purchase)))

#?(:cljs
   (defmethod effects/perform-effects events/api-success-assign-servicing-stylist-post-purchase [_ _ _ _ app-state]
     (history/enqueue-navigate events/navigate-adventure-match-success-post-purchase)))
