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
   [storefront.keypaths :as keypaths]
   [storefront.effects :as effects]))

(defn ^:private as-low-as-html [data]
  (component/html
   [:a.affirm-as-low-as.mx2
    {:data-promo-id "promo_set_default"
     :data-amount (mf/as-cents (:amount data))
     :on-click (fn [event]
                 (.preventDefault event))}
    "Learn more"]))

(def ^:private modal-html
  (component/html
   [:a.inline-block.affirm-site-modal.navy.underline
    {:data-promo-id "promo_set_default"}
    "Learn more."]))

(defn as-low-as-component [data owner]
  #?(:cljs (reify
             om/IDidMount
             (did-mount [this]
               (m/handle-message events/affirm-request-refresh))
             om/IRender
             (render [_]
               (as-low-as-html data)))
     :clj (as-low-as-html data)))

(defn modal-component [data owner]
  #?(:cljs (reify
             om/IDidMount
             (did-mount [this]
               (m/handle-message events/affirm-request-refresh {}))
             om/IRender
             (render [_] modal-html))
     :clj modal-html))

(defn as-low-as-box [data]
  [:div.py3
   [:div.center.border.rounded.border-aqua.col-12.py1.col-6-on-tb-dt.mx-auto
    [:div.col-10.mx-auto.dark-gray.h6.py1
     (component/build as-low-as-component data {})
     #_[:span "Continue with Affirm below."]]]])

(defn ^:private reset-refresh-timeout [timeout f]
  #?(:cljs
     (do (js/clearTimeout timeout)
         (js/setTimeout f 50))))

(defmethod transitions/transition-state events/affirm-request-refresh [_ _ _ app-state]
  #?(:cljs (update-in app-state
                      keypaths/affirm-refresh-timeout
                      reset-refresh-timeout
                      #(m/handle-message events/affirm-perform-refresh))))

(defmethod effects/perform-effects events/affirm-perform-refresh [_ _ _ _ _]
  #?(:cljs (affirm/refresh)))
