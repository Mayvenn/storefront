(ns storefront.accessors.pixlee)

(def taxon-slug->sku
  {"straight"   "NSH"
   "loose-wave" "LWH"
   "body-wave"  "BWH"
   "deep-wave"  "DWH"
   "curly"      "CUR"
   "closures"   "CLO"
   "frontals"   "FRO"})

(defn sku [{:keys [slug]}]
  (taxon-slug->sku slug))

(defn content-available? [taxon]
  (boolean (sku taxon)))
