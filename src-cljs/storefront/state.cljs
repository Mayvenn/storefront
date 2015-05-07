(ns storefront.state
  (:require [cljs.core.async :refer [chan]]
            [storefront.events :as events])
  (:import [goog.history Html5History]))

(defn initial-state []
  {:event-ch (chan)
   :stop-ch (chan)

   :history nil
   :routes []

   :taxons []
   :products-for-taxons {}

   :ui {:navigation-event events/navigate-home}})

(def event-ch-path [:event-ch])
(def stop-ch-path [:stop-ch])

(def history-path [:history])
(def routes-path [:routes])

(def taxons-path [:taxons])
(def products-for-taxons [:products-for-taxons])

(def ui-path [:ui])
(def navigation-event-path (conj ui-path :navigation-event))
