(ns storefront.platform.images
  (:require [storefront.components.ui :as ui]))

(defn lqip [image fallback-size]
  (let [{:keys [resizable_url resizable_filename]} image]
    (if resizable_url
      (let [lqip-url (str resizable_url "-/resize/50x/-/quality/lighter/" resizable_filename)]
        [:picture.bg-cover {:style {:background-image lqip-url}}
         [:img.col-12
          {:src   lqip-url
           :alt   (:alt {:keys [resizable_url resizable_filename]})
           :style {:filter     "blur(10px)"
                   :transition "opacity .35s"
                   :opacity    "0"}}]
         [:img.col-12
          (assoc (ui/img-attrs {:keys [resizable_url resizable_filename]} fallback-size)
                 :style {:transition "opacity .35s"
                         :opacity    "1"})]])
      [:picture [:img.col-12 (ui/img-attrs {:keys [resizable_url resizable_filename]} fallback-size)]])))
