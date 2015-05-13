(ns storefront.cookie-jar
  (:import [goog.net Cookies]))

(defn make-cookie []
  (Cookies. js/document))

(def cookie-attrs [:email :token :store-slug :id])

(defn retrieve-login [cookie]
  (zipmap cookie-attrs
          (map #(.get cookie %) cookie-attrs)))

(defn set-login [cookie attrs {:keys [remember?]}]
  (let [age (if remember? (* 60 60 24 7 4)  -1)]
    (doseq [attr cookie-attrs]
      (.set cookie attr (attr attrs) age))))

(defn clear-login [cookie]
  (doseq [attr cookie-attrs]
    (.remove cookie attr)))
