(ns storefront.components.affirm
  (:require
   #?@(:cljs [[storefront.component :as component]
              [om.core :as om]
              [storefront.hooks.affirm :as affirm]]
       :clj  [[storefront.component-shim :as component]])
   [storefront.platform.component-utils :as utils]
   [storefront.events :as events]
   [storefront.keypaths :as keypaths]
   [storefront.components.ui :as ui]
   [storefront.platform.messages :as m]
   [storefront.transitions :as transitions]
   [storefront.components.money-formatters :as mf]
   [catalog.keypaths]
   [storefront.effects :as effects]))

(defn ^:private reset-refresh-timeout [timeout f]
  #?(:cljs
     (do (js/clearTimeout timeout)
         (js/setTimeout f 50))))

(defn ^:private product-card [data]
  (component/html
   [:a.dark-gray.h7.affirm-as-low-as.mx2
    {:data-promo-id "promo_set_default"
     :data-amount (mf/as-cents (:amount data))
     :on-click (fn [event]
                 (.preventDefault event))}
    "Learn more"]))

(defn product-card-component [data owner]
  #?(:cljs (reify
             om/IDidMount
             (did-mount [this]
               (m/handle-message events/affirm-product-card-mounted))
             om/IRender
             (render [_]
               (product-card data)))
     :clj (product-card data)))

(def ^:private modal-html
  (component/html
   [:a.inline-block.affirm-site-modal.navy.underline
    {:data-promo-id "promo_set_default"}
    "Learn more."]))

(defn modal-component [data owner]
  #?(:cljs (reify
             om/IDidMount
             (did-mount [this]
               (m/handle-message events/affirm-modal-refresh {}))
             om/IRender
             (render [_] modal-html))
     :clj modal-html))

(defmethod transitions/transition-state events/affirm-product-card-mounted [_ _ _ app-state]
  #?(:cljs (update-in app-state
                      catalog.keypaths/affirm-product-card-refresh-timeout
                      reset-refresh-timeout
                      #(m/handle-message events/affirm-product-card-refresh))))

(defmethod effects/perform-effects events/affirm-product-card-refresh [_ _ _ _ _]
  #?(:cljs (affirm/refresh)))

(defmethod effects/perform-effects events/affirm-modal-refresh [_ _ _ _ _]
  #?(:cljs (affirm/refresh)))
