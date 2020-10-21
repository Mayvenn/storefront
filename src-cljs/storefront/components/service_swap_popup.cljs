(ns storefront.components.service-swap-popup
  (:require [storefront.accessors.orders :as orders]
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
  [{:service-swap-popup/keys [dismiss-target confirm-target title confirm-copy dismiss-copy] :as data} _ _]
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
             (let [{:service-swap-popup.secondary/keys [notices id]} data]
               [:div.col-10
                [:div.title-2.canela.center
                 [:div title]]
                [:ul.mx1.my4.list-purple-diamond {:data-test id}
                 (for [notice notices]
                   [:li.py2
                    (for [copy notice]
                      [:div copy])])]])
             [:div.flex.mb5.items-center.justify-center.col-12
              (ui/button-medium-primary (merge (apply utils/fake-href confirm-target)
                                               {:class "col-8"
                                                :data-test "service-swap-popup-confirm"}) confirm-copy)]
             [:div.flex.mb10.items-center.justify-center.col-12
              (ui/button-medium-underline-primary (merge (apply utils/fake-href dismiss-target)
                                                         {:class "col-5 center"})
                                                  dismiss-copy)]]))

(defmethod popup/query :service-swap
  [data]
  (let [service-title-to-be-swapped (->> (get-in data storefront.keypaths/order)
                                         orders/service-line-items
                                         (filter (comp :promo.mayvenn-install/discountable :variant-attrs))
                                         first
                                         :variant-name)
        intended-service-title      (-> data (get-in catalog.keypaths/sku-intended-for-swap) :sku/title)]
    {:service-swap-popup/confirm-target [events/control-service-swap-popup-confirm]
     :service-swap-popup/dismiss-target [events/control-service-swap-popup-dismiss]
     :service-swap-popup/confirm-copy "Confirm Swap"
     :service-swap-popup/dismiss-copy "Cancel"
     :service-swap-popup/title          "Before we move on..."
     :service-swap-popup.secondary/notices [["1 Free Mayvenn Service per order."
                                             (str  "You are about to swap " service-title-to-be-swapped
                                                   " with " intended-service-title ".")]]
     :service-swap-popup.secondary/id   "service-swap-explanation"}))

;; (defmethod popup/component :service-swap
;;   [{:service-swap-popup/keys [dismiss-target confirm-target] :as data} _ _]
;;   (ui/modal {:col-class   "col-12 col-6-on-tb col-6-on-dt my8-on-tb-dt flex justify-center"
;;              :close-attrs (apply utils/fake-href dismiss-target)
;;              :bg-class    "bg-darken-4"}
;;             [:div.bg-white.flex.flex-column.items-center.p2.mx2
;;              [:div.flex.justify-end.self-stretch
;;               [:a.p2 (svg/x-sharp
;;                       (merge (apply utils/fake-href dismiss-target)
;;                              {:data-test "service-swap-popup-dismiss"
;;                               :height    "20px"
;;                               :width     "20px"}))]]
;;              [:div.my5 (svg/swap-arrows {:width  "30px"
;;                                          :height "36px"
;;                                          :class  "stroke-p-color fill-p-color"})]
;;              (let [{:service-swap-popup.secondary/keys [copy id]} data]
;;                [:div.col-10
;;                 [:div.title-2.canela.center
;;                  [:div "Swap your"]
;;                  [:div "Free Mayvenn Service?"]]
;;                 [:div.center.my4 {:data-test id} copy]])
;;              [:div.flex.mt10.mb5.items-center.justify-around.col-12
;;               (ui/button-medium-underline-primary (merge (apply utils/fake-href dismiss-target)
;;                                                          {:class "col-5 center"})
;;                                                   "Cancel")
;;               (ui/button-medium-primary (merge (apply utils/fake-href confirm-target)
;;                                                {:class "col-5"
;;                                                 :data-test "service-swap-popup-confirm"}) "Swap")]]))

;; (defmethod popup/query :service-swap
;;   [data]
;;   (let [service-title-to-be-swapped (->> (get-in data storefront.keypaths/order)
;;                                          orders/service-line-items
;;                                          (filter (comp :promo.mayvenn-install/discountable :variant-attrs))
;;                                          first
;;                                          :variant-name)
;;         intended-service-title      (-> data (get-in catalog.keypaths/sku-intended-for-swap) :sku/title)]
;;     {:service-swap-popup/confirm-target [events/control-service-swap-popup-confirm]
;;      :service-swap-popup/dismiss-target [events/control-service-swap-popup-dismiss]
;;      :service-swap-popup.secondary/copy (str "You can only add 1 Free Mayvenn Service per order. Would you like to swap "
;;                                              service-title-to-be-swapped " with " intended-service-title " instead?")
;;      :service-swap-popup.secondary/id   "service-swap-explanation"}))

(defmethod transitions/transition-state events/control-service-swap-popup-dismiss
  [_ event args app-state]
  (-> app-state
      (assoc-in catalog.keypaths/sku-intended-for-swap nil)
      (assoc-in storefront.keypaths/popup nil)))

(defmethod effects/perform-effects events/control-service-swap-popup-dismiss
  [_ event args app-state]
  (messages/handle-message events/popup-hide))

(defmethod transitions/transition-state events/popup-show-service-swap
  [_ event {:keys [sku-intended confirmation-command]} app-state]
  (-> app-state
      (assoc-in catalog.keypaths/sku-intended-for-swap sku-intended)
      (assoc-in catalog.keypaths/service-swap-confirmation-command confirmation-command)
      (assoc-in storefront.keypaths/popup :service-swap)))

(defmethod effects/perform-effects events/control-service-swap-popup-confirm
  [_ _ _ previous-app-state]
  (apply messages/handle-message (get-in previous-app-state catalog.keypaths/service-swap-confirmation-command))
  (messages/handle-message events/popup-hide))

(defmethod transitions/transition-state events/control-service-swap-popup-confirm
  [_ event _ app-state]
  (-> app-state
      (assoc-in catalog.keypaths/service-swap-confirmation-command nil)
      (assoc-in catalog.keypaths/sku-intended-for-swap nil)))
