(ns storefront.platform.strings
  (:require [goog.string]))

(defn format [fmt & args]
  (apply goog.string/format fmt args))

