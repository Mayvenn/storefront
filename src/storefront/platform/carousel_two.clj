(ns storefront.platform.carousel-two
  (:require [storefront.component-shim :as component]))

(defn component [{:keys [items]} _ _]
  (component/create
   [:div.overflow-hidden.nowrap
    (for [item items]
      [:div.inline-block item])]))
