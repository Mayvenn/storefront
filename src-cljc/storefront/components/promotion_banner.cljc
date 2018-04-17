(ns storefront.components.promotion-banner
  (:require [storefront.accessors.promos :as promos]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.accessors.experiments :as experiments]
            [storefront.platform.component-utils :as utils]))

(def allowed-navigation?
  #{events/navigate-home
    events/navigate-cart
    events/navigate-shop-by-look
    events/navigate-shop-by-look-details})

(defn auto-complete-allowed?
  [data]
  (let [nav-event                      (get-in data keypaths/navigation-event)
        experiment-allowed-navigation? (disj allowed-navigation? events/navigate-cart)
        current-promotion-codes        (get-in data keypaths/order-promotion-codes)]
    (or (experiment-allowed-navigation? nav-event)
        (and (= events/navigate-cart nav-event)
             (empty? current-promotion-codes)))))

(defn promotion-to-advertise [data]
  (let [promotions (get-in data keypaths/promotions)]
    (or (promos/find-promotion-by-code promotions (first (get-in data keypaths/order-promotion-codes)))
        (promos/find-promotion-by-code promotions (get-in data keypaths/pending-promo-code))
        (promos/default-advertised-promotion promotions))))

(defn component
  [{:keys [allowed? the-ville? promo]} owner opts]
  (component/create
   (cond
     (and allowed? the-ville?) [:a {:on-click (utils/send-event-callback events/popup-show-free-install {})
                                 :data-test "free-install-promo-banner"}
                                [:div.white.center.pp5.bg-teal.h5.bold.pointer
                                 "Mayvenn will pay for your install! " [:span.underline "Learn more"]]]
     (and allowed? promo)      [:div.white.center.pp5.bg-teal.h5.bold
                                {:data-test "promo-banner"}
                                (:description promo)]
     :else                     nil)))

(defn query
  [data]
  (let [auto-complete? (experiments/auto-complete? data)]
    {:allowed?   (if auto-complete?
                   (auto-complete-allowed? data)
                   (allowed-navigation? (get-in data keypaths/navigation-event)))
     :the-ville? (experiments/the-ville? data)
     :promo      (promotion-to-advertise data)}))

(defn built-component
  [data opts]
  (component/build component (query data) opts))
