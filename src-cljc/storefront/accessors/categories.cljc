(ns storefront.accessors.categories
  (:require [storefront.utils.query :as query]
            [storefront.accessors.products :as products]
            [storefront.accessors.experiments :as experiments]
            [storefront.request-keys :as request-keys]
            [storefront.keypaths :as keypaths]))

(def named-search->category
  {"closures" {:category-id 0
               :category-slug "closures"}})
