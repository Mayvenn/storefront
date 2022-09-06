(ns homepage.core
  "Homepages are apt to change often; fork and use feature-flags."
  (:require #?(:cljs [storefront.loader :as loader])
            [homepage.classic-v2020-07 :as classic]
            [homepage.shop-v2021-03 :as shop]
            [homepage.shop-v2022-09 :as shop-22]
            [storefront.accessors.sites :as sites]))

(defn ^:export page
  [app-state _]
  (if (not= :shop (sites/determine-site app-state))
    (classic/page app-state)
    (shop/page app-state)))

#?(:cljs (loader/set-loaded! :homepage))
