(ns storefront.components.category
  (:require [storefront.components.utils :as utils]
            [storefront.components.stylist-kit :as stylist-kit]
            [storefront.components.formatters :refer [as-money]]
            [storefront.accessors.products :as products]
            [storefront.accessors.promos :as promos]
            [storefront.accessors.taxons :as taxons]
            [storefront.components.reviews :as reviews]
            [storefront.components.carousel :refer [carousel-component]]
            [storefront.components.bundle-builder :as bundle-builder]
            [storefront.hooks.experiments :as experiments]
            [clojure.string :as string]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.accessors.bundle-builder :as accessors.bundle-builder]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.request-keys :as request-keys]))

;; TODO: move this to bundle bundler directory

(defn category-component [data owner]
  (om/component
   (html
    (if (accessors.bundle-builder/included-taxon? (taxons/current-taxon data))
      (bundle-builder/built-component data)
      (stylist-kit/built-component data)))))
