(ns storefront.accessors.sites
  (:require [storefront.keypaths :as keypaths]))

(defn determine-site
  [app-state]
  (if (or (= "shop" (get-in app-state keypaths/store-slug))
          (= "retail-location" (get-in app-state keypaths/store-experience)))
    :shop
    :classic))
