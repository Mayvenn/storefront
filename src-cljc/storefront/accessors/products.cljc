(ns storefront.accessors.products
  (:require [storefront.keypaths :as keypaths]
            [storefront.utils.query :as query]))

(defn loaded-ids [data]
  (set (keys (get-in data keypaths/v2-products))))

(defn product-title
  "Prefer variant-name, if available. Otherwise use product name (product-name
  from waiter line item; name from cellar variant)"
  [{:keys [variant-name product-name name]}]
  (or variant-name product-name name))

(defn product-img-with-size [product size]
  (when product
    (let [size-url (keyword (str (name size) "_url"))
          img      (query/get
                    {:type                   "product"
                     (comp boolean size-url) true}
                    (get product :images))]
      {:src (size-url img)
       :alt (:name product)})))

(defn small-img [products product-id]
  (product-img-with-size (get products product-id) :small))

(defn old-large-img [products product-id]
  (product-img-with-size (get products product-id) :large))

(defn find-product-by-sku-id [products line-item-sku]
  (->> (vals products)
       (filter (fn [product]
                 (contains? (set (:selector/skus product))
                            line-item-sku)))
       first))

(defn image-by-use-case [use-case skuer]
  ;;TODO fix this!!! PLEASE!!! (should be using selector and doing something more clever than this.)
  (let [image (->> skuer
                   :selector/images
                   (filter #(= (:use-case %) use-case))
                   first)]
    {:src (:url image)
     :alt (:copy/title skuer)}))

(defn medium-img [skuer]
  (image-by-use-case "cart" skuer))

(defn large-img [skuer]
  (image-by-use-case "carousel" skuer))
