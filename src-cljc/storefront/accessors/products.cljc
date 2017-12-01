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

(defn old-large-img [products product-id]
  (product-img-with-size (get products product-id) :large))

(defn find-product-by-sku-id [products line-item-sku]
  (->> (vals products)
       (filter (fn [product]
                 (contains? (set (:selector/skus product))
                            line-item-sku)))
       first))

(defn medium-img [product sku]
  (let [image  (->> product
                    :selector/images
                    (filter #((:hair/color sku) (:hair/color %)))
                    (filter #(or
                              (= (:image/of %) "product")
                              (= (:use-case %) "cart")))
                    (sort-by #(case (:use-case %)
                                "cart" 0
                                "catalog" 1
                                5))
                    first)]
    {:src (:url image)
     :alt (:copy/title product)}))

(defn large-img [product sku]
  ;;TODO fix this!!! PLEASE!!! (should be using selector and doing something more clever than this.)
  (let [image  (->> product
                    :selector/images
                    (filter #((:hair/color sku) (:hair/color %)))
                    #_(filter #(or
                              (= (:image/of (:criteria/attributes %)) "product")
                              ;; FIXME Please remove once we add cart use cases to all images
                              (= (:use-case %) "cart")))
                    (filter #(= (:use-case %) "carousel"))
                    first)]
    {:src (:url image)
     :alt (:copy/title product)}))
