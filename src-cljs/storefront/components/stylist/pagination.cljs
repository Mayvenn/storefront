(ns storefront.components.stylist.pagination
  (:require [storefront.components.utils :as utils]
            [storefront.request-keys :as request-keys]
            [storefront.keypaths :as keypaths]
            [storefront.utils.query :as query]))

(defn more-pages? [page pages]
  (> (or pages 0) (or page 0)))

(defn fetch-more [event fetching? page pages]
  (when (more-pages? page pages)
    [:.col-5.mx-auto.my3
     (if fetching?
       (utils/spinner)
       [:.btn.btn-outline.teal.col-12
        {:on-click (utils/send-event-callback event)}
        "Load More"])]))
