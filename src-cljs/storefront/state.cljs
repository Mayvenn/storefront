(ns storefront.state
  (:require [cljs.core.async :refer [chan]]))

(defn initial-state []
  {:event-ch (chan)
   :stop-ch (chan)

   :taxons []
   :products-for-taxons {}

   :ui {:navigation-point :home}})

(def event-ch-path [:event-ch])
(def stop-ch-path [:stop-ch])

(def taxons-path [:taxons])
(def products-for-taxons [:products-for-taxons])

(def ui-path [:ui])
(def navigation-point-path (conj ui-path :navigation-point))
