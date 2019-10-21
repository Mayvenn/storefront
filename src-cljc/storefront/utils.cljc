(ns storefront.utils)

;; TODO: move to spice
(defn ?update
  "Don't apply the update if the key doesn't exist. Prevents keys being added
  when they shouldn't be"
  [m k & args]
  (if (k m)
    (apply update m k args)
    m))
