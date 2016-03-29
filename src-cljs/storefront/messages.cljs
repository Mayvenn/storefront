(ns storefront.messages
  (:require [storefront.keypaths :as keypaths]))

(def handle-message) ;; Dependency Injection, populated by storefront.core/main

(defn handle-later [event & [args]]
  (.setTimeout js/window #(handle-message event args)))
