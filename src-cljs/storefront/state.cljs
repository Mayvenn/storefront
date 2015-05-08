(ns storefront.state
  (:require [cljs.core.async :refer [chan]]
            [storefront.events :as events]
            [clojure.string :as string])
  (:import [goog.history Html5History]))

(defn get-store-subdomain []
  (first (string/split (.-hostname js/location) #"\.")))

(defn initial-state []
  {:event-ch (chan)
   :stop-ch (chan)

   :history nil
   :routes []

   :store {:store_slug (get-store-subdomain)}
   :taxons []
   :products-for-taxons {}

   :ui {:navigation-event events/navigate-home
        :browse-taxon nil
        :menu-expanded false}})

(def event-ch-path [:event-ch])
(def stop-ch-path [:stop-ch])

(def history-path [:history])
(def routes-path [:routes])

(def store-path [:store])
(def store-slug-path (conj store-path :store_slug))

(def taxons-path [:taxons])
(def products-for-taxons-path [:products-for-taxons])

(def ui-path [:ui])
(def navigation-event-path (conj ui-path :navigation-event))
(def browse-taxon-path (conj ui-path :browse-taxon))
(def menu-expanded-path (conj ui-path :menu-expanded))
