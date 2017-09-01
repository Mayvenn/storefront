(ns leads.call-slot
  (:require [clojure.string :as string]))

(def ^:private tz->simple-tz
  {"HAST" "HAST"
   "HADT" "HAST"
   "AKST" "AKST"
   "AKDT" "AKST"
   "PST"  "PST"
   "PDT"  "PST"
   "MST"  "MST"
   "MDT"  "MST"
   "CST"  "CST"
   "CDT"  "CST"
   "EST"  "EST"
   "EDT"  "EST"})

(defn- parse-timezone [date-string]
  (-> (re-find #"\(\w+\)" date-string)
      (string/replace #"[\(\)]" "")))

(defn- timezone [js-date]
  #?(:cljs
     (tz->simple-tz (parse-timezone (.toString js-date)))
     :clj
     "EST"))

(defn- ->hr [hour]
  (let [tod    (if (< hour 12) "AM" "PM")
        result (mod hour 12)
        result (if (zero? result)
                 12
                 result)]
    (str result " " tod)))

(def ^:private start-times-eastern
  {"early-morning"   11
   "late-morning"    13
   "early-afternoon" 15
   "late-afternoon"  17})

(defn options
  [eastern-offset]
  #?(:cljs
     (let [js-date               (js/Date.)
           local-offset          (/ (.getTimezoneOffset js-date) 60)
           offset                (- eastern-offset local-offset)
           timezone-abbreviation (timezone js-date)]
       (concat (for [[option-val eastern-t] start-times-eastern
                     :let                   [t (+ eastern-t (or offset 0))]]
                 [(str (->hr t) " to " (->hr (+ t 2)) " " timezone-abbreviation) option-val])
               [["Anytime" "anytime"]]))
     :clj
     [["Anytime" "anytime"]]))
