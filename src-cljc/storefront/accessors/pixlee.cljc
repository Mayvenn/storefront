(ns storefront.accessors.pixlee)

(def named-search-slug->sku
  {"straight"   "NSH"
   "loose-wave" "LWH"
   "body-wave"  "BWH"
   "deep-wave"  "DWH"
   "curly"      "CUR"
   "closures"   "CLO"
   "frontals"   "FRO"})

(defn sku [{:keys [slug]}]
  (named-search-slug->sku slug))

(defn content-available? [named-search]
  (boolean (sku named-search)))
