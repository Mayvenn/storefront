(ns storefront.browser.cookie-jar
  (:require [storefront.config :as config]
            [clojure.string :as string])
  (:import [goog.net Cookies]))

(defn ^:private root-domain []
  (-> js/window
      .-location
      .-hostname
      (string/split #"\.")
      (->> (take-last 2)
           (string/join ".")
           (str "."))))

(defn make-cookie []
  (Cookies. js/document))

(def user
  {:domain        nil
   :optional-keys [:store-slug]
   :required-keys [:email :user-token :id]})

(def order
  {:domain        nil
   :optional-keys []
   :required-keys [:token :number]})

(def pending-promo
  {:domain        nil
   :optional-keys []
   :required-keys [:pending-promo-code]})

(def utms
  {:domain        (root-domain)
   :optional-keys [:storefront/utm-source :storefront/utm-medium :storefront/utm-campaign :storefront/utm-content :storefront/utm-term]
   :required-keys []})

(def account-cookies
  (let [specs [user order pending-promo]]
    {:domain nil
     :optional-keys (apply concat (map :optional-keys specs))
     :required-keys (apply concat (map :required-keys specs))}))

(def remember-me-age (* 60 60 24 7 4))
(def session-age (* 60 60 24 7 52))

(defn all-keys [spec]
  (concat (:optional-keys spec) (:required-keys spec)))

(defn clear-cookie [spec cookie]
  (doseq [key (all-keys spec)]
    (.remove cookie key "/" (:domain spec))))

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

(def ^:private default-cookie-opts
  {:max-age remember-me-age
   :path    "/"
   :secure? config/secure?})

(defn save-cookie
  ([spec cookie attrs]
   (save-cookie spec cookie attrs default-cookie-opts))
  ([spec cookie attrs {:keys [max-age path secure?]}]
   (doseq [attr (all-keys spec)]
     (if-let [val (attr attrs)]
       (.set cookie attr val max-age path (:domain spec) secure?)
       (.remove cookie attr)))))

(def clear-order (partial clear-cookie order))
(def clear-pending-promo-code (partial clear-cookie pending-promo))
(def clear-utm-params (partial clear-cookie utms))
(def clear-account (partial clear-cookie account-cookies))

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
  (clear-utm-params cookie)
  (save-cookie utms cookie utm-params))
