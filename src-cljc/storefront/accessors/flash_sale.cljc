(ns storefront.accessors.flash-sale
  (:require [spice.date :as date]
            [storefront.accessors.experiments :as experiments]))

(def flash-sale-start (date/date-time 2017 12 13 5))
(def flash-sale-end   (date/date-time 2017 12 15 5))

(defn active? []
  (and (date/after? (date/now) flash-sale-start)
       (date/after? flash-sale-end (date/now))))
