(ns checkout.shop.addon-services-menu
  (:require
   #?@(:cljs [[storefront.api :as api]
              [storefront.browser.scroll :as scroll]
              [storefront.components.payment-request-button :as payment-request-button]
              [storefront.components.popup :as popup]
              [storefront.confetti :as confetti]
              [storefront.history :as history]
              [storefront.hooks.quadpay :as quadpay]
              [storefront.platform.messages :as messages]])
   [spice.maps :as maps]
   spice.selector
   [storefront.accessors.mayvenn-install :as mayvenn-install]
   [storefront.accessors.orders :as orders]
   [storefront.component :as component :refer [defcomponent defdynamic-component]]
   [storefront.components.header :as components.header]
   [storefront.components.money-formatters :as mf]
   [storefront.components.ui :as ui]
   [storefront.effects :as effects]
   [storefront.events :as events]
   [storefront.keypaths :as keypaths]
   [storefront.platform.component-utils :as utils]
   [storefront.request-keys :as request-keys]
   [storefront.transitions :as transitions]))


(def addon-sku->addon-specialty
  {"SRV-DPCU-000" :specialty-addon-hair-deep-conditioning,
   "SRV-3CU-000"  :specialty-addon-360-frontal-customization,
   "SRV-TKDU-000" :specialty-addon-weave-take-down,
   "SRV-CCU-000"  :specialty-addon-closure-customization,
   "SRV-FCU-000"  :specialty-addon-frontal-customization,
   "SRV-TRMU-000" :specialty-addon-natural-hair-trim})

(defn stylist-can-perform-addon-service?
  [stylist addon-service-sku]
  (let [service-key (get addon-sku->addon-specialty addon-service-sku)]
    (-> stylist
        :service-menu
        service-key
        boolean)))

(defn addon-service-sku->addon-service-menu-entry
  [data {:keys    [catalog/sku-id sku/price legacy/variant-id copy/description addon-unavailable-reason addon-selected?]
         sku-name :sku/name}]
  (let [checkbox-spinning? (or (utils/requesting? data (fn [req]
                                                         (subvec (:request-key req []) 0 1))
                                                  request-keys/add-to-bag)
                               (utils/requesting? data (fn [req]
                                                         (subvec (:request-key req []) 0 1))
                                                  request-keys/delete-line-item))]
    {:addon-service-entry/id                 (str "addon-service-" sku-id)
     :addon-service-entry/disabled-classes   (when addon-unavailable-reason "bg-refresh-gray dark-gray")
     :addon-service-entry/warning            addon-unavailable-reason
     :addon-service-entry/primary            sku-name
     :addon-service-entry/secondary          description
     :addon-service-entry/tertiary           (mf/as-money price)
     :addon-service-entry/target             [events/control-addon-checkbox {:sku-id              sku-id
                                                                             :variant-id          variant-id
                                                                             :previously-checked? addon-selected?}]
     :addon-service-entry/checkbox-spinning? (and checkbox-spinning? (not addon-unavailable-reason))
     :addon-service-entry/disabled?          (or addon-unavailable-reason
                                                 checkbox-spinning?)
     :addon-service-entry/checked?           addon-selected?}))

