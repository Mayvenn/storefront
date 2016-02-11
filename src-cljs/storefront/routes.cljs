(ns storefront.routes
  (:require [bidi.bidi :as bidi]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.messages :refer [send]]
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

(defn set-current-page [app-state]
  (let [uri (.getToken (get-in app-state keypaths/history))

        {nav-event :handler params :route-params}
        (bidi/match-route (get-in app-state keypaths/routes) uri)

        query-params (:query (url js/location.href))]
    (send app-state
          (if nav-event (bidi->edn nav-event) events/navigate-not-found)
          (-> params
              (merge (when query-params {:query-params query-params}))
              keywordize-keys))))

(defn history-callback [app-state]
  (fn [e]
    (set-current-page @app-state)))

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
    (goog.events/listen EventType/NAVIGATE callback)))

(defn routes []
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
       "/account/edit" (edn->bidi events/navigate-manage-account)
       "/cart" (edn->bidi events/navigate-cart)
       "/stylist/commissions" (edn->bidi events/navigate-stylist-commissions)
       "/stylist/store_credits" (edn->bidi events/navigate-stylist-bonus-credit)
       "/stylist/referrals" (edn->bidi events/navigate-stylist-referrals)
       "/stylist/edit" (edn->bidi events/navigate-stylist-manage-account)
       "/share" (edn->bidi events/navigate-friend-referrals)
       ["/orders/" :number] (edn->bidi events/navigate-order)
       "/checkout/address" (edn->bidi events/navigate-checkout-address)
       "/checkout/delivery" (edn->bidi events/navigate-checkout-delivery)
       "/checkout/payment" (edn->bidi events/navigate-checkout-payment)
       "/checkout/confirm" (edn->bidi events/navigate-checkout-confirmation)
       ["/orders/" :number "/complete"] (edn->bidi events/navigate-order-complete)}
   true (edn->bidi events/navigate-not-found)])

(defn install-routes [app-state]
  (let [history (or (get-in @app-state keypaths/history)
                    (make-history (history-callback app-state)))]
    (swap! app-state
           merge
           {:routes (routes)
            :history history})))

(defn set-query-string [s query-params]
  (-> (Uri.parse s)
      (.setQueryData (map->query (if (seq query-params)
                                   query-params
                                   {})))
      .toString))

(defn path-for [app-state navigation-event & [args]]
  (let [query-params (:query-params args)
        args (dissoc args :query-params)]
    (-> (apply bidi/path-for
               (get-in app-state keypaths/routes)
               (edn->bidi navigation-event)
               (apply concat (seq args)))
        (set-query-string query-params))))

(defn enqueue-redirect [app-state navigation-event & [args]]
  (let [query-params (:query-params args)
        args (dissoc args :query-params)]
    (.replaceToken (get-in app-state keypaths/history)
                   (-> (path-for app-state navigation-event args)
                       (set-query-string query-params)))))

(defn enqueue-navigate [app-state navigation-event & [args]]
  (let [query-params (:query-params args)
        args (dissoc args :query-params)]
    (.setToken (get-in app-state keypaths/history)
               (-> (path-for app-state navigation-event args)
                   (set-query-string query-params)))))

(defn current-path [app-state]
  (apply path-for app-state (get-in app-state keypaths/navigation-message)))
