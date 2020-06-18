(ns homepage.core
  "Homepages are apt to change often, fork and use feature-flags."
  (:require [homepage.unified-v2020-06 :as unified]
            [homepage.shop-v2020-06 :as shop]
            [storefront.accessors.sites :as sites]))

(defn page
  [app-state]
  (if (= :shop (sites/determine-site app-state))
    (shop/page app-state)
    (unified/page app-state)))
