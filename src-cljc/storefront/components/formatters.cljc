(ns storefront.components.formatters
  (:require [spice.date :as date]
            [storefront.platform.numbers :as numbers]
            [clojure.string :as string]))

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




;; TODO: perhaps we should make this work with clojure as well
(do
  #?@(:cljs
      [(defn format-date
         "Formats a date into a string given the formatting system described here:
        https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/DateTimeFormat/DateTimeFormat"
         [fmt date-like]
         (when-let [date (date/to-datetime date-like)]
           (-> (Intl.DateTimeFormat. 'en-US' (clj->js fmt))
               (.format date))))

       (defn format-date-to-parts
         "Formats a date into parts given the formatting system described here:
        https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/DateTimeFormat/DateTimeFormat"
         [fmt date-like]
         (when-let [date (date/to-datetime date-like)]
           (into {}
                 (comp
                  (map (fn [date-part] (js->clj date-part :keywordize-keys true)))
                  (remove (fn [date-part] (= "literal" (:type date-part))))
                  (map (fn [date-part] [(keyword (:type date-part)) (:value date-part)])))
                 (-> (Intl.DateTimeFormat. 'en-US' (clj->js fmt))
                     (.formatToParts date)))))

       (defn long-date [date-like]
         ;; April 5, 2022
         (format-date {:dateStyle "long"} date-like))

       (defn abbr-date [date-like]
         ;; Apr 5, 2022
         (format-date {:month "short"
                       :day   "numeric"
                       :year  "numeric"} date-like))

       (defn date-tuple [date-like]
         (let [parts (format-date-to-parts {:month "short"
                                            :day   "numeric"
                                            :year  "numeric"}
                                           date-like)]
           (vec ((juxt :year :month :day) parts))))]))

(defn day->day-abbr [date-like]
  ;; Tue
  #?(:cljs
     (format-date {:weekday "short"} date-like)
     :clj nil))

(defn month+day [date-like]
  ;; 4/5
  (when-let [date (date/to-datetime date-like)]
    #?(:cljs (format-date {:month "numeric" :day "numeric"} date)
       :clj (.print (org.joda.time.format.DateTimeFormat/forPattern "M/d") date))))

(defn slash-date [date-like]
  ;; 4/5/22
  #?(:clj date-like
     :cljs
     (let [{:keys [month day year]}
           (format-date-to-parts {:month "numeric" :day "numeric" :year "2-digit"}
                                 date-like)]
       (string/join "/" [month day year]))))

(defn short-date [date-like]
  ;; 04.05.2022
  #?(:clj date-like
     :cljs
     (let [{:keys [month day year]}
           (format-date-to-parts {:month "2-digit" :day "2-digit" :year "numeric"}
                                 date-like)]
       (string/join "." [month day year]))))

(defn time-12-hour [date-like]
  ;; 2:48 PM
  (when-let [datetime (date/to-datetime date-like)]
    #?(:cljs (format-date {:timeStyle "short"} datetime)
       :clj (.print (org.joda.time.format.DateTimeFormat/forPattern "h:mm a") datetime))))

(defn e164-phone [phone-number]
  (let [digits (-> phone-number str numbers/digits-only)]
    (cond (= 10 (count digits))
          (str "+1" digits)
          
          (and (= 11 (count digits))
               (= "1" (first digits)))
          (str "+" digits)

          :else
          nil)))

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
