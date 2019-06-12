(ns storefront.loader
  (:require [cljs.loader :as loader]
            [storefront.config :as config]))

(defn set-loaded! [module-name]
  (loader/set-loaded! module-name))

(defn loaded? [module-name]
  (loader/loaded? module-name))

(defn load [module-name completed-fn]
  (loader/load module-name completed-fn))
