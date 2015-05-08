(ns storefront.events)

(def navigate [:navigation])
(def navigate-home (conj navigate :home))
(def navigate-category (conj navigate :category))

(def api [:api])
(def api-success (conj api :success))

(def api-success-taxons (conj api-success :taxons))
(def api-success-store (conj api-success :store))
(def api-success-category (conj api-success :category))
(def api-success-products (conj api-success :products))
