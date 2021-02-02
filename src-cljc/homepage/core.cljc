(ns homepage.core
  "Homepages are apt to change often; fork and use feature-flags."
  (:require #?(:cljs [storefront.loader :as loader])
            [homepage.classic-v2020-07 :as classic]
            [homepage.shop-v2020-07 :as shop]
            [mayvenn-install.about :as about]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.sites :as sites]))

(defn ^:export page
  [app-state _]
  (cond
    (not= :shop (sites/determine-site app-state))
    (classic/page app-state)

    (experiments/homepage-revert? app-state)
    (about/built-component app-state {})

    :else
    (shop/page app-state)))

#?(:cljs (loader/set-loaded! :homepage))
