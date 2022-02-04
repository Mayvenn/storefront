(ns stylist-matching.search.accessors.filters
  (:require [spice.maps :as maps]))

(def free-install-filter-data
  [{:sku-id           "SRV-LBI-000"
    :service-menu-key :specialty-sew-in-leave-out
    :price            0
    :title            "Leave Out Install"}
   {:sku-id           "SRV-CBI-000"
    :price            0
    :service-menu-key :specialty-sew-in-closure
    :title            "Closure Install"}
   {:sku-id           "SRV-FBI-000"
    :price            0
    :service-menu-key :specialty-sew-in-frontal
    :title            "Frontal Install"}
   {:sku-id           "SRV-3BI-000"
    :price            0
    :title            "360 Frontal Install"
    :service-menu-key :specialty-sew-in-360-frontal}])

(def addon-filter-data
  [{:sku-id           "SRV-TRMU-000"
    :price            1
    :title            "Natural Hair Trim"
    :service-menu-key :specialty-addon-natural-hair-trim}
   {:sku-id           "SRV-TKDU-000"
    :price            1
    :title            "Weave Take Down"
    :service-menu-key :specialty-addon-weave-take-down}
   {:sku-id           "SRV-DPCU-000"
    :price            1
    :title            "Hair Deep Conditioning"
    :service-menu-key :specialty-addon-hair-deep-conditioning}
   {:sku-id           "SRV-CCU-000"
    :price            1
    :title            "Closure Customization"
    :service-menu-key :specialty-addon-closure-customization}
   {:sku-id           "SRV-FCU-000"
    :price            1
    :title            "Frontal Customization"
    :service-menu-key :specialty-addon-frontal-customization}
   {:sku-id           "SRV-3CU-000"
    :price            1
    :title            "360 Frontal Customization"
    :service-menu-key :specialty-addon-360-frontal-customization}])

(def service-filter-data
  (concat free-install-filter-data addon-filter-data))

(def services-by-sku-id
  (maps/index-by :sku-id service-filter-data))

(def services-by-menu-key
  (maps/index-by :service-menu-key service-filter-data))

(defn service-sku-id->service-menu-key
  [sku-id]
  (-> services-by-sku-id
      (get sku-id)
      :service-menu-key))

(defn service-menu-key->title
  [menu-key]
  (-> services-by-menu-key
      (get (keyword menu-key))
      :title))

(defn service-menu-key->addon?
  [menu-key]
  (->> addon-filter-data
       (filter #(= menu-key (name (:service-menu-key %))))
       not-empty))

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
