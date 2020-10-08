(ns stylist-matching.core
  "
  Represents the matching (searching) of a stylist.

  The outcome of this flow is a matched stylist.
  "
  (:require #?@(:cljs [adventure.keypaths
                       [storefront.api :as api]
                       [storefront.accessors.categories :as categories]
                       [storefront.browser.cookie-jar :as cookie-jar]
                       [storefront.history :as history]
                       [storefront.hooks.google-maps :as google-maps]
                       [catalog.skuers :as skuers]
                       [clojure.set :refer [union]]
                       [storefront.platform.messages :as messages]
                       storefront.keypaths])
            [stylist-matching.keypaths :as k]
            [clojure.string :refer [join]]
            [storefront.effects :as fx]
            [storefront.transitions :as t]
            [storefront.events :as e]))

;; TODO not-empty on model keys

;; {:address  ""
;;  :location {:latitude  0.0 ;; longs
;;             :longitude 0.0
;;             :radius    "100mi"}
;;  :services #{ "A" "B" }
;;  :ids []
;;  :name     ""

;;  :results  []}

;; {:params {}
;;  :name/input
;;  :google/input ""
;;  :google/location {}
;;  :results/stylists
;;  :results/suggestions}


(defn stylist-matching<-
  [state]
  (not-empty
   (get-in state k/stylist-matching)))

(def query-param-keys #{:lat
                        :long
                        :preferred-services
                        :s})

(defn query-params<-
  [query-params {:stylist-matching/keys [location services]}]
  (merge (apply dissoc query-params query-param-keys)
         (when-let [{:keys [latitude longitude]} location]
           {:lat latitude :long longitude})
         (when (seq services)
           {:preferred-services (join "~" services)})))

(defn google-place-autocomplete<-
  [state]
  {:input    (get-in state k/google-input)
   :location (get-in state k/google-location)})

(def initial-state {})

;;; -------------- Events

;; Init
;; <- screen: collect address
;; -> model: unit
(defmethod t/transition-state e/flow|stylist-matching|init ;; FIXME(matching) initialized
  [_ _ _ state]
  (assoc-in state k/stylist-matching initial-state))
(defmethod fx/perform-effects e/flow|stylist-matching|init
  [_ _ _ _ state]
  #?(:cljs
     (let [cache         (get-in state storefront.keypaths/api-cache)
           categories-db (get-in state storefront.keypaths/categories)
           criteria      (merge-with union
                                     (skuers/essentials (categories/id->category "35" categories-db))
                                     (skuers/essentials (categories/id->category "31" categories-db)))]
       (api/get-products cache
                         criteria
                         #(messages/handle-message e/api-success-v3-products-for-stylist-filters %))))
  #?(:cljs (google-maps/insert)))

;; Param 'location' constrained
;; -> model <> location
(defmethod t/transition-state e/flow|stylist-matching|param-address-constrained
  [_ _ {:keys [address]} state]
  (cond-> state
    (not-empty address)
    (assoc-in k/address address)))

;; Param 'location' constrained
;; -> model <> location
(defmethod t/transition-state e/flow|stylist-matching|param-location-constrained
  [_ _ {:as location :keys [latitude longitude]} state]
  (cond-> state
    (and latitude longitude)
    (assoc-in k/location location)))

(defmethod t/transition-state e/flow|stylist-matching|param-services-constrained
  [_ _ {:keys [services]} state]
  ;; FIXME(matching) filter valid services
  (assoc-in state k/services services))

;; Param 'ids' constrained
;; -> model <> ids
(defmethod t/transition-state e/flow|stylist-matching|param-ids-constrained
  [_ _ {:keys [ids]} state]
  (assoc-in state k/ids ids))

;; Param 'name' constrained
;; -> model <> name
(defmethod fx/perform-effects e/flow|stylist-matching|param-name-constrained
  [_ _ _ _ _]
  )

;; Prepared
;; -> screen: results
;; -> uri: params set
(defmethod fx/perform-effects e/flow|stylist-matching|prepared
  [_ _ _ _ state]
  #?(:cljs
     (let [query-params (->> (stylist-matching<- state)
                             (query-params<- {}))]
       (history/enqueue-navigate e/navigate-adventure-stylist-results
                                 {:query-params query-params}))))

;; Searched
;; -> screen: results
;; -> api: stylist search
;; -> stylist db: sync
(defmethod fx/perform-effects e/flow|stylist-matching|searched
  [_ _ _ _ state]
  #?(:cljs
     (let [{:stylist-matching/keys [ids location services]} (stylist-matching<- state)]
       (cond
         ids
         (api/fetch-matched-stylists (get-in state storefront.keypaths/api-cache)
                                     ids
                                     #(messages/handle-message e/api-success-fetch-matched-stylists
                                                               %))
         location
         (let [query
               (-> location
                   (select-keys [:latitude :longitude])
                   (assoc :radius "100mi")
                   (assoc :preferred-services services))]
           (api/fetch-stylists-matching-filters query
                                                #(messages/handle-message
                                                  e/api-success-fetch-stylists-matching-filters
                                                  (merge {:query query} %))))))))

;; Stylists: Resulted
(defmethod t/transition-state e/flow|stylist-matching|resulted
  [_ _ {:keys [results]} state]
  (cond-> state
    (pos? (count results))
    (assoc-in k/stylist-results stylists)))


;; FIXME Location queries send this, but why? is it just historical?
;; No, it's because the the apis are chained...
;; :choices (get-in app-state adventure.keypaths/adventure-choices) ; For trackings purposes only

;; Matched
;; -> current stylist: selected
(defmethod fx/perform-effects e/flow|stylist-matching|matched
  [_ _ {:keys [stylist result-index]} _ state]
  #?(:cljs (cookie-jar/save-adventure (get-in state storefront.keypaths/cookie)
                                      (get-in state adventure.keypaths/adventure)))
  #?(:cljs (let [{:keys [number token]} (get-in state storefront.keypaths/order)
                 features               (get-in state storefront.keypaths/features)]
             (api/assign-servicing-stylist (:stylist-id stylist)
                                           1
                                           number
                                           token
                                           features
                                           (fn [order]
                                             (messages/handle-message e/api-success-assign-servicing-stylist
                                                                      {:order        order
                                                                       :stylist      stylist
                                                                       :result-index result-index})))))
  )

