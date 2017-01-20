(ns storefront.hooks.convert
  (:require [storefront.browser.tags :as tags]
            [storefront.accessors.experiments :as experiments]
            [storefront.keypaths :as keypaths]
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
  (tags/insert-tag-with-callback (tags/src-tag (str "//cdn-3.convertexperiments.com/js/"
                                                    config/convert-project-id
                                                    ".js" )
                                               "convert")
                                 #(m/handle-message events/inserted-convert))
  (js/setTimeout #(m/handle-message events/inserted-convert) 15000))

(defn remove-tracking []
  (tags/remove-tags-by-class "convert"))

(defn ^:private enqueue [& args]
  (ensure-queue)
  (let [q-args (clj->js args)]
    (js/console.log q-args)
    (.push js/_conv_q q-args)))

(defn track-conversion [label]
  (enqueue "triggerConversion" (label->goal-id label)))

(defn track-revenue [{:keys [order-number revenue products-count]}]
  (enqueue "sendRevenue" order-number revenue products-count (label->goal-id "revenue")))

(defn experiment-name->id [experiment-name]
  (get-in config/manual-experiments [experiment-name :convert-id]))

(defn join-variation [experiment-name variation]
  (js/console.log (str experiment-name " " (:feature variation)))
  (enqueue "assignVariation" (experiment-name->id experiment-name) (:convert-id variation)))
