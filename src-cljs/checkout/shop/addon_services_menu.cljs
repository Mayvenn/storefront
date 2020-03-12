(ns checkout.shop.addon-services-menu
  (:require
   [clojure.set :as set]
   [clojure.string :as string]
   [spice.maps :as maps]
   [storefront.accessors.mayvenn-install :as mayvenn-install]
   [storefront.accessors.orders :as orders]
   [storefront.api :as api]
   [storefront.component :as component]
   [storefront.components.header :as components.header]
   [storefront.components.money-formatters :as mf]
   [storefront.components.popup :as popup]
   [storefront.components.ui :as ui]
   [storefront.effects :as effects]
   [storefront.events :as events]
   [storefront.hooks.stringer :as stringer]
   [storefront.keypaths :as keypaths]
   [storefront.platform.component-utils :as utils]
   [storefront.platform.messages :as messages]
   [storefront.request-keys :as request-keys]
   [storefront.trackings :as trackings]
   [storefront.transitions :as transitions]
   spice.selector
   storefront.utils))

(def addon-sku->addon-specialty
  {"SRV-DPCU-000" :specialty-addon-hair-deep-conditioning
   "SRV-3CU-000"  :specialty-addon-360-frontal-customization
   "SRV-TKDU-000" :specialty-addon-weave-take-down
   "SRV-CCU-000"  :specialty-addon-closure-customization
   "SRV-FCU-000"  :specialty-addon-frontal-customization
   "SRV-TRMU-000" :specialty-addon-natural-hair-trim})

(defn stylist-can-perform-addon-service?
  [stylist addon-service-sku]
  (let [service-key (get addon-sku->addon-specialty addon-service-sku)]
    (-> stylist
        :service-menu
        service-key
        boolean)))

(defn addon-service-sku->addon-service-menu-entry
  [data {:keys    [catalog/sku-id sku/price legacy/variant-id copy/description addon-unavailable-reason]
         sku-name :sku/name}]
  (let [selected?    (contains? (set (map :sku (orders/service-line-items (get-in data keypaths/order)))) sku-id)
        any-updates? (or (utils/requesting-from-endpoint? data request-keys/add-to-bag)
                         (utils/requesting-from-endpoint? data request-keys/delete-line-item))]
    {:addon-service-entry/id                 (str "addon-service-" sku-id)
     :addon-service-entry/disabled-classes   (when addon-unavailable-reason "bg-refresh-gray dark-gray")
     :addon-service-entry/warning            addon-unavailable-reason
     :addon-service-entry/primary            sku-name
     :addon-service-entry/secondary          description
     :addon-service-entry/tertiary           (mf/as-money price)
     :addon-service-entry/target             [events/control-addon-checkbox {:sku-id              sku-id
                                                                             :variant-id          variant-id
                                                                             :previously-checked? selected?}]
     :addon-service-entry/checkbox-spinning? (or (utils/requesting? data (conj request-keys/add-to-bag sku-id))
                                                 (utils/requesting? data (conj request-keys/delete-line-item variant-id)))
     :addon-service-entry/ready?             (not (or addon-unavailable-reason any-updates?))
     :addon-service-entry/checked?           selected?}))

(defn ^:private determine-unavailability-reason
  [facets
   {:mayvenn-install/keys [stylist service-type]}
   {addon-service-hair-family :hair/family
    addon-sku-id              :catalog/sku-id
    :as                       addon-service}]
  (let [hair-family-facet (->> facets (maps/index-by :facet/slug) :hair/family :facet/options (maps/index-by :option/slug))]
    (storefront.utils/?assoc addon-service
           :addon-unavailable-reason
           (or
            (when (not (stylist-can-perform-addon-service? stylist addon-sku-id))
              "Your stylist does not yet offer this service on Mayvenn")
            (when (not (contains? (set (map mayvenn-install/hair-family->service-type addon-service-hair-family)) service-type))
              (let [facet-name (->> addon-service-hair-family first (get hair-family-facet) :sku/name)]
                (str "Only available with " facet-name " Install")))))))


(defn addon-skus-for-stylist-grouped-by-availability
  [{:keys [facets mayvenn-install skus]}]
  (let [[available-addon-skus
         unavailable-addon-skus] (->> skus
                                      vals
                                      (spice.selector/match-all {} {:catalog/department "service" :service/type "addon"})
                                      (map (partial determine-unavailability-reason facets mayvenn-install))
                                      (sort-by (comp boolean :addon-unavailable-reason))
                                      (partition-by (comp boolean :addon-unavailable-reason))
                                      (map (partial sort-by :order.view/addon-sort)))]
    {:available-addon-skus   available-addon-skus
     :unavailable-addon-skus unavailable-addon-skus}))

