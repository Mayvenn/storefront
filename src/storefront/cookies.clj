(ns storefront.cookies
  (:refer-clojure :exclude [get set])
  (:require [storefront.config :as config]
            [ring.middleware.cookies :as cookies]
            [ring.util.codec :as codec]))

(defn minutes [n]
  (* 60 n))

(defn days [n]
  (* 60 60 24 n))

(defn get [req-or-resp name] (some-> (get-in req-or-resp [:cookies name :value])
                                     codec/form-decode))
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
   (assoc-in req-or-resp [:cookies name] (merge {:value   (codec/form-encode (str value))
                                                 :max-age (days 28)
                                                 :secure  (not (config/development? environment))
                                                 :path    "/"}
                                                overrides))))
