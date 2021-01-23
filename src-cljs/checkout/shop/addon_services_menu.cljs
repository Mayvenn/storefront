(ns checkout.shop.addon-services-menu
  (:require
   api.current
   api.orders
   [clojure.string :as string]
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
   spice.selector))

;; ----------------------

(def ^:private select
  (comp seq (partial spice.selector/match-all {:selector/strict? true})))

(def ^:private ?discountable
  {:catalog/department                 #{"service"}
   :service/type                       #{"base"}
   :promo.mayvenn-install/discountable #{true}})

(def ^:private ?addons
  {:catalog/department #{"service"}
   :service/type       #{"addon"}})

;; ----------------------

(defn ^:private unavailability-reason
  [other-services
   offered-sku-ids
   service-item
   {:catalog/keys [sku-id] :as addon-service}]
  (cond-> addon-service
    (not (contains? offered-sku-ids sku-id))
    (assoc :addon-unavailable-reason
           "Your stylist does not yet offer this service on Mayvenn")

    ;; TODO(corey) why are some of these :hair/family not sets, but vecs?
    (not (contains? (set (:hair/family addon-service))
                    (first (:hair/family service-item))))
    (assoc :addon-unavailable-reason
           (str "Only available with "
                (->> other-services
                     (select (select-keys addon-service [:hair/family]))
                     (map :sku/name)
                     (string/join " or "))))))

(defn addons-by-availability|SRV
  "GROT(SRV)"
  [skus-db offered-sku-ids service-item]
  (let [skus (->> skus-db vals (remove (comp (partial = "SV2") first :service/world)))

        other-services (->> (select ?discountable skus)
                              (sort-by :sku/price))]
    (->> (select ?addons skus)
         (map (partial unavailability-reason
                       other-services
                       offered-sku-ids
                       service-item))
         (sort-by (comp boolean :addon-unavailable-reason))
         (partition-by (comp boolean :addon-unavailable-reason))
         (map (partial sort-by :order.view/addon-sort)))))

(defn addon-service-menu-entry<-
  [data
   {:item.service/keys [addons]}
   {:keys    [catalog/sku-id
              sku/price
              legacy/variant-id
              copy/description
              addon-unavailable-reason]
    sku-name :sku/name}]
  (let [selected?    (boolean (select {:catalog/sku-id #{sku-id}} addons))
        any-updates? (or (utils/requesting-from-endpoint? data request-keys/add-to-bag)
                         (utils/requesting-from-endpoint? data request-keys/delete-line-item))]
    {:addon-service-entry/id                 (str "addon-service-" sku-id)
     :addon-service-entry/disabled-classes   (when addon-unavailable-reason
                                               "bg-refresh-gray dark-gray")
     :addon-service-entry/warning            addon-unavailable-reason
     :addon-service-entry/primary            sku-name
     :addon-service-entry/secondary          description
     :addon-service-entry/tertiary           (mf/as-money price)
     :addon-service-entry/target             [events/control-addon-checkbox {:sku-id              sku-id
                                                                             :variant-id          variant-id
                                                                             :previously-checked? selected?}]
     :addon-service-entry/checkbox-spinning? (or (utils/requesting? data (conj request-keys/add-to-bag sku-id))
                                                 (utils/requesting? data (conj request-keys/delete-line-item variant-id)))
     :addon-service-entry/ready?             (not (or addon-unavailable-reason
                                                      any-updates?))
     :addon-service-entry/checked?           selected?}))

;; NOTE this is focused by the first discountable item, atm
;; In the future there will likely need to be a set-focus for the service-item
;; so we can support addons on more than one service
(defmethod popup/query :addon-services-menu
  [data]
  (let [{:order/keys [items]}                      (api.orders/current data)
        {:stylist.services/keys [offered-sku-ids]} (api.current/stylist data)
        skus-db                                    (get-in data keypaths/v2-skus)

        service-item (->> items (select ?discountable) first) ; <- focus of menu

        [available unavailable]
        (addons-by-availability|SRV skus-db offered-sku-ids service-item)]
    {:addon-services/spinning? (utils/requesting? data request-keys/get-skus)
     :addon-services/services  (map (partial addon-service-menu-entry<- data service-item)
                                    (concat available unavailable))}))

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

(defmethod transitions/transition-state events/control-show-addon-service-menu
  [_ _ _ state]
  (assoc-in state keypaths/popup :addon-services-menu))

(defmethod transitions/transition-state events/control-addon-service-menu-dismiss
  [_ _ _ state]
  (assoc-in state keypaths/popup nil))

(defmethod effects/perform-effects events/control-addon-checkbox
  [_ _ {:keys [sku-id variant-id previously-checked?]} _ app-state]
  (let [session-id (get-in app-state keypaths/session-id)
        order      (get-in app-state keypaths/order)
        sku        (get-in app-state (conj keypaths/v2-skus sku-id))
        quantity   1]
    (if previously-checked?
      (api/delete-line-item session-id order variant-id)
      (api/add-sku-to-bag session-id
                          {:token              (:token order)
                           :number             (:number order)
                           :user-id            (get-in app-state keypaths/user-id)
                           :user-token         (get-in app-state keypaths/user-token)
                           :heat-feature-flags (get-in app-state keypaths/features)
                           :sku                sku
                           :quantity           quantity}
                          #(messages/handle-message events/api-success-add-sku-to-bag
                                                    {:order    %
                                                     :quantity quantity
                                                     :sku      sku})))))

(defmethod trackings/perform-track events/control-show-addon-service-menu
  [_ _ _ state]
  ;; TODO(corey)
  ;; I can't tell if this reporting should diverge from the view, that seems
  ;; odd. But it did. I added the current stylist offerings as an input
  ;; on availability.
  (let [{:order/keys [items]}                      (api.orders/current state)
        {:stylist.services/keys [offered-sku-ids]} (api.current/stylist state)
        skus-db                                    (get-in state keypaths/v2-skus)

        service-item (->> items (select ?discountable) first)

        [available unavailable]
        (addons-by-availability|SRV skus-db offered-sku-ids service-item)]
    ;; GROT(SRV) we can back-map service addon facets to sku-ids
    ;; but eventually, we won't have sku-ids form new addon facets
    (stringer/track-event "add_on_services_button_pressed"
                          {:available_services   (->> available
                                                     (mapv :catalog/sku-id)
                                                     (string/join ","))
                           :unavailable_services (->> unavailable
                                                     (mapv :catalog/sku-id)
                                                     (string/join ","))})))
