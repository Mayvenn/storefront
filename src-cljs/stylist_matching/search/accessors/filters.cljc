(ns stylist-matching.search.accessors.filters)

(def service-filter-data
  (concat
   [{:sku-id "SRV-LBI-000" :query-parameter-value "leave-out"}
    {:sku-id "SRV-CBI-000" :query-parameter-value "closure"}
    {:sku-id "SRV-FBI-000" :query-parameter-value "frontal"}
    {:sku-id "SRV-3BI-000" :query-parameter-value "360-frontal"}
    {:sku-id "SRV-WGC-000" :query-parameter-value "wig-customization"}]
   (mapv (fn [s] {:sku-id s :query-parameter-value s})
         ["SRV-SPBI-000" "SRV-WMBI-000" "SRV-WIBI-000" "SRV-BDBI-000"])))

(def service-sku-id->query-parameter-value
  (reduce #(assoc %1 (:sku-id %2) (:query-parameter-value %2)) {} service-filter-data))

(def allowed-stylist-filters
  (->> service-filter-data (map (comp keyword :query-parameter-value)) set))
