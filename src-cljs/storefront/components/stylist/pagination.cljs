(ns storefront.components.stylist.pagination
  (:require [storefront.components.utils :as utils]))

(defn fetch-more [data event page pages]
  (when (> (or pages 0) (or page 0))
    [:.col-5.mx-auto.my3
     [:.btn.btn-outline.teal.col-12
      {:on-click (utils/send-event-callback data event)}
      "Load More"]]))