(defmethod popup/query :addon-services-menu
  [data]
  (let [{:keys [available-addon-skus
                unavailable-addon-skus]} (addon-skus-for-stylist-grouped-by-availability
                                          {:facets          (get-in data keypaths/v2-facets)
                                           :mayvenn-install (mayvenn-install/mayvenn-install data)
                                           :skus            (get-in data keypaths/v2-skus)})]
    {:addon-services/spinning? (utils/requesting? data request-keys/get-skus)
     :addon-services/services  (map (partial addon-service-sku->addon-service-menu-entry data)
                                    (concat available-addon-skus unavailable-addon-skus))}))

(defmethod popup/component :addon-services-menu
  [{:addon-services/keys [spinning? services]} _ _]
  (component/html
   (if spinning?
     [:div.py3.h2 ui/spinner]
     (ui/modal
      {:body-style  {:max-width "625px"}
       :close-attrs (utils/fake-href events/control-addon-service-menu-dismiss)
       :col-class   "col-12"}
      [:div.bg-white
       (components.header/mobile-nav-header
        {:class "border-bottom border-gray" } nil
        (component/html [:div.center.proxima.content-1 "Add-on Services"])
        (component/html [:div (ui/button-medium-underline-primary
                               (merge {:data-test "addon-services-popup-close"}
                                      (utils/fake-href events/control-addon-service-menu-dismiss))
                               "DONE")]))
       (mapv
        (fn [{:addon-service-entry/keys [id ready? disabled-classes primary secondary tertiary warning target checked? checkbox-spinning?]}]
          [:div.p3.py4.pr4.flex
           (merge
            {:key   id
             :class disabled-classes}
            (when ready?
              {:on-click (apply utils/send-event-callback target)}))
           (if checkbox-spinning?
             [:div.mt1
              [:div.pr2 {:style {:width "41px"}} ui/spinner]]
             [:div.mt1.pl1 (ui/check-box {:value     checked?
                                          :data-test id})])
           [:div.flex-grow-1.mr2
            [:div.proxima.content-2 primary]
            [:div.proxima.content-3 secondary]
            [:div.proxima.content-3.red warning]]
           [:div tertiary]])
        services)]))))

(defmethod transitions/transition-state events/control-show-addon-service-menu [_ event args app-state]
  (-> app-state
      (assoc-in keypaths/confetti-mode "fired")
      (assoc-in keypaths/popup :addon-services-menu)))

(defmethod transitions/transition-state events/control-addon-service-menu-dismiss [_ event args app-state]
  (assoc-in app-state keypaths/popup nil))

(defmethod effects/perform-effects events/control-addon-checkbox
  [_ event {:keys [sku-id variant-id previously-checked?] :as args} _ app-state]
  (let [session-id (get-in app-state keypaths/session-id)
        order      (get-in app-state keypaths/order)
        sku        (get-in app-state (conj keypaths/v2-skus sku-id))
        quantity   1]
    (if previously-checked?
      (api/delete-line-item session-id order variant-id)
      (api/add-sku-to-bag session-id
                          {:token      (:token order)
                           :number     (:number order)
                           :user-id    (get-in app-state keypaths/user-id)
                           :user-token (get-in app-state keypaths/user-token)
                           :sku        sku
                           :quantity   quantity}
                          #(messages/handle-message events/api-success-add-sku-to-bag
                                                    {:order    %
                                                     :quantity quantity
                                                     :sku      sku})))))

(defmethod trackings/perform-track events/control-show-addon-service-menu [_ event args app-state]
  (let [{:keys [available-addon-sku-ids
                unavailable-addon-sku-ids]} (set/rename-keys
                                             (->> (addon-skus-for-stylist-grouped-by-availability
                                                   {:facets          (get-in app-state keypaths/v2-facets)
                                                    :mayvenn-install (mayvenn-install/mayvenn-install app-state)
                                                    :skus            (get-in app-state keypaths/v2-skus)})
                                                  (maps/map-values #(string/join "," (mapv :catalog/sku-id %))))
                                             {:available-addon-skus   :available-addon-sku-ids
                                              :unavailable-addon-skus :unavailable-addon-sku-ids})]
    (stringer/track-event "add_on_services_button_pressed" {:available_services   available-addon-sku-ids
                                                            :unavailable_services unavailable-addon-sku-ids})))
