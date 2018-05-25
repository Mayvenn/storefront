(ns storefront.components.promotion-banner
  (:require [storefront.accessors.experiments :as experiments]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.promos :as promos]
            [storefront.component :as component]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.components.svg :as svg]))

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

(defn ^:private to-show?*
  "Determine whether to show for a nav-event"
  [auto-complete? no-promotions? nav-event promo-type]
  (get (cond-> #{events/navigate-home
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
         (disj events/navigate-cart))
       nav-event))

(defn ^:private no-promotion?
  "TODO move to order accessors"
  [order]
  (or (nil? (orders/applied-promo-code order))
      (= 0 (orders/product-quantity order))))

(defn ^:private promotion-to-advertise
  [data]
  (let [promotions (get-in data keypaths/promotions)]
    (or (promos/find-promotion-by-code promotions (first (get-in data keypaths/order-promotion-codes)))
        (promos/find-promotion-by-code promotions (get-in data keypaths/pending-promo-code))
        (promos/default-advertised-promotion promotions))))

(defn promo-type*
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
  (let [to-show?   (partial to-show?*
                            (experiments/auto-complete? data)
                            (no-promotion? (get-in data keypaths/order))
                            (get-in data keypaths/navigation-event))
        promo-type (promo-type* data)]
    (cond-> {:promo (promotion-to-advertise data)}
      (to-show? promo-type)
      (assoc :promo/type promo-type))))

(defn built-component
  [data opts]
  (component/build component (query data) opts))
