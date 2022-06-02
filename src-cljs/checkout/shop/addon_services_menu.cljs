(ns checkout.shop.addon-services-menu
  (:require
   api.current
   api.orders
   api.products
   [api.catalog :refer [select ?addons ?discountable ?service]]
   [clojure.string :as string]
   [storefront.api :as api]
   [storefront.component :as c]
   [storefront.components.header :as components.header]
   [storefront.components.money-formatters :as mf]
   [storefront.components.popup :as popup]
   [storefront.components.ui :as ui]
   [storefront.effects :as fx]
   [storefront.events :as e]
   [storefront.hooks.stringer :as stringer]
   [storefront.keypaths :as keypaths]
   [storefront.platform.component-utils :as utils]
   [storefront.platform.messages :refer [handle-message]]
   [storefront.request-keys :as request-keys]
   [storefront.trackings :as trackings]
   [storefront.transitions :as t]
   [checkout.addons :as addons]))

(defn ^:private unavailability-reason|SRV
  "GROT(SRV)"
  [other-services
   offered-sku-ids
   service-item
   {:catalog/keys [sku-id] :as addon-service}]
  (cond-> addon-service
    (and (seq offered-sku-ids) ; No stylist means all available
         (not (contains? offered-sku-ids sku-id)))
    (assoc :addon-unavailable-reason
           "Your stylist does not yet offer this service on Mayvenn")

    (not-any? (set (:hair/family addon-service)) (:hair/family service-item))
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
         (map (partial unavailability-reason|SRV
                       other-services
                       offered-sku-ids
                       service-item))
         (sort-by (comp boolean :addon-unavailable-reason))
         (partition-by (comp boolean :addon-unavailable-reason))
         (map (partial sort-by :order.view/addon-sort)))))

