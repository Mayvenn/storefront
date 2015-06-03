(ns storefront.cookie-jar
  (:require [storefront.uuid :refer [random-uuid]])
  (:import [goog.net Cookies]))

(defn make-cookie []
  (Cookies. js/document))

(def user-attrs [:email :token :store-slug :id :order-token :order-id])
(def remember-me-age (* 60 60 24 7 4))
(def session-age (* 60 60 24 7 52))

(defn retrieve-login [cookie]
  (zipmap user-attrs
          (map #(.get cookie %) user-attrs)))

(defn force-session-id [cookie]
  (if-let [session-id (.get cookie :session-id)]
    session-id
    (let [created-session-id (random-uuid)]
      (.set cookie :session-id created-session-id session-age)
      created-session-id)))

(defn save [cookie attrs {:keys [remember?]}]
  (let [age (if remember? remember-me-age -1)]
    (doseq [attr user-attrs]
      (if-let [val (attr attrs)]
        (.set cookie attr val age)
        (.remove cookie attr)))))

(defn clear [cookie]
  (doseq [attr user-attrs]
    (.remove cookie attr)))
