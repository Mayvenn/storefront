(ns storefront.components.checkout-steps
  (:require [storefront.platform.component-utils :refer [route-to]]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.svg :as svg]))

(def ^:private steps
  [{:step-index 1 :events [events/navigate-checkout-address events/navigate-checkout-returning-or-guest] :name "your details" :id "address"}
   {:step-index 2 :events [events/navigate-checkout-payment] :name "payment" :id "payment"}
   {:step-index 3 :events [events/navigate-checkout-confirmation] :name "review & pay" :id "confirm"}])

(defcomponent component [{:keys [current-navigation-event checkout-title]} owner _]
  (let [current-step  (->> steps
                           (filter #(contains? (set (:events %)) current-navigation-event))
                           first)
        current-index (:step-index current-step)]
    [:div
     [:div.title-1.canela.center.pt5 checkout-title]
     [:.flex.flex-column.items-center.col-12.my2
      {:data-test (str "checkout-step-" (:id current-step))}
      [:.relative.border-bottom.col-8 {:style {:top "6px"}}]
      [:.flex.justify-center.col-12
       (for [{:keys [step-index name id events]} steps]
         [:.content-3.canela.col-12.center.flex.flex-column.justify-center
          {:key id :id id}
          [:a.inherit-color (when (< step-index current-index) (route-to (first events)))
           [:.mx-auto {:style {:width "12px" :height "12px"}}
            [:.relative {:style {:width "0"}}
             [:div.absolute {:style {:margin-top "-3px"}}
              (svg/purple-diamond {:height "16px" :width "16px"})]
             (when (> step-index current-index)
               [:div.absolute {:style {:margin-top "-3px"}}
                (svg/white-diamond {:height "16px" :width"16px"})])]]
           [:.mt2 name]]])]]]))

(defn query [data]
  {:current-navigation-event (get-in data keypaths/navigation-event)
   :checkout-title           "Secure Checkout"})
