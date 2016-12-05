(ns storefront.hooks.woopra
  (:require [storefront.config :as config]
            [storefront.browser.tags :refer [insert-tag-with-callback
                                             src-tag
                                             remove-tag]]
            [storefront.platform.uri :as uri]
            [storefront.utils.maps :refer [filter-nil]]))

(defn- name-capitalization [s]
  (when s
    (if (< (count s) 2)
      (.toUpperCase s)
      (str (.toUpperCase (subs s 0 1))
           (subs s 1)))))

(defn- order->customer [order]
  {:first_name (name-capitalization
                (or (-> order :billing-address :first-name)
                    (-> order :shipping-address :first-name)))
   :last_name  (name-capitalization
                (or (-> order :billing-address :last-name)
                    (-> order :shipping-address :last-name)))})

(defn $ [value]
  (.toFixed (float value) 2))

(defn- user->event-data [user]
  {:ce_user_id    (:id user)
   :ce_user_email (:email user)})

(defn- order->visitor-data [order]
  (let [customer (order->customer order)]
    {:cv_id         (some-> order :user :id)

     :cv_email      (some-> customer :user :email)
     :cv_first_name (-> customer :first_name)
     :cv_last_name  (-> customer :last_name)

     :cv_bill_address_zipcode (some-> order :billing-address :zipcode)
     :cv_bill_address_city    (some-> order :billing-address :city)
     :cv_bill_address_state   (some-> order :billing-address :state)

     :cv_ship_address_zipcode (some-> order :shipping-address :zipcode)
     :cv_ship_address_city    (some-> order :shipping-address :city)
     :cv_ship_address_state   (some-> order :shipping-address :state)}))

(defn- woopra-request [endpoint {:keys [params]}]
  (let [uri (uri/set-query-string endpoint params)]
    (insert-tag-with-callback (src-tag uri "woopra")
                              #(remove-tag (.-target %)))))

(defn track-identify [{:keys [session-id user]}]
  (woopra-request
   "https://www.woopra.com/track/identify"
   {:params (filter-nil {:host     config/woopra-host
                         :cookie   session-id
                         :cv_id    (:id user)
                         :cv_email (:email user)})}))


(defn- track-event [event-name session-id user params]
  (woopra-request
   "https://www.woopra.com/track/ce"
   {:params (filter-nil (merge {:cookie                  session-id
                                :event                   event-name
                                :timestamp               (.getTime (js/Date.))
                                :host                    config/woopra-host}
                               (user->event-data user)
                               params))}))

(defn track-user-email-captured [session-id user email]
  (track-identify {:session-id session-id
                   :user (assoc user :email email)})
  (track-event "email_captured" session-id user {:ce_email email}))

(defn track-experiment [session-id user variation]
  (track-event "experiment_joined" session-id user {:ce_variation variation}))
