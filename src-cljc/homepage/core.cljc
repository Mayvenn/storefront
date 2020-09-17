(ns homepage.core
  "Homepages are apt to change often, fork and use feature-flags."
  (:require [homepage.classic-v2020-07 :as classic]
            [homepage.shop-v2020-07 :as shop]
            [storefront.accessors.sites :as sites]))

(defn page
  [app-state]
  (if (= :shop (sites/determine-site app-state))
    (shop/page app-state)
    (classic/page app-state)))
