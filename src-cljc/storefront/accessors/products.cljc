(ns storefront.accessors.products
  (:require [storefront.keypaths :as keypaths]))

(defn loaded-ids [data]
  (set (keys (get-in data keypaths/products))))

(def ^:private frontal-product-title [:color :style :material :origin :length (constantly "frontal")])
(def ^:private closure-product-title [:color :style :material :origin :length (constantly "closure")])
(def ^:private bundle-product-title [:color :origin :length :style])

(defn closure? [variant]
  (= "closures" (get-in variant [:variant-attrs :category])))

(defn frontal? [variant]
  (= "frontals" (get-in variant [:variant-attrs :category])))

(defn bundle? [variant]
  (boolean (get-in variant [:variant-attrs :category])))

(defn product-title [{:keys [variant-attrs product-name] :as variant}]
  (let [product-title-fns (cond (closure? variant) closure-product-title
                                (frontal? variant) frontal-product-title
                                (bundle? variant)  bundle-product-title
                                :else [(constantly product-name)])
        strs (filter identity ((apply juxt product-title-fns) variant-attrs))]
    (clojure.string/join " " strs)))

(defn thumbnail-url [products product-id]
  (first (keep (fn [{:keys [type small_url]}]
                 (when (= type "product") small_url))
               (get-in products [product-id :images]))))
