(ns storefront.platform.strings)

(defn format [fmt & args]
  (apply goog.string/format fmt args))
