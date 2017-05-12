(ns storefront.platform.images
  (:require [storefront.components.ui :as ui]))

(defn lqip [image fallback-size]
  (let [{:keys [resizable_url resizable_filename]} image]
    (if resizable_url
      [:picture
       [:img.col-12
        {:src   (str resizable_url "-/resize/50x/-/quality/lighter/" resizable_filename)
         :alt   (:alt image)
         :style {:filter     "blur(10px)"
                 :transition "opacity .35s"
                 :opacity    "1"}}]
       [:img.col-12
        (assoc (ui/img-attrs image fallback-size)
               :style {:transition "opacity .35s"
                       :opacity    "0"})]]
      [:picture [:img.col-12 (ui/img-attrs image fallback-size)]])))
