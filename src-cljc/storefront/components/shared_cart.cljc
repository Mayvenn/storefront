(ns storefront.components.shared-cart
  (:require #?(:cljs [storefront.api :as api])
            [catalog.products :as products]
            [spice.maps :as maps]
            [storefront.accessors.promos :as promos]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]
            [storefront.transitions :as transitions]
            [storefront.accessors.experiments :as experiments]))

(defcomponent component
  [{:keys [spinning? shared-cart-id shared-cart-promotion store fetching-products? creating-cart? advertised-promo]}
   owner
   opts]
  (let [{:keys [portrait store-nickname]} store]
    (if spinning?
      [:div.container.p4
       ui/spinner]
      [:div.container.p4
       [:div.pb3
        (when (:resizable-url portrait)
          [:div.mb2.h2
           (ui/circle-picture {:class "mx-auto"} (ui/square-image portrait 96))])
        [:p.center.h3.medium {:data-test "cart-creator-nickname"}
         store-nickname " has created a bag for you!"]]
       [:div.flex.items-center.px1.py3.border-top.border-bottom
        (ui/ucare-img {:width 90} "8787e30c-2879-4a43-8d01-9d6790575084")
        [:div.ml2.flex-auto
         [:p.medium.shout.mb2 "Free shipping & 30 day guarantee"]
         [:p "Shop with confidence: Wear it, dye it, even color it. "
          "If you do not love your Mayvenn hair we will exchange it within 30 days of purchase!"]]]
       [:div.p3.h4.center
        (or (:description shared-cart-promotion)
            (:description advertised-promo))]
       [:form
        {:on-submit (utils/send-event-callback events/control-create-order-from-shared-cart
                                               {:shared-cart-id shared-cart-id})}
        (ui/submit-button "View your bag"
                          {:data-test "create-order-from-shared-cart"
                           :spinning? (or fetching-products?
                                          creating-cart?)
                           :disabled? (or fetching-products?
                                          creating-cart?)})]])))

(defn shared-cart-promotion
  [data]
  (let [shared-cart    (get-in data keypaths/shared-cart-current)
        promotion-code (some-> shared-cart :promotion-codes first)
        all-promotions (get-in data keypaths/promotions)]
    (first (filter #(= promotion-code (:code %)) all-promotions))))

(defn query
  [data]
  {:shared-cart-id        (get-in data keypaths/shared-cart-id)
   :shared-cart-promotion (shared-cart-promotion data)
   :store                 (or ;; If stylist fails to be fetched, then it falls back to current store
                           (get-in data keypaths/shared-cart-creator)
                           (get-in data keypaths/store))
   :advertised-promo      (promos/default-advertised-promotion (get-in data keypaths/promotions))
   :fetching-products?    (utils/requesting? data (conj request-keys/get-products {}))
   :creating-cart?        (utils/requesting? data request-keys/create-order-from-shared-cart)
   :spinning?             (utils/requesting? data request-keys/fetch-shared-cart)})

(defn ^:export built-component
  [data opts]
  (component/build component (query data) opts))

(defmethod transitions/transition-state events/api-success-shared-cart-fetch
  [_ event {:as args :keys [shared-cart skus products shared-cart-creator]} app-state]
  (-> app-state
      (assoc-in keypaths/shared-cart-current shared-cart)
      (assoc-in keypaths/shared-cart-creator shared-cart-creator)
      (update-in keypaths/v2-skus merge (products/index-skus skus))
      (update-in keypaths/v2-products merge (products/index-products products))))

;; TODO: make this work server side
;; TODO: destructuring in the look detail page is throwing an exception (around gathering images?)
(defmethod transitions/transition-state events/api-success-shared-carts-fetch
  [_ event {:keys [carts skus images]} app-state]
  (-> app-state
      (update-in keypaths/v2-skus merge (catalog.products/index-skus (vals skus)))
      (update-in keypaths/v2-images merge (maps/map-keys (fnil name "") images))
      (update-in keypaths/v1-looks-shared-carts merge (maps/index-by :number carts))))

(defmethod effects/perform-effects events/api-success-shared-cart-fetch
  [_ _ _ _ app-state]
  #?(:cljs
     (api/get-promotions (get-in app-state keypaths/api-cache)
                         (some-> app-state
                                 (get-in keypaths/shared-cart-current)
                                 :promotion-codes
                                 first))))

(defmethod transitions/transition-state events/navigate-shared-cart
  [_ event {:keys [shared-cart-id]} app-state]
  (assoc-in app-state keypaths/shared-cart-id shared-cart-id))

(defmethod effects/perform-effects events/navigate-shared-cart
  [_ _ {:keys [shared-cart-id]} _ app-state]
  #?(:cljs (api/fetch-shared-cart shared-cart-id)))
