(ns storefront.components.promotion-banner
  (:require [storefront.accessors.promos :as promos]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.accessors.experiments :as experiments]
            [storefront.platform.component-utils :as utils]
            [storefront.accessors.orders :as orders]))

(def allowed-navigation?
  #{events/navigate-home
    events/navigate-cart
    events/navigate-shop-by-look
    events/navigate-shop-by-look-details})

(defn auto-complete-allowed?
  [data]
  (let [order                          (get-in data keypaths/order)
        empty-cart?                    (= 0 (orders/product-quantity order))
        nav-event                      (get-in data keypaths/navigation-event)
        experiment-allowed-navigation? (disj allowed-navigation? events/navigate-cart)
        applied-promo-code             (orders/applied-promo-code order)]
    (or (experiment-allowed-navigation? nav-event)
        (and (= events/navigate-cart nav-event)
             (or (nil? applied-promo-code)
                 empty-cart?)))))

(defn promotion-to-advertise [data]
  (let [promotions (get-in data keypaths/promotions)]
    (or (promos/find-promotion-by-code promotions (first (get-in data keypaths/order-promotion-codes)))
        (promos/find-promotion-by-code promotions (get-in data keypaths/pending-promo-code))
        (promos/default-advertised-promotion promotions))))

(defn component
  [{:keys [allowed? the-ville? seventy-five-off-install? promo]} owner opts]
  (component/create
   (cond
     (and allowed? seventy-five-off-install?)
     [:a {:on-click  (utils/send-event-callback events/popup-show-seventy-five-off-install {})
          :data-test "seventy-five-off-install-promo-banner"}
      [:div.white.center.pp5.bg-teal.h5.bold.pointer
       "Get $100 off your install! " [:span.underline "Learn more"]]]

     (and allowed? the-ville?)
     [:a {:on-click  (utils/send-event-callback events/popup-show-free-install {})
          :data-test "free-install-promo-banner"}
      [:div.white.center.pp5.bg-teal.h5.bold.pointer
       "Mayvenn will pay for your install! " [:span.underline "Learn more"]]]

     (and allowed? promo)
     [:div.white.center.pp5.bg-teal.h5.bold
      {:data-test "promo-banner"}
      (:description promo)]

     :else nil)))

(defn should-display?
  "Used here to decide whether to display, and also by the flyout menu scrim to
  calculate top margin."
  [data]
  (if (experiments/auto-complete? data)
    (auto-complete-allowed? data)
    (allowed-navigation? (get-in data keypaths/navigation-event))))

(defn query
  [data]
  {:promo    (promotion-to-advertise data)
   :allowed? (should-display? data)

   :the-ville?                (experiments/the-ville? data)
   :seventy-five-off-install? (experiments/seventy-five-off-install? data)})

(defn built-component
  [data opts]
  (component/build component (query data) opts))
