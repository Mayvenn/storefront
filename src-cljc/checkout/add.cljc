(ns checkout.add
  (:require
   #?@(:cljs [[storefront.accessors.auth :as auth]
              [storefront.api :as api]
              [storefront.history :as history]])
    api.orders
    catalog.services
    clojure.set
    spice.selector
   [storefront.component :as component :refer [defcomponent]]
   [storefront.components.money-formatters :as mf]
   [storefront.components.svg :as svg]
   [storefront.components.ui :as ui]
   [storefront.effects :as effects]
   [storefront.events :as events]
   [storefront.keypaths :as keypaths]
   [storefront.platform.component-utils :as utils]
   [storefront.platform.messages :as messages]
   [storefront.request-keys :as request-keys]
   [storefront.transitions :as transitions]
    storefront.utils))

(defcomponent component
  [{:checkout-add/keys        [services title free-service-name]
    :checkout-add-button/keys [cta-id cta-target cta-title]} _ _]
  [:div.container.p2
   {:style {:max-width "580px"}}
   [:div.center.mt4.mb2
    (svg/mirror {:width  40
                 :height 45
                 :class  "fill-p-color"})]
   [:div.center.canela.title-2
    title]
   [:div.m2
    [:div.proxima.title-2.shout.my6
     free-service-name]
    (mapv
     (fn [{:addon-service-entry/keys [id ready? disabled-classes primary secondary tertiary warning target checked? checkbox-spinning?]}]
       [:div.flex.my5
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
     services)
    [:div.my7.mx3
     (when cta-id
       (ui/button-medium-primary (-> (apply utils/route-to cta-target)
                                     (assoc :data-test cta-id))
                                cta-title))]]])

(defn ^:private determine-unavailability-reason
  [stylist-offered-services-sku-ids
   {addon-sku-id              :catalog/sku-id
    :as                       addon-service}]
  (storefront.utils/?assoc addon-service
                           :addon-unavailable-reason
                           (when (and (seq stylist-offered-services-sku-ids)
                                      (not (contains? stylist-offered-services-sku-ids addon-sku-id)))
                             "Your stylist does not yet offer this service on Mayvenn")))

(defn addon-service-sku->addon-service-menu-entry
  [data {:keys    [catalog/sku-id sku/price legacy/variant-id copy/description addon-unavailable-reason selected?]
         sku-name :sku/name
         :as sku}]
  (let [any-updates? (or (utils/requesting-from-endpoint? data request-keys/add-to-bag)
                         (utils/requesting-from-endpoint? data request-keys/delete-line-item))]
    {:addon-service-entry/id                 (str "addon-service-" sku-id)
     :addon-service-entry/disabled-classes   (when addon-unavailable-reason "bg-refresh-gray dark-gray")
     :addon-service-entry/warning            addon-unavailable-reason
     :addon-service-entry/primary            sku-name
     :addon-service-entry/secondary          description
     :addon-service-entry/tertiary           (mf/as-money price)
     :addon-service-entry/target             [events/control-checkout-add-checked {:sku sku
                                                                                   :variant-id variant-id
                                                                                   :previously-checked? selected?}]
     :addon-service-entry/checkbox-spinning? (or (utils/requesting? data (conj request-keys/add-to-bag sku-id))
                                                 (utils/requesting? data (conj request-keys/delete-line-item variant-id)))
     :addon-service-entry/ready?             (not (or addon-unavailable-reason any-updates?))
     :addon-service-entry/checked?           selected?}))

(def ^:private select
  (partial spice.selector/match-all {:selector/strict? true}))

(defn ^:private show-addon?
  [base-service-line-item
   {addon-service-hair-family :hair/family}]
  (contains? (->> addon-service-hair-family
                  (map api.orders/hair-family->service-sku-ids)
                  (reduce clojure.set/union #{}))
             (:catalog/sku-id base-service-line-item)))

(defn addon-skus-for-stylist-sorted-by-availability
  [{:keys [addon-skus stylist-service-menu base-service-line-item]}]
  (->> addon-skus
       (sequence
        (comp
         (map (partial determine-unavailability-reason stylist-service-menu))
         (filter (partial show-addon? base-service-line-item))))
       (sort-by (juxt (comp boolean :addon-unavailable-reason) :order.view/addon-sort))))

(defn query [data]
  (let [{waiter-order :waiter/order
         order-items  :order/items}                      (api.orders/current data)
        {stylist-offered-services-sku-ids
         :services/offered-services-sku-ids}             (api.orders/services data waiter-order)
        {existing-addons :item.service/addons
         :as             discountable-service-line-item} (->> order-items
                                                              (select catalog.services/discountable)
                                                              first)

        services (->> {:base-service-line-item           discountable-service-line-item
                       :stylist-offered-services-sku-ids stylist-offered-services-sku-ids
                       :addon-skus                       (->> (get-in data keypaths/v2-skus)
                                                              vals
                                                              (select catalog.services/addons))}
                      addon-skus-for-stylist-sorted-by-availability
                      (map #(assoc % :selected? (contains? (into #{} (map :catalog/sku-id existing-addons))
                                                           (:catalog/sku-id %))))
                      (map (partial addon-service-sku->addon-service-menu-entry data)))]
    {:checkout-add/spinning?         (utils/requesting? data request-keys/get-skus)
     :checkout-add/services          services
     :checkout-add/title             "Pair with Add-ons"
     :checkout-add/free-service-name (:product-name discountable-service-line-item)
     :checkout-add-button/cta-id     "continue-to-checkout"
     :checkout-add-button/cta-target [events/control-checkout-add-continued]
     :checkout-add-button/cta-title  "Continue To Check Out"}))

(defn ^:export built-component [data opts]
  (component/build component (query data) opts))

(defmethod effects/perform-effects events/control-checkout-add-continued
  [_ _ _ _ _]
  (messages/handle-message events/checkout-add-flow-completed))

(defmethod effects/perform-effects events/checkout-add-flow-completed
  [_ event args _ app-state]
  #?(:cljs
     (if (-> app-state auth/signed-in :storefront.accessors.auth/at-all)
       (history/enqueue-navigate events/navigate-checkout-address {})
       (history/enqueue-navigate events/navigate-checkout-returning-or-guest {}))))

#?(:cljs
   (defmethod effects/perform-effects events/control-checkout-add-checked
     [_ _ {:keys [variant-id sku previously-checked?]} _ app-state]
     (let [session-id (get-in app-state keypaths/session-id)
           order      (get-in app-state keypaths/order)]
       (if previously-checked?
         (api/delete-line-item session-id order variant-id)
         (api/add-sku-to-bag session-id
                             {:token              (:token order)
                              :number             (:number order)
                              :user-id            (get-in app-state keypaths/user-id)
                              :user-token         (get-in app-state keypaths/user-token)
                              :heat-feature-flags (get-in app-state keypaths/features)
                              :sku                sku
                              :quantity           1}
                             #(messages/handle-message events/api-success-add-sku-to-bag
                                                       {:order    %
                                                        :quantity 1
                                                        :sku      sku}))))))
