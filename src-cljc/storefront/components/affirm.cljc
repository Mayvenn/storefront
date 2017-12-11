(ns storefront.components.affirm
  (:require
   #?@(:cljs [[storefront.component :as component]
              [om.core :as om]
              [storefront.hooks.affirm :as affirm]]
       :clj  [[storefront.component-shim :as component]])
   [storefront.events :as events]
   [storefront.platform.messages :as m]
   [storefront.transitions :as transitions]
   [storefront.components.money-formatters :as mf]
   [storefront.keypaths :as keypaths]
   [storefront.effects :as effects]))


(defn ^:private type->promo-id [t]
  (get {:text-only "promo_set_category"} t "promo_set_pdp"))

(defn ^:private as-low-as-html [data]
  (component/html
   [:a.affirm-as-low-as.mx2.dark-gray
    {:data-promo-id       (type->promo-id (:type data))
     :data-amount         (mf/as-cents (:amount data))
     :data-learnmore-show (or (:show-learnmore data) false)
     :on-click            (fn [event]
                            (.preventDefault event))}]))

(defn ^:private modal-html [data]
  (component/html
   [:a.inline-block.affirm-product-modal.navy.underline
    {:data-promo-id "promo_set_pdp"
     :data-amount (mf/as-cents (:amount data))
     :data-learnmore-show (or (:show-learnmore data) false)}
    "Learn more."]))

(defn as-low-as-component [data owner _]
  #?(:cljs (reify
             om/IDidUpdate
             (did-update [this _ _]
               (m/handle-message events/affirm-request-refresh))
             om/IDidMount
             (did-mount [this]
               (m/handle-message events/affirm-request-refresh))
             om/IRender
             (render [_]
               (as-low-as-html data)))
     :clj (as-low-as-html data)))

(defn modal-component [data owner _]
  #?(:cljs (reify
             om/IDidUpdate
             (did-update [this _ _]
               (m/handle-message events/affirm-request-refresh))
             om/IDidMount
             (did-mount [this]
               (m/handle-message events/affirm-request-refresh {}))
             om/IRender
             (render [_] (modal-html data)))
     :clj (modal-html data)))

(defn as-low-as-box [data]
  (when (>= (:amount data) 50)  ; Affirm doesn't support items less than $50
    [:div.py3
     [:div.center.border.rounded.border-aqua.col-12.py1.mx-auto
      [:div.mx1.dark-gray.h6.py1
       [:p.h6 (component/build as-low-as-component data {})]
       [:p.h6 (:middle-copy data) " " (component/build modal-component data {})]]]]))

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
