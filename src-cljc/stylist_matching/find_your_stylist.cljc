(ns stylist-matching.find-your-stylist
  (:require #?@(:cljs [[storefront.hooks.google-maps :as google-maps]])
            api.orders
            spice.selector
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.flash :as flash]
            [storefront.components.header :as header]
            [storefront.effects :as effects]
            [storefront.events :as events]
            storefront.keypaths
            [storefront.platform.messages :as messages]
            [stylist-matching.core :refer [google-place-autocomplete<-]]
            [stylist-matching.ui.spinner :as spinner]
            [stylist-matching.ui.stylist-search :as stylist-search]))

(def ^:private select
  (comp seq (partial spice.selector/match-all {:selector/strict? true})))

(def ^:private ?service
  {:catalog/department #{"service"}})

;; ------------------------

(def find-your-stylist-error-codes
  {"stylist-not-found"
   (str
    "The stylist you are looking for is not available."
    " Please search for another stylist in your area below.")})

(defmethod effects/perform-effects events/navigate-adventure-find-your-stylist
  [_ event {:keys [query-params] :as args} _ state]
  (if-let [error-message (some-> query-params :error find-your-stylist-error-codes)]
    (do
      (let [args-sans-error (update-in args [:query-params] dissoc :error)]
        (messages/handle-message events/redirect
                                 {:nav-message [event args-sans-error]}))
      (messages/handle-message events/flash-later-show-failure
                               {:message error-message}))
    (let [{:order/keys [items]} (api.orders/current state)]
      #?(:cljs (google-maps/insert))
      (messages/handle-message events/flow|stylist-matching|initialized)
      (when-let [preferred-services (->> (select ?service items)
                                         (mapv :catalog/sku-id)
                                         not-empty)]
        (messages/handle-message events/flow|stylist-matching|param-services-constrained
                                 {:services preferred-services})))))

;; ------------------------

(defn spinner<-
  [app-state]
  (when-not (boolean (get-in app-state storefront.keypaths/loaded-google-maps))
    {:spinner/id "loading-google-maps"}))

(defn stylist-search<-
  [{:keys [input location]}]
  {:stylist-search.title/id                        "find-your-stylist-stylist-search-title"
   :stylist-search.title/primary                   "Where do you want to get your hair done?"
   :stylist-search.location-search-box/id          "stylist-match-address"
   :stylist-search.location-search-box/placeholder "Enter city or street address"
   :stylist-search.location-search-box/value       (str input)
   :stylist-search.location-search-box/clear?      (seq location)
   :stylist-search.button/id                       "stylist-match-address-submit"
   :stylist-search.button/disabled?                (or (empty? location)
                                                       (empty? input))
   :stylist-search.button/target                   [events/control-adventure-location-submit]
   :stylist-search.button/label                    "Search"})

(defn header<-
  [{:order.items/keys [quantity]} undo-history]
  {:header.back-navigation/back   (not-empty (first undo-history))
   :header.back-navigation/id     "adventure-back"
   :header.back-navigation/target [events/navigate-adventure-find-your-stylist]
   :header.cart/color             "white"
   :header.cart/id                "mobile-cart"
   :header.cart/value             quantity
   :header.title/id               "adventure-title"
   :header.title/primary          "Meet Your Stylist"})

(defcomponent template
  [{:keys [flash header stylist-search spinner cart]} _ _]
  [:div.center.flex.flex-auto.flex-column
   (header/adventure-header header)
   (component/build flash/component flash nil)
   (if (seq spinner)
     (component/build spinner/organism spinner nil)
     [:div.px2.mt8.pt4
      (component/build stylist-search/organism stylist-search nil)])])

(defn page
  [state]
  (let [place-autocomplete (google-place-autocomplete<- state)
        current-order      (api.orders/current state)
        undo-history       (get-in state storefront.keypaths/navigation-undo-stack)]
    (component/build template
                     {:stylist-search (stylist-search<- place-autocomplete)
                      :flash          (flash/query state)
                      :spinner        (spinner<- state)
                      :header         (header<- current-order undo-history)})))
