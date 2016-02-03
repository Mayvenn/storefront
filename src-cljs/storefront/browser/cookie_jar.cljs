(ns storefront.browser.cookie-jar
  (:require [storefront.config :as config])
  (:import [goog.net Cookies]))

(defn make-cookie []
  (Cookies. js/document))

(def user-attrs [:email :user-token :store-slug :id])
(def user-req-attrs [:email :user-token :id])

(def order-attrs [:token :number])
(def order-req-attrs order-attrs)

(def pending-promo-attrs [:pending-promo-code])
(def pending-promo-req-attrs [:pending-promo-code])

(def remember-me-age (* 60 60 24 7 4))
(def session-age (* 60 60 24 7 52))
(def secure? (not config/development?))

(defn clear-attrs [attrs cookie]
  (doseq [attr attrs]
    (.remove cookie attr)))

(def clear-user (partial clear-attrs user-attrs))
(def clear-order (partial clear-attrs order-attrs))
(def clear-pending-promo-code (partial clear-attrs pending-promo-attrs))
(def clear (juxt clear-user clear-order clear-pending-promo-code))

(defn has-required-attrs? [m req-attrs]
  (every? seq (vals (select-keys m req-attrs))))

(defn retrieve-from-cookie [attr-keys req-attr-keys clear-fn cookie]
  (let [attrs (zipmap attr-keys
                      (map #(.get cookie %) attr-keys))]
    (if (has-required-attrs? attrs req-attr-keys)
      attrs
      (clear-fn cookie))))

(def retrieve-login (partial retrieve-from-cookie user-attrs user-req-attrs clear-user))
(def retrieve-current-order (partial retrieve-from-cookie order-attrs order-req-attrs clear-order))
(def retrieve-pending-promo-code (partial retrieve-from-cookie pending-promo-attrs pending-promo-req-attrs clear-pending-promo-code))

(defn force-session-id [cookie]
  (if-let [session-id (.get cookie :session-id)]
    session-id
    (let [created-session-id (str (random-uuid))]
      (.set cookie :session-id created-session-id session-age "/" nil secure?)
      created-session-id)))

(defn save-cookie-attrs [cookie attrs attr-keys {:keys [remember?]}]
  (let [age (if remember? remember-me-age -1)]
    (doseq [attr attr-keys]
      (if-let [val (attr attrs)]
        (.set cookie attr val age "/" nil secure?)
        (.remove cookie attr)))))

(defn save-user [cookie attrs options]
  (save-cookie-attrs cookie attrs user-attrs options))

(defn save-order [cookie attrs options]
  (save-cookie-attrs cookie attrs order-attrs options))

(defn save-pending-promo-code [cookie pending-promo-code]
  (save-cookie-attrs cookie
                     {:pending-promo-code pending-promo-code}
                     pending-promo-attrs
                     {:remember? true}))
