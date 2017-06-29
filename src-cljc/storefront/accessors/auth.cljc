(ns storefront.accessors.auth
  (:require [storefront.accessors.stylists :as stylists]
            [storefront.keypaths :as keypaths]))

(defn signed-in [data]
  (let [as-stylist? (stylists/own-store? data)
        as-user?    (get-in data keypaths/user-email)
        store-slug  (get-in data (conj keypaths/store :store_slug))]
    {::at-all (or as-stylist? as-user?)
     ::as     (cond
                as-stylist? :stylist
                as-user?    :user
                :else       :guest)
     ::to     (if (contains? #{"store" "shop"} store-slug)
                :dtc
                :marketplace)}))
