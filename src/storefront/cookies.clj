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

(defn get [req name] (get-in req [:cookies name :value]))
(defn expire
  ([resp environment name]
   (assoc-in resp [:cookies name] {:value   ""
                                   :max-age 0
                                   :secure  (not (config/development? environment))
                                   :path    "/"}))
  ([resp environment domain name]
   (-> resp
       (expire environment name)
       (assoc-in [:cookies name :domain] domain)
       (assoc-in [:cookies name :http-only] true))))

(defn set
  ([resp environment name value]
   (assoc-in resp [:cookies name] {:value   value
                                   :max-age (* 60 60 24 7 4)
                                   :secure  (not (config/development? environment))
                                   :path    "/"}))
  ([resp environment domain name value]
   (-> resp
       (set environment name value)
       (assoc-in [:cookies name :domain] domain)
       (assoc-in [:cookies name :http-only] true))))

