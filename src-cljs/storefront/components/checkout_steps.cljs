(ns storefront.components.checkout-steps
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.utils :refer [route-to]]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

;;TODO Possibly clean up ID
(def ^:private steps
  [{:event events/navigate-checkout-address :name "your details" :id "address"}
   {:event events/navigate-checkout-payment :name "payment" :id "payment"}
   {:event events/navigate-checkout-confirmation :name "review" :id "confirm"}])

(defn component [{:keys [current-navigation-event]} owner]
  (om/component
   (html
    (let [[current-index current-step] (->> steps
                                            (map-indexed vector)
                                            (filter #(= (:event (second %)) current-navigation-event))
                                            first)]
      [:.flex.flex-column.items-center.col-12.my2
       {:data-test (str "checkout-step-" (:id current-step))}
       [:.relative.border-bottom.border-navy.col-8 {:style {:top "6px"}}]
       [:.flex.justify-center.col-12
        (for [[step-index {:keys [name id event] :as step}] (map-indexed vector steps)]
          [:.h4.col-12.center.titleize.flex.flex-column.justify-center.navy
           {:key id :id id}
           [:a.navy (when (< step-index current-index) (route-to event))
            [:.mx-auto {:style {:width "12px" :height "12px"}}
             [:.relative {:style {:width "0"}}
              [:.bg-navy.circle.absolute {:style {:width "12px" :height "12px"}}]
              (when (> step-index current-index)
                [:.bg-pure-white.circle.absolute {:style {:top "1px" :left "1px" :width "10px" :height "10px"}}])]]
            [:.mt2 name]]])]]))))

(defn query [data]
  {:current-navigation-event (get-in data keypaths/navigation-event)})
