(ns storefront.hooks.talkable
  (:require [storefront.browser.tags :as tags]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [clojure.set :as set]
            [storefront.messages :as m]
            [storefront.config :as config]))

(defn insert [data]
  (when-not (.hasOwnProperty js/window "showPP")
    (tags/insert-tag-with-callback
     (tags/src-tag config/talkable-script "talkable-script")
     #(m/send data events/inserted-talkable))))

(defn- discounted-subtotal [order]
  (- (:line-items-total order) (:promotion-discount order)))

(defn completed-order [order]
  {:purchase {:order_number (:number order)
              :subtotal (discounted-subtotal order)
              :coupon_code (-> order :promotion-codes first)}
   :customer {:email (-> order :user :email)}})

(defn show-pending-offer [data]
  (when (get-in data keypaths/loaded-talkable)
    (when-let [order (get-in data keypaths/pending-talkable-order)]
      (js/showPP (clj->js order))
      (m/send data events/talkable-offer-shown))))

(defn show-referrals [user]
  #_(when-let [email (:email user)]
      ;; Needed to show dashboard of past referrals, instead of make-a-new-referral.  Maybe?
      (.push js/_talkableq (clj->js ["authenticate_customer" {:email email}])))
  (js/showSA (clj->js {:affiliate_member {:email (:email user)}})))
