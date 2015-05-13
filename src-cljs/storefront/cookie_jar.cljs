(ns storefront.cookie-jar
  (:import [goog.net Cookies]))

(defn make-cookie []
  (Cookies. js/document))

(def user-attrs [:email :token :store-slug :id :order-token :order-id])
(def remember-me-age (* 60 60 24 7 4))

(defn retrieve-login [cookie]
  (zipmap user-attrs
          (map #(.get cookie %) user-attrs)))

(defn save [cookie attrs {:keys [remember?]}]
  (let [age (if remember? remember-me-age -1)]
    (doseq [attr user-attrs]
      (.set cookie attr (attr attrs) age))))

(defn clear [cookie]
  (doseq [attr user-attrs]
    (.remove cookie attr)))
