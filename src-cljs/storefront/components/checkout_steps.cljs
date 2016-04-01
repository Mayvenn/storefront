(ns storefront.components.checkout-steps
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.keypaths :as keypaths]
            [storefront.components.utils :refer [route-to]]
            [storefront.events :as events]
            [storefront.hooks.experiments :as experiments]
            [clojure.string :as string]))

(defn ^:private steps [three-steps?]
  (if three-steps?
    [{:event events/navigate-checkout-address :name "your details"}
     {:event events/navigate-checkout-payment :name "payment"}
     {:event events/navigate-checkout-confirmation :name "review"}]
    [{:event events/navigate-checkout-address :name "address"}
     {:event events/navigate-checkout-delivery :name "shipping"}
     {:event events/navigate-checkout-payment :name "payment"}
     {:event events/navigate-checkout-confirmation :name "confirm"}]))

(defn ^:private display-progress-step [steps current-index index {step-name :name event :event}]
  [:li.progress-step
   {:class (->> [step-name
                 (str "col-" (/ 12 (count steps)))
                 (when (< index current-index) "completed")
                 (when (= index (inc current-index)) "next")
                 (when (= index current-index) "current")
                 (when (zero? index) "first")
                 (when (= index (dec (count steps))) "last")]
                (filter identity)
                (string/join " "))}
   [:span
    (let [text [:div.progress-step-index
                (str (inc index) " ")
                [:br]
                (string/capitalize step-name)]]
      (if (< index current-index)
        [:a (route-to event) text]
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
