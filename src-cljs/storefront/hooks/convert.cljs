(ns storefront.hooks.convert
  (:require [storefront.browser.tags :as tags]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.hooks.exception-handler :as exception-handler]
            [storefront.keypaths :as keypaths]
            [storefront.platform.messages :as m]
            [storefront.config :as config]))

(defn label->goal-id [label]
  (get config/convert-goals label))

(defn ^:private ensure-queue []
  (or (.hasOwnProperty js/window "_conv_q")
      (set! (.-_conv_q js/window) (clj->js []))))

(defmethod effects/perform-effects events/did-insert-convert [_ _ _ _ app-state]
  (when (not (get-in app-state keypaths/loaded-convert))
    (exception-handler/report :convert-not-loaded {})))

(defn insert-tracking []
  (ensure-queue)
  (tags/insert-tag-with-callback (tags/src-tag (str "https://cdn-3.convertexperiments.com/js/"
                                                    config/convert-project-id
                                                    ".js")
                                               "convert")
                                 #(m/handle-message events/inserted-convert))
  (m/handle-later events/did-insert-convert {} 30000))

(defn remove-tracking []
  (tags/remove-tags-by-class "convert"))

(defn ^:private enqueue [& args]
  (ensure-queue)
  (.push js/_conv_q (clj->js args)))

(defn track-conversion [label]
  (enqueue "triggerConversion" (label->goal-id label)))

(defn track-revenue [{:keys [order-number revenue products-count]}]
  (enqueue "sendRevenue" order-number revenue products-count (label->goal-id "revenue")))
