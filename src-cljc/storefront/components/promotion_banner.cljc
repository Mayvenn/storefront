(ns storefront.components.promotion-banner
  (:require [storefront.accessors.experiments :as experiments]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.promos :as promos]
            [storefront.component :as component]
            [storefront.components.svg :as svg]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]))

(defmulti component
  "Display a different component based on type of promotion"
  (fn [data owner opts] (:promo/type data))
  :default :none)

(defmethod component :none
  [_ _ _]
  (component/create [:div]))

(defmethod component :install-discount/eligible
  [_ _ _]
  (component/create
   [:a {:on-click  (utils/send-event-callback events/popup-show-seventy-five-off-install
                                              {})
        :data-test "seventy-five-off-install-promo-banner"}
    [:div.white.center.pp5.bg-teal.h5.bold.pointer
     "Get $100 off your install! " [:span.underline "Learn more"]]]))

(defmethod component :install-discount/applied
  [_ _ _]
  (component/create
   [:div.white.center.p2.bg-teal.mbnp5.h6.bold.flex.items-center.justify-center
    (svg/celebration-horn {:height "1.6em"
                           :width  "1.6em"
                           :class  "mr1 fill-white stroke-white"})
    "CONGRATS — Enjoy $100 off your next install"]))

(defmethod component :free-install
  [_ _ _]
  (component/create
   [:a {:on-click  (utils/send-event-callback events/popup-show-free-install {})
        :data-test "free-install-promo-banner"}
    [:div.white.center.pp5.bg-teal.h5.bold.pointer
     "Mayvenn will pay for your install! " [:span.underline "Learn more"]]]))

(defmethod component :basic
  [{:keys [promo]} _ _]
  (component/create
   [:div.white.center.pp5.bg-teal.h5.bold
    {:data-test "promo-banner"}
    (:description promo)]))

(defn ^:private promotion-to-advertise
  [data]
  (let [promotion-db (get-in data keypaths/promotions)
        applied      (get-in data keypaths/order-promotion-codes)
        pending      (get-in data keypaths/pending-promo-code)]
    (or (promos/find-promotion-by-code promotion-db (first applied))
        (promos/find-promotion-by-code promotion-db pending)
        (promos/default-advertised-promotion promotion-db))))

(defn ^:private nav-whitelist-for*
  "Promo code banner should only show on these nav-events

   Depending on experiments, this whitelist may be modified"
  [auto-complete? no-promotions? promo-type]
  (cond-> #{events/navigate-home
            events/navigate-cart
            events/navigate-shop-by-look
            events/navigate-shop-by-look-details}

    ;; Incentivize checkout by reminding them they are saving
    (= :install-discount/applied promo-type)
    (conj events/navigate-checkout-returning-or-guest
          events/navigate-checkout-address
          events/navigate-checkout-payment
          events/navigate-checkout-confirmation)

    ;; Do promotion banner on the cart page w/ auto-complete experiment
    (and auto-complete?
         (not no-promotions?))
    (disj events/navigate-cart)))

(defn ^:private promo-type*
  "Determine what type of promotion behavior we are under
   experiment for"
  [data]
  (cond
    (and
     (orders/install-qualified? (get-in data keypaths/order))
     (experiments/seventy-five-off-install? data))
    :install-discount/applied

    (experiments/seventy-five-off-install? data)
    :install-discount/eligible

    (experiments/the-ville? data)
    :free-install

    :else :basic))

(defn query
  [data]
  (let [nav-whitelist-for
        (partial nav-whitelist-for*
                 (experiments/auto-complete? data)
                 (orders/no-applied-promo? (get-in data
                                                   keypaths/order)))

        nav-event  (get-in data keypaths/navigation-event)
        promo-type (promo-type* data)]
    (cond-> {:promo (promotion-to-advertise data)}
      (contains? (nav-whitelist-for promo-type) nav-event)
      (assoc :promo/type promo-type))))

(defn built-component
  [data opts]
  (component/build component (query data) opts))
