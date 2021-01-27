(ns api.products
  (:require [clojure.set :as set]
            [storefront.effects :as fx]
            [storefront.events :as e]
            [storefront.transitions :as t]
            [storefront.keypaths :as k]
            [storefront.platform.messages :as m]
            spice.selector
            [spice.maps :as maps]
            #?@(:cljs [[storefront.api :as api]])))

;; -------

(def ^:private select
  (comp seq (partial spice.selector/match-all
                     {:selector/strict? true})))

;; -------

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
                       (apply merge-with set-accumulate))}}))

(defn product<-
  [state cellar-product]
  (merge
   {:catalog/product-id (:catalog/product-id cellar-product)
    :catalog/department (set (:catalog/department cellar-product))

    :service/type       (set (:service/type cellar-product))

    :product/title      (:legacy/product-name cellar-product)

    :cellar/product     cellar-product}

   (product>sku state cellar-product)))

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
  [_ _ {cellar-products :products cellar-skus :skus} state]
  (-> state
      ;; legacy skus-db
      (update-in k/v2-skus #(merge % cellar-skus))

      ;; Legacy products-db
      (update-in k/v2-products
                 #(merge % (->> cellar-products
                                (maps/index-by :catalog/product-id))))

      ;; Product baked for storefront
      (update-in k/models-products
                 #(merge % (->> cellar-products
                                (mapv (partial product<- state))
                                (maps/index-by :catalog/product-id))))))
