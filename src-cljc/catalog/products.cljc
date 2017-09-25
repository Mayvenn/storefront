(ns catalog.products
  (:require [catalog.keypaths :as k]
            [spice.maps :as maps]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.transitions :refer [transition-state]]
            [clojure.set :as set]))

(defn product-by-id [app-state product-id]
  (get-in app-state (conj keypaths/sku-sets product-id)))

(def ^:private id->named-search
  {"74" "360-frontals"
   "75" "360-frontals"
   "76" "360-frontals"
   "77" "360-frontals"
   "78" "360-frontals"
   "79" "360-frontals"
   "15" "body-wave"
   "8"  "body-wave"
   "0"  "closures"
   "11" "closures"
   "13" "closures"
   "16" "closures"
   "17" "closures"
   "18" "closures"
   "19" "closures"
   "20" "closures"
   "21" "closures"
   "23" "closures"
   "25" "closures"
   "3"  "closures"
   "31" "closures"
   "32" "closures"
   "33" "closures"
   "34" "closures"
   "35" "closures"
   "36" "closures"
   "4"  "closures"
   "5"  "closures"
   "52" "closures"
   "53" "closures"
   "56" "closures"
   "57" "closures"
   "6"  "closures"
   "60" "closures"
   "64" "closures"
   "67" "closures"
   "7"  "closures"
   "70" "closures"
   "73" "closures"
   "28" "curly"
   "29" "curly"
   "10" "deep-wave"
   "30" "deep-wave"
   "61" "deep-wave"
   "1"  "frontals"
   "39" "frontals"
   "40" "frontals"
   "41" "frontals"
   "42" "frontals"
   "43" "frontals"
   "44" "frontals"
   "45" "frontals"
   "46" "frontals"
   "47" "frontals"
   "48" "frontals"
   "50" "frontals"
   "54" "frontals"
   "58" "frontals"
   "59" "frontals"
   "63" "frontals"
   "66" "frontals"
   "69" "frontals"
   "72" "frontals"
   "51" "kinky-straight"
   "55" "kinky-straight"
   "2"  "loose-wave"
   "22" "loose-wave"
   "26" "loose-wave"
   "12" "straight"
   "37" "straight"
   "9"  "straight"
   "62" "water-wave"
   "65" "water-wave"
   "68" "yaki-straight"
   "71" "yaki-straight"
   "80" "wigs"
   "81" "wigs"
   "82" "wigs"
   "83" "wigs"
   "84" "wigs"
   "85" "wigs"
   "86" "wigs"
   "87" "wigs"})

(defn is-hair?
  [skuer]
  (some-> skuer :product/department (contains? "hair")))

(defn stylist-only?
  [skuer]
  (some-> skuer :product/department (contains? "stylist-exclusives")))

(def eligible-for-reviews? (complement stylist-only?))

(defn eligible-for-triple-bundle-discount?
  [skuer]
  (or (:promo.eligible/triple-bundle skuer)
      is-hair?))

(defn normalize-skus [skus]
  (maps/index-by :sku
                 (sequence (comp
                            (map #(assoc % :id (:slug %)))
                            (map #(merge % (:attributes %)))
                            (map #(dissoc % :attributes :images)))
                           skus)))

(defn index-sku-sets [sku-sets f]
  "Reshape sku-sets by indexing by :sku-set/id and
   converting :criteria/selectors (which are strings) to keywords"
  (maps/index-by :sku-set/id (map f sku-sets)))

(defn normalize-sku-sets [sku-sets]
  "Do all things necessary to transform a coll of sku-sets into the shape most
   desirable to work with."
  (index-sku-sets sku-sets
                  (fn [sku-set]
                    (update sku-set :criteria/selectors
                            (partial mapv keyword)))))

(defmethod transition-state events/api-success-sku-sets
  [_ event {:keys [sku-sets skus] :as response} app-state]
  (-> app-state
      (update-in keypaths/db-images set/union
                 (set (sequence (comp (mapcat :sku-set/images)
                                      (map #(assoc % :id (str (:use-case %) "-" (:url %))))
                                      (map #(assoc % :order (or (:order %)
                                                                (case (:image/of (:criteria/attributes %))
                                                                  "model" 1
                                                                  "product" 2
                                                                  "seo" 3
                                                                  "catalog" 4
                                                                  5))))
                                      (map #(merge % (:criteria/attributes %)))
                                      (map #(dissoc % :criteria/attributes :filename)))
                                sku-sets)))
      (update-in keypaths/sku-sets merge (normalize-sku-sets sku-sets))
      (update-in keypaths/skus merge (normalize-skus skus))))

(defn ->skuer-schema
  "Reshapes product skuer to v1"
  [product]
  "keys to dealt with in the future:
   :sku-set/seo
   :sku-set/copy
   :sku-set/images
   :sku-set/name"
  (let [essentials (maps/map-values first (:criteria/essential product))]
    (-> product
        (dissoc :criteria/essential)
        (clojure.set/rename-keys
         {:sku-set/id          :catalog/product-id
          :sku-set/launched-at :catalog/launched-at
          :sku-set/skus        :selector/skus
          :sku-set/slug        :page/slug
          :criteria/selectors  :selector/electives})
        (merge essentials)
        (assoc :legacy/named-search-slug (id->named-search (:sku-set/id product)))
        (assoc :selector/essentials (keys essentials)))))

;; TODO move to product details
(defn current-product [app-state]
  (->> (get-in app-state k/detailed-product-id)
       (product-by-id app-state)
       ->skuer-schema))
