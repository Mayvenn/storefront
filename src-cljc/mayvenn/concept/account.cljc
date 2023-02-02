(ns mayvenn.concept.account
  (:require [storefront.keypaths :as keypaths]
            [storefront.transitions :as transitions]
            [storefront.effects :as effects]
            [storefront.trackings :as trackings]
            [storefront.events :as events]
            [clojure.set :as set]
            [storefront.platform.messages :refer [handle-message] :rename {handle-message publish}]
            [clojure.string :as string]
            #?@(:cljs [[storefront.hooks.stringer :as stringer]])))

(defn omni-determinates [data]
  (let [feature-flag              (get-in data (conj keypaths/features :experience-omni))
        previously-experienced    (contains? (get-in data keypaths/account-profile-experiences) :experience/omni)
        ip-address-in-texas       (seq (filter #(and (= "TX" (:region_code %))
                                                     (= "US" (:country_code %)))
                                               (get-in data keypaths/account-profile-ip-addresses)))
        shipping-address-in-texas (= "TX" (:state (get-in data keypaths/order-shipping-address)))
        landfall-debug-param      (some-> data (get-in keypaths/account-profile-landfalls) first :query-params (get "omni") (= "true"))
        landfall-on-walmart-page  (some-> data (get-in keypaths/account-profile-landfalls) first :path (string/starts-with? "/info/walmart"))]
    {:feature-flag              feature-flag
     :previously-experienced    previously-experienced
     :ip-address-in-texas       ip-address-in-texas
     :shipping-address-in-texas shipping-address-in-texas
     :landfall-debug-param      landfall-debug-param
     :landfall-on-walmart-page  landfall-on-walmart-page}))

(defn omni? [data]
  (let [determinates         (omni-determinates data)
        feature-flag         (:feature-flag determinates)
        ip-address-in-texas  (:ip-address-in-texas determinates)
        landfall-debug-param (:landfall-debug-param determinates)]
    (and feature-flag
         (or ip-address-in-texas
             landfall-debug-param))))

(def experiences
  {:experience/omni omni?})

(defn <- [data]
  {:experiences       (set (keep (fn [[exp-name exp-validator]]
                             (when (exp-validator data)
                               exp-name))
                           experiences))
   :omni-determinates (omni-determinates data)})

(defmethod effects/perform-effects events/account-profile|experience|evaluated
  [_ _ _ _ data]
  (let [prior-experiences   (get-in data keypaths/account-profile-experiences)
        account             (<- data)
        experiences-to-join (set/difference (:experiences account) prior-experiences)]
    (doseq [experience experiences-to-join]
      (publish events/account-profile|experience|joined {:experience        experience
                                                         :omni-determinates (:omni-determinates account)}))))

(defmethod transitions/transition-state events/account-profile|experience|joined
  [_ _ {:keys [experience]} data]
  (-> data
      (update-in keypaths/account-profile-experiences #(conj % experience))))

(defmethod trackings/perform-track events/account-profile|experience|joined
  [_ _ {:keys [experience omni-determinates]} _]
  #?(:cljs
     (when (= :experience/omni experience)
       (stringer/track-event "omni-determined" {:factors (dissoc omni-determinates :feature-flag)}))))
