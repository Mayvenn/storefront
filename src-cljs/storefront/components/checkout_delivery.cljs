(ns storefront.components.checkout-delivery
  (:require [clojure.string :as string]
            [om.core :as om]
            [sablono.core :refer [html]]
            [spice.date :as date]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.shipping :as shipping]
            [storefront.components.formatters :as f]
            [storefront.components.money-formatters :as mf]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]))

(defn ^:private select-shipping-method
  [shipping-method]
  (utils/send-event-callback events/control-checkout-shipping-method-select
                             shipping-method))

(defn original-component [{:keys [shipping-methods selected-sku guaranteed-delivery-date]} owner]
  (om/component
    (html
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
            [:.right.ml1.medium {:class (if (pos? price) "navy" "teal")} (mf/as-money-without-cents-or-free price)]
            [:.overflow-hidden
             [:div (when (= selected-sku sku) {:data-test "selected-shipping-method"}) name]
             [:.h6 (or (shipping/timeframe sku) "")]]))]])))

(defn guaranteed-by-component [{:keys [shipping-methods selected-sku guaranteed-delivery-date]} owner]
  (om/component
    (html
      [:div
       [:.medium.purple.h4 (str "Guaranteed delivery by " guaranteed-delivery-date)]
       [:.py1
        (for [{:keys [sku name price copy/title] :as shipping-method} shipping-methods]
          (ui/radio-section
            (merge {:key          sku
                    :name         "shipping-method"
                    :id           (str "shipping-method-" sku)
                    :data-test    "shipping-method"
                    :data-test-id sku
                    :on-click     (select-shipping-method shipping-method)}
                   (when (= selected-sku sku) {:checked "checked"}))
            [:.right.ml1.medium {:class (if (pos? price) "navy" "teal")} (mf/as-money-without-cents-or-free price)]
            [:.overflow-hidden
             [:div.teal.medium.h4 (when (= selected-sku sku) {:data-test "selected-shipping-method"}) title]
             [:.h6 (or (shipping/timeframe sku) "")]]))]])))

(defn day-with-month [date]
  (str (f/day->day-abbr date)
       " (" (f/month+day date) ")"))

(defn ^:private add-date-ranges [now shipping-data]
  (assoc shipping-data
         :date-ranges
         (map (partial date/add-business-days now)
              (:business-days shipping-data))))

(defn ^:private add-title [{:as shipping-data :keys [date-ranges]}]
  (assoc shipping-data :copy/title (string/join "-" (map day-with-month date-ranges))))

(defn determine-ship-date [now shipping-method]
  (cond-> shipping-method
    ;; if after 9:45, it won't ship today, so effectively we add a shipping day
    (<= 10 (.getHours (spice.date/add-delta now {:minutes 15})))
    (update :business-days (partial map inc))))

(defn query [data]
  (let [now              (spice.date/now)
        shipping-methods (into []
                               (comp
                                (map shipping/enrich-shipping-method)
                                (map (partial determine-ship-date now))
                                (map (partial add-date-ranges now))
                                (map add-title))
                               (get-in data keypaths/shipping-methods))
        selected-sku     (get-in data keypaths/checkout-selected-shipping-method-sku)]
    {:shipping-methods         shipping-methods
     :selected-sku             selected-sku
     :guaranteed-delivery-date (when (experiments/guaranteed-delivery? data)
                                 (f/long-date (last (:date-ranges (get (spice.maps/index-by :sku shipping-methods) selected-sku)))))}))

(defn component [{:keys [guaranteed-delivery-date] :as data} owner]
  (om/component
   (html
    (if guaranteed-delivery-date
      (om/build guaranteed-by-component data)
      (om/build original-component data)))))
