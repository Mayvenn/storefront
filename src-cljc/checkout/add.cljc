(ns checkout.add
  (:require
   #?@(:cljs [[storefront.api :as api]])
   [adventure.keypaths :as adv-keypaths]
   api.orders
   clojure.set
   [storefront.component :as component :refer [defcomponent]]
   [storefront.components.svg :as svg]
   [storefront.components.ui :as ui]
   [storefront.components.money-formatters :as mf]
   [storefront.events :as events]
   [storefront.keypaths :as keypaths]
   [storefront.accessors.orders :as orders]
   [storefront.platform.component-utils :as utils]
   [storefront.request-keys :as request-keys]
   storefront.utils
   [storefront.transitions :as transitions]
   [storefront.effects :as effects]
   [storefront.platform.messages :as messages]))

(defcomponent component
  [{:checkout-add/keys        [services title free-service-name]
    :checkout-add-button/keys [cta-id cta-target cta-title]} owner opts]
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

(def addon-sku->addon-specialty
  {"SRV-DPCU-000" :specialty-addon-hair-deep-conditioning
   "SRV-3CU-000"  :specialty-addon-360-frontal-customization
   "SRV-TKDU-000" :specialty-addon-weave-take-down
   "SRV-CCU-000"  :specialty-addon-closure-customization
   "SRV-FCU-000"  :specialty-addon-frontal-customization
   "SRV-TRMU-000" :specialty-addon-natural-hair-trim})

(defn stylist-can-perform-addon-service?
  [stylist-service-menu addon-service-sku]
  (let [service-key (get addon-sku->addon-specialty addon-service-sku)]
    (-> stylist-service-menu
        service-key
        boolean)))

(defn ^:private determine-unavailability-reason
  [all-base-skus
   service-menu ;can be moved
   base-service-line-item
   {addon-service-hair-family :hair/family
    addon-sku-id              :catalog/sku-id
    :as                       addon-service}]
  (storefront.utils/?assoc addon-service
                           :addon-unavailable-reason
                           (if (and service-menu
                                  (not (stylist-can-perform-addon-service? service-menu addon-sku-id)))
                             "Your stylist does not yet offer this service on Mayvenn"
                             nil)))

(defn ^:private show-addon?
  [base-service-line-item
   {addon-service-hair-family :hair/family}]
  (contains? (->> addon-service-hair-family
                  (map api.orders/hair-family->service-sku-ids)
                  (reduce clojure.set/union #{}))
             (:sku base-service-line-item)))

(defn addon-skus-for-stylist-sorted-by-availability
  [{:keys [skus stylist-service-menu base-service-line-item]}]
  (let [all-base-skus (->> skus
                           vals
                           (spice.selector/match-all {} {:catalog/department "service" :service/type "base" :promo.mayvenn-install/discountable true})
                           (sort-by :sku/price))]
    (->> skus
         vals
         (spice.selector/match-all {} {:catalog/department "service" :service/type "addon"})
         (map (partial determine-unavailability-reason all-base-skus stylist-service-menu base-service-line-item))
         (filter (partial show-addon? base-service-line-item))
         (sort-by (juxt (comp boolean :addon-unavailable-reason) :order.view/addon-sort)))))


(defn addon-service-sku->addon-service-menu-entry
  [data {:keys    [catalog/sku-id sku/price legacy/variant-id copy/description addon-unavailable-reason selected?]
         sku-name :sku/name}]
  (let [any-updates? (or (utils/requesting-from-endpoint? data request-keys/add-to-bag)
                         (utils/requesting-from-endpoint? data request-keys/delete-line-item))]
    {:addon-service-entry/id                 (str "addon-service-" sku-id)
     :addon-service-entry/disabled-classes   (when addon-unavailable-reason "bg-refresh-gray dark-gray")
     :addon-service-entry/warning            addon-unavailable-reason
     :addon-service-entry/primary            sku-name
     :addon-service-entry/secondary          description
     :addon-service-entry/tertiary           (mf/as-money price)
     :addon-service-entry/target             [events/control-checkout-add-checked {:sku-id sku-id}]
     :addon-service-entry/checkbox-spinning? (or (utils/requesting? data (conj request-keys/add-to-bag sku-id))
                                                 (utils/requesting? data (conj request-keys/delete-line-item variant-id)))
     :addon-service-entry/ready?             (not (or addon-unavailable-reason any-updates?))
     :addon-service-entry/checked?           selected?}))

(defn query [data]
  (let [{current-waiter-order :waiter/order} (api.orders/current data)
        servicing-stylist                    (:services/stylist (api.orders/services data current-waiter-order))
        selected-items                       (or (get-in data keypaths/checkout-add-selected-items) #{})
        discountable-service-line-item       (->> current-waiter-order
                                                  (api.orders/free-mayvenn-service servicing-stylist)
                                                  :free-mayvenn-service/service-item)
        services                             (->> {:base-service-line-item discountable-service-line-item
                                                   :stylist-service-menu   (get-in data adv-keypaths/adventure-servicing-stylist-service-menu)
                                                   :skus                   (get-in data keypaths/v2-skus)}
                                                  addon-skus-for-stylist-sorted-by-availability
                                                  (map #(assoc % :selected? (selected-items (:catalog/sku-id %))))
                                                  (map (partial addon-service-sku->addon-service-menu-entry data)))]
    {:checkout-add/spinning?         (utils/requesting? data request-keys/get-skus)
     :checkout-add/services          services
     :checkout-add/title             "Pair with Add-ons"
     :checkout-add/free-service-name (:product-name discountable-service-line-item)
     :checkout-add-button/cta-id     "continue-to-checkout"
     :checkout-add-button/cta-target [events/control-checkout-add-continued]
     :checkout-add-button/cta-title  "Continue To Check Out"}))

(defn built-component [data opts]
  (component/build component (query data) opts))

(defmethod effects/perform-effects events/control-checkout-add-continued
  [_ _ _ _ _]
  (messages/handle-message events/checkout-add-flow-completed))

(defmethod effects/perform-effects events/checkout-add-flow-completed
  [_ event args _ app-state]
  #?(:cljs (let [session-id       (get-in app-state keypaths/session-id)
                 selected-items   (get-in app-state keypaths/checkout-add-selected-items)
                 order            (get-in app-state keypaths/order)
                 sku-id->quantity (into {} (map #(vector % 1) selected-items))
                 items            (mapv (fn [addon]
                                          {:sku      addon
                                           :quantity 1}) selected-items)]
             ;; NOTE: if we ever need to remove line items from this page, we'll need a new endpoint
             (when (not-empty items)
               (api/add-skus-to-bag session-id
                                   {:token              (:token order)
                                    :number             (:number order)
                                    :user-id            (get-in app-state keypaths/user-id)
                                    :user-token         (get-in app-state keypaths/user-token)
                                    :heat-feature-flags (get-in app-state keypaths/features)
                                    :sku-id->quantity   sku-id->quantity}
                                   #(messages/handle-message events/api-success-bulk-add-to-bag-checkout-add
                                                             {:order order
                                                              :items items}))))))

(defmethod transitions/transition-state events/control-checkout-add-checked
  [_ _ {:keys [sku-id]} app-state]
  (update-in app-state keypaths/checkout-add-selected-items #(let [selected-items (or % #{})
                                                                   operator       (if (selected-items sku-id)
                                                                                    disj
                                                                                    conj)]
                                                               (operator selected-items sku-id))))

(defmethod transitions/transition-state events/navigate-checkout-add
  [_ _ _ app-state]
  (assoc-in app-state keypaths/checkout-add-selected-items #{}))
