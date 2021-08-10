(ns catalog.keypaths
  (:require [storefront.keypaths :as keypaths]))

(def ui (conj keypaths/ui :catalog))

(def detailed-product (conj ui :detailed-product))
(def detailed-product-related-addons (conj detailed-product :related-addons))
(def detailed-product-options (conj detailed-product :options))
(def detailed-product-id (conj detailed-product :id))
(def detailed-product-product-skus (conj detailed-product :product-skus))
(def detailed-product-selections (conj detailed-product :selections))
(def detailed-product-selected-sku (conj detailed-product :selected-sku))
(def detailed-product-selected-sku-id (conj detailed-product-selected-sku :catalog/sku-id))
(def detailed-product-selected-picker (conj detailed-product :selected-picker))
(def detailed-product-picker-visible? (conj detailed-product :picker-visible?))

(def detailed-pdp (conj ui :detailed-pdp))
(def detailed-pdp-options (conj detailed-pdp :options))
(def detailed-pdp-selections (conj detailed-pdp :selections))
(def detailed-pdp-skus-db (conj detailed-pdp :skus-db))
(def detailed-pdp-selected-sku (conj detailed-pdp :selected-sku))
(def detailed-pdp-availability (conj detailed-pdp :availability))
(def detailed-pdp-selected-picker (conj detailed-pdp :selected-picker))
(def detailed-pdp-picker-visible? (conj detailed-pdp :picker-visible?))

;; TODO: re-address
(def detailed-look (conj ui :detailed-look))
(def detailed-look-options (conj detailed-look :options))
(def detailed-look-selections (conj detailed-look :selections))

(def detailed-look-skus-db (conj detailed-look :skus-db))
(def detailed-look-availability (conj detailed-look :availability))
(def detailed-look-services (conj detailed-look :services))

(def detailed-look-selected-picker (conj detailed-look :selected-picker))
(def detailed-look-picker-visible? (conj detailed-look :picker-visible?))

(def length-guide-image (conj detailed-product :length-guide-image))

(def category (conj ui :category))
(def category-panel (conj ui :panel))
(def category-id (conj ui :id))
(def category-selections (conj category :selections))

;; Contains the data used to render the category page (ICP)
(def category-query (conj ui :category-query))

(def initial (-> {}
                 (assoc-in detailed-product {})))

(def k-models-facet-filtering
  (conj keypaths/models-root :facet-filtering))

(def k-models-facet-filtering-filters
  (conj k-models-facet-filtering :facet-filtering/filters))

(def k-models-facet-filtering-panel
  (conj k-models-facet-filtering :facet-filtering/panel))

(def k-models-facet-filtering-sections
  (conj k-models-facet-filtering :facet-filtering/sections))
