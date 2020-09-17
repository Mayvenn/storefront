(ns storefront.accessors.sites
  (:require [storefront.keypaths :as keypaths]))

(defn determine-site
  [app-state]
  (if (= "shop" (get-in app-state keypaths/store-slug))
    :shop
    :classic))
