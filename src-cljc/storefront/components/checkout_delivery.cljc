(ns storefront.components.checkout-delivery
  (:require [spice.core :as spice]
            [spice.date :as date]
            [storefront.accessors.shipping :as shipping]
            [storefront.component :as c :refer [defcomponent]]
            [storefront.components.formatters :as formatters]
            [storefront.components.money-formatters :as mf]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]))

(defn delivery-note-box
  [{:delivery.note/keys [id copy]}]
  (c/html
   (if id
     (ui/note-box {:data-test id
                   :color "s-color"}
                  [:div.proxima.content-3.px4.py2 copy])
     [:div])))

(defcomponent component
  [{:delivery/keys [primary note-box options] :as data} owner _]
  [:div.pb2.pt4.mx3
   [:div.proxima.title-2 primary]
   (delivery-note-box data)
   [:div.py1
    (for [option options]
      [:div.my2 {:key (:react/key option)}
       (ui/radio-section
        (let [{:control/keys [data-test id data-test-id target selected?]} option]
          (merge {:name data-test
                  :id id
                  :data-test data-test
                  :data-test-id data-test-id
                  :on-click     (apply utils/send-event-callback target)}
                 (when selected? {:checked "checked"})))
        [:div.right.ml1.medium (:detail/value option)]
        [:div.overflow-hidden
         [:div {:data-test (:primary/data-test option)} (:primary/copy option)]
         [:div.content-3 (:secondary/copy option)]
         [:div.content-3 (:tertiary/copy option)]])])]])

(defn shipping-method->shipping-method-option
  [selected-shipping-method-sku-id
   {:keys [sku name price] :as shipping-method}]
  (let [selected? (= selected-shipping-method-sku-id sku)]
    {:react/key            (str "non-experimental-" sku)
     :primary/data-test    (when selected? "selected-shipping-method")
     :primary/copy         name
     :secondary/copy       (or (shipping/longform-timeframe sku) "")
     :control/id           (str "shipping-method-" sku)
     :control/data-test    "shipping-method"
     :control/data-test-id sku
     :control/target       [events/control-checkout-shipping-method-select shipping-method]
     :control/selected?    selected?
     :detail/value         ^:ignore-interpret-warning [:span {:class (if (pos? price) "black" "p-color")}
                                                       (mf/as-money-or-free price)]}))

(defn query
  [data]
  (let [shipping-methods (get-in data keypaths/shipping-methods)
        selected-sku     (get-in data keypaths/checkout-selected-shipping-method-sku)]
    {:delivery/primary "Shipping Method"
     :delivery/options (map (partial shipping-method->shipping-method-option selected-sku) shipping-methods)}))

(def shipping-method-rules
  {"WAITER-SHIPPING-1" {:min-delivery 4 :max-delivery 6 :saturday-delivery? true}
   "WAITER-SHIPPING-7" {:min-delivery 2 :max-delivery 4 :saturday-delivery? true}
   "WAITER-SHIPPING-2" {:min-delivery 1 :max-delivery 2 :saturday-delivery? false}
   "WAITER-SHIPPING-4" {:min-delivery 1 :max-delivery 1 :saturday-delivery? false}})

(defn convert-weekend
  "Converts weekend days to additional number of days that delay delivery"
  [saturday-delivery? d]
  (case d
    "Sat" (if saturday-delivery? 0 2)
    "Sun" 1
    0))

