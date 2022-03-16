(ns stylist-matching.find-your-stylist
  (:require #?@(:cljs [[storefront.hooks.google-maps :as google-maps]])
            [api.catalog :refer [select ?service]]
            api.orders
            [clojure.string :as string]
            [mayvenn.concept.wait :as wait]
            [storefront.accessors.sites :as sites]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.flash :as flash]
            [storefront.components.header :as header]
            [storefront.components.svg :as svg]
            [storefront.effects :as effects]
            [storefront.events :as events]
            storefront.keypaths
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [storefront.request-keys :as request-keys]
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

(defn maps-spinner<-
  [app-state]
  (when-not (boolean (get-in app-state storefront.keypaths/loaded-google-maps))
    {:spinner/id "loading-google-maps"}))

(defn stylist-search<-
  [{:keys [input location]}]
  {:stylist-search.title/id                        "find-your-stylist-stylist-search-title"
   :stylist-search.title/primary                   "Where do you want to get your hair done?"
   :stylist-search.location-search-box/id          "stylist-match-address"
   :stylist-search.location-search-box/placeholder "Enter address, city, or zip code"
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
  [{:keys [flash header stylist-search maps-spinner query-spinner? cart]} _ _]
  (if query-spinner?
    [:div.bg-pale-purple.absolute.overlay
     [:div.absolute.overlay.border.border-white.border-framed-white.m4.p5.flex.flex-column.items-center.justify-center
      [:div (svg/mayvenn-logo {:class "spin-y"
                               :style {:width "54px"}})]
      [:div {:style {:height "50%"}}
       [:div.title-2.canela.center
        [:div "Sit back and relax."]
        [:div "There’s no end to what your hair can do."]]]]]
    [:div.flex.flex-auto.flex-column
     (header/adventure-header header)
     (component/build flash/component flash nil)
     [:div.max-580.mx-auto
      (if (seq maps-spinner)
             (component/build spinner/organism maps-spinner nil)
             [:div.px2.mt8.pt4
              (component/build stylist-search/organism stylist-search nil)])]]))

(defn ^:export page
  [state _]
  (let [place-autocomplete (google-place-autocomplete<- state)
        current-order      (api.orders/current state)
        undo-history       (get-in state storefront.keypaths/navigation-undo-stack)]
    (component/build template
                     {:stylist-search (stylist-search<- place-autocomplete)
                      :flash          (flash/query state)
                      :query-spinner? (or (wait/<- state "location-submit")
                                          (utils/requesting? state request-keys/fetch-matched-stylists)
                                          (utils/requesting? state request-keys/fetch-stylists-matching-filters))
                      :maps-spinner   (maps-spinner<- state)
                      :header         (header<- current-order undo-history)})))
