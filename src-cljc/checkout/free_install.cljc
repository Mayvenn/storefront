(ns checkout.free-install
  (:require
   #?@(:cljs [[storefront.accessors.auth :as auth]
              [storefront.api :as api]
              [storefront.history :as history]
              [storefront.trackings :refer [perform-track]]
              [storefront.hooks.stringer :as stringer]])
   [storefront.component :as component]
   [storefront.components.ui :as ui]
   [storefront.events :as events]
   [storefront.keypaths :as keypaths]
   [storefront.platform.messages :as messages]
   [storefront.platform.component-utils :as utils]
   [storefront.effects :as effects]
   [adventure.components.layered :as layered]))

(component/defcomponent component
  [{:keys [heading shop-framed-checklist ctas] :as thing} _ _]
  [:div.container.p2
   {:style {:max-width "580px"}}
  [:div.title-2.canela.center.mt6.mb3
    (-> heading :copy :primary)]
   [:div.center.col-11.mx-auto
    (-> heading :copy :secondary)
    (ui/button-small-underline-primary (merge {:class "ml1"}
                                              (utils/fake-href
                                               (-> heading :popup :target))) (-> heading :popup :label))]
   (component/build layered/shop-framed-checklist shop-framed-checklist)
   [:div.flex.flex-column.items-center
    (ui/button-medium-primary (merge {:class "mb3" :style {:width "275px"}}
                                     (-> ctas :upsell-button :target)) (-> ctas :upsell-button :label))
    (ui/button-medium-underline-primary (merge {:class "mb6"}
                                               (-> ctas :continue-button :target)) (-> ctas :continue-button :label))]])

(defn query [state]
  {:heading               {:popup {:target events/popup-show-consolidated-cart-free-install
                                   :label  "LEARN MORE"}
                           :copy  {:primary   "Your Install Is On Us"
                                   :secondary "You qualify for our complimentary Mayvenn Install service. No catch - it's free."}}
   :shop-framed-checklist {:header/value "What's included?"
                           :bullets      ["Shampoo"
                                          "Braid down"
                                          "Sew-in and style"
                                          "Paid for by Mayvenn"]}
   :ctas                  {:upsell-button   {:target (utils/fake-href events/control-checkout-free-install-added {:query-params {:free-install-added true}})
                                             :label  "Add My Free Install"}
                           :continue-button {:target (utils/fake-href events/control-checkout-free-install-skipped)
                                             :label  "Skip & continue"}}})

(defn ^:export built-component [data opts]
  (component/build component (query data) opts))

(defn ^:private continue [app-state args]
  #?(:cljs
     (if (-> app-state auth/signed-in :storefront.accessors.auth/at-all)
       (history/enqueue-navigate events/navigate-checkout-address args)
       (history/enqueue-navigate events/navigate-checkout-returning-or-guest args))))

(defmethod effects/perform-effects events/control-checkout-free-install-added
  [_ _ args _ app-state]
  (messages/handle-message events/free-install-upsold args))

(defmethod effects/perform-effects events/control-checkout-free-install-skipped
  [_ _ _ _ app-state]
  (continue app-state {}))

#?(:cljs
 (defmethod perform-track events/control-checkout-free-install-skipped
   [_ event args app-state]
   (stringer/track-event "add_free_install-skipped")))

(defmethod effects/perform-effects events/free-install-upsold
  [_ _ args _ app-state]
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
         (continue app-state args)))))
