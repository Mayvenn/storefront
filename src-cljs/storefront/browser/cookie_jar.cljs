(ns storefront.browser.cookie-jar
  (:require [storefront.config :as config])
  (:import [goog.net Cookies]))

(defn make-cookie []
  (Cookies. js/document))

(def user-attrs [:email :user-token :store-slug :id])
(def user-req-attrs [:email :user-token :id])

(def order-attrs [:token :number])
(def order-req-attrs order-attrs)

(def remember-me-age (* 60 60 24 7 4))
(def session-age (* 60 60 24 7 52))
(def secure? (not config/development?))

(defn clear-attrs [attrs cookie]
  (doseq [attr attrs]
    (.remove cookie attr)))

(def clear-user (partial clear-attrs user-attrs))
(def clear-order (partial clear-attrs order-attrs))
(def clear (juxt clear-user clear-order))

(defn has-required-attrs? [m req-attrs]
  (every? seq (vals (select-keys m req-attrs))))

(defn retrieve-login [cookie]
  (let [attrs (zipmap user-attrs
                      (map #(.get cookie %) user-attrs))]
    (if (has-required-attrs? attrs user-req-attrs)
      attrs
      (clear-user cookie))))

(defn retrieve-current-order [cookie]
  (let [attrs (zipmap order-attrs
                      (map #(.get cookie %) order-attrs))]
    (if (has-required-attrs? attrs order-req-attrs)
      attrs
      (clear-order cookie))))

(defn force-session-id [cookie]
  (if-let [session-id (.get cookie :session-id)]
    session-id
    (let [created-session-id (random-uuid)]
      (.set cookie :session-id created-session-id session-age "/" nil secure?)
      created-session-id)))

(defn save-user [cookie attrs {:keys [remember?]}]
  (let [age (if remember? remember-me-age -1)]
    (doseq [attr user-attrs]
      (if-let [val (attr attrs)]
        (.set cookie attr val age "/" nil secure?)
        (.remove cookie attr)))))

(defn save-order [cookie attrs {:keys [remember?]}]
  (let [age (if remember? remember-me-age -1)]
    (doseq [attr order-attrs]
      (if-let [val (attr attrs)]
        (.set cookie attr val age "/" nil secure?)
        (.remove cookie attr)))))
