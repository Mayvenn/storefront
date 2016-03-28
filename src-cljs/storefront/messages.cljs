(ns storefront.messages
  (:require [storefront.keypaths :as keypaths]))

(def handle-message) ;; Dependency Injection, populated by storefront.core/main

(defn send [_ event & [args]]
  (handle-message event args))

(defn send-later [app-state event & [args]]
  (.setTimeout js/window #(send app-state event args)))
