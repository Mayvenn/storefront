(ns storefront.system.contentful
  (:require [camel-snake-kebab.core :as csk]
            [camel-snake-kebab.extras :as cske]
            [clojure.set :as set]
            [com.stuartsierra.component :as component]
            [lambdaisland.uri :as uri]
            [overtone.at-at :as at-at]
            [ring.util.response :as util.response]
            [spice.core :as spice]
            [spice.date :as date]
            [spice.maps :as maps]
            [tugboat.core :as tugboat]))

(defn contentful-request
  "Wrapper for the Content Delivery endpoint"
  [{:keys [endpoint env-param api-key space-id]} params]
  (let [base-params {:access_token                     api-key
                     :order                            (str "-fields." env-param)
                     (str "fields." env-param "[lte]") (date/to-iso (date/now)) }]
    (try
      (tugboat/request {:endpoint endpoint}
                       :get (str "/spaces/" space-id "/entries")
                       {:socket-timeout 30000
                        :conn-timeout   30000
                        :as             :json
                        :query-params   (merge base-params params)})
      (catch java.io.IOException ioe
        nil))))

(defn extract-fields
  "Contentful resources are boxed.

  We extract the fields, merging useful meta-data."
  [{:keys [fields sys]}]
  (merge (cske/transform-keys csk/->kebab-case fields)
         {:content/updated-at (spice.date/to-millis (:updatedAt sys 0))
          :content/type       (or
                               (some-> sys :contentType :sys :id)
                               "Asset")
          :content/id         (-> sys :id)}))

(defn extract-latest-by
  "Extract a resource (e.g. items), grouped by a key, and taking the latest"
  [resource index-key]
  (->> (map extract-fields resource)
       (group-by index-key)
       (maps/map-values (partial apply max-key :content/updated-at))))

(defn extract
  "Extract resources in a response body"
  [body]
  (-> body
      (update-in [:items]
                 extract-latest-by :content/type)
      (update-in [:includes :Entry]
                 extract-latest-by :content/id)
      (update-in [:includes :Asset]
                 extract-latest-by :content/id)))

(defn resolve-link
  "Resolves contentful links in field values by stitching included
  resources in situ."
  [includes [key value]]
  [key (if (map? value)
         (let [id        (some-> value :sys :id)
               link-type (some-> value :sys :link-type keyword)]
           (get-in includes [link-type id] value))
         value)])

(defn resolve-children
  "Resolves child links in parent maps by lookup in includes."
  [includes parent-map]
  (maps/map-values (fn [child-map]
                     (into {}
                           (map (partial resolve-link includes))
                           child-map))
                   parent-map))

(defn resolve-all
  "Resolves Assets under Entries within items."
  [{:keys [items includes]}]
  (-> includes
      (update :Entry (partial resolve-children includes))
      (resolve-children items)))

(defn do-fetch-entries
  ([contentful content-type]
   (do-fetch-entries contentful content-type 1))
  ([{:keys [logger exception-handler cache space-id] :as contentful} content-type attempt-number]
   (try
     (when (<= attempt-number 2)
       (let [{:keys [status body]} (contentful-request contentful
                                                       {"content_type" (name content-type)
                                                        "limit"        1})]
         (if (and status (<= 200 status 299))
           (swap! cache merge (some-> body extract resolve-all clojure.walk/keywordize-keys (select-keys [content-type])))
           (do-fetch-entries contentful content-type (inc attempt-number)))))
     (catch Throwable t
       ;; Ideally, we should never get here, but at-at halts all polls that throw exceptions silently.
       ;; This simply reports it and lets the polling continue
       (exception-handler t)
       (logger :error t)))))

(defn- give-or-take [dt {:keys [minutes]}]
  (let [start (date/add-delta dt {:minutes (- minutes)})
        end   (date/add-delta dt {:minutes minutes})]
    [start end]))

(defn- date-time-for-every [[start end] {:keys [seconds]}]
  (range (date/to-millis start) (date/to-millis end) (* 1000 seconds)))

;; GROT after black friday
(def black-friday (date/date-time 2018 11 23 5 0 0))

(def increased-polling-intervals
  [(-> black-friday
       (give-or-take {:minutes 5})
       (date-time-for-every {:seconds 10}))])

(defn before-black-friday-launch?
  []
  (date/after? (date/add-delta black-friday {:minutes 10})
               (date/now)))

(defprotocol CMSCache
  (read-cache [_] "Returns a map representing the CMS cache"))

(defrecord ContentfulContext [logger exception-handler environment cache-timeout api-key space-id endpoint]
  component/Lifecycle
  (start [c]
    (let [pool   (at-at/mk-pool)
          cache  (atom {})
          config {:space-id  space-id
                  :endpoint  endpoint
                  :env-param (if (= environment "production")
                               "production"
                               "acceptance")
                  :cache     cache
                  :api-key   api-key}]
      (when before-black-friday-launch?
        (doseq [content-type  [:homepage :advertisedPromo]
                timestamps    increased-polling-intervals
                :let          [num-checks (count timestamps)]
                [i ts-millis] (map-indexed vector timestamps)]
          (at-at/at ts-millis #(do-fetch-entries config content-type)
                    pool
                    :desc (str "scheduled poll for " content-type " " i " of " num-checks))))
      (doseq [content-type [:homepage :mayvennMadePage :advertisedPromo]]
        (at-at/interspaced cache-timeout
                           #(do-fetch-entries config content-type)
                           pool
                           :desc (str "poller for " content-type)))
      (println "Pool polling status at start: " (at-at/show-schedule pool))
      (assoc c
             :pool  pool
             :cache cache)))
  (stop [c]
    (when (:pool c) (at-at/stop-and-reset-pool! (:pool c)))
    (dissoc c :cache :pool))
  CMSCache
  (read-cache [c] (deref (:cache c))))

(defn marketing-site-redirect [req]
  (let [prefix (partial str "https://")
        url    (-> req :query-params (get "to") prefix)
        host   (-> url uri/uri :host)
        to     (if (contains? #{"looks.mayvenn.com"} host)
                 url "https://shop.mayvenn.com")]
    (util.response/redirect to)))
