(ns adventure.handlers
  (:require #?@(:cljs
                [[storefront.history :as history]
                 [storefront.hooks.stringer :as stringer]
                 [storefront.hooks.talkable :as talkable]
                 [storefront.browser.cookie-jar :as cookie]
                 [storefront.api :as api]
                 [storefront.platform.messages :as messages]
                 [clojure.string :as string]])
            api.orders
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as storefront.keypaths]
            [adventure.keypaths :as keypaths]
            [storefront.trackings :as trackings]
            [storefront.transitions :as transitions]
            [catalog.products :as products]
            [storefront.accessors.orders :as orders]))

(def events-not-to-direct-load
  #{events/navigate-adventure-out-of-area
    events/navigate-adventure-match-success-pre-purchase})

(defmethod transitions/transition-state events/control-adventure-choice
  [_ event {:keys [choice]} app-state]
  (-> app-state (update-in keypaths/adventure-choices merge (:value choice))))

(defmethod effects/perform-effects events/navigate-adventure
  [_ event {:keys [query-params]} app-state-before app-state]
  #?(:cljs
     (do
       (let [adventure-choices (get-in app-state keypaths/adventure-choices)]
         (when (and (events-not-to-direct-load event)
                    (empty? adventure-choices)
                    (not (and (= events/navigate-adventure-match-stylist event)
                              (some-> query-params :utm_source (string/includes? "toadventure")))))
           (history/enqueue-navigate events/navigate-home nil))))))

(defmethod transitions/transition-state events/api-success-fetch-matched-stylists
  [_ _ {:keys [stylists]} app-state]
  (assoc-in app-state
            keypaths/adventure-matched-stylists stylists))

(defmethod effects/perform-effects events/api-success-fetch-matched-stylists
  [_ event _ _ app-state]
  #?(:cljs
     (when-not (get-in app-state adventure.keypaths/adventure-stylist-results-delaying?)
       (messages/handle-message events/adventure-stylist-results-wait-resolved))))

(defmethod transitions/transition-state events/api-success-fetch-stylists-within-radius
  [_ _ {:keys [stylists query]} app-state]
  (cond->
   (assoc-in app-state adventure.keypaths/adventure-matched-stylists stylists)

    (seq query)
    (assoc-in adventure.keypaths/adventure-stylist-match-location
              {:latitude  (:latitude query)
               :longitude (:longitude query)
               :radius    (:radius query)})))

(def ^:private slug->video
  {"we-are-mayvenn" {:youtube-id "hWJjyy5POTE"}
   "free-install"   {:youtube-id "oR1keQ-31yc"}})

(defmethod transitions/transition-state events/api-success-remove-servicing-stylist [_ _ {:keys [order]} app-state]
  (-> app-state
      (assoc-in keypaths/adventure-choices-selected-stylist-id nil)
      (assoc-in keypaths/adventure-servicing-stylist nil)))

(defmethod effects/perform-effects events/api-success-remove-servicing-stylist [_ _ {:keys [order]} _ app-state]
  #?(:cljs
     (messages/handle-message events/save-order {:order order})))

(defmethod transitions/transition-state events/navigate-adventure-match-success-post-purchase
  [_ _ _ {:keys [completed-order] :as app-state}]
  #?(:cljs
     (assoc-in app-state storefront.keypaths/pending-talkable-order (talkable/completed-order completed-order))))

(defmethod effects/perform-effects events/navigate-adventure-match-success-post-purchase [_ _ _ _ app-state]
  #?(:cljs
     (let [{install-applied? :mayvenn-install/applied?
            completed-order  :waiter/order} (api.orders/completed app-state)
           servicing-stylist-id             (:servicing-stylist-id completed-order)]
       (if (and install-applied? servicing-stylist-id)
         (do
           (talkable/show-pending-offer app-state)
           (api/fetch-matched-stylist (get-in app-state storefront.keypaths/api-cache)
                                      servicing-stylist-id))
         (history/enqueue-navigate events/navigate-home)))))

(defmethod transitions/transition-state events/api-success-fetch-matched-stylist
  [_ event {:keys [stylist] :as args} app-state]
  (assoc-in app-state adventure.keypaths/adventure-servicing-stylist stylist))
