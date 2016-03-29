(ns storefront.components.stylist.pagination
  (:require [storefront.components.utils :as utils]
            [storefront.request-keys :as request-keys]
            [storefront.keypaths :as keypaths]
            [storefront.utils.query :as query]))

(defn at-end? [page pages]
  (> (or pages 0) (or page 0)))

(defn fetch-more [data event request-key page pages]
  (when (at-end? page pages)
    [:.col-5.mx-auto.my3
     (if (query/get {:request-key request-key} (get-in data keypaths/api-requests))
       (utils/spinner)
       [:.btn.btn-outline.teal.col-12
        {:on-click (utils/send-event-callback data event)}
        "Load More"])]))
