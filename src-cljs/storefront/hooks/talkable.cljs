(ns storefront.hooks.talkable
  (:require [storefront.browser.tags :as tags]
            [storefront.events :as events]
            [clojure.set :as set]
            [storefront.messages :as m]
            [storefront.config :as config]))

(defn insert []
  (tags/insert-tag-with-src config/talkable-script "talkable-script"))

(defn discounted-subtotal [order]
  (- (:line-items-total order) (:promotion-discount order)))

(defn order-completed [order]
  (js/showPP (clj->js {:purchase {:order_number (:number order)
                                  :subtotal (discounted-subtotal order)
                                  :coupon_code (-> order :promotion-codes first)}
                       :customer {:email (-> order :user :email)}})))
