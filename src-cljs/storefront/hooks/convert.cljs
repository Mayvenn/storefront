(ns storefront.hooks.convert
  (:require [storefront.browser.tags :as tags]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.platform.messages :as m]
            [storefront.config :as config]))

(def goals
  {"view-categories"     {"production" "100016254"
                          "sandbox"    "100016257"}
   "view-category"       {"production" "100016255"
                          "sandbox"    "100016256"}
   "place-order"         {"production" "100016055"
                          "sandbox"    "100016047"}
   "revenue"             {"production" "100016054"
                          "sandbox"    "100016046"}
   "apple-pay-checkout"  {"production" "TODO"
                          "sandbox"    "100017131"}
   "checkout"            {"production" "TODO"
                          "sandbox"    "100017132"}
   "paypal-checkout"     {"production" "TODO"
                          "sandbox"    "100017133"}
   "apple-pay-available" {"production" "TODO"
                          "sandbox"    "100017134"}})

(defn label->goal-id [label]
  (let [goal-ids (get goals label)]
    (or
     (get goal-ids js/environment)
     (get goal-ids "sandbox"))))

(defn insert-tracking []
  (or (.hasOwnProperty js/window "_conv_q") (set! (.-_conv_q js/window) (clj->js [])))
  (tags/insert-tag-with-callback (tags/src-tag (str "//cdn-3.convertexperiments.com/js/"
                                                    config/convert-project-id
                                                    ".js" )
                                               "convert")
                                 #(m/handle-message events/inserted-convert))
  (js/setTimeout #(m/handle-message events/inserted-convert) 15000))

(defn remove-tracking []
  (tags/remove-tags-by-class "convert"))

(defn ^:private track [& args]
  (when (.hasOwnProperty js/window "_conv_q")
    (.push js/_conv_q (clj->js args))))

(defn track-conversion [label]
  (track "triggerConversion" (label->goal-id label)))

(defn track-revenue [{:keys [order-number revenue products-count]}]
  (track "sendRevenue" order-number revenue products-count (label->goal-id "revenue")))
