(ns storefront.components.checkout-delivery
  (:require api.orders
            [api.catalog :refer [select]]
            [spice.core :as spice]
            [spice.date :as date]
            [storefront.accessors.shipping :as shipping]
            [storefront.accessors.experiments :as experiments]
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
  [{:delivery.note/keys [severity id copy]}]
  (c/html
   (if id
     [:div.pt2
      (ui/note-box {:data-test id
                    :color     (if (= :warning severity)
                                 "yellow"
                                 "s-color")}
                   [:div.proxima.content-3.px4.py2 copy])]
     [:div])))

(defcomponent component
  [{:delivery/keys [id primary note-box options] :as data} owner _]
  (when id
    [:div.pb2.pt4.mx3
     [:h2.proxima.title-2 primary]
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
           (merge
            {:data-test (:primary/data-test option)}
            (when-let [classes (:disabled/classes option)]
              {:class classes}))
           [:div (:primary/copy option)]
           [:div.content-3 (:secondary/copy option) " " (:tertiary/copy option)]
           [:div.content-3.p-color (:quaternary/copy option)]])])]]))

(defn convert-weekend
  "Converts weekend days to additional number of days that delay delivery"
  [saturday-delivery? d]
  (case d
    "Sat" (if saturday-delivery? 0 2)
    "Sun" 1
    0))

(def additional-holiday-delay-days 1)

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
       transit-days
       additional-holiday-delay-days)))

(defn format-delivery-date [date]
  (str (formatters/day->day-abbr date) "(" (formatters/month+day date) ")"))

(defn shipping-method->shipping-method-option
  [selected-shipping-method-sku-id
   base-starting-date
   east-coast-weekday
   in-window?
   drop-shipping?
   {:keys [sku price] :as shipping-method}]
  (let [{:keys [min-delivery
                max-delivery
                saturday-delivery?]} (shipping/shipping-method-rules sku drop-shipping?)

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
    (merge
     {:react/key            sku
      :disabled/classes     (when disabled? "gray")
      :primary/data-test    (when selected? "selected-shipping-method")
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
      :detail/value (mf/as-money-or-free price)

      :primary/copy    #?(:clj nil
                          :cljs
                          (formatters/format-date {:weekday "short"
                                                   :month   "long"
                                                   :day     "numeric"}
                                                  (date/add-delta base-starting-date {:days revised-max})))
      :secondary/copy  (str (shipping/names-with-time-range sku
                                                            drop-shipping?)
                            " "
                            (shipping/shipping-note sku drop-shipping?))
      :quaternary/copy (when (and (not disabled?) drop-shipping?)
                         "This order contains items that are only eligible for Free Standard Shipping.")})))

(defn query [data]
  (let [shipping-methods                           (get-in data keypaths/shipping-methods)
        selected-sku                               (get-in data keypaths/checkout-selected-shipping-method-sku)
        {:order/keys [items] :waiter/keys [order]} (api.orders/current data)
        shipping                                   (orders/shipping-item order)
        free-shipping?                             (= "WAITER-SHIPPING-1" (:sku shipping))
        only-services?                             (every? line-items/service? (orders/product-and-service-items order))
        drop-shipping?                             (boolean (select {:warehouse/slug #{"factory-cn"}} items))
        inventory-count-shipping-halt?             (experiments/inventory-count-shipping-halt? data)
        show-guaranteed-shipping?                  (:show-guaranteed-shipping (get-in data keypaths/features))
        {checkout-shipping-note :note
         now                    :now
         east-coast-weekday     :east-coast-weekday}   (get-in data keypaths/checkout-shipping)]
    (merge
     {:delivery/id                           (when-not (and free-shipping? only-services?)
                                               "shipping-method")
      :delivery/primary                      "Choose Delivery Date"
      :delivery/options                      (->> shipping-methods
                                                  (map (partial shipping-method->shipping-method-option
                                                                selected-sku
                                                                (if (and (:show-shipping-delay (get-in data keypaths/features))
                                                                         (->> (orders/product-and-service-items order)
                                                                              (map :variant-attrs)
                                                                              ;; Saddlecreek skus don't have a warehouse
                                                                              (remove :warehouse/slug)
                                                                              seq))
                                                                  (date/to-datetime "2023-05-01T04:00:00.000Z")
                                                                  now)
                                                                east-coast-weekday
                                                                (= checkout-shipping-note :in-shipping-window)
                                                                drop-shipping?)))}
     (when show-guaranteed-shipping?
       (if inventory-count-shipping-halt?
         {:delivery.note/id       "inventory-warning"
          :delivery.note/copy     "Due to our annual year-end inventory count, Mayvenn will not be shipping new orders until Monday, December 13."
          :delivery.note/severity :warning}
         (cond
           (= checkout-shipping-note :in-shipping-window)
           {:delivery.note/id   "delivery-note"
            :delivery.note/copy (when-not (and (:show-shipping-delay (get-in data keypaths/features))
                                               (->> (orders/product-and-service-items order)
                                                    (map :variant-attrs)
                                                    ;; Saddlecreek skus don't have a warehouse
                                                    (remove :warehouse/slug)
                                                    seq))
                                  (if (= "Fri" east-coast-weekday)
                                   "Order by 10am ET today to have the guaranteed delivery dates below"
                                   "Order by 1pm ET today to have the guaranteed delivery dates below"))}
           (= checkout-shipping-note :was-in-shipping-window)
           {:delivery.note/id       "delivery-note"
            :delivery.note/severity :warning
            :delivery.note/copy     "The estimated delivery dates below have been updated."}))))))
