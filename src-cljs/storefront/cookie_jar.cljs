(ns storefront.cookie-jar
  (:require [storefront.uuid :refer [random-uuid]]
            [storefront.config :as config])
  (:import [goog.net Cookies]))

(defn make-cookie []
  (Cookies. js/document))

(def user-attrs [:email :token :store-slug :id])
(def order-attrs [:guest-token :number])
(def remember-me-age (* 60 60 24 7 4))
(def session-age (* 60 60 24 7 52))
(def secure? (not config/development?))

(defn retrieve-login [cookie]
  (zipmap user-attrs
          (map #(.get cookie %) user-attrs)))

(defn retrieve-current-order [cookie]
  (zipmap order-attrs
          (map #(.get cookie %) order-attrs)))

(defn force-session-id [cookie]
  (if-let [session-id (.get cookie :session-id)]
    session-id
    (let [created-session-id (random-uuid)]
      (.set cookie :session-id created-session-id session-age nil nil secure?)
      created-session-id)))

(defn save-user [cookie attrs {:keys [remember?]}]
  (let [age (if remember? remember-me-age -1)]
    (doseq [attr user-attrs]
      (if-let [val (attr attrs)]
        (.set cookie attr val age nil nil secure?)
        (.remove cookie attr)))))

(defn save-order [cookie attrs {:keys [remember?]}]
  (let [age (if remember? remember-me-age -1)]
    (doseq [attr order-attrs]
      (if-let [val (attr attrs)]
        (.set cookie attr val age nil nil secure?)
        (.remove cookie attr)))))

(defn clear [cookie]
  (doseq [attr user-attrs]
    (.remove cookie attr)))
