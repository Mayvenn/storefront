(ns storefront.browser.cookie-jar
  (:require [storefront.config :as config])
  (:import [goog.net Cookies]))

(defn make-cookie []
  (Cookies. js/document))

(def user
  {:optional-keys [:store-slug]
   :required-keys [:email :user-token :id]})

(def order
  {:optional-keys []
   :required-keys [:token :number]})

(def pending-promo
  {:optional-keys []
   :required-keys [:pending-promo-code]})

(def all
  (let [specs [user order pending-promo]]
    {:optional-keys (apply concat (map :optional-keys specs))
     :required-keys (apply concat (map :required-keys specs))}))

(def remember-me-age (* 60 60 24 7 4))
(def session-age (* 60 60 24 7 52))
(def secure? (not config/development?))

(defn all-keys [spec]
  (concat (:optional-keys spec) (:required-keys spec)))

(defn clear-cookie [spec cookie]
  (doseq [key (all-keys spec)]
    (.remove cookie key)))

(defn has-required-attrs? [m req-attrs]
  (every? seq (vals (select-keys m req-attrs))))

(defn retrieve [spec cookie]
  (let [attr-keys (all-keys spec)
        req-attr-keys (:required-keys spec)
        attrs (zipmap attr-keys
                      (map #(.get cookie %) attr-keys))]
    (if (has-required-attrs? attrs req-attr-keys)
      attrs
      (clear-cookie spec cookie))))

(defn save-cookie
  ([spec cookie attrs] (save-cookie spec cookie attrs true))
  ([spec cookie attrs remember?]
   (let [age (if remember? remember-me-age -1)]
     (doseq [attr (all-keys spec)]
       (if-let [val (attr attrs)]
         (.set cookie attr val age "/" nil secure?)
         (.remove cookie attr))))))

(def clear-user (partial clear-cookie user))
(def clear-order (partial clear-cookie order))
(def clear-pending-promo-code (partial clear-cookie pending-promo))
(def clear (partial clear-cookie all))

(def retrieve-login (partial retrieve user))
(def retrieve-current-order (partial retrieve order))
(def retrieve-pending-promo-code (partial retrieve pending-promo))

(defn force-session-id [cookie]
  (if-let [session-id (.get cookie :session-id)]
    session-id
    (let [created-session-id (str (random-uuid))]
      (.set cookie :session-id created-session-id session-age "/" nil secure?)
      created-session-id)))

(def save-user (partial save-cookie user))
(def save-order (partial save-cookie order))
(defn save-pending-promo-code [cookie pending-promo-code]
  (save-cookie pending-promo-code cookie {:pending-promo-code pending-promo-code}))
