(ns adventure.checkout.wait
  (:require [clojure.string :as string]
            [storefront.components.ui :as ui]
            #?@(:cljs [[storefront.api :as api]
                       [storefront.browser.cookie-jar :as cookie-jar]
                       [storefront.history :as history]])
            [storefront.component :as component]
            [storefront.keypaths :as keypaths]
            adventure.keypaths
            [storefront.transitions :as transitions]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.platform.messages :as messages]))

(defn component
  [{:keys []} _ _]
  (component/create
   (ui/narrow-container
    [:div.py6.h2
     [:div.py4 (ui/large-spinner {:style {:height "6em"}})]
     [:h2.center.navy "Processing your order..."]])))

(defn query [data] {})

(defn built-component [data opts]
  (component/build component (query data) opts))

(defmethod effects/perform-effects events/navigate-adventure-checkout-wait [dispatch event args _ app-state]
  #?(:cljs
     (let [completed-order        (get-in app-state keypaths/completed-order)
           has-servicing-stylist? (boolean (:servicing-stylist-id completed-order))]
       (cond
         has-servicing-stylist? (history/enqueue-redirect events/navigate-order-complete completed-order)
         completed-order        (messages/handle-message events/api-shipping-address-geo-lookup)
         :direct-load           (history/enqueue-redirect events/navigate-adventure-home)))))

(defmethod effects/perform-effects events/api-shipping-address-geo-lookup [_ event _ _ app-state]
  #?(:cljs
     (if (not-empty (get-in app-state adventure.keypaths/adventure-stylist-match-location))
       (messages/handle-message events/api-fetch-stylists-within-radius-post-purchase)
       (let [{:keys [address1 address2 city state zipcode]} (get-in app-state keypaths/completed-order-shipping-address)
             params                                         (clj->js {"address" (string/join " " [address1 address2 (str city ",") state zipcode])
                                                                      "region"  "US"})]
         (. (js/google.maps.Geocoder.)
            (geocode params
                     (fn [results status]
                       (if (= "OK" status)
                         (messages/handle-message events/api-success-shipping-address-geo-lookup {:locations results})
                         (messages/handle-message events/api-failure-shipping-address-geo-lookup results)))))))))

(defmethod transitions/transition-state events/api-success-shipping-address-geo-lookup [_ _ {:keys [locations]} app-state]
  #?(:cljs
     (assoc-in app-state adventure.keypaths/adventure-stylist-match-location (some-> locations
                                                                                     (js->clj :keywordize-keys true)
                                                                                     first
                                                                                     :geometry
                                                                                     :location
                                                                                     .toJSON
                                                                                     (js->clj :keywordize-keys true)
                                                                                     (clojure.set/rename-keys {:lat :latitude
                                                                                                               :lng :longitude})))))

(defmethod effects/perform-effects events/api-success-shipping-address-geo-lookup [_ event _ _ app-state]
  #?(:cljs
     (let [cookie    (get-in app-state keypaths/cookie)
           adventure (get-in app-state adventure.keypaths/adventure)]
       (cookie-jar/save-adventure cookie adventure)
       (messages/handle-message events/api-fetch-stylists-within-radius-post-purchase))))

(defmethod effects/perform-effects events/api-fetch-stylists-within-radius-post-purchase [_ event _ _ app-state]
  #?(:cljs
     (let [{:keys [latitude longitude]} (get-in app-state adventure.keypaths/adventure-stylist-match-location)
           choices                      (get-in app-state adventure.keypaths/adventure-choices)
           api-cache                    (get-in app-state keypaths/api-cache)
           query                        {:latitude     latitude
                                         :longitude    longitude
                                         :radius       "100mi"
                                         :install-type (:install-type choices)
                                         :choices      choices}] ; For trackings purposes only
       (api/fetch-stylists-within-radius api-cache
                                         query
                                         #(messages/handle-message events/api-success-fetch-stylists-within-radius-post-purchase
                                                                   (merge {:query query} %))))))

(defmethod effects/perform-effects events/api-success-fetch-stylists-within-radius-post-purchase [_ event _ _ app-state]
  #?(:cljs
     (let [completed-order (get-in app-state keypaths/completed-order)]
       (history/enqueue-redirect events/navigate-need-match-order-complete completed-order))))
