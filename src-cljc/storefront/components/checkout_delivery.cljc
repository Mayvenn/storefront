(ns storefront.components.checkout-delivery
  (:require api.orders
            [api.catalog :refer [select]]
            [spice.core :as spice]
            [spice.date :as date]
            [storefront.accessors.shipping :as shipping]
            [storefront.component :as c :refer [defcomponent]]
            [storefront.components.formatters :as formatters]
            [storefront.components.money-formatters :as mf]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.accessors.line-items :as line-items]
            [storefront.accessors.orders :as orders]))

(defn delivery-note-box
  [{:delivery.note/keys [id copy]}]
  (c/html
   (if id
     [:div.pt2
      (ui/note-box {:data-test id
                    :color     "s-color"}
                   [:div.proxima.content-3.px4.py2 copy])]
     [:div])))

(defcomponent component
  [{:delivery/keys [id primary note-box options] :as data} owner _]
  (when id
    [:div.pb2.pt4.mx3
     [:div.proxima.title-2 primary]
     (delivery-note-box data)
     [:div
      (for [option options]
        [:div.my2 {:key (:react/key option)}
         (ui/radio-section
          (let [{:control/keys [data-test id data-test-id target selected? disabled?]} option]
            (merge {:name         data-test
                    :id           id
                    :data-test    data-test
                    :data-test-id data-test-id
                    :disabled     disabled?}
                   (when target
                     {:on-click (apply utils/send-event-callback target)})
                   (when selected? {:checked "checked"})))
          [:div.right.ml1.medium
           (when-let [classes (:detail/classes option)]
             {:class classes})
           (:detail/value option)]
          [:div.overflow-hidden
           (when-let [classes (:disabled/classes option)]
             {:class classes})
           [:div {:data-test (:primary/data-test option)} (:primary/copy option)]
           [:div.content-3 (:secondary/copy option)]
           [:div.content-3 (:tertiary/copy option)]
           [:div.content-3.p-color (:quaternary/copy option)]])])]]))

