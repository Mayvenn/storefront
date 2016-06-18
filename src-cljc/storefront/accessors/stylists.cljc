(ns storefront.accessors.stylists
  (:require [storefront.keypaths :as keypaths]))

(defn own-store? [data]
  (= (get-in data keypaths/user-store-slug)
     (get-in data keypaths/store-slug)))
