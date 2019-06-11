(ns storefront.loader
  (:require [cljs.loader :as loader]
            [storefront.config :as config]))

(defn set-loaded! [module-name]
  (when config/enable-loader?
    (loader/set-loaded! module-name)))

(defn loaded? [module-name]
  (if config/enable-loader?
    (loader/loaded? module-name)
    true))

(defn load [module-name completed-fn]
  (when config/enable-loader?
    (loader/load module-name completed-fn)))
