(ns checkout.addons
  (:require
   #?(:cljs [storefront.api :as api])
   [storefront.components.money-formatters :as mf]
   [storefront.effects :as fx]
   [storefront.events :as e]
   [storefront.keypaths :as k]
   [storefront.platform.component-utils :as utils]
   [storefront.platform.messages :refer [handle-message]]
   [storefront.request-keys :as request-keys]
   spice.selector))

(def ^:private select
  (comp seq (partial spice.selector/match-all {:selector/strict? true})))

;; ----------------------

(defn included?
  [{:as _addon-facet :facet/keys [slug]} product]
  (->> product
       :selector/product>sku
       :selector/electives
       vector
       (select {slug #{true}})))

(defn availability<-
  [addon-facets offered-facet-slugs service-products focused-product]
  (->> addon-facets
       (map (fn stylist-not-avail? [{:as addon-facet :facet/keys [slug]}]
              (cond-> addon-facet
                (and (seq offered-facet-slugs) ; No stylist means all available
                     (not (contains? offered-facet-slugs slug)))
                (assoc :facet/unavailable-reason
                       "Your stylist does not yet offer this service on Mayvenn"))))
       (map (fn product-not-avail? [addon-facet]
              (cond-> addon-facet
                (not (included? addon-facet focused-product))
                (assoc :facet/unavailable-reason
                       (if-let [alternatives (->> service-products
                                                  (filter (partial included? addon-facet))
                                                  not-empty)]
                         (str "Only available with "
                              (->> (map :product/title alternatives)
                                   (interpose " or ")
                                   (apply str)))
                         "Only available with other services")))))
       (sort-by (juxt (comp boolean :facet/unavailable-reason)
                      :filter/order))))

(defn menu-entry<-
  [state
   toggled-event
   service-item
   {:facet/keys   [slug name description unavailable-reason]
    :service/keys [price]}]
  (let [checked?     (contains? (get service-item slug) true)
        any-updates? (or (utils/requesting-from-endpoint? state request-keys/add-to-bag)
                         (utils/requesting-from-endpoint? state request-keys/delete-line-item))]
    {:addon-service-entry/id                 (str "addon-service-" slug)
     :addon-service-entry/disabled-classes   (when unavailable-reason
                                               "bg-refresh-gray dark-gray")
     :addon-service-entry/warning            unavailable-reason
     :addon-service-entry/primary            name
     :addon-service-entry/secondary          description
     :addon-service-entry/tertiary           (mf/as-money price)
     :addon-service-entry/target             [toggled-event
                                              {:service-item service-item
                                               :elections
                                               (let [essentials (:selector/essentials service-item)]
                                                 (merge
                                                  (select-keys service-item essentials)
                                                  {slug #{(if checked? false true)}}))}]
     ;; TODO broken?
     :addon-service-entry/checkbox-spinning? any-updates?
     :addon-service-entry/ready?             (not (or unavailable-reason
                                                      any-updates?))
     :addon-service-entry/checked?           checked?}))

;; -- Behavior

(defmethod fx/perform-effects e/flow|post-cart-service-addons|toggled
  [_ _ {:keys [elections product service-item]} _ _ _]
  (handle-message e/biz|product|options-elected
                  {:elections elections
                   :product   product
                   :on/success
                   #(handle-message e/biz|service-item|addons-replaced
                                    (merge
                                     %
                                     {:focus/line-item-id
                                      (:item/id service-item)}))}))

(defmethod fx/perform-effects e/flow|cart-service-addons|toggled
  [_ _ {:keys [elections product service-item]} _ _ _]
  (handle-message e/biz|product|options-elected
                  {:elections elections
                   :product   product
                   :on/success
                   #(handle-message e/biz|service-item|addons-replaced
                                    (merge
                                     %
                                     {:focus/line-item-id
                                      (:item/id service-item)}))}))

(defmethod fx/perform-effects e/biz|product|options-elected
  [_ _ {:keys [elections product] :on/keys [success]} _ state]
  (when (fn? success)
    (let [elected-sku (->> (get-in state k/v2-skus)
                           vals
                           ;; TODO(corey) shouldn't this narrow to product first?
                           (select elections)
                           first)]
      (success {:elected/sku elected-sku}))))

(def remove-item #?(:cljs api/remove-line-item :clj list))
(def add-item #?(:cljs api/add-sku-to-bag :clj list))

(defmethod fx/perform-effects e/biz|service-item|addons-replaced
  [_ _ {:elected/keys [sku] :focus/keys [line-item-id]} _ state]
  (let [session-id  (get-in state k/session-id)
        order-creds (-> state
                        (get-in k/order)
                        (select-keys [:number :token]))]
    (remove-item
     session-id
     (merge order-creds
            {:variant-id line-item-id
             :sku-code   (:catalog/sku-id sku)})
     (fn removed [_]
       (add-item
        session-id
        (merge order-creds
               {:user-id            (get-in state k/user-id)
                :user-token         (get-in state k/user-token)
                :heat-feature-flags (get-in state k/features)
                :sku                sku
                :quantity           1})
        (fn added [order]
          (handle-message e/api-success-add-sku-to-bag
                          {:order    order
                           :quantity 1
                           :sku      sku})))))))
