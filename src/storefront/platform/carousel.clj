(ns storefront.platform.carousel
  (:require [storefront.component :as component]))

(defn component [_ _ {:keys [slides mode]}]
  (component/create
   ;; NOTE: We're naming the mode as multi or not. Maybe it should be more open
   ;; ended than this and take the classes directly
   (let [col-class (case mode
                     :multi "col-4"
                     "col-12")]
     [:div.overflow-hidden.nowrap
      (for [slide slides]
        [:div.inline-block {:class col-class} slide])])))
