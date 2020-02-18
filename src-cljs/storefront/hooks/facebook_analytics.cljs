(ns storefront.hooks.facebook-analytics)

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

(defn subscribe []
  (track-event "Subscribe"))
