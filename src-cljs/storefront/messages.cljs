(ns storefront.messages
  (:require [cljs.core.async :refer [put!]]
            [storefront.keypaths :as keypaths]))

(defn enqueue-message [ch [event args]]
  ;; (js/console.trace (clj->js event) "\n" (clj->js args)) ;; uncomment to trace message enqueues
  (put! ch [event args]))

(defn send [app-state event & [args]]
  ((get-in app-state keypaths/send-message) event args))
