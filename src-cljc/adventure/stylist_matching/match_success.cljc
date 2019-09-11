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
            api.orders
            [storefront.accessors.experiments :as experiments]))

(defn ^:private query [data]
  (let [servicing-stylist     (get-in data keypaths/adventure-servicing-stylist)
        order                 (get-in data storefront-keypaths/order)
        consolidated-cart?    (experiments/consolidated-cart? data)
        cart-has-hair?        (> (orders/product-quantity order) 0)
        button-text           (if cart-has-hair? "Continue to Cart" "Show me hair")
        button-target-message (if (or cart-has-hair? consolidated-cart?) [events/navigate-cart] [events/navigate-adventure-shop-hair])]
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
     :header-data          {:progress                (if consolidated-cart? 6 progress/match-success)
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
     (let [consolidated-cart?                         (experiments/consolidated-cart? app-state)
           cart-contains-a-freeinstall-eligible-item? (-> app-state
                                                          api.orders/current
                                                          :mayvenn-install/quantity-added
                                                          (> 0))]
       (if (and cart-contains-a-freeinstall-eligible-item? consolidated-cart?)
         (history/enqueue-navigate events/navigate-cart)
         (history/enqueue-navigate events/navigate-adventure-match-success-pre-purchase)))))

#?(:cljs
   (defmethod effects/perform-effects events/navigate-adventure-match-success
     [_ event {:keys [query-params]} app-state-before app-state]
     (let [adventure-choices (get-in app-state keypaths/adventure-choices)]
       (when (nil? (get-in app-state adventure.keypaths/adventure-servicing-stylist))
         (history/enqueue-redirect events/navigate-adventure-find-your-stylist)))))

#?(:cljs
   (defmethod effects/perform-effects events/api-success-assign-servicing-stylist-post-purchase [_ _ _ _ app-state]
     (history/enqueue-navigate events/navigate-adventure-match-success-post-purchase)))
