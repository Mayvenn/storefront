(ns storefront.components.promotion-banner
  (:require [storefront.accessors.promos :as promos]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.accessors.experiments :as experiments]
            [storefront.platform.component-utils :as utils]))

(def allowed-navigation-events
  #{events/navigate-home
    events/navigate-cart
    events/navigate-shop-by-look
    events/navigate-shop-by-look-details})

(defn promotion-to-advertise [data]
  (let [promotions (get-in data keypaths/promotions)]
    (or (promos/find-promotion-by-code promotions (first (get-in data keypaths/order-promotion-codes)))
        (promos/find-promotion-by-code promotions (get-in data keypaths/pending-promo-code))
        (promos/default-advertised-promotion promotions))))

(defn component [{:keys [allowed? the-ville? promo]} owner opts]
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

(defn query [data]
  {:allowed?   (allowed-navigation-events (get-in data keypaths/navigation-event))
   :the-ville? (experiments/the-ville? data)
   :promo      (promotion-to-advertise data)})

(defn built-component [data opts]
  (component/build component (query data) opts))