(defn addon-service-menu-entry<-|SRV
  "GROT(SRV)"
  [state
   {:item.service/keys [addons]}
   {:keys    [catalog/sku-id
              sku/price
              legacy/variant-id
              copy/description
              addon-unavailable-reason]
    sku-name :sku/name}]
  (let [selected?    (boolean (select {:catalog/sku-id #{sku-id}} addons))
        any-updates? (or (utils/requesting? state request-keys/add-to-bag)
                         (utils/requesting? state request-keys/delete-line-item))]
    {:addon-service-entry/id                 (str "addon-service-" sku-id)
     :addon-service-entry/disabled-classes   (when addon-unavailable-reason
                                               "bg-refresh-gray dark-gray")
     :addon-service-entry/warning            addon-unavailable-reason
     :addon-service-entry/primary            sku-name
     :addon-service-entry/secondary          description
     :addon-service-entry/tertiary           (mf/as-money price)
     :addon-service-entry/target             [e/control-addon-checkbox {:sku-id              sku-id
                                                                        :variant-id          variant-id
                                                                        :previously-checked? selected?}]
     :addon-service-entry/checkbox-spinning? (or (utils/requesting? state (conj request-keys/add-to-bag sku-id))
                                                 (utils/requesting? state (conj request-keys/delete-line-item variant-id)))
     :addon-service-entry/ready?             (not (or addon-unavailable-reason
                                                      any-updates?))
     :addon-service-entry/checked?           selected?}))

;; NOTE this is focused by the first discountable item, atm
;; In the future there will likely need to be a set-focus for the service-item
;; so we can support addons on more than one service
(defmethod popup/query :addon-services-menu
  [state]
  (let [{:order/keys [items] :service/keys [world]} (api.orders/current state)]
    (if (= "SV2" world)
      (let [addon-facets        (->> (get-in state keypaths/v2-facets)
                                     (filter :service/addon?))
            offered-facet-slugs (->> (api.current/stylist state)
                                     :stylist.services/offered-facet-slugs)

            focused-item     (->> items (select ?discountable) first)
            product-services (->> {:catalog/department #{"service"}
                                   :service/world      #{"SV2"}
                                   :service/type       #{"base"}}
                                  (api.products/by-query state))
            focused-product  (->> focused-item
                                  :selector/from-products
                                  first
                                  (api.products/by-id state))]
        {:addon-services/spinning? (utils/requesting? state request-keys/get-skus)
         :addon-services/services  (->> (addons/availability<- addon-facets
                                                               offered-facet-slugs
                                                               product-services
                                                               focused-product)
                                        (map (partial addons/menu-entry<-
                                                      state
                                                      e/flow|cart-service-addons|toggled
                                                      focused-item)))})
      ;; GROT(SRV)
      (let [skus-db         (get-in state keypaths/v2-skus)
            offered-sku-ids (->> (api.current/stylist state)
                                 :stylist.services/offered-sku-ids)
            service-item    (->> items (select ?discountable) first) ; <- focus of menu

            [available unavailable]
            (addons-by-availability|SRV skus-db offered-sku-ids service-item)]
        {:addon-services/spinning? (utils/requesting? state request-keys/get-skus)
         :addon-services/services  (map (partial addon-service-menu-entry<-|SRV state service-item)
                                        (concat available unavailable))}))))

(defmethod popup/component :addon-services-menu
  [{:addon-services/keys [spinning? services]} _ _]
  (c/html
   (if spinning?
     [:div.py3.h2 ui/spinner]
     (ui/modal
      {:body-style  {:max-width "625px"}
       :close-attrs (utils/fake-href e/control-addon-service-menu-dismiss)
       :col-class   "col-12"}
      [:div.bg-white
       (components.header/nav-header
        {:class "border-bottom border-gray"} nil
        (c/html [:div.center.proxima.content-1 "Add-on Services"])
        (c/html [:div (ui/button-medium-underline-primary
                       (merge {:data-test "addon-services-popup-close"}
                              (utils/fake-href e/control-addon-service-menu-dismiss))
                       "DONE")]))
       (mapv
        (fn [{:addon-service-entry/keys [id ready? disabled-classes primary secondary tertiary warning target checked? checkbox-spinning?]}]
          [:div.p3.py4.pr4.flex
           (merge
            {:key   id
             :class disabled-classes}
            (when ready?
              {:on-click (apply utils/send-event-callback target)
               :on-key-down  #(when (= 32 (.-keyCode %))
                                (apply utils/send-event-callback target))}))
           (if checkbox-spinning?
             [:div.mt1
              [:div.pr2 {:style {:width "41px"}} ui/spinner]]
             [:div.mt1.pl1
              (ui/check-box {:value     checked?
                             :data-test id})])
           [:div.flex-grow-1.mr2
            [:div.proxima.content-2 primary]
            [:div.proxima.content-3 secondary]
            [:div.proxima.content-3.red warning]]
           [:div tertiary]])
        services)]))))

(defmethod t/transition-state e/control-show-addon-service-menu
  [_ _ _ state]
  (assoc-in state keypaths/popup :addon-services-menu))

(defmethod fx/perform-effects e/control-show-addon-service-menu
  [_ _ _ _ _]
  (handle-message e/cache|product|requested
                  {:query (merge ?service
                                 {:service/category ["preparation" "customization" "install"]})}))

(defmethod trackings/perform-track e/control-show-addon-service-menu
  [_ _ _ state]
  ;; TODO(corey)
  ;; I can't tell if this reporting should diverge from the view above.
  ;; I added the current stylist offerings as an input on availability.
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

(defmethod t/transition-state e/control-addon-service-menu-dismiss
  [_ _ _ state]
  (assoc-in state keypaths/popup nil))

;; GROT(SRV)
(defmethod fx/perform-effects e/control-addon-checkbox
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
                           :heat-feature-flags (keys (get-in app-state keypaths/features))
                           :sku                sku
                           :quantity           quantity}
                          #(handle-message e/api-success-add-sku-to-bag
                                           {:order    %
                                            :quantity quantity
                                            :sku      sku})))))
