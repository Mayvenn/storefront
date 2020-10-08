(ns stylist-matching.core
  "
  Represents the matching (searching) of a stylist.

  The outcome of this flow is a matched stylist.

  Stylists can be matched according to many different parameters
  - Location
  - Services offered
  - IDs
  - Name
  "
  (:require #?@(:cljs [adventure.keypaths
                       [storefront.api :as api]
                       [storefront.accessors.categories :as categories]
                       [storefront.browser.cookie-jar :as cookie-jar]
                       [storefront.history :as history]
                       [storefront.hooks.google-maps :as google-maps]
                       [catalog.skuers :as skuers]
                       [clojure.set :refer [union]]
                       storefront.keypaths])
            [stylist-matching.keypaths :as k]
            [stylist-matching.search.accessors.filters :as filters]
            stylist-matching.selected
            [clojure.string :refer [join]]
            [storefront.effects :as fx]
            [storefront.transitions :as t]
            [storefront.platform.messages :refer [handle-message] :rename {handle-message publish}]
            [storefront.events :as e]))

(def ^:private query-param-keys
  #{:lat :long :preferred-services :s})

(def service-delimiter #"~")

(defn ^:private query-params<-
  [query-params {:param/keys [location services]}]
  (merge (apply dissoc query-params query-param-keys)
         (when-let [{:keys [latitude longitude]} location]
           {:lat latitude :long longitude})
         (when (seq services)
           {:preferred-services (join service-delimiter
                                      services)})))

;; ---------------------- Models

(defn stylist-matching<-
  [state]
  (not-empty
   (get-in state k/stylist-matching)))

(defn google-place-autocomplete<-
  [state]
  {:input    (get-in state k/google-input)
   :location (get-in state k/google-location)})

(def initial-state {})

;;; -------------- Domain events

;; Init
;; <- screen: collect address
;; -> model: unit
(defmethod t/transition-state e/flow|stylist-matching|initialized
  [_ _ _ state]
  (assoc-in state k/stylist-matching initial-state))
(defmethod fx/perform-effects e/flow|stylist-matching|initialized
  [_ _ _ _ state]
  #?(:cljs
     (let [cache         (get-in state storefront.keypaths/api-cache)
           categories-db (get-in state storefront.keypaths/categories)
           criteria      (merge-with union
                                     (skuers/essentials (categories/id->category "35" categories-db))
                                     (skuers/essentials (categories/id->category "31" categories-db)))]
       (api/get-products cache
                         criteria
                         #(publish e/api-success-v3-products-for-stylist-filters
                                   %))))
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
  (let [valid-services (->> services
                            (filter filters/allowed-stylist-filters)
                            not-empty
                            set)]
    (assoc-in state k/services valid-services)))

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
  #?(:cljs (cookie-jar/save-adventure (get-in state storefront.keypaths/cookie)
                                      (get-in state adventure.keypaths/adventure)))
  (let [query-params (->> (stylist-matching<- state)
                          (query-params<- {}))]
    #?(:cljs
       (history/enqueue-navigate e/navigate-adventure-stylist-results
                                 {:query-params query-params}))))

;; Searched
;; -> screen: results
;; -> api: stylist search
;; -> stylist db: sync
(defmethod t/transition-state e/flow|stylist-matching|searched
  [_ _ _ state]
  (update-in state k/status (comp set #(conj % :results/stylists))))

(defmethod fx/perform-effects e/flow|stylist-matching|searched
  [_ _ _ _ state]
  #?(:cljs
     (let [{:param/keys [ids location services]} (stylist-matching<- state)]
       (cond
         ids
         (api/fetch-matched-stylists (get-in state storefront.keypaths/api-cache)
                                     ids
                                     #(publish e/api-success-fetch-matched-stylists
                                               %))
         location
         (let [query
               (-> location
                   (select-keys [:latitude :longitude])
                   (assoc :radius "100mi")
                   (assoc :preferred-services services))]
           (api/fetch-stylists-matching-filters query
                                                #(publish e/api-success-fetch-stylists-matching-filters
                                                          (merge {:query query} %))))))))

;; Stylists: Resulted
(defmethod t/transition-state e/flow|stylist-matching|resulted
  [_ _ {:keys [results]} state]
  (cond-> state
    (pos? (count results))
    (assoc-in k/stylist-results results)
    :always
    (update-in k/status disj :results/stylists)))

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
                                             (publish e/api-success-assign-servicing-stylist
                                                      {:order        order
                                                       :stylist      stylist
                                                       :result-index result-index}))))))

;; -------------------------- stylist search by ids

(defmethod fx/perform-effects e/api-success-fetch-matched-stylists
  [_ _ {:keys [stylists]} _]
  (publish e/flow|stylist-matching|resulted
           {:method  :by-ids
            :results stylists}))

;; -------------------------- stylist search by filters behavior

(defmethod fx/perform-effects e/api-success-fetch-stylists-matching-filters
  [_ _ {:keys [stylists]} _ _]
  (publish e/flow|stylist-matching|resulted
           {:method  :by-location
            :results stylists}))
