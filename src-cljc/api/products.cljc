(ns api.products
  (:require [api.catalog :refer [select]]
            [clojure.set :as set]
            [catalog.skuers :as skuers]
            [storefront.effects :as fx]
            [storefront.events :as e]
            [storefront.transitions :as t]
            [storefront.keypaths :as k]
            [storefront.platform.messages :as m]
            [spice.maps :as maps]
            #?@(:cljs [[storefront.api :as api]])))

(defn ^:private set-accumulate
  [x y]
  (into (if (set? x) x #{x}) y))

(defn ^:private product>sku
  [state cellar-product]
  (let [cached-skus    (-> (get-in state k/v2-skus)
                           (map (:selector/sku-ids cellar-product)))
        sku-essentials (->> cached-skus
                            (mapcat :selector/essentials)
                            (mapv keyword)
                            set)
        essentials     (->> cellar-product
                            :selector/essentials
                            (mapv keyword)
                            set)
        electives      (set/difference sku-essentials essentials)]
    {:selector/product>sku
     #:selector
     {:essentials (select-keys cellar-product
                               essentials)
      :electives  (->> cached-skus
                       (map #(select-keys % electives))
                       (apply merge-with set-accumulate))
      :result     cached-skus}}))

(defn ^:private extend-services
  [{:as            product
    :catalog/keys  [department]
    :selector/keys [product>sku]}]
  (merge
   product
   (when (= #{"service"} department)
     ;; Special selector for essential service sku-id,
     ;; a narrowing of the general product>sku selector.
     (let [essentials (:selector/essentials product>sku)
           no-addons  (->> (:selector/electives product>sku)
                           keys
                           (mapv (fn [k] [k #{false}]))
                           (into {}))]
       #:product?essential-service
       {:essentials essentials
        :electives  no-addons
        :result     (->> (:selector/result product>sku)
                         (select
                          (merge essentials
                                 ;; GROT(SRV)
                                 {:service/world #{"SV2"}}
                                 no-addons)))}))))

(defn ^:private extend-promotions
  [{:as           product
    :catalog/keys [department]
    cellar-product :cellar/product}]
  (merge
   product
   (when (= #{"service"} department)
     (select-keys cellar-product
                  [:promo.mayvenn-install/requirement-copy]))))

(defn product<-
  [state cellar-product]
  (-> (merge
       {:catalog/product-id (:catalog/product-id cellar-product)
        :catalog/department (set (:catalog/department cellar-product))

        :service/type       (set (:service/type cellar-product))

        :product/title      (:legacy/product-name cellar-product)

        :cellar/product     cellar-product}

       (product>sku state cellar-product))
      extend-services
      extend-promotions))

;; - Read API

(defn by-id
  [state product-id]
  (get-in state (conj k/models-products product-id)))

(defn by-query
  [state query]
  (->> (get-in state k/models-products)
       vals
       (select query)))

;; - Behavior API

(defmethod fx/perform-effects e/cache|product|requested
  [_ _ {:on/keys [success] :keys [query]} _ state]
  (let [cache (get-in state k/api-cache)]
    #?(:cljs
       (api/get-products cache
                         query
                         #(m/handle-message e/cache|product|fetched
                                            (assoc % :on/success success))))))

(defmethod t/transition-state e/cache|product|fetched
  [_ _ {cellar-products :products cellar-skus :skus cellar-images :images} state]
  (let [state'
        (-> state
            (update-in k/v2-images #(merge % cellar-images))

            ;; legacy skus-db
            (update-in k/v2-skus #(merge % cellar-skus))

            ;; Legacy products-db
            (update-in k/v2-products
                       #(merge %
                               (->> cellar-products
                                    (map skuers/->skuer)
                                    (maps/index-by :catalog/product-id)))))]
    ;; Product baked for storefront
    (update-in state'
               k/models-products
               #(merge %
                       (->> cellar-products
                            (mapv (partial product<- state'))
                            (maps/index-by :catalog/product-id))))))
