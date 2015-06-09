(ns storefront.messages
  (:require [cljs.core.async :refer [put!]]))

(defn enqueue-message [ch [event args]]
  ;; (js/console.trace (clj->js event) "\n" (clj->js args)) ;; uncomment to trace message enqueues
  (put! ch [event args]))
