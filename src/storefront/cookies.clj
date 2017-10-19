(ns storefront.cookies
  (:refer-clojure :exclude [get set])
  (:require [storefront.config :as config]
            [ring.middleware.cookies :as cookies]))

(defn dumb-encoder
  "Our cookies have colons at the beginning of their names (e.g. ':token'). But
  the default cookie encoder is codec/form-encode, which escapes colons. It is
  only safe to use this encoder if you know the cookie names are URL safe"
  [map-entry]
  (let [[k v] (first map-entry)]
    (str k "=" v)))

(defn encode [resp]
  (cookies/cookies-response resp {:encoder dumb-encoder}))

(defn days [n]
  (* 60 60 24 n))

(defn get [req-or-resp name] (get-in req-or-resp [:cookies name :value]))
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
