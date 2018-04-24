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
   [storefront.components.svg :as svg]
   [storefront.keypaths :as keypaths]
   [storefront.effects :as effects]))

(defn valid-order-total? [amount-in-usd]  ; Affirm doesn't support items less than $50
  (>= amount-in-usd 50))

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

(defn ^:private modal-html [data content link-classes]
  (component/html
   [:a.inline-block.affirm-product-modal
    {:class link-classes
     :data-promo-id "promo_set_pdp"
     :data-amount (mf/as-cents (:amount data))
     :data-learnmore-show (or (:show-learnmore data) false)}
    content]))

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

(defn modal-component [data owner {:keys [link-classes content]}]
  #?(:cljs (reify
             om/IDidUpdate
             (did-update [this _ _]
               (m/handle-message events/affirm-request-refresh))
             om/IDidMount
             (did-mount [this]
               (m/handle-message events/affirm-request-refresh {}))
             om/IRender
             (render [_] (modal-html data content link-classes)))
     :clj (modal-html data content link-classes)))

(defn as-low-as-box [data]
  (when (valid-order-total? (:amount data))
    [:div.py3
     [:div.center.border.rounded.border-aqua.col-12.py1.mx-auto
      [:div.mx1.dark-gray.h6.py1
       [:p.h6 (component/build as-low-as-component data {})]
       [:p.h6 (:middle-copy data) " " (component/build modal-component data {:opts {:content "Learn more."
                                                                                    :link-classes "navy underline"}})]]]]))
(defn auto-complete-as-low-as-box [data]
  (when (valid-order-total? (:amount data))
    [:div.center.col-12.py1.mx-auto
     [:div.mx1.dark-gray.h6.py1
      [:p.h5.flex.justify-center.black
       (component/build as-low-as-component data {})
       (component/build modal-component data {:opts {:link-classes "flex self-center mxn1"
                                                     :content      (svg/question-circle {:width "1em"
                                                                                         :height "1em"})}})]]]))

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
