(ns mayvenn.concept.account
  (:require [storefront.keypaths :as keypaths]
            [storefront.transitions :as transitions]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [clojure.set :as set]
            [storefront.platform.messages :refer [handle-message] :rename {handle-message publish}]
            [clojure.string :as string]))

(def experiences
  {:experience/omni (fn omni? [data]
                      (let [feature-flag              (get-in data (conj keypaths/features :experience-omni))
                            previously-experienced    (contains? (get-in data keypaths/account-profile-experiences) :experience/omni)
                            ip-address-in-texas       (seq (filter #(and (= "TX" (:region_code %))
                                                                         (= "US" (:country_code %)))
                                                                   (get-in data keypaths/account-profile-ip-addresses)))
                            shipping-address-in-texas (= "TX" (:state (get-in data keypaths/order-shipping-address)))
                            landfall-in-texas         (some #(or (some-> % :query-params (get "omni") (= "true"))
                                                                 #_(some-> % :path (string/starts-with? "/info/walmart")))
                                                            (get-in data keypaths/account-profile-landfalls))]
                        (and feature-flag
                             (or #_previously-experienced
                                 ip-address-in-texas
                                 #_shipping-address-in-texas
                                 landfall-in-texas))))})

(defn <- [data]
  {:experiences (set (keep (fn [[exp-name exp-validator]]
                             (when (exp-validator data)
                               exp-name))
                           experiences))})

(defmethod effects/perform-effects events/account-profile|experience|evaluated
  [_ _ _ _ data]
  (let [prior-experiences   (get-in data keypaths/account-profile-experiences)
        account             (<- data)
        experiences-to-join (set/difference (:experiences account) prior-experiences)]
    (doseq [experience experiences-to-join]
      (publish events/account-profile|experience|joined {:experience experience}))))

(defmethod transitions/transition-state events/account-profile|experience|joined
  [_ _ {:keys [experience]} data]
  (-> data
      (update-in keypaths/account-profile-experiences #(conj % experience))))
