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

(def utms
  {:optional-keys [:utm-source :utm-medium :utm-campaign :utm-content :utm-term]
   :required-keys []})

(def all
  (let [specs [user order pending-promo]]
    {:optional-keys (apply concat (map :optional-keys specs))
     :required-keys (apply concat (map :required-keys specs))}))

(def remember-me-age (* 60 60 24 7 4))
(def session-age (* 60 60 24 7 52))

(defn all-keys [spec]
  (concat (:optional-keys spec) (:required-keys spec)))

(defn clear-cookie [spec cookie]
  (doseq [key (all-keys spec)]
    (.remove cookie key "/")))

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
         (.set cookie attr val age "/" nil config/secure?)
         (.remove cookie attr))))))

(def clear-order (partial clear-cookie order))
(def clear-pending-promo-code (partial clear-cookie pending-promo))
(def clear (partial clear-cookie all))

(def retrieve-login (partial retrieve user))
(def retrieve-current-order (partial retrieve order))
(def retrieve-pending-promo-code (partial retrieve pending-promo))
(def retrieve-utm-params (partial retrieve utms))

(def ^:private session-id-length 24)

(defn- must-generate-session-id? [existing-session-id]
  (or (nil? existing-session-id)
      (> (count existing-session-id) session-id-length)))

(defn- random-id []
  (let [s (.toString (js/Math.random) 36)]
    (subs s 2 (+ 2 session-id-length))))

(defn force-session-id [cookie]
  (let [session-id (.get cookie :session-id)]
    (if (must-generate-session-id? session-id)
      (let [created-session-id (random-id)]
        (.set cookie :session-id created-session-id session-age "/" nil config/secure?)
        created-session-id)
      session-id)))

(def save-user (partial save-cookie user))
(def save-order (partial save-cookie order))
(defn save-pending-promo-code [cookie promo-code]
  (save-cookie pending-promo cookie {:pending-promo-code promo-code}))
(defn save-utm-params [cookie utm-params]
  (save-cookie utms cookie utm-params))
