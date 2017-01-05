(ns storefront.accessors.products
  (:require [storefront.keypaths :as keypaths]
            [storefront.utils.query :as query]))

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

(defn product-img-with-size [product size]
  (let [size-url (keyword (str (name size) "_url"))
        img      (query/get
                  {:type                   "product"
                   (comp boolean size-url) true}
                  (get product :images))]
    {:src (size-url img)
     :alt (:name product)}))

(defn thumbnail-img [products product-id]
  (product-img-with-size (get products product-id) :small))

(defn closeup-img [products product-id]
  (product-img-with-size (get products product-id) :large))
