(ns storefront.cookies
  (:refer-clojure :exclude [get set])
  (:require [storefront.config :as config]
            [ring.middleware.cookies :as cookies]
            [ring.util.codec :as codec]
            [clojure.string :as string]))

(defn minutes [n]
  (* 60 n))

(defn days [n]
  (* 60 60 24 n))

;; ring wrap-cookies will encode/decode values for us
(defn get [req-or-resp name]
  (some-> (get-in req-or-resp [:cookies name :value])
          ;; see function below: get-and-attempt-parsing-poorly-encoded
          codec/form-decode-str))

(defn get-and-attempt-parsing-poorly-encoded
  "There exists tokens that are doubly encoded that storefront server must be able to parse for at least one month
  (since those cookies can still be around for 1 month)

  TODO: AFTER July 31st, 2018:
   - delete this function and replace it with cookies/get
   - update cookies/get to not form decode (since wrap-cookies does that once already)"
  [req-or-resp key]
  (some-> (get req-or-resp key)
          (string/replace #" " "+")))

(defn expire
  ([req-or-resp environment name] (expire req-or-resp environment name {}))
  ([req-or-resp environment name overrides]
   (assoc-in req-or-resp [:cookies name] (merge {:value   ""
                                                 :max-age 0
                                                 :secure  (not (config/development? environment))
                                                 :path    "/"}
                                                overrides))))

(defn set
  ([req-or-resp environment name value] (set req-or-resp environment name value {}))
  ([req-or-resp environment name value overrides]
   (assoc-in req-or-resp [:cookies name] (merge {:value   (str value)
                                                 :max-age (days 28)
                                                 :secure  (not (config/development? environment))
                                                 :path    "/"}
                                                overrides))))
