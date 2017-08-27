(ns catalog.products
  (:require [storefront.keypaths :as keypaths]
            [clojure.string :as string]
            [storefront.utils.maps :as maps]
            [storefront.accessors.category-filters :as category-filters]))

(defn sku-set-by-id [app-state sku-set-id]
  (get-in app-state (conj keypaths/sku-sets sku-set-id)))

(defn current-sku-set [app-state]
  (sku-set-by-id app-state (get-in app-state keypaths/product-details-sku-set-id)))

(def id->named-search
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
   "71" "yaki-straight"})

(defn is-hair? [sku-set]
  (some-> sku-set :criteria/essential :product/department (contains? "hair")))
(defn stylist-only? [sku-set] (some-> sku-set :criteria/essential :product/department (contains? "stylist-exclusives")))
(def eligible-for-reviews? (complement stylist-only?))
(def eligible-for-triple-bundle-discount? is-hair?)
