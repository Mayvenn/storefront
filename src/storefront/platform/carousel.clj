(ns storefront.platform.carousel
  (:require [storefront.component :as component]))

(defn component [{:keys [slides]} _ _]
  (component/create
   [:div.overflow-hidden.nowrap
    (for [slide slides]
      [:div.col-12.inline-block slide])]))
