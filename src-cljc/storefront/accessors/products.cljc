(ns storefront.accessors.products
  (:require [storefront.keypaths :as keypaths]
            [storefront.utils.query :as query]))

(defn loaded-ids [data]
  (set (keys (get-in data keypaths/products))))

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

(defn old-medium-img [products product-id]
  (product-img-with-size (get products product-id) :product))

(defn large-img [products product-id]
  (product-img-with-size (get products product-id) :large))
