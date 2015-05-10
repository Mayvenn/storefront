(ns storefront.cookie-jar
  (:import [goog.net Cookies]))

(defn make-cookie []
  (Cookies. js/document))

(def cookie-attrs [:email :token :store-slug])

(defn retrieve-login [cookie]
  (zipmap cookie-attrs
          (map #(.get cookie %) cookie-attrs)))

(defn set-login [cookie attrs]
  (doseq [attr cookie-attrs]
    (.set cookie attr (attr attrs))))

(defn clear-login [cookie]
  (doseq [attr cookie-attrs]
    (.remove cookie attr)))
