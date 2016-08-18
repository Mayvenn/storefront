(ns storefront.platform.carousel-two
  (:require [storefront.component-shim :as component]))

(defn component [{:keys [items]} _ _]
  (component/create
   [:div "TODO: server-side carousel rendering"]))
