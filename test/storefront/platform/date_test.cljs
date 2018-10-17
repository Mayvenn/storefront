(ns storefront.platform.date-test
  (:require [clojure.test.check :as tcheck]
            [clojure.spec.alpha :as s]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop :include-macros true]
            [spice.date]
            [storefront.platform.date :as date]))

(s/def ::year (s/with-gen pos-int? #(gen/choose 1980 2050)))
(s/def ::month (s/with-gen pos-int? #(gen/choose 1 12)))
(s/def ::day (s/with-gen pos-int? #(gen/choose 1 31)))
(s/def ::hours (s/with-gen pos-int? #(gen/choose 0 23)))
(s/def ::minutes (s/with-gen pos-int? #(gen/choose 0 59)))

(s/def ::date
  (s/with-gen js/Date
    #(let [ps (mapv s/gen [::year ::month ::day ::hours ::minutes])]
       (gen/return (apply spice.date/date-time
                          (map gen/generate ps))))))

(defn week-mod [date]
  (inc (mod (dec (.getDay date))
            5)))

(defn ends-on-day-that-makes-sense?
  [start-date result-date ndays]
  (let [start-day (.getDay start-date)
        end-day   (.getDay result-date)]
    (= (inc (mod (dec end-day) 5))
       (inc (mod (+ ndays (if (not= 6 start-day)
                            (dec start-day)
                            0)) 5)))))

(def add-business-days-test-prop
  ;; check that `add-business-days` always lands on a weekday and a correct day of week
  (prop/for-all
    [start-date (s/gen ::date)
     ndays (gen/choose 3 20)]
    (let [result-date (date/add-business-days start-date ndays)]
      (and (date/weekday? result-date)
           (ends-on-day-that-makes-sense? start-date result-date ndays)))))

(tcheck/quick-check 100 add-business-days-test-prop)
