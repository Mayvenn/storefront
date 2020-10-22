(ns stylist-profile.swap-popup
  (:require #?@(:cljs [[storefront.components.popup :as popup]])
            [storefront.events :as events]
            [storefront.transitions :as transitions]
            [stylist-directory.stylists :as stylists]
            [stylist-profile.core :as core]
            storefront.keypaths
            [storefront.platform.component-utils :as utils]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.accessors.orders :as orders]
            catalog.keypaths
            [storefront.platform.messages :as messages]
            [storefront.effects :as effects]
            [adventure.keypaths :as adventure.keypaths]
            [api.orders :as api.orders]
            [clojure.string :as str]
            [storefront.keypaths :as storefront.keypaths]))

(defn service-swap-popup-component
  [{:service-swap-popup/keys [dismiss-target confirm-target title confirm-copy dismiss-copy] :as data}]
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

(defn service-swap-popup-query
  [data]
  (let [service-title-to-be-swapped (->> (get-in data storefront.keypaths/order)
                                         orders/service-line-items
                                         (filter (comp :promo.mayvenn-install/discountable :variant-attrs))
                                         first
                                         :variant-name)
        intended-service-title      (-> data (get-in core/sku-intended-for-swap) :sku/title)
        stylist-to-be-swapped  (-> data
                                   (get-in adventure.keypaths/adventure-servicing-stylist)
                                   stylists/->display-name)
        stylist-id             (get-in data adventure.keypaths/stylist-profile-id)
        intended-stylist-name  (-> data
                                   (stylists/by-id stylist-id)
                                   stylists/->display-name)]
    {:service-swap-popup/confirm-target    [events/control-stylist-profile-swap-popup-confirm]
     :service-swap-popup/dismiss-target    [events/control-stylist-profile-swap-popup-dismiss]
     :service-swap-popup/confirm-copy      "Confirm Swap"
     :service-swap-popup/dismiss-copy      "Cancel"
     :service-swap-popup/title             "Before we move on..."
     :service-swap-popup.secondary/notices (cond-> []
                                             (not= service-title-to-be-swapped intended-service-title)
                                             (conj ["1 Free Mayvenn Service per order."
                                                    (str  "You are about to swap " service-title-to-be-swapped
                                                          " with " intended-service-title ".")])
                                             (not= stylist-to-be-swapped intended-stylist-name)
                                             (conj ["1 Stylist per order."
                                                    (str "You are about to swap " stylist-to-be-swapped
                                                         " with " intended-stylist-name ".")]))
     :service-swap-popup.secondary/id      "service-swap-explanation"}))

#?(:cljs
   [(defmethod popup/query :stylist-profile-swap [data]
      (service-swap-popup-query data))
    (defmethod popup/component :stylist-profile-swap [data _ _]
      (service-swap-popup-component data))])

(defmethod transitions/transition-state events/stylist-profile-swap-popup-show
  [_ event {:keys [sku-intended selected-stylist-intended confirmation-commands]} app-state]
  (-> app-state
      (assoc-in core/sku-intended-for-swap sku-intended)
      (assoc-in core/selected-stylist-intended-for-swap selected-stylist-intended)
      (assoc-in core/service-swap-confirmation-commands confirmation-commands)
      (assoc-in storefront.keypaths/popup :stylist-profile-swap)))

(defmethod effects/perform-effects events/control-stylist-profile-swap-popup-confirm
  [_ _ _ previous-app-state]
  (let [confirmation-commands (get-in previous-app-state core/service-swap-confirmation-commands)]
    (doseq [command confirmation-commands]
      (apply messages/handle-message command))
    (messages/handle-message events/popup-hide)))

(defmethod transitions/transition-state events/control-stylist-profile-swap-popup-confirm
  [_ event _ app-state]
  (-> app-state
      (assoc-in core/selected-stylist-intended-for-swap nil)
      (assoc-in core/service-swap-confirmation-commands nil)
      (assoc-in core/sku-intended-for-swap nil)))

(defmethod transitions/transition-state events/control-stylist-profile-swap-popup-dismiss
  [_ event args app-state]
  (-> app-state
      (assoc-in core/selected-stylist-intended-for-swap nil)
      (assoc-in core/sku-intended-for-swap nil)
      (assoc-in storefront.keypaths/popup nil)))

(defmethod effects/perform-effects events/control-stylist-profile-swap-popup-dismiss
    [_ event args app-state]
    (messages/handle-message events/popup-hide))
