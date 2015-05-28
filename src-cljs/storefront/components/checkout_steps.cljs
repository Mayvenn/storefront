(ns storefront.components.checkout-steps
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.components.utils :refer [route-to]]
            [clojure.string :as string]))

(def checkout-steps ["address" "delivery" "payment" "confirm"])
(def checkout-step-events
  {"address" events/navigate-checkout-address
   "delivery" events/navigate-checkout-delivery
   "payment" events/navigate-checkout-payment
   "confirm" events/navigate-checkout-confirmation})

(defn display-progress-step [app-state current-index index state]
  [:li.progress-step
   {:class (->> [state
                 (if (< index current-index) "completed")
                 (if (= index (inc current-index)) "next")
                 (if (= index current-index) "current")
                 (if (zero? index) "first")
                 (if (= index (dec (count state))) "last")]
                (filter seq)
                (string/join " "))}
   [:span
    (let [text [:div.progress-step-index
                (str (inc index) " ")
                (string/capitalize state)]]
      (if (< index current-index)
        [:a (route-to app-state (checkout-step-events state)) text]
        text))]])

(defn checkout-progress [app-state current-step]
  (let [current-step-index (->> checkout-steps
                                (map-indexed vector)
                                (filter #(= (second %) current-step))
                                ffirst)]
    [:ol {:class (str "progress-steps checkout-step-" current-step)}
     (map-indexed (partial display-progress-step app-state current-step-index)
                  checkout-steps)]))

(defn checkout-step-bar [data]
  (let [checkout-current-step (get-in data keypaths/checkout-current-step-path)]
    [:div.row
     [:div.columns.thirteen.omega (checkout-progress data checkout-current-step)]]))
