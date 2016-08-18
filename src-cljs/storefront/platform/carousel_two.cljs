(ns storefront.platform.carousel-two
  (:require [sablono.core :refer-macros [html]]
            [swiper :as swiper]
            [om.core :as om]))

(defn component [{:keys [container-id items]} _ _]
  (reify
    om/IDidMount
    (did-mount [_]
      (js/Swiper. (str "#" container-id)
                  (clj->js {:slidesPerView 2
                            :simulateTouch true
                            :loop          true
                            :autoplay      3000
                            :nextButton    (str "#" container-id " .swiper-button-next")
                            :prevButton    (str "#" container-id " .swiper-button-prev")})))
    om/IRender
    (render [_]
      (html
       [:div.swiper-container {:id container-id}
        [:div.swiper-wrapper items]
        [:div.swiper-button-next]
        [:div.swiper-button-prev]]))))
