(ns storefront.routes
  (:require [bidi.bidi :as bidi]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.messages :refer [handle-message]]
            [cljs.reader :refer [read-string]]
            [clojure.walk :refer [keywordize-keys]]
            [goog.events]
            [goog.history.EventType :as EventType]
            [cemerick.url :refer [map->query url]])
  (:import [goog.history Html5History]
           [goog Uri]))

(extend-protocol bidi.bidi/Pattern
  cljs.core.PersistentHashMap
  (match-pattern [this env]
    (when (every? (fn [[k v]]
                    (cond
                      (or (fn? v) (set? v)) (v (get env k))
                      :otherwise (= v (get env k))))
                  (seq this))
      env))
  (unmatch-pattern [_ _] ""))

(extend-protocol bidi.bidi/Matched
  cljs.core.PersistentHashMap
  (resolve-handler [this m] (some #(bidi.bidi/match-pair % m) this))
  (unresolve-handler [this m] (some #(bidi.bidi/unmatch-pair % m) this)))

(defn edn->bidi [value]
  (keyword (prn-str value)))

(defn bidi->edn [value]
  (read-string (name value)))

(def app-routes
  ["" {"/" (edn->bidi events/navigate-home)
       "/categories" (edn->bidi events/navigate-categories)
       ["/categories/hair/" :taxon-path] (edn->bidi events/navigate-category)
       ["/products/" :product-path] (edn->bidi events/navigate-product)
       "/guarantee" (edn->bidi events/navigate-guarantee)
       "/help" (edn->bidi events/navigate-help)
       "/policy/privacy" (edn->bidi events/navigate-privacy)
       "/policy/tos" (edn->bidi events/navigate-tos)
       "/login" (edn->bidi events/navigate-sign-in)
       "/login/getsat" (edn->bidi events/navigate-sign-in-getsat)
       "/signup" (edn->bidi events/navigate-sign-up)
       "/password/recover" (edn->bidi events/navigate-forgot-password)
       ["/m/" :reset-token] (edn->bidi events/navigate-reset-password)
       "/account/edit" (edn->bidi events/navigate-account-manage)
       "/account/referrals" (edn->bidi events/navigate-account-referrals)
       "/cart" (edn->bidi events/navigate-cart)
       "/stylist/commissions" (edn->bidi events/navigate-stylist-dashboard-commissions)
       "/stylist/store_credits" (edn->bidi events/navigate-stylist-dashboard-bonus-credit)
       "/stylist/referrals" (edn->bidi events/navigate-stylist-dashboard-referrals)
       "/stylist/edit" (edn->bidi events/navigate-stylist-manage-account)
       "/share" (edn->bidi events/navigate-friend-referrals)
       "/checkout/login" (edn->bidi events/navigate-checkout-sign-in)
       "/checkout/address" (edn->bidi events/navigate-checkout-address)
       "/checkout/delivery" (edn->bidi events/navigate-checkout-delivery)
       "/checkout/payment" (edn->bidi events/navigate-checkout-payment)
       "/checkout/confirm" (edn->bidi events/navigate-checkout-confirmation)
       ["/orders/" :number "/complete"] (edn->bidi events/navigate-order-complete)}
   true (edn->bidi events/navigate-not-found)])

;; Html5History transformer defaults to always appending location.search
;; to any token we give it.
;;
;; This allows us to override it to never append location.search
(def non-search-preserving-history-transformer
  (let [opts #js {}]
    (set! (.-createUrl opts) (fn [pathPrefix location]
                               (str pathPrefix)))
    (set! (.-retrieveToken opts) (fn [pathPrefix location]
                                   (.-pathname location)))
    opts))

(defn make-history [callback]
  (doto (Html5History. nil non-search-preserving-history-transformer)
    (.setUseFragment false)
    (.setPathPrefix "")
    (.setEnabled true)
    (goog.events/listen EventType/NAVIGATE (fn [e] (callback)))))

(def app-history)

(defn set-current-page []
  (let [uri (.getToken app-history)
        {nav-event :handler params :route-params}
        (bidi/match-route app-routes uri)

        query-params (:query (url js/location.href))]
    (handle-message (if nav-event (bidi->edn nav-event) events/navigate-not-found)
                    (-> params
                        (merge (when query-params {:query-params query-params}))
                        keywordize-keys))))

(defn install-routes []
  (set! app-history (make-history set-current-page)))

(defn set-query-string [s query-params]
  (-> (Uri.parse s)
      (.setQueryData (map->query (if (seq query-params)
                                   query-params
                                   {})))
      .toString))

(defn path-for [navigation-event & [args]]
  (let [query-params (:query-params args)
        args (dissoc args :query-params)]
    (-> (apply bidi/path-for
               app-routes
               (edn->bidi navigation-event)
               (apply concat (seq args)))
        (set-query-string query-params))))

(defn enqueue-redirect [navigation-event & [args]]
  (let [query-params (:query-params args)
        args (dissoc args :query-params)]
    (.replaceToken app-history
                   (-> (path-for navigation-event args)
                       (set-query-string query-params)))))

(defn enqueue-navigate [navigation-event & [args]]
  (let [query-params (:query-params args)
        args (dissoc args :query-params)]
    (.setToken app-history
               (-> (path-for navigation-event args)
                   (set-query-string query-params)))))

(defn current-path [app-state]
  (apply path-for (get-in app-state keypaths/navigation-message)))
