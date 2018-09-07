(ns storefront.components.stylist.pagination
  (:require [storefront.platform.component-utils :as utils]
            [storefront.components.ui :as ui]))

(defn more-pages? [page pages]
  (> (or pages 0) (or page 0)))

(defn fetch-more [event fetching? page pages]
  (if fetching?
    [:.h2 ui/spinner]
    (when (more-pages? page pages)
      [:.col-5.mx-auto.my3
       [:.btn.btn-outline.navy.col-12
        {:on-click (utils/send-event-callback event)}
        "Load More"]])))
