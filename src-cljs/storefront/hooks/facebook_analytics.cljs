(ns storefront.hooks.facebook-analytics
  (:require [spice.maps :as maps]
            [storefront.config :as config]
            [storefront.platform.numbers :as numbers]))

(defn ^:private format-phone-number [phone]
  (let [digits-only-phone (->> phone str numbers/digits-only)]
    (if (= 10 (count digits-only-phone))
      (str "1" digits-only-phone)
      digits-only-phone)))

(defn init-with-customer-data [email address]
  (when (.hasOwnProperty js/window "fbq")
    (js/fbq "init" config/facebook-pixel-id
            (clj->js
             (maps/deep-remove-nils
              {:em email
               :fn (some-> address :first-name string/lower-case)
               :ln (some-> address :last-name string/lower-case)
               :ph (some-> address :phone format-phone-number)
               :ct (some-> address :city string/lower-case (string/replace " " ""))
               :st (some-> address :state string/lower-case)
               :zp (some-> address :zipcode numbers/digits-only)
               :cn "us"})))))

(defn track-event
  ([action]
   (when (.hasOwnProperty js/window "fbq")
     (js/fbq "track" action)))
  ([action args]
   (when (.hasOwnProperty js/window "fbq")
     (js/fbq "track" action (clj->js args)))))

(defn track-custom-event
  ([action]
   (when (.hasOwnProperty js/window "fbq")
     (js/fbq "trackCustom" action)))
  ([action args]
   (when (.hasOwnProperty js/window "fbq")
     (js/fbq "trackCustom" action (clj->js args)))))

(defn track-page [path]
  (track-event "PageView"))

(defn subscribe []
  (track-event "Subscribe"))
