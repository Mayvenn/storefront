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

     :history nil
     :cookie cookie
     :routes []

     :user (cookie-jar/retrieve-login cookie)
     :order nil

     :store {:store_slug (get-store-subdomain)}
     :taxons []
     :products {}

     :ui {:navigation-event events/navigate-home
          :browse-taxon-query nil
          :browse-product-query nil
          :browse-variant-query nil
          :browse-variant-quantity 1
          :menu-expanded false
          :account-menu-expanded false
          :sign-in {:email ""
                    :password ""
                    :remember-me true}
          :sign-up {:email ""
                    :password ""
                    :password-confirmation ""}
          :forgot-password {:email ""}
          :reset-password {:password ""
                           :password-confirmation ""
                           :token ""}
          :flash {:success {:message nil
                            :navigation []}}}}))

(def event-ch-path [:event-ch])

(def history-path [:history])
(def cookie-path [:cookie])
(def routes-path [:routes])

(def user-path [:user])
(def user-email-path (conj user-path :email))
(def user-token-path (conj user-path :token))
(def user-store-slug-path (conj user-path :store-slug))
(def user-order-token-path (conj user-path :order-token))
(def user-order-id-path (conj user-path :order-id))

(def order-path [:order])

(def store-path [:store])
(def store-slug-path (conj store-path :store_slug))

(def taxons-path [:taxons])
(def products-path [:products])

(def ui-path [:ui])
(def navigation-event-path (conj ui-path :navigation-event))
(def browse-taxon-query-path (conj ui-path :browse-taxon-query))
(def browse-product-query-path (conj ui-path :browse-product-query))
(def browse-variant-query-path (conj ui-path :browse-variant-query))
(def browse-variant-quantity-path (conj ui-path :browse-variant-quantity))
(def menu-expanded-path (conj ui-path :menu-expanded))
(def account-menu-expanded-path (conj ui-path :account-menu-expanded))

(def sign-in-path (conj ui-path :sign-in))
(def sign-in-email-path (conj sign-in-path :email))
(def sign-in-password-path (conj sign-in-path :password))
(def sign-in-remember-path (conj sign-in-path :remember-me))

(def sign-up-path (conj ui-path :sign-up))
(def sign-up-email-path (conj sign-up-path :email))
(def sign-up-password-path (conj sign-up-path :password))
(def sign-up-password-confirmation-path (conj sign-up-path :password-confirmation))

(def forgot-password-path (conj ui-path :forgot-password))
(def forgot-password-email-path (conj forgot-password-path :email))

(def reset-password-path (conj ui-path :reset-password))
(def reset-password-password-path (conj reset-password-path :password))
(def reset-password-password-confirmation-path (conj reset-password-path :password-confirmation))
(def reset-password-token-path (conj reset-password-path :token))

(def flash-path (conj ui-path :flash))
(def flash-success-path (conj flash-path :success))
(def flash-success-message-path (conj flash-success-path :message))
(def flash-success-nav-path (conj flash-success-path :navigation))
