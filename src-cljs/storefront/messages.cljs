(ns storefront.messages
  (:require [storefront.keypaths :as keypaths]))

(defn send [app-state event & [args]]
  ((get-in app-state keypaths/handle-message) event args))

(defn send-later [app-state event & [args]]
  (.setTimeout js/window #(send app-state event args)))
