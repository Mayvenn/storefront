(ns storefront.hooks.convert
  (:require [storefront.config :as config]))

(defn label->goal-id [label]
  (get config/convert-goals label))

(defn ^:private ensure-queue []
  (or (.hasOwnProperty js/window "_conv_q")
      (set! (.-_conv_q js/window) (clj->js []))))

(defn ^:private enqueue [& args]
  (ensure-queue)
  (.push js/_conv_q (clj->js args)))

(defn track-conversion [label]
  (enqueue "triggerConversion" (label->goal-id label)))

(defn track-revenue [{:keys [order-number revenue products-count]}]
  (enqueue "sendRevenue" order-number revenue products-count (label->goal-id "revenue")))
