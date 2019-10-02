(ns storefront.components.checkout-delivery
  (:require [clojure.string :as string]
            [spice.date :as date]
            [storefront.accessors.shipping :as shipping]
            [storefront.components.formatters :as f]
            [storefront.components.money-formatters :as mf]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.component :as component]))

(defn ^:private select-shipping-method
  [shipping-method]
  (utils/send-event-callback events/control-checkout-shipping-method-select
                             shipping-method))

(defn component
  [{:keys [shipping-methods selected-sku]} owner]
  (component/create
   [:div
    [:.h3 "Shipping Method"]
    [:.py1
     (for [{:keys [sku name price] :as shipping-method} shipping-methods]
       (ui/radio-section
        (merge {:key          sku
                :name         "shipping-method"
                :id           (str "shipping-method-" sku)
                :data-test    "shipping-method"
                :data-test-id sku
                :on-click     (select-shipping-method shipping-method)}
               (when (= selected-sku sku) {:checked "checked"}))
        [:.right.ml1.medium {:class "navy"} (mf/as-money-without-cents-or-free price)]
        [:.overflow-hidden
         [:div (when (= selected-sku sku) {:data-test "selected-shipping-method"}) name]
         [:.h6 (or (shipping/timeframe sku) "")]]))]]))

(defn day-with-month
  [date]
  (str (f/day->day-abbr date)
       " (" (f/month+day date) ")"))

(defn delay-if-after-9:45am
  [now days]
  (cond-> days
    ;; if after 9:45, it won't ship today, so effectively we add a shipping day
    (<= 10 (.getHours (date/add-delta now {:minutes 15})))
    inc))

(defn enrich-shipping-method
  [now shipping-method]
  (let [business-days (case (:sku shipping-method)
                        "WAITER-SHIPPING-1" [3 5]
                        "WAITER-SHIPPING-2" [1 2]
                        "WAITER-SHIPPING-4" [1])
        short-name    (case (:sku shipping-method)
                        "WAITER-SHIPPING-1" "Standard Shipping"
                        "WAITER-SHIPPING-2" "Express Shipping"
                        "WAITER-SHIPPING-4" "Overnight Shipping")
        timeframe     (str (string/join "‐" business-days) " business " (ui/pluralize (count business-days) "day"))
        date-ranges   (into []
                            (comp
                             (map (partial delay-if-after-9:45am now))
                             (map (partial date/add-business-days now)))
                            business-days)]
    (assoc shipping-method
           :copy/timeframe timeframe
           :copy/short-name short-name
           :copy/title (string/join "—" (map day-with-month date-ranges))
           :date-ranges date-ranges)))

(defn query
  [data]
  (let [now              (date/now)
        shipping-methods (map (partial enrich-shipping-method now) (get-in data keypaths/shipping-methods))
        selected-sku     (get-in data keypaths/checkout-selected-shipping-method-sku)]
    {:shipping-methods shipping-methods
     :selected-sku     selected-sku}))
