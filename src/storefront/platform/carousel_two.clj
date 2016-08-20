(ns storefront.platform.carousel-two
  (:require [storefront.component-shim :as component]))

(defn component [{:keys [slides]} _ _]
  (component/create
   [:div.overflow-hidden.nowrap
    (for [slide slides]
      [:div.inline-block slide])]))
