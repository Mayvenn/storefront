(ns storefront.accessors.bundle-builder)

(defn- bundle-builder-included-stylist-only [stylist-only-map]
  (not (:stylist_only? stylist-only-map)))

(def included-product? bundle-builder-included-stylist-only)

(def included-taxon? bundle-builder-included-stylist-only)
