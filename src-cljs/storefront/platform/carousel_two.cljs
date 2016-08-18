(ns storefront.platform.carousel-two
  (:require [sablono.core :refer-macros [html]]
            [swiper :as swiper]
            [om.core :as om]))

(defn component [{:keys [items]} owner _]
  (reify
    om/IDidMount
    (did-mount [_]
      (js/Swiper. (om/get-ref owner "swiper-container")
                  (clj->js {:slidesPerView 2
                            ;; simulateTouch is the default, but seems to get lost
                            :simulateTouch true
                            :grabCursor    true
                            :loop          true
                            :autoplay      3000
                            ;; swiper is smart enough to default to the
                            ;; next/prev buttons *inside* the container, but if
                            ;; you use ids, it can accomodate buttons outside of
                            ;; the container.
                            :nextButton    ".swiper-button-next"
                            :prevButton    ".swiper-button-prev"})))
    om/IRender
    (render [_]
      (html
       [:div.swiper-container {:ref "swiper-container"}
        [:div.swiper-wrapper items]
        [:div.swiper-button-next]
        [:div.swiper-button-prev]]))))
