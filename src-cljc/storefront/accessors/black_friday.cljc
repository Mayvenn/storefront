(ns storefront.accessors.black-friday
  (:require [spice.date :as date]
            [storefront.accessors.experiments :as experiments]))

(def black-friday-start (date/date-time 2017 11 24 5))
(def cyber-monday-start (date/date-time 2017 11 27 5))

;; TODO replace when spice is updated
(def date-after?
  #?(:clj date/after?
     :cljs (fn [this that]
             (> (date/to-millis this)
                (date/to-millis that)))))

(defn stage [data]
  (let [now (date/now)]
    (cond
      (or (date-after? now cyber-monday-start)
          (experiments/cyber-monday? data))
      :cyber-monday

      (or (date-after? now black-friday-start)
          (experiments/black-friday? data))
      :black-friday

      (experiments/black-friday-run-up? data)
      :black-friday-run-up

      :else nil)))
