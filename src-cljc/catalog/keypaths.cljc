(ns catalog.keypaths
  (:require [storefront.keypaths :as keypaths]))

(def ui (conj keypaths/ui :catalog))

(def detailed-product (conj ui :detailed-product))
(def detailed-product-options (conj detailed-product :options))
(def detailed-product-id (conj detailed-product :id))
(def detailed-product-product-skus (conj detailed-product :product-skus))
(def detailed-product-selections (conj detailed-product :selections))
(def detailed-product-selected-sku (conj detailed-product :selected-sku))
(def detailed-product-selected-sku-id (conj detailed-product-selected-sku :catalog/sku-id))

(def detailed-product-selected-picker
  (conj detailed-product :selected-picker))

(def category (conj ui :category))
(def category-panel (conj ui :panel))
(def category-id (conj ui :id))
(def category-selections (conj category :selections))

;; Contains the data used to render the category page (ICP)
(def category-query (conj ui :category-ui))

(def initial (-> {}
                 (assoc-in detailed-product {})))
