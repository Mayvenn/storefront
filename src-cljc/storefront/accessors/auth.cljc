(ns storefront.accessors.auth
  (:require [storefront.keypaths :as keypaths]
            [storefront.accessors.stylists :as stylists]))

(defn signed-in [data]
  (let [as-stylist? (get-in data keypaths/user-store-id)
        as-user?    (get-in data keypaths/user-email)
        store-slug  (get-in data keypaths/store-slug)]
    {::at-all (or as-stylist? as-user?)
     ::as     (cond
                as-stylist? :stylist
                as-user?    :user
                :else       :guest)
     ::to     (cond
                (contains? #{"store" "shop"} store-slug) :dtc
                (stylists/own-store? data)               :own-store
                :else                                    :marketplace)}))

(defn stylist-on-own-store? [signed-in]
  (-> signed-in
      (select-keys [::as ::to])
      (= {::as :stylist
          ::to :own-store})))

(defn stylist? [signed-in]
  (-> signed-in ::as (= :stylist)))

(defn permitted-category? [data category]
  ((:auth/requires category #{:guest :user :stylist})
   (::as (signed-in data))))

(defn permitted-product? [data product]
  (if (-> product :catalog/department set (contains? "stylist-exclusives"))
    (stylist? (signed-in data))
    true))

(defn signed-in-or-initiated-guest-checkout? [data]
  (or (get-in data keypaths/user-id)
      (get-in data keypaths/order-user-email)))
