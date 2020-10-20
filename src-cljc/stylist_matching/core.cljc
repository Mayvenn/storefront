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
  (:require #?@(:cljs [[storefront.api :as api]
                       [storefront.accessors.categories :as categories]
                       [storefront.browser.cookie-jar :as cookie-jar]
                       [storefront.frontend-effects :as ffx]
                       [storefront.history :as history]
                       [storefront.hooks.facebook-analytics :as facebook-analytics]
                       [storefront.request-keys :as request-keys]
                       [storefront.hooks.google-maps :as google-maps]
                       [goog.functions]
                       [catalog.skuers :as skuers]
                       storefront.keypaths])
            [adventure.keypaths]
            [stylist-directory.keypaths]
            [storefront.effects :as fx]
            [storefront.events :as e]
            [stylist-matching.keypaths :as k]
            [stylist-matching.search.accessors.filters :as filters]
            [storefront.trackings :as trackings]
            [storefront.platform.messages :refer [handle-message] :rename {handle-message publish}]
            clojure.set
            [clojure.string :as string]
            [storefront.transitions :as t]
            [storefront.platform.messages :as messages]))

(def ^:private query-param-keys
  #{:lat :long :preferred-services :s})

(def service-delimiter "~")

(defn ^:private query-params<-
  [query-params {:param/keys [location services] moniker :param/name}]
  (merge (apply dissoc query-params query-param-keys)
         (when (seq moniker)
           {:name moniker})
         (when-let [{:keys [latitude longitude]} location]
           {:lat latitude :long longitude})
         (when (seq services)
           {:preferred-services (string/join service-delimiter
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
     (api/get-products (get-in state storefront.keypaths/api-cache)
                       (merge-with clojure.set/union catalog.services/discountable catalog.services/a-la-carte)
                       (partial publish e/api-success-v3-products-for-stylist-filters))))

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

;; Param 'name' presearched
(defmethod t/transition-state e/flow|stylist-matching|param-name-presearched
  [_ _ {name-presearch :presearch/name} state]
  (cond-> state
    (< (count name-presearch) 3)
    (update-in k/status disj :results.presearch/name)
    :always
    (assoc-in k/presearch-name name-presearch)))

#?(:cljs
   (def debounced-presearch
     (goog.functions/debounce api/presearch-name 200)))

(defmethod fx/perform-effects e/flow|stylist-matching|param-name-presearched
  [_ _ {name-presearch :presearch/name} _ state]
  #?(:cljs
     (let [{:param/keys [location services]} (stylist-matching<- state)]
       (when (and location (> (count name-presearch) 2))
         (let [query
               (-> (select-keys location [:latitude :longitude])
                   (assoc :radius "100mi")
                   (assoc :name name-presearch)
                   (assoc :preferred-services services))]
           (when-let [pending-requests (->> storefront.keypaths/api-requests
                                            (get-in state)
                                            (filter (comp (partial = request-keys/presearch-name) :request-key))
                                            not-empty)]
             (ffx/abort-pending-requests pending-requests))
           (debounced-presearch query
                                #(publish e/api-success-presearch-name
                                          (merge {:query          query
                                                  :presearch/name name-presearch}
                                                 %))))))))

;; Param 'name' constrained
;; -> model <> name
(defmethod t/transition-state e/flow|stylist-matching|param-name-constrained
  [_ _ {moniker :name} state]
  (-> state
      (assoc-in k/name moniker)
      (update-in k/status disj :results.presearch/name)))

(defmethod t/transition-state e/flow|stylist-matching|presearch-canceled
  [_ _ _ state]
  (update-in state k/status disj :results.presearch/name))


(defmethod t/transition-state e/flow|stylist-matching|presearch-cleared
  [_ _ _ state]
  (-> state
      (assoc-in k/presearch-name nil)
      (assoc-in k/name nil)
      (update-in k/status disj :results.presearch/name)))

(defmethod fx/perform-effects e/flow|stylist-matching|presearch-cleared
  [_ _ _ _ state]
  (messages/handle-message e/flow|stylist-matching|prepared))

;; Prepared
;; -> screen: results
;; -> uri: params set
(defmethod t/transition-state e/flow|stylist-matching|prepared
  [_ _ _ state]
  (update-in state k/status disj :results/stylists))

(defmethod fx/perform-effects e/flow|stylist-matching|prepared
  [_ _ _ _ state]
  #?(:cljs (cookie-jar/save-adventure (get-in state storefront.keypaths/cookie)
                                      (get-in state adventure.keypaths/adventure)))
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
     (let [{:param/keys [ids location services] moniker :param/name} (stylist-matching<- state)]
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
                   (assoc :name moniker)
                   (assoc :preferred-services services))]
           (api/fetch-stylists-matching-filters query
                                                #(publish e/api-success-fetch-stylists-matching-filters
                                                          (merge {:query query} %))))))))

;; Stylists: Resulted
(defmethod t/transition-state e/flow|stylist-matching|resulted
  [_ _ {:keys [results]} state]
  (-> state
      (assoc-in k/stylist-results results)
      (update-in k/status clojure.set/union #{:results/stylists})))

;; FIXME Location queries send this, but why? is it just historical?
;; No, it's because the the apis are chained...
;; :choices (get-in app-state adventure.keypaths/adventure-choices) ; For trackings purposes only

;; ------------------ Stylist selected for inspection
(defmethod fx/perform-effects e/flow|stylist-matching|selected-for-inspection
  [_ _ {:keys [stylist-id store-slug]} state]
  #?(:cljs
     (history/enqueue-navigate e/navigate-adventure-stylist-profile {:stylist-id stylist-id
                                                                     :store-slug store-slug})))

;; ------------------- Matched
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

;; -------------------------- presearch name


;; TODO(corey) probably should represent 'presearch resultings' in the domain
(defmethod t/transition-state e/api-success-presearch-name
  [_ _ {:keys [results query]} state]
  (cond-> (assoc-in state k/name-presearch-results results)

    (= (:name query) (get-in state k/presearch-name))
    (update-in k/status clojure.set/union #{:results.presearch/name})))