(defn number-of-days-to-ship
  ;; Key:
  ;; o - day ordered
  ;; h - handling
  ;; x - no movement or delivery
  ;; # - day of movement
  ;; Express: "WAITER-SHIPPING-2" {:min 1 :max 2 :saturday-delivery? false} after 1pm et
  ;; S M T W TH F Sa
  ;;          o h x
  ;;;x 1 2

  ;; Priority: "WAITER-SHIPPING-7" {:min 2 :max 4 :saturday-delivery? true} after 1pm et
  ;; S M T W TH F Sa
  ;;       x sh 1 2
  ;;;x 3 4

  ;; Priority: "WAITER-SHIPPING-7" {:min 2 :max 4 :saturday-delivery? true}
  ;; before 1pm et
  ;; S M T W TH F Sa
  ;;       h  1 2 3
  ;;;x 4

  [weekday-order-is-placed
   within-shipping-window?
   saturday-delivery?
   days-to-ship]
  (let [handling-time        1
        after-window-penalty (if within-shipping-window? 0 1)]
    (+ days-to-ship
       after-window-penalty
       (->> (cycle ["Sun" "Mon" "Tue" "Wed" "Thu" "Fri" "Sat"])
            (drop-while (complement #{weekday-order-is-placed}))
            (drop (+ handling-time after-window-penalty))
            (take days-to-ship)
            (map (partial convert-weekend saturday-delivery?))
            (apply max)))))

(defn format-delivery-date [date]
  (str (formatters/day->day-abbr date) "(" (formatters/month+day date) ")"))

(defn shipping-estimates-experiment--shipping-method->shipping-method-option
  [selected-shipping-method-sku-id
   current-local-time
   east-coast-weekday
   in-window?
   {:keys [sku price] :as shipping-method}]
  (let [{:keys [min-delivery
                max-delivery
                saturday-delivery?]} (get shipping-method-rules sku)

        revised-min (number-of-days-to-ship
                     east-coast-weekday
                     in-window?
                     saturday-delivery?
                     min-delivery)

        revised-max (number-of-days-to-ship
                     east-coast-weekday
                     in-window?
                     saturday-delivery?
                     max-delivery)
        selected?   (= selected-shipping-method-sku-id sku)]
    {:react/key         sku
     :primary/data-test (when selected? "selected-shipping-method")
     :primary/copy      (shipping/names-with-time-range sku)
     :secondary/copy    (str "Delivery Date: "
                             (format-delivery-date (date/add-delta current-local-time {:days revised-min}))
                             (when-not (= revised-min revised-max)
                               (str "–" (format-delivery-date (date/add-delta current-local-time {:days revised-max})))))
     :tertiary/copy     (shipping/shipping-note sku)

     :control/id           (str "shipping-method-" sku)
     :control/data-test    "shipping-method"
     :control/data-test-id sku
     :control/target       [events/control-checkout-shipping-method-select shipping-method]
     :control/selected?    selected?
     :detail/value         ^:ignore-interpret-warning [:span {:class (if (pos? price) "black" "p-color")}
                                                       (mf/as-money-or-free price)]}))


(defn shipping-estimates-query [data]
  (let [shipping-methods       (get-in data keypaths/shipping-methods)
        selected-sku           (get-in data keypaths/checkout-selected-shipping-method-sku)
        now                    (date/now)
        {east-coast-hour-str :hour
         east-coast-weekday  :weekday}
        #?(:cljs
           (->> (.formatToParts
                 (js/Intl.DateTimeFormat
                  "en-US" #js
                  {:timeZone "America/New_York"
                   :weekday  "short"
                   :hour     "numeric"
                   :hour12   false}) now)
                js->clj
                (mapv js->clj)
                (mapv (fn [{:strs [type value]}]
                        {(keyword type) value}))
                (reduce merge {}))
           :clj nil)
        parsed-east-coast-hour (spice/parse-int east-coast-hour-str)
        weekday?               (contains? #{"Mon" "Tue" "Wed" "Thu" "Fri"} east-coast-weekday)
        in-window?             (and parsed-east-coast-hour
                                    (< parsed-east-coast-hour 13))]
    {:delivery/primary "Shipping Method"
     :delivery/options (map (partial shipping-estimates-experiment--shipping-method->shipping-method-option
                                 selected-sku
                                 now
                                 east-coast-weekday
                                 in-window?) shipping-methods)
     :delivery.note/id   (when (and weekday? in-window?)
                           "delivery-note")
     :delivery.note/copy "Order by 1pm ET today to have the guaranteed delivery dates below"}))
