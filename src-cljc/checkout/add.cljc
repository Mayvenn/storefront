(ns checkout.add
  (:require
   [adventure.keypaths :as adv-keypaths]
   api.orders
   clojure.set
   [clojure.string :as string]
   [storefront.component :as component :refer [defcomponent]]
   [storefront.components.svg :as svg]
   [storefront.components.ui :as ui]
   [storefront.components.money-formatters :as mf]
   [storefront.events :as events]
   [storefront.keypaths :as keypaths]
   [storefront.accessors.orders :as orders]
   [storefront.platform.component-utils :as utils]
   [storefront.request-keys :as request-keys]
   storefront.utils))

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

(defn addon-skus-for-stylist-grouped-by-availability
  [{:keys [skus stylist-service-menu base-service-line-item]}]
  (let [all-base-skus            (->> skus
                                      vals
                                      (spice.selector/match-all {} {:catalog/department "service" :service/type "base" :promo.mayvenn-install/discountable true})
                                      (sort-by :sku/price))
        [available-addon-skus
         unavailable-addon-skus] (->> skus
                                      vals
                                      (spice.selector/match-all {} {:catalog/department "service" :service/type "addon"})
                                      (map (partial determine-unavailability-reason all-base-skus stylist-service-menu base-service-line-item))
                                      (filter (partial show-addon? base-service-line-item))
                                      (sort-by (comp boolean :addon-unavailable-reason))
                                      (partition-by (comp boolean :addon-unavailable-reason))
                                      (map (partial sort-by :order.view/addon-sort)))]
    {:available-addon-skus   available-addon-skus
     :unavailable-addon-skus unavailable-addon-skus}))


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

(defn query [data]
  (let [{current-waiter-order :waiter/order} (api.orders/current data)
        servicing-stylist                    (:services/stylist (api.orders/services data current-waiter-order))
        discountable-service-line-item       (->> current-waiter-order
                                                  (api.orders/free-mayvenn-service servicing-stylist)
                                                  :free-mayvenn-service/service-item)
        {:keys [available-addon-skus
                unavailable-addon-skus]}     (addon-skus-for-stylist-grouped-by-availability
                                              {:base-service-line-item discountable-service-line-item
                                               :stylist-service-menu   (get-in data adv-keypaths/adventure-servicing-stylist-service-menu)
                                               :skus                   (get-in data keypaths/v2-skus)})]
    {:checkout-add/spinning?         (utils/requesting? data request-keys/get-skus)
     :checkout-add/services          (map (partial addon-service-sku->addon-service-menu-entry data)
                                          (concat available-addon-skus unavailable-addon-skus))
     :checkout-add/title             "Pair with Add-ons"
     :checkout-add/free-service-name (:product-name discountable-service-line-item)
     :checkout-add-button/cta-id     "continue-to-checkout"
     :checkout-add-button/cta-target [events/control-checkout-add-continued]
     :checkout-add-button/cta-title  "Continue To Check Out"}))

(defn built-component [data opts]
  (component/build component (query data) opts))
