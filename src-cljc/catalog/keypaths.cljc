(ns catalog.keypaths
  (:require [storefront.keypaths :as keypaths]))

(def ui (conj keypaths/ui :catalog))

(def detailed-product (conj ui :detailed-product))
(def detailed-product-id (conj detailed-product :id))
(def detailed-product-selected-sku (conj detailed-product :selected-sku))
(def detailed-product-selected-sku-id (conj detailed-product-selected-sku :sku))

(def affirm-product-card-refresh-timeout (concat ui [:affirm :product-card :refresh :timeout] ))

(def initial (-> {}
                 (assoc-in detailed-product {})))
