(ns storefront.components.checkout-steps
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.keypaths :as keypaths]
            [storefront.components.utils :refer [route-to]]
            [storefront.events :as events]
            [storefront.hooks.experiments :as experiments]
            [clojure.string :as string]))

(def ^:private address-step
  {:event events/navigate-checkout-address
   :name "address"})

(def ^:private delivery-step
  {:event events/navigate-checkout-delivery
   :name "shipping"})

(def ^:private payment-step
  {:event events/navigate-checkout-payment
   :name "payment"})

(def ^:private confirm-step
  {:event events/navigate-checkout-confirmation
   :name "confirm"})

(defn ^:private steps [three-steps?]
  (if three-steps?
    [address-step               payment-step confirm-step]
    [address-step delivery-step payment-step confirm-step]))

(defn ^:private display-progress-step [steps current-index index {step-name :name}]
  [:li.progress-step
   {:class (->> [step-name
                 (str "col-" (/ 12 (count steps)))
                 (if (< index current-index) "completed")
                 (if (= index (inc current-index)) "next")
                 (if (= index current-index) "current")
                 (if (zero? index) "first")
                 (if (= index (dec (count steps))) "last")]
                (filter seq)
                (string/join " "))}
   [:span
    (let [text [:div.progress-step-index
                (str (inc index) " ")
                [:br]
                (string/capitalize step-name)]]
      (if (< index current-index)
        [:a (route-to (get-in steps [index :event])) text]
        text))]])

(defn checkout-step-bar [data]
  (let [steps (steps (experiments/three-steps? data))
        [current-index current-step] (->> steps
                                          (map-indexed vector)
                                          (filter #(= (:event (second %)) (get-in data keypaths/navigation-event)))
                                          first)]
    [:div.row
     [:div.columns.thirteen.omega
      [:ol {:class (str "progress-steps checkout-step-" (:name current-step))}
       (map-indexed (partial display-progress-step steps current-index)
                    steps)]]]))
