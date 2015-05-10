(ns storefront.cookie-jar
  (:import [goog.net Cookies]))

(defn make-cookie []
  (Cookies. js/document))

(defn retrieve-login [cookie]
  {:email (.get cookie :email)
   :token (.get cookie :token)
   :store-slug (.get cookie :store-slug)})

(defn set-login [cookie {:keys [email token store-slug]}]
  (doto cookie
    (.set :email email)
    (.set :token token)
    (.set :store-slug store-slug)))

(defn clear-login [cookie]
  (doto cookie
    (.remove :email)
    (.remove :token)
    (.remove :store-slug)))
