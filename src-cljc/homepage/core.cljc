(ns homepage.core
  "Homepages are apt to change often, fork and use feature-flags."
  (:require #?(:cljs [storefront.loader :as loader])
            [homepage.classic-v2020-07 :as classic]
            [homepage.shop-v2020-07 :as shop]
            [storefront.accessors.sites :as sites]))

(defn ^:export page
  [app-state _]
  (if (= :shop (sites/determine-site app-state))
    (shop/page app-state)
    (classic/page app-state)))

#?(:cljs (loader/set-loaded! :homepage))
