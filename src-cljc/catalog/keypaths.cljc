(ns catalog.keypaths
  (:require [storefront.keypaths :as keypaths]))

(def ui (conj keypaths/ui :catalog))

(def detailed-pdp (conj ui :detailed-pdp))
(def detailed-pdp-product-id (conj detailed-pdp :product-id))
(def detailed-pdp-options (conj detailed-pdp :options))
(def detailed-pdp-selections (conj detailed-pdp :selections))
(def detailed-pdp-skus-db (conj detailed-pdp :skus-db))
(def detailed-pdp-selected-sku (conj detailed-pdp :selected-sku))
(def detailed-pdp-availability (conj detailed-pdp :availability))
(def detailed-pdp-selected-picker (conj detailed-pdp :selected-picker))
(def detailed-pdp-picker-visible? (conj detailed-pdp :picker-visible?))

(def length-guide-image (conj detailed-pdp :length-guide-image))

;; TODO: re-address
(def detailed-look (conj ui :detailed-look))
(def detailed-look-options (conj detailed-look :options))
(def detailed-look-selections (conj detailed-look :selections))

(def detailed-look-skus-db (conj detailed-look :skus-db))
(def detailed-look-availability (conj detailed-look :availability))
(def detailed-look-services (conj detailed-look :services))

(def detailed-look-selected-picker (conj detailed-look :selected-picker))
(def detailed-look-picker-visible? (conj detailed-look :picker-visible?))


(def category (conj ui :category))
(def category-panel (conj ui :panel))
(def category-id (conj ui :id))
(def category-selections (conj category :selections))

;; Contains the data used to render the category page (ICP)
(def category-query (conj ui :category-query))

(def k-models-facet-filtering
  (conj keypaths/models-root :facet-filtering))

(def k-models-facet-filtering-filters
  (conj k-models-facet-filtering :facet-filtering/filters))

(def k-models-facet-filtering-panel
  (conj k-models-facet-filtering :facet-filtering/panel))

(def k-models-facet-filtering-sections
  (conj k-models-facet-filtering :facet-filtering/sections))
