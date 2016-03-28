(ns storefront.components.carousel
  (:require [storefront.events :as events]
            [storefront.components.utils :as utils]
            [storefront.keypaths :as keypaths]
            [storefront.request-keys :as request-keys]
            [storefront.utils.query :as query]
            [sablono.core :refer-macros [html]]
            [om.core :as om]))

(defn css-url [url] (str "url(" url ")"))

(defn carousel-component [data owner {:keys [index-path images-path]}]
  (om/component
   (html
    [:.carousel-component
     (let [idx (get-in data index-path)
           images (get-in data images-path [])]
       (list
        [:.hair-category-image {:style {:background-image (css-url (get images idx))}}]
        [:.left {:on-click
                 (utils/send-event-callback data
                                            events/control-carousel-move
                                            {:index-path index-path
                                             :index (mod (dec idx) (count images))})}]
        [:.right {:on-click
                  (utils/send-event-callback data
                                             events/control-carousel-move
                                             {:index-path index-path
                                              :index (mod (inc idx) (count images))})}]))])))
