(ns stylist-matching.search.accessors.filters
  (:require [spice.maps :as maps]))

(def service-filter-data
  [{:sku-id "SRV-LBI-000"  :service-menu-key :specialty-sew-in-leave-out}
   {:sku-id "SRV-CBI-000"  :service-menu-key :specialty-sew-in-closure}
   {:sku-id "SRV-FBI-000"  :service-menu-key :specialty-sew-in-frontal}
   {:sku-id "SRV-3BI-000"  :service-menu-key :specialty-sew-in-360-frontal}
   {:sku-id "SRV-WGC-000"  :service-menu-key :specialty-wig-customization}
   {:sku-id "SRV-SPBI-000" :service-menu-key :specialty-silk-press}
   {:sku-id "SRV-WMBI-000" :service-menu-key :specialty-weave-maintenance}
   {:sku-id "SRV-WIBI-000" :service-menu-key :specialty-wig-install}
   {:sku-id "SRV-BDBI-000" :service-menu-key :specialty-braid-down}])

(def services-by-sku-id
  (maps/index-by :sku-id service-filter-data))

(defn service-sku-id->service-menu-key
  [sku-id]
  (-> services-by-sku-id
      (get sku-id)
      :service-menu-key))

(def allowed-stylist-filters
  (->> service-filter-data (map :sku-id) set))

(defn stylist-provides-service
  [stylist service-product]
  (->> service-product
       :selector/sku-ids
       first
       service-sku-id->service-menu-key
       (get (:service-menu stylist))
       boolean))
