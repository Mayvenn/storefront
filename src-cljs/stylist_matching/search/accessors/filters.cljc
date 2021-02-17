(ns stylist-matching.search.accessors.filters
  (:require [spice.maps :as maps]))

(def service-filter-data
  [
   ;; Free Services
   {:sku-id "SRV-LBI-000" :service-menu-key :specialty-sew-in-leave-out}
   {:sku-id "SRV-CBI-000" :service-menu-key :specialty-sew-in-closure}
   {:sku-id "SRV-FBI-000" :service-menu-key :specialty-sew-in-frontal}
   {:sku-id "SRV-3BI-000" :service-menu-key :specialty-sew-in-360-frontal}
   ;; Addons
   {:sku-id "SRV-TRMU-000" :service-menu-key :specialty-addon-natural-hair-trim}
   {:sku-id "SRV-TKDU-000" :service-menu-key :specialty-addon-weave-take-down}
   {:sku-id "SRV-DPCU-000" :service-menu-key :specialty-addon-hair-deep-conditioning}
   {:sku-id "SRV-CCU-000" :service-menu-key :specialty-addon-closure-customization}
   {:sku-id "SRV-FCU-000" :service-menu-key :specialty-addon-frontal-customization}
   {:sku-id "SRV-3CU-000" :service-menu-key :specialty-addon-360-frontal-customization}])

(def services-by-sku-id
  (maps/index-by :sku-id service-filter-data))

(defn service-sku-id->service-menu-key
  [sku-id]
  (-> services-by-sku-id
      (get sku-id)
      :service-menu-key))

(def allowed-stylist-filters
  (->> service-filter-data (map :sku-id) set))

(defn stylist-provides-service-by-sku-id?
  [stylist service-sku-id]
  (->> service-sku-id
       service-sku-id->service-menu-key
       (get (:service-menu stylist))
       boolean))

(defn stylist-provides-service?
  [stylist service-product]
  (->> service-product
       :selector/sku-ids
       first
       (stylist-provides-service-by-sku-id? stylist)))
