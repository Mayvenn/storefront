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

(defn get [req name] (get-in req [:cookies name :value]))
(defn expire
  ([resp environment name] (expire resp environment name {}))
  ([resp environment name overrides]
   (assoc-in resp [:cookies name] (merge {:value   ""
                                          :max-age 0
                                          :secure  (not (config/development? environment))
                                          :path    "/"}
                                         overrides))))

(defn set
  ([resp environment name value] (set resp environment name value {}))
  ([resp environment name value overrides]
   (assoc-in resp [:cookies name] (merge {:value   value
                                          :max-age (days 28)
                                          :secure  (not (config/development? environment))
                                          :path    "/"}
                                         overrides))))
