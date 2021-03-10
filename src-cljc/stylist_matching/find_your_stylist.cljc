(ns stylist-matching.find-your-stylist
  (:require #?@(:cljs [[storefront.hooks.google-maps :as google-maps]])
            [api.catalog :refer [select ?service]]
            api.orders
            [clojure.string :as string]
            [storefront.accessors.sites :as sites]
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

(def find-your-stylist-error-codes
  {"stylist-not-found"
   (str
    "The stylist you are looking for is not available."
    " Please search for another stylist in your area below.")})

(def ^:private sv2-codes->srvs
  {"LBI" "SRV-LBI-000"
   "CBI" "SRV-CBI-000"
   "FBI" "SRV-FBI-000"
   "3BI" "SRV-3BI-000"
   \3    "SRV-3CU-000"
   \C    "SRV-DPCU-000"
   \D    "SRV-TKDU-000"
   \F    "SRV-FCU-000"
   \L    "SRV-CCU-000"
   \T    "SRV-TRMU-000"})

(defn ^:private services->srv-sku-ids
  [srv-sku-ids {:keys [catalog/sku-id]}]
  (concat srv-sku-ids
          (if (string/starts-with? sku-id "SV2")
            (let [[_ base addons] (re-find #"SV2-(\w+)-(\w+)" sku-id)]
              (->> addons
                   (concat [base])
                   (map sv2-codes->srvs)
                   (remove nil?)))
            [sku-id])))

(defmethod effects/perform-effects events/navigate-adventure-find-your-stylist
  [_ event {:keys [query-params] :as args} _ state]
  (if (not= :shop (sites/determine-site state))
    (effects/redirect events/navigate-home)
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
        (when-let [preferred-services (->> items
                                           (select ?service)
                                           (reduce services->srv-sku-ids [])
                                           not-empty)]
          (messages/handle-message events/flow|stylist-matching|param-services-constrained
                                   {:services preferred-services}))))))

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
  {:header.back-navigation/back   (when-let [back (not-empty (first undo-history))]
                                    (when (not= events/navigate-adventure-stylist-results (first (:navigation-message back)))
                                      back))
   :header.back-navigation/id     "adventure-back"
   :header.back-navigation/target [events/navigate-home]
   :header.cart/color             "white"
   :header.cart/id                "mobile-cart"
   :header.cart/value             (or quantity 0)
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

(defn ^:export page
  [state _]
  (let [place-autocomplete (google-place-autocomplete<- state)
        current-order      (api.orders/current state)
        undo-history       (get-in state storefront.keypaths/navigation-undo-stack)]
    (component/build template
                     {:stylist-search (stylist-search<- place-autocomplete)
                      :flash          (flash/query state)
                      :spinner        (spinner<- state)
                      :header         (header<- current-order undo-history)})))
