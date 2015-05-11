(ns storefront.taxons
  (:require [clojure.string :as string]
            [storefront.state :as state]))

(defn taxon-path-for [taxon]
  (string/replace (:name taxon) #" " "-"))

(defn taxon-name-from [taxon-path]
  (string/replace taxon-path #"-" " "))

(defn default-taxon-path [app-state]
  (when-let [default-taxon (first (get-in app-state state/taxons-path))]
    (taxon-path-for default-taxon)))
