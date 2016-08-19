(ns storefront.platform.carousel-two
  (:require [sablono.core :refer-macros [html]]
            react-slick
            [om.core :as om]))

(defn component [{:keys [items]} owner _]
  (om/component
   (js/React.createElement js/Slider
                           (clj->js {:autoplay true
                                     :slidesToShow 2
                                     :pauseOnHover true
                                     :arrows true
                                     :draggable true})
                           (html items))))
