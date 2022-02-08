(ns storefront.accessors.auth
  (:require [storefront.keypaths :as keypaths]
            [storefront.events :as e]
            [storefront.accessors.stylists :as stylists]))
 ;; TODO(ellie+andres, 2022-02-08): Replace this with a less hacky solution
 ;; Ideally one where it's not just a list of events

(def ^:private hard-session-pages
  #{e/navigate-account-manage})

(defn requires-hard-session? [navigation-event]
  (contains? hard-session-pages navigation-event))

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