(defn query [data]
  (let [hair-family-facet                   (->> (get-in data keypaths/v2-facets)
                                                 (maps/index-by :facet/slug)
                                                 :hair/family
                                                 :facet/options
                                                 (maps/index-by :option/slug))
        {servicing-stylist :mayvenn-install/stylist
         :as               mayvenn-install} (mayvenn-install/mayvenn-install data)
        sorted-addon-services               (->> (get-in data keypaths/v2-skus)
                                                 vals
                                                 (spice.selector/match-all {} {:catalog/department "service" :service/type "addon"})
                                                 (map (fn [{addon-service-hair-family :hair/family
                                                            addon-sku-id              :catalog/sku-id
                                                            :as                       addon-service}]
                                                        (-> addon-service
                                                            (assoc
                                                             :addon-unavailable-reason
                                                             (or
                                                              (when (not (stylist-can-perform-addon-service? servicing-stylist addon-sku-id))
                                                                "Your stylist does not yet offer this service on Mayvenn")
                                                              (when (not (contains?
                                                                          (set (map mayvenn-install/hair-family->service-type addon-service-hair-family))
                                                                          (:mayvenn-install/service-type mayvenn-install)))
                                                                (let [facet-name (->> addon-service-hair-family
                                                                                    first
                                                                                    (get hair-family-facet)
                                                                                    :sku/name)]
                                                                  (str "Only Available with " facet-name " Install" )))))
                                                            (assoc :addon-selected?
                                                                   (contains? (set (map :sku (orders/service-line-items (get-in data keypaths/order))))
                                                                              addon-sku-id)))))
                                                 (sort-by (comp boolean :addon-unavailable-reason))
                                                 (partition-by (comp boolean :addon-unavailable-reason))
                                                 (map (partial sort-by :order.view/addon-sort))
                                                 flatten
                                                 (map (partial addon-service-sku->addon-service-menu-entry data)))]
    {:addon-services/spinning? (utils/requesting? data request-keys/get-skus)
     :addon-services/services  sorted-addon-services}))

(defn addon-services-popup-template [{:addon-services/keys [spinning? services]}]
  (component/html
   (if spinning?
     [:div.py3.h2 ui/spinner]
     (ui/modal
      {:body-style {:max-width "625px"}
       :col-class  "col-12"}
      [:div.bg-white
       (components.header/mobile-nav-header {:class "border-bottom border-gray" } nil
                                            (component/html [:div.center.proxima.content-1 "Add-on Services"])
                                            (component/html [:div (ui/button-medium-underline-secondary (merge {:data-test "addon-services-popup-close"}
                                                                                                               (utils/fake-href events/control-addons-popup-done-button)) "DONE")]))
       (mapv
        (fn [{:addon-service-entry/keys [id disabled? disabled-classes primary secondary tertiary warning target checked? checkbox-spinning?]}]
          [:div.p3.py4.pr4.flex
           (merge
            {:key   id
             :class disabled-classes}
            (when-not disabled?
              {:on-click (apply utils/send-event-callback target)}))
           (if checkbox-spinning?
             [:div.mt1
              [:div.pr2 {:style {:width "41px"}} ui/spinner]]
             [:div.mt1.pl1 (ui/check-box {:value     checked?
                                          :disabled  disabled?
                                          :data-test id})])
           [:div.flex-grow-1.mr2
            [:div.proxima.content-2 primary]
            [:div.proxima.content-3 secondary]
            [:div.proxima.content-3.red warning]]
           [:div tertiary]])
        services)]))))

(defmethod transitions/transition-state events/control-browse-addons-button [_ event args app-state]
  (assoc-in app-state keypaths/addons-popup-displayed? true))

(defmethod transitions/transition-state events/control-addons-popup-done-button [_ event args app-state]
  (assoc-in app-state keypaths/addons-popup-displayed? false))

(defmethod effects/perform-effects events/control-addon-checkbox
  [_ event {:keys [sku-id variant-id previously-checked?] :as args} _ app-state]
  #?(:cljs
     (let [session-id (get-in app-state keypaths/session-id)
           order      (get-in app-state keypaths/order)]
       (if previously-checked?
         (api/delete-line-item session-id order variant-id)
         (api/add-sku-to-bag session-id
                             {:token      (:token order)
                              :number     (:number order)
                              :user-id    (get-in app-state keypaths/user-id)
                              :user-token (get-in app-state keypaths/user-token)
                              :sku        {:catalog/sku-id sku-id}
                              :quantity   1}
                             #(messages/handle-message events/api-success-update-order-add-service-line-item
                                                       {:order %
                                                        :shop? (get-in app-state keypaths/store-slug)}))))))
