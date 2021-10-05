(ns checkout.free-install
  (:require
   #?@(:cljs [[storefront.accessors.auth :as auth]
              [storefront.api :as api]
              [storefront.history :as history]])
   [storefront.component :as component]
   [storefront.components.ui :as ui]
   [storefront.events :as events]
   [storefront.keypaths :as keypaths]
   [storefront.platform.messages :as messages]
   [storefront.platform.component-utils :as utils]
   [storefront.effects :as effects]
   [adventure.components.layered :as layered]))

(component/defcomponent component
  [data _ _]
  [:div.container.p2
   {:style {:max-width "580px"}}
   [:div.title-2.canela.center.mt6.mb3
    "Your Install Is On Us"]
   [:div.center.col-11.mx-auto
    "You qualify for our complimentary Mayvenn Install service. No catch - it's free."
    (ui/button-small-underline-primary (merge {:class "ml1"}
                                              (utils/fake-href
                                               events/popup-show-consolidated-cart-free-install)) "LEARN MORE")]
   (component/build layered/shop-framed-checklist {:header/value "What's included?"
                                                   :bullets ["Shampoo" "Braid down" "Sew-in and style" "Paid for by Mayvenn"]
                                                   :divider-img nil})
   [:div.flex.flex-column.items-center
    (ui/button-medium-primary (merge {:class "mb3" :style {:width "275px"}}
                                     (utils/fake-href events/control-checkout-free-install-added)) "Add My Free Install")
    (ui/button-medium-underline-primary (merge {:class "mb6"}
                                               (utils/fake-href events/control-checkout-free-install-skipped)) "Skip & continue")]])

(defn query [state])

(defn ^:export built-component [data opts]
  (component/build component (query data) opts))

(defn ^:private continue [app-state]
  #?(:cljs
     (if (-> app-state auth/signed-in :storefront.accessors.auth/at-all)
       (history/enqueue-navigate events/navigate-checkout-address {})
       (history/enqueue-navigate events/navigate-checkout-returning-or-guest {}))))

(defmethod effects/perform-effects events/control-checkout-free-install-added
  [_ _ args _ app-state]
  (messages/handle-message events/upsold-free-install args))

(defmethod effects/perform-effects events/control-checkout-free-install-skipped
  [_ _ _ _ app-state]
  (continue app-state))

(defmethod effects/perform-effects events/upsold-free-install
  [_ _ _ _ app-state]
  #?(:cljs
     (api/add-sku-to-bag
      (get-in app-state keypaths/session-id)
      {:sku                {:catalog/sku-id "SV2-LBI-X"}
       :quantity           1
       :stylist-id         (get-in app-state keypaths/store-stylist-id)
       :token              (get-in app-state keypaths/order-token)
       :number             (get-in app-state keypaths/order-number)
       :user-id            (get-in app-state keypaths/user-id)
       :user-token         (get-in app-state keypaths/user-token)
       :heat-feature-flags (get-in app-state keypaths/features)}
      #(do
         (messages/handle-message events/api-success-add-sku-to-bag
                                  {:order    %
                                   :quantity 1
                                   :sku      {:catalog/sku-id "SV2-LBI-X"}})
         (continue app-state)))))
