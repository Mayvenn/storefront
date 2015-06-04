(ns storefront.messages
  (:require [cljs.core.async :refer [<! chan close! put!]]))

(defn enqueue-message [ch [event args]]
  ;; (js/console.trace "enqueue-message" (clj->js event) (clj->js args)) ;; uncomment to trace message enqueues
  (put! ch [event args]))
