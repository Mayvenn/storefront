(ns storefront.components.formatters
  (:require [spice.date :as date]
            [storefront.platform.numbers :as numbers]
            [clojure.string :as string]
            #?@(:cljs [[goog.string.format]
                       [goog.string]])))

(def month-names ["January"
                  "February"
                  "March"
                  "April"
                  "May"
                  "June"
                  "July"
                  "August"
                  "September"
                  "October"
                  "November"
                  "December"])

(def month-abbr ["Jan"
                 "Feb"
                 "Mar"
                 "Apr"
                 "May"
                 "June"
                 "July"
                 "Aug"
                 "Sept"
                 "Oct"
                 "Nov"
                 "Dec"])

(def day-abbr ["Sun"
               "Mon"
               "Tue"
               "Wed"
               "Thu"
               "Fri"
               "Sat"])

(defn day->day-abbr [date]
  ;; This is actually the recommended way to do this in JavaScript.
  ;; The other option is to use a time library, but goog.i18n adds 500K to the
  ;; page size.
  #?(:cljs (get day-abbr (.getDay date))
     :clj nil))

(defn month+day [date-like]
  (when-let [date (date/to-datetime date-like)]
    #?(:cljs (goog.string/format "%d/%d" (inc (.getMonth date)) (.getDate date))
       :clj (.print (org.joda.time.format.DateTimeFormat/forPattern "M/d") date))))

(defn short-date [date-like]
  #?(:clj date-like
     :cljs
     (when-let [date (date/to-datetime date-like)]
       (goog.string/format "%02d.%02d.%d"
                           (inc (.getMonth date))
                           (.getDate date)
                           (.getFullYear date)))))

;; TODO: perhaps we should make this work with clojure as well
(do
  #?@(:cljs
      [(defn short-date-with-interposed-str [date-like s]
         (when-let [date (date/to-datetime date-like)]
           (goog.string/format "%d%s%ds%d" (inc (.getMonth date)) s (.getDate date) s (.getFullYear date))))

       (defn less-year-more-day-date [date-like]
         (when-let [date (date/to-datetime date-like)]
           (goog.string/format "%02d/%02d/%d" (inc (.getMonth date)) (.getDate date) (mod (.getFullYear date) 100))))

       (defn date->month-name [date]
         ;; This is actually the recommended way to do this in JavaScript.
         ;; The other option is to use a time library, but goog.i18n adds 500K to the
         ;; page size.
         (get month-names (.getMonth date)))

       (defn date->month-abbr [date]
         ;; This is actually the recommended way to do this in JavaScript.
         ;; The other option is to use a time library, but goog.i18n adds 500K to the
         ;; page size.
         (get month-abbr (.getMonth date)))

       (defn long-date [date-like]
         (when-let [date (date/to-datetime date-like)]
           (goog.string/format "%s %d, %d" (date->month-name date) (.getDate date) (.getFullYear date))))

       (defn abbr-date [date-like]
         (when-let [date (date/to-datetime date-like)]
           (goog.string/format "%s %d, %d" (date->month-abbr date) (.getDate date) (.getFullYear date))))

       (defn epoch-date [date-like]
         (some-> (date/to-datetime date-like)
                 (.toLocaleDateString)))

       (defn date-tuple [date-like]
         (when-let [date (date/to-datetime date-like)]
           [(.getFullYear date) (.getMonth date) (.getDate date)]))]))

(defn phone-number [phone]
  {:pre [(#{10 11} (->> phone str numbers/digits-only count))]}
  (->> phone
       str
       numbers/digits-only
       (re-find #"(1?)(\d{3})(\d{3})(\d{4})")
       rest
       (remove empty?)
       (string/join "-")))

(defn phone-number-parens [phone]
  (let [[area-code first-group second-group]
        (-> phone
            phone-number
            (string/split #"-" 3))]
    (str "(" area-code ") - " first-group " - " second-group)))
