(ns storefront.browser.cookie-jar
  (:require [storefront.config :as config]
            [clojure.string :as string]
            [spice.core :as spice]
            [clojure.set :as set])
  (:import [goog.net Cookies]))

(defn ^:private root-domain []
  (-> js/window
      .-location
      .-hostname
      (string/split #"\.")
      (->> (take-last 2)
           (string/join ".")
           (str "."))))

(def thirty-minutes (* 60 30))
(def one-day (* 60 60 24))
(def three-days (* 3 one-day))
(def week (* 7 one-day))
(def four-weeks (* 4 week))
(def one-year (* 52 week))

(defn make-cookie []
  (Cookies. js/document))

(def adventure
  {:domain        nil
   :max-age       four-weeks
   :required-keys [:choices]})

(def user
  {:domain        nil
   :max-age       four-weeks
   :optional-keys [:store-slug :store-id :stylist-experience]
   :required-keys [:email :user-token :id]})

(def order
  {:domain        nil
   :max-age       four-weeks
   :optional-keys []
   :required-keys [:token :number]})

(def completed-order
  {:domain        nil
   :max-age       three-days
   :optional-keys []
   :required-keys [:completed-order-token :completed-order-number]})

(def pending-promo
  {:domain        nil
   :max-age       four-weeks
   :optional-keys []
   :required-keys [:pending-promo-code]})

(def utm-params
  {:domain        (root-domain)
   :max-age       four-weeks
   :optional-keys [:storefront/utm-source :storefront/utm-medium :storefront/utm-campaign :storefront/utm-content :storefront/utm-term]
   :required-keys []})

(def affiliate-stylist-id
  {:domain        (root-domain)
   :max-age       four-weeks
   :optional-keys []
   :required-keys [:affiliate-stylist-id]})

(def account-specs [user order pending-promo])

(defn all-keys [spec]
  (concat (:optional-keys spec) (:required-keys spec)))

(defn clear-cookie [spec cookie]
  (doseq [key (all-keys spec)]
    (.remove cookie key "/" (:domain spec))
    (.remove cookie (spice/kw-name key) "/" (:domain spec))))

(defn has-required-attrs? [m req-attrs]
  (every? seq (vals (select-keys m req-attrs))))

(defn retrieve [spec cookie]
  (let [attr-keys (all-keys spec)
        m (zipmap attr-keys
                  (map #(or
                         (.get cookie %)
                         (.get cookie (spice/kw-name %)))
                       attr-keys))]
    (if (has-required-attrs? m (:required-keys spec))
      m
      (clear-cookie spec cookie))))

(defn save-cookie [{:keys [max-age domain] :as spec} cookie attrs]
  (doseq [attr (all-keys spec)]
    (if-let [val (get attrs attr)]
      (.set cookie (spice/kw-name attr) (js/encodeURIComponent val) max-age "/" domain (get spec :secure config/secure?))
      (do (.remove cookie attr                 "/" domain)
          (.remove cookie (spice/kw-name attr) "/" domain)))))

(def clear-order (partial clear-cookie order))
(def clear-pending-promo-code (partial clear-cookie pending-promo))
(def clear-utm-params (partial clear-cookie utm-params))
(defn clear-account [cookie]
  (doseq [spec account-specs]
    (clear-cookie spec cookie)))
(def retrieve-login (partial retrieve user))
(def retrieve-current-order (partial retrieve order))
(def retrieve-pending-promo-code (partial retrieve pending-promo))
(def retrieve-utm-params (partial retrieve utm-params))
(def retrieve-adventure (partial retrieve adventure))
(defn retrieve-completed-order [cookie]
  (-> (retrieve completed-order cookie)
      (set/rename-keys
       {:completed-order-number :number
        :completed-order-token  :token})
      (update :token js/decodeURIComponent)))

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
        (.set cookie :session-id created-session-id one-year "/" nil config/secure?)
        created-session-id)
      session-id)))

(def save-user (partial save-cookie user))
(def save-order (partial save-cookie order))
(defn save-completed-order [cookie order]
  (save-cookie completed-order
               cookie
               (set/rename-keys order {:number :completed-order-number
                                       :token  :completed-order-token})))

(defn save-phone-capture-session [cookie]
  (.set cookie "phone-popup-session" "opted-in" (* 200 one-year) "/" nil config/secure?))

(defn get-phone-capture-session [cookie]
  (.get cookie "phone-popup-session"))

(defn save-dismissed-free-install [cookie value]
  (.set cookie "dismissed-free-install" value four-weeks "/" nil config/secure?))

(defn get-dismissed-free-install [cookie]
  (.get cookie "dismissed-free-install"))

(defn save-pending-promo-code [cookie promo-code]
  (save-cookie pending-promo cookie {:pending-promo-code promo-code}))

(def save-utm-params (partial save-cookie utm-params))

(def clear-adventure (partial clear-cookie adventure))

(def save-affiliate-stylist-id (partial save-cookie affiliate-stylist-id))
(def retrieve-affiliate-stylist-id (partial retrieve affiliate-stylist-id))

(defn save-adventure
  [cookie adventure-attrs]
  (let [json-serialize (comp js/JSON.stringify #(clj->js %))]
    (save-cookie adventure
                 cookie
                 (update adventure-attrs
                         :choices json-serialize))))
