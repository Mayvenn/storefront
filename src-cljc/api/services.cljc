(ns api.services
  (:require [catalog.products :as products]
            [storefront.keypaths :as k]
            [storefront.effects :as effects]
            [storefront.transitions :as transitions]
            [storefront.events :as e]
            [spice.selector :as selector]
            [storefront.platform.messages :as messages]
            #?@(:cljs
                [[storefront.api :as api]])))

(defn ^:private addon-selector
  "Selector to find add-ons for a base"
  [{:hair/keys [family]}]
  {:catalog/department "service"
   :service/type       "addon"
   :hair/family        family})

(def ^:private base-selector
  {:catalog/department "service"
   :service/type       "addon"})

(def ^:private select
  (partial selector/match-all {:selector/strict? true}))

(defn ^:private verify?
  [selector sku]
  (boolean (seq (select selector [sku]))))


;; Models

(defn service<-
  "
  Build: Service product in the Catalog domain for a base-sku
  "
  [app-state sku-id]
  (let [skus-db  (get-in app-state k/v2-skus)
        base-sku (get skus-db sku-id)]
    (when (verify? base-selector base-sku)
      {:service/base-sku   base-sku
       :service/addon-skus (select (addon-selector base-sku)
                                   skus-db)})))

(defn service
  "
  Cached: Service product in the Catalog domain
  "
  [app-state {:catalog/keys [sku-id]}]
  (get-in app-state (conj k/models-services sku-id)))

;; BEHAVIOR

(defmethod transitions/transition-state e/service-model-fetched
  [_ _ {:service/keys [base-sku] :http/keys [response]} app-state]
  (when-let [skus (:skus response)]
    (let [app-state'  (update-in app-state
                                k/v2-skus
                                #(merge % (products/index-skus skus)))
          base-sku-id (:catalog/sku-id base-sku)]
      (assoc-in app-state'
                (conj k/models-services base-sku-id)
                (service<- app-state base-sku-id)))))

(defmethod effects/perform-effects e/service-model-requested
  [_ _ {:service/keys [base-sku]} _ app-state]
  #?(:cljs
     (api/get-skus (get-in app-state k/api-cache)
                   (addon-selector base-sku)
                   (fn [response]
                     (messages/handle-message e/service-model-fetched
                                              (merge
                                               {:service/base-sku base-sku
                                                :http/response    response}))))))