(defn shipping-method-rules
  [sku drop-shipping?]
  (case sku
    "WAITER-SHIPPING-1" (if drop-shipping?
                          {:min-delivery 7 :max-delivery 10 :saturday-delivery? true}
                          {:min-delivery 4 :max-delivery 6 :saturday-delivery? true})
    "WAITER-SHIPPING-7" {:min-delivery 2 :max-delivery 4 :saturday-delivery? true}
    "WAITER-SHIPPING-2" {:min-delivery 1 :max-delivery 2 :saturday-delivery? false}
    "WAITER-SHIPPING-4" {:min-delivery 1 :max-delivery 1 :saturday-delivery? false}))

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
  ;; s - day shipped
  ;; x - no movement or delivery
  ;; ~ - unevented day of transit
  ;; b - best case delivery
  ;; w - worst case delivery

  ;; Express: "WAITER-SHIPPING-2" {:min 1 :max 2 :saturday-delivery? false} after 1pm on Thursday et
  ;; S M T W TH F Sa
  ;;          o s x
  ;; x b w

  ;; Priority: "WAITER-SHIPPING-7" {:min 2 :max 4 :saturday-delivery? true} after 1pm on Thursday et
  ;; S M T W TH F Sa
  ;;          o s  ~
  ;; x b ~ w

  ;; Priority: "WAITER-SHIPPING-7" {:min 2 :max 4 :saturday-delivery? true} before 1pm on Wednesday et
  ;; before 1pm et
  ;; S M T W TH F Sa
  ;;       os ~ b  ~
  ;; x w

  ;; Priority: "WAITER-SHIPPING-7" {:min 2 :max 4 :saturday-delivery? true} before 10am on Friday et
  ;; before 1pm et
  ;; S M T W TH F Sa
  ;;            os ~
  ;; x b ~ w

  ;; Priority: "WAITER-SHIPPING-7" {:min 2 :max 4 :saturday-delivery? true} after 10am on Friday et
  ;; before 1pm et
  ;; S M T W TH F Sa
  ;;            o  x
  ;; x s ~ b  ~ w

  ;; Express: "WAITER-SHIPPING-2" {:min 1 :max 2 :saturday-delivery? false} before 10am on Friday et
  ;; S M T W TH F Sa
  ;;            os x
  ;; x b w

  ;; Free: "WAITER-SHIPPING-1" {:min-delivery 4 :max-delivery 6 :saturday-delivery? true} before 10am on Friday et
  ;; S M T W TH F Sa
  ;;            os ~
  ;; x ~ ~ b  ~ w

  ;; Free: "WAITER-SHIPPING-1" {:min-delivery 4 :max-delivery 6 :saturday-delivery? true} after 10am on Friday et
  ;; S M T W TH F Sa
  ;;            o  x
  ;; x s ~ ~  b ~  w

  [day-of-week-order-was-placed
   within-shipping-window?
   saturday-delivery?
   transit-days]
  (let [days-until-shipped (cond
                             (= "Sat" day-of-week-order-was-placed) 2
                             (= "Sun" day-of-week-order-was-placed) 1
                             within-shipping-window?                0
                             (= "Fri" day-of-week-order-was-placed) 3
                             :weekday-not-in-shipping-window        1)
        transit-delay      (->> (cycle ["Sun" "Mon" "Tue" "Wed" "Thu" "Fri" "Sat"])
                                (drop-while (complement #{day-of-week-order-was-placed}))
                                (drop (+ days-until-shipped 1))
                                (take transit-days)
                                (map (partial convert-weekend saturday-delivery?))
                                (apply max))]
    (+ days-until-shipped
       transit-delay
       transit-days)))

(defn format-delivery-date [date]
  (str (formatters/day->day-abbr date) "(" (formatters/month+day date) ")"))

(defn shipping-method->shipping-method-option
  [selected-shipping-method-sku-id
   current-local-time
   east-coast-weekday
   in-window?
   drop-shipping?
   {:keys [sku price] :as shipping-method}]
  (let [{:keys [min-delivery
                max-delivery
                saturday-delivery?]} (shipping-method-rules sku drop-shipping?)

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
        selected?   (= selected-shipping-method-sku-id sku)
        disabled?   (and (not= sku "WAITER-SHIPPING-1") drop-shipping?)]
    {:react/key            sku
     :disabled/classes     (when disabled? "gray")
     :primary/data-test    (when selected? "selected-shipping-method")
     :primary/copy         (shipping/names-with-time-range sku drop-shipping?)
     :secondary/copy       (str "Delivery Date: "
                                (format-delivery-date (date/add-delta current-local-time {:days revised-min}))
                                (when-not (= revised-min revised-max)
                                  (str "–" (format-delivery-date (date/add-delta current-local-time {:days revised-max})))))
     :tertiary/copy        (shipping/shipping-note sku)
     :quaternary/copy      (when (and (not disabled?) drop-shipping?)
                             "This order contains items that are only eligible for Free Standard Shipping.")
     :control/id           (str "shipping-method-" sku)
     :control/data-test    "shipping-method"
     :control/data-test-id sku
     :control/target       (when-not drop-shipping?
                             [events/control-checkout-shipping-method-select shipping-method])
     :control/selected?    selected?
     :control/disabled?    disabled?
     :detail/classes       (cond
                             (and (not= sku "WAITER-SHIPPING-1") drop-shipping?)
                             "gray"

                             (pos? price)
                             "black"

                             :else "p-color")
     :detail/value (mf/as-money-or-free price)}))

(defn query [data]
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
        friday?                (= "Fri" east-coast-weekday)
        in-window?             (and weekday?
                                    parsed-east-coast-hour
                                    (< parsed-east-coast-hour 13)
                                    (or (not friday?)
                                        (< parsed-east-coast-hour 10)))

        {:order/keys [items] :waiter/keys [order]} (api.orders/current data)

        shipping       (orders/shipping-item order)
        free-shipping? (= "WAITER-SHIPPING-1" (:sku shipping))
        only-services? (every? line-items/service? (orders/product-and-service-items order))
        drop-shipping? (boolean (select {:warehouse/slug #{"factory-cn"}} items))]
    {:delivery/id        (when-not (and free-shipping? only-services?)
                           "shipping-method")
     :delivery/primary   "Shipping Method"
     :delivery/options   (->> shipping-methods
                              (map (partial shipping-method->shipping-method-option
                                            selected-sku
                                            now
                                            east-coast-weekday
                                            in-window?
                                            drop-shipping?)))
     :delivery.note/id   (when in-window? "delivery-note")
     :delivery.note/copy (if friday?
                           "Order by 10am ET today to have the guaranteed delivery dates below"
                           "Order by 1pm ET today to have the guaranteed delivery dates below")}))
