(ns storefront.components.checkout-steps
  (:require [storefront.platform.component-utils :refer [route-to]]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.component :as component :refer [defcomponent]]))

(def ^:private steps
  [{:step-index 1 :events [events/navigate-checkout-address events/navigate-checkout-returning-or-guest] :name "your details" :id "address"}
   {:step-index 2 :events [events/navigate-checkout-payment] :name "payment" :id "payment"}
   {:step-index 3 :events [events/navigate-checkout-confirmation] :name "review & pay" :id "confirm"}])

(defcomponent component [{:keys [current-navigation-event]} owner _]
  (let [current-step  (->> steps
                           (filter #(contains? (set (:events %)) current-navigation-event))
                           first)
        current-index (:step-index current-step)]
    [:.flex.flex-column.items-center.col-12.my2
     {:data-test (str "checkout-step-" (:id current-step))}
     [:.relative.border-bottom.border-navy.col-8 {:style {:top "6px"}}]
     [:.flex.justify-center.col-12
      (for [{:keys [step-index name id events]} steps]
        [:.h5.col-12.center.titleize.flex.flex-column.justify-center.navy
         {:key id :id id}
         [:a.navy (when (< step-index current-index) (route-to (first events)))
          [:.mx-auto {:style {:width "12px" :height "12px"}}
           [:.relative {:style {:width "0"}}
            [:.bg-navy.circle.absolute {:style {:width "12px" :height "12px"}}]
            (when (> step-index current-index)
              [:.bg-white.circle.absolute {:style {:top "1px" :left "1px" :width "10px" :height "10px"}}])]]
          [:.mt2 name]]])]]))

(defn query [data]
  {:current-navigation-event (get-in data keypaths/navigation-event)})
