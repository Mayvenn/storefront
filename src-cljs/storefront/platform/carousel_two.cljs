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
                                     :draggable true
                                     :responsive [{:breakpoint 640
                                                   :settings {:slidesToShow 2} }
                                                  {:breakpoint 768
                                                   :settings {:slidesToShow 3} }
                                                  {:breakpoint 1024
                                                   :settings {:slidesToShow 5} }
                                                  {:breakpoint 100000
                                                   :settings {:slidesToShow 7
                                                              :autoplay false
                                                              :arrows false}}]})
                           (html items))))
