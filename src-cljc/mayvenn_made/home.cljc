(ns mayvenn-made.home
  (:require [storefront.component :as component]
            [storefront.effects :as effects]
            [storefront.events :as events]))

(defn component [data opts]
  (component/create
   [:div "HERE is a HERO"]))

(defn query [data]
  {})

(defn built-component [data opts]
  (component/build component (query data) opts))

(defmethod effects/perform-effects events/navigate-mayvenn-made
  [dispatch event args prev-app-state app-state])
