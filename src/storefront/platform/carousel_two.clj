(ns storefront.platform.carousel-two
  (:require [storefront.component-shim :as component]))

(defn component [{:keys [slides]} _ _]
  (component/create
   [:div.center.relative
    [:div.overflow-hidden.relative
     {:ref "items"}
     [:div.overflow-hidden.relative
      (for [i (range (count slides))
            :let [slide (nth slides i)]]
        [:div.left.col-12.relative
         (merge {:key (str i)}
                (when (> i 0)
                  {:class "display-none"}))
         slide])]]]))
