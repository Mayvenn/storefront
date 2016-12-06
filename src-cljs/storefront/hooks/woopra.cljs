(ns storefront.hooks.woopra
  (:require [storefront.config :as config]
            [storefront.browser.tags :refer [insert-tag-with-callback
                                             src-tag
                                             remove-tag]]
            [storefront.platform.uri :as uri]
            [storefront.utils.maps :refer [filter-nil]]))

(defn- user->event-data [user]
  {:ce_user_id    (:id user)
   :ce_user_email (:email user)})

(defn- woopra-request [endpoint {:keys [params]}]
  (let [uri (uri/set-query-string endpoint params)]
    (insert-tag-with-callback (src-tag uri "woopra")
                              #(remove-tag (.-target %)))))

(defn- track-event [event-name session-id user params]
  (woopra-request
   "https://www.woopra.com/track/ce"
   {:params (filter-nil (merge {:cookie                  session-id
                                :event                   event-name
                                :timestamp               (.getTime (js/Date.))
                                :host                    config/woopra-host}
                               (user->event-data user)
                               params))}))
