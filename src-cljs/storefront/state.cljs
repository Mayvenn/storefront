(ns storefront.state
  (:require [cljs.core.async :refer [chan]]
            [storefront.events :as events]
            [clojure.string :as string]
            [storefront.cookie-jar :as cookie-jar]))

(defn get-store-subdomain []
  (first (string/split (.-hostname js/location) #"\.")))

(defn initial-state []
  (let [cookie (cookie-jar/make-cookie)]
    {:event-ch (chan)
     :stop-ch (chan)

     :history nil
     :cookie cookie
     :routes []

     :user (cookie-jar/retrieve-login cookie)

     :store {:store_slug (get-store-subdomain)}
     :taxons []
     :products-for-taxons {}

     :ui {:navigation-event events/navigate-home
          :browse-taxon nil
          :menu-expanded false
          :sign-in {:email ""
                    :password ""}}}))

(def event-ch-path [:event-ch])
(def stop-ch-path [:stop-ch])

(def history-path [:history])
(def cookie-path [:cookie])
(def routes-path [:routes])

(def user-path [:user])
(def user-email-path (conj user-path :email))
(def user-token-path (conj user-path :token))
(def user-store-slug-path (conj user-path :store-slug))

(def store-path [:store])
(def store-slug-path (conj store-path :store_slug))

(def taxons-path [:taxons])
(def products-for-taxons-path [:products-for-taxons])

(def ui-path [:ui])
(def navigation-event-path (conj ui-path :navigation-event))
(def browse-taxon-path (conj ui-path :browse-taxon))
(def menu-expanded-path (conj ui-path :menu-expanded))
(def sign-in-path (conj ui-path :sign-in))
(def sign-in-email-path (conj sign-in-path :email))
(def sign-in-password-path (conj sign-in-path :password))
