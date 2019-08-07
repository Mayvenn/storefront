(ns storefront.ui.promo-banner
  (:require [storefront.accessors.experiments :as experiments]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.promos :as promos]
            [storefront.component :as component]
            [storefront.components.svg :as svg]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.accessors.experiments :as experiments]
            [storefront.platform.component-utils :as utils]
            [storefront.components.ui :as ui]))


(defmulti component
  "Display a different component based on type of promotion"
  (fn [data owner opts] (:promo/type data))
  :default :none)

(defmethod component :none
  [_ _ _]
  (component/create [:div]))

(defmethod component :adventure-freeinstall/applied
  [_ _ _]
  (component/create
   [:a.white.center.p2.bg-teal.mbnp5.h6.bold.flex.items-center.justify-center
    {:on-click  (utils/send-event-callback events/popup-show-adventure-free-install)
     :data-test "adventure-promo-banner"}
    (svg/celebration-horn {:height "1.6em"
                           :width  "1.6em"
                           :class  "mr1 fill-white stroke-white"})
    [:div.pointer "CONGRATS — Your next install is FREE! "
     [:span.underline "More info"]]]))

(defmethod component :v2-freeinstall/eligible
  [_ _ _]
  (component/create
   [:a {:on-click  (utils/send-event-callback events/popup-show-v2-homepage)
        :data-test "v2-free-install-promo-banner"}
    [:div.white.center.pp5.bg-teal.h5.bold.pointer
     "Mayvenn will pay for your install! " [:span.underline "Learn more"]]]))

(defmethod component :v2-freeinstall/applied
  [_ _ _]
  (component/create
   [:a.white.center.p2.bg-teal.mbnp5.h6.bold.flex.items-center.justify-center
    {:on-click  (utils/send-event-callback events/popup-show-v2-homepage)
     :data-test "v2-free-install-promo-banner"}
    (svg/celebration-horn {:height "1.6em"
                           :width  "1.6em"
                           :class  "mr1 fill-white stroke-white"})
    [:div.pointer "CONGRATS — Your next install is FREE! "
     [:span.underline "More info"]]]))

(defmethod component :shop/freeinstall
  [_ _ _]
  (component/create
   [:a.block.white.p2.bg-lavender.flex.justify-center
    {:on-click  (utils/send-event-callback events/popup-show-adventure-free-install)
     :data-test "shop-freeinstall-promo-banner"}
    (svg/info {:height "14px"
               :width  "14px"
               :class  "mr1 mt1"})
    [:div.pointer.h6.medium "Buy 3 items and receive your free "
     [:span.bold.underline
      "Mayvenn" ui/nbsp "Install"]]]))

(defmethod component :basic-promo-banner
  [{:basic-promo-banner/keys [promo]} _ _]
  (component/create
   [:div.white.center.pp5.bg-teal.h5.bold
    {:data-test "promo-banner"}
    (:description promo)]))


(defn ^:private promotion-to-advertise
  [data]
  (let [promotion-db (get-in data keypaths/promotions)
        applied      (get-in data keypaths/order-promotion-codes)
        pending      (get-in data keypaths/pending-promo-code)]
    (or (promos/find-promotion-by-code promotion-db (first applied)) ;; on the order
        (promos/find-promotion-by-code promotion-db pending) ;; on a potential order
        (if-let [default-advertised-promo-text (get-in data keypaths/cms-advertised-promo-text)]
          ;; NOTE(jeff, justin): ideally contentful should provide the entire
          ;; promo object, but it's so much easier to pretend we have a
          ;; promotion object here.
          {:id -1
           :code nil
           :description default-advertised-promo-text
           :advertised true}
          (promos/default-advertised-promotion promotion-db)))))

(defn ^:private nav-whitelist-for*
  "Promo code banner should only show on these nav-events

   Depending on experiments, this whitelist may be modified"
  [no-promotions? promo-type]
  (cond-> #{events/navigate-home
            events/navigate-cart
            events/navigate-shop-by-look
            events/navigate-shop-by-look-details
            events/navigate-category
            events/navigate-product-details}

    ;; Incentivize checkout by reminding them they are saving
    (#{:v2-freeinstall/applied
       :adventure-freeinstall/applied} promo-type)
    (conj events/navigate-checkout-returning-or-guest
          events/navigate-checkout-address
          events/navigate-checkout-payment
          events/navigate-checkout-confirmation)

    (not no-promotions?)
    (disj events/navigate-cart)))

(defn ^:private promo-type*
  "Determine what type of promotion behavior we are under
   experiment for"
  [data]
  (cond

    (= "shop" (get-in data keypaths/store-slug))
    :shop/freeinstall

    ;; GROT: freeinstall-applied? when adventure orders using freeinstall promo code are no longer relevant
    (and
     (or (orders/freeinstall-applied? (get-in data keypaths/order))
         (orders/freeinstall-included? (get-in data keypaths/order)))
     (= "freeinstall" (get-in data keypaths/store-slug)))
    :adventure-freeinstall/applied

    (and
     (orders/freeinstall-applied? (get-in data keypaths/order))
     (experiments/aladdin-experience? data))
    :v2-freeinstall/applied

    (experiments/aladdin-experience? data)
    :v2-freeinstall/eligible

    :else :basic))

(defn query
  [data]
  (let [no-applied-promo? (orders/no-applied-promo? (get-in data keypaths/order))
        nav-whitelist-for (partial nav-whitelist-for* no-applied-promo?)
        nav-event         (get-in data keypaths/navigation-event)
        promo-type        (promo-type* data)]
    (cond-> {:promo     (promotion-to-advertise data)
             :nav-event nav-event}  ;; ADDED!
      (contains? (nav-whitelist-for promo-type) nav-event)
      (assoc :promo/type promo-type))))
