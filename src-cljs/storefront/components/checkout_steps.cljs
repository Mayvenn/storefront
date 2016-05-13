(ns storefront.components.checkout-steps
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.keypaths :as keypaths]
            [storefront.components.utils :refer [route-to]]
            [storefront.events :as events]
            [storefront.hooks.experiments :as experiments]
            [clojure.string :as string]))

(def ^:private three-steps
  [{:event events/navigate-checkout-address :name "your details" :id "address"}
   {:event events/navigate-checkout-payment :name "payment" :id "payment"}
   {:event events/navigate-checkout-confirmation :name "review" :id "confirm"}])

(def ^:private four-steps
  [{:event events/navigate-checkout-address :name "address" :id "address"}
   {:event events/navigate-checkout-delivery :name "shipping" :id "shipping"}
   {:event events/navigate-checkout-payment :name "payment" :id "payment"}
   {:event events/navigate-checkout-confirmation :name "confirm" :id "confirm"}])

(defn ^:private display-progress-step [steps-count current-index index {step-name :name step-id :id event :event}]
  [:li.progress-step
   {:class (->> [step-id
                 (str "col-" (/ 12 steps-count))
                 (when (< index current-index) "completed")
                 (when (= index (inc current-index)) "next")
                 (when (= index current-index) "current")
                 (when (zero? index) "first")
                 (when (= index (dec steps-count)) "last")]
                (filter identity)
                (string/join " "))}
   [:span
    (let [text [:div.progress-step-index
                (inc index)
                [:br]
                (string/capitalize step-name)]]
      (if (< index current-index)
        [:a (route-to event) text]
        text))]])

(defn checkout-step-bar [data]
  (let [steps (if (experiments/three-steps? data) three-steps four-steps)
        [current-index current-step] (->> steps
                                          (map-indexed vector)
                                          (filter #(= (:event (second %)) (get-in data keypaths/navigation-event)))
                                          first)]
    [:div.row
     [:div.columns.thirteen.omega
      [:ol {:class (str "progress-steps checkout-step-" (:id current-step))}
       (map-indexed (partial display-progress-step (count steps) current-index)
                    steps)]]]))

(defn redesigned-checkout-step-bar [{:keys [current-navigation-event]} owner]
  (om/component
   (html
    (let [steps three-steps
          [current-index current-step] (->> steps
                                            (map-indexed vector)
                                            (filter #(= (:event (second %)) current-navigation-event))
                                            first)]
      [:.flex.flex-column.items-center.col-12.my2
       [:.relative.border-bottom.border-green.col-8 {:style {:top "6px"}}]
       [:.flex.justify-center.col-12.z1
        (for [[step-index {:keys [name id event] :as step}] (map-indexed vector steps)]
          [:.h4.col-12.center.titleize.flex.flex-column.justify-center.green
           {:key id :id id}
           [:a.green (when (< step-index current-index) (route-to event))
            [:.mx-auto {:style {:width "12px" :height "12px"}}
             [:.relative {:style {:width "0"}}
              [:.bg-green.circle.absolute {:style {:width "12px" :height "12px"}}]
              (when (> step-index current-index)
                [:.bg-pure-white.circle.absolute {:style {:top "1px" :left "1px" :width "10px" :height "10px"}}])]]
            [:.mt2 name]]])]]))))

(defn query [data]
  {:current-navigation-event (get-in data keypaths/navigation-event)})
