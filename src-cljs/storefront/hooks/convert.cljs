(ns storefront.hooks.convert
  (:require [storefront.browser.tags :as tags]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.platform.messages :as m]
            [storefront.config :as config]))

(def goals
  {"place-order" {"production" "100016055"
                  "sandbox"    "100016047"}
   "revenue"     {"production" "100016054"
                  "sandbox"    "100016046"}})

(defn label->goal-id [label]
  (let [goal-ids (get goals label)]
    (or
     (get goal-ids js/environment)
     (get goal-ids "sandbox"))))

(defn insert-tracking []
  (tags/insert-tag-with-callback (tags/src-tag (str "//cdn-3.convertexperiments.com/js/"
                                                    config/convert-project-id
                                                    ".js" )
                                               "convert")
                                 #(m/handle-message events/inserted-convert))
  (js/setTimeout #(m/handle-message events/inserted-convert) 15000))

(defn remove-tracking []
  (tags/remove-tags-by-class "convert"))

(defn ^:private track [& args]
  (when (.hasOwnProperty js/window "convert")
    (.push js/_conv_q (clj->js args))))

(defn track-conversion [label]
  (track "triggerConversion" (label->goal-id label)))

(defn track-revenue [{:keys [order-number revenue products-count]}]
  (track "sendRevenue" order-number revenue products-count (label->goal-id "revenue")))
