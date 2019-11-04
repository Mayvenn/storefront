(ns storefront.hooks.convert
  (:require [storefront.browser.tags :as tags]
            [storefront.events :as events]
            [storefront.platform.messages :as m]
            [storefront.config :as config]))

(defn label->goal-id [label]
  (get config/convert-goals label))

(defn ^:private ensure-queue []
  (or (.hasOwnProperty js/window "_conv_q")
      (set! (.-_conv_q js/window) (clj->js []))))

(defn insert-tracking []
  (ensure-queue)
  (tags/insert-tag-with-callback (tags/src-tag (str "https://cdn-3.convertexperiments.com/js/"
                                                    config/convert-project-id
                                                    ".js")
                                               "convert")
                                 #(m/handle-message events/inserted-convert))
  (js/setTimeout #(m/handle-message events/inserted-convert) 15000))

(defn remove-tracking []
  (tags/remove-tags-by-class "convert"))

(defn ^:private enqueue [& args]
  (ensure-queue)
  (.push js/_conv_q (clj->js args)))

(defn track-conversion [label]
  (enqueue "triggerConversion" (label->goal-id label)))

(defn track-revenue [{:keys [order-number revenue products-count]}]
  (enqueue "sendRevenue" order-number revenue products-count (label->goal-id "revenue")))
