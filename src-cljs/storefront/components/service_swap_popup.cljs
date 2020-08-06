(ns storefront.components.service-swap-popup
  (:require api.orders
            [storefront.components.popup :as popup]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            storefront.keypaths
            catalog.keypaths
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [storefront.transitions :as transitions]))

(defmethod popup/component :service-swap
  [{:service-swap-popup/keys [dismiss-target confirm-target] :as data} _ _]
  (ui/modal {:col-class   "col-12 col-6-on-tb col-6-on-dt my8-on-tb-dt flex justify-center"
             :close-attrs (apply utils/fake-href dismiss-target)
             :bg-class    "bg-darken-4"}
            [:div.bg-white.flex.flex-column.items-center.p2.mx2
             [:div.flex.justify-end.self-stretch
              [:a.p2 (svg/x-sharp
                      (merge (apply utils/fake-href dismiss-target)
                             {:data-test "service-swap-popup-dismiss"
                              :height    "20px"
                              :width     "20px"}))]]
             [:div.my5 (svg/swap-arrows {:width  "30px"
                                         :height "36px"
                                         :class  "stroke-p-color fill-p-color"})]
             (let [{:service-swap-popup.secondary/keys [copy id]} data]
               [:div.col-10
                [:div.title-2.canela.center
                 [:div "Swap your"]
                 [:div "Free Mayvenn Service?"]]
                [:div.center.my4 {:data-test id} copy]])
             [:div.flex.mt10.mb5.items-center.justify-around.col-12
              (ui/button-medium-underline-primary (merge (apply utils/fake-href dismiss-target)
                                                         {:class "col-5 center"})
                                                  "Cancel")
              (ui/button-medium-primary (merge (apply utils/fake-href confirm-target)
                                               {:class "col-5"}) "Swap")]]))

(defmethod popup/query :service-swap
  [data]
  (let [service-title-to-be-swapped (-> data api.orders/current :mayvenn-install/service-title)
        intended-service-title      (-> data (get-in catalog.keypaths/sku-intended-for-swap) :sku/title)]
    {:service-swap-popup/confirm-target [events/control-service-swap-popup-confirm]
     :service-swap-popup/dismiss-target [events/control-service-swap-popup-dismiss]
     :service-swap-popup.secondary/copy (str "You can only add 1 Free Mayvenn Service per order. Would you like to swap "
                                             service-title-to-be-swapped " with " intended-service-title " instead?")
     :service-swap-popup.secondary/id   "service-swap-explanation"}))

(defmethod transitions/transition-state events/control-service-swap-popup-dismiss
  [_ event args app-state]
  (-> app-state
      (assoc-in catalog.keypaths/sku-intended-for-swap nil)
      (assoc-in storefront.keypaths/popup nil)))

(defmethod transitions/transition-state events/popup-show-service-swap
  [_ event {:keys [sku-intended]} app-state]
  (-> app-state
      (assoc-in catalog.keypaths/sku-intended-for-swap sku-intended)
      (assoc-in storefront.keypaths/popup :service-swap)))

(defmethod effects/perform-effects events/control-service-swap-popup-confirm
  [_ _ _ previous-app-state _]
  (messages/handle-message events/add-sku-to-bag {:sku           (get-in previous-app-state catalog.keypaths/sku-intended-for-swap)
                                                  :stay-on-page? true
                                                  :quantity      1})
  (messages/handle-message events/popup-hide))

(defmethod transitions/transition-state events/control-service-swap-popup-confirm
  [_ event _ app-state]
  (assoc-in app-state catalog.keypaths/sku-intended-for-swap nil))
