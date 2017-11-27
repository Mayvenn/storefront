(ns storefront.accessors.black-friday
  (:require [spice.date :as date]
            [storefront.accessors.experiments :as experiments]))

(def black-friday-start (date/date-time 2017 11 24 5))
(def cyber-monday-start (date/date-time 2017 11 27 5))
(def cyber-monday-extended (date/date-time 2017 11 28 5))

(defn stage [data]
  (let [now (date/now)]
    (cond
      (date/after? now cyber-monday-extended)
      :cyber-monday-extended

      (date/after? now cyber-monday-start)
      :cyber-monday

      (date/after? now black-friday-start)
      :black-friday

      :else nil)))
