(ns storefront.components.phone-consult
  (:require [storefront.accessors.experiments :as experiments]
            [storefront.component :as c :refer [defdynamic-component]]
            [storefront.components.share-links :as share-links]
            [storefront.transitions :as t]
            [storefront.effects :as fx]
            [storefront.trackings :as trk]
            [storefront.events :as e]
            [catalog.cms-dynamic-content :as cms-dynamic-content]
            [storefront.platform.messages
             :as messages
             :refer [handle-message handle-later] :rename {handle-message publish}]
            [storefront.platform.component-utils :as utils]
            #?(:cljs [storefront.api :as api])
            #?(:cljs [storefront.hooks.stringer :as stringer])
            [storefront.components.ui :as ui]))

(def support-phone-number "+1 (888) 562-7952")

(defmethod fx/perform-effects e/phone-consult-cta-click
  [_ _ {:keys [number]} _ _]
  (publish e/external-redirect-phone {:number support-phone-number}))

(defmethod trk/perform-track e/phone-consult-cta-click
  [_ _ {:keys [number place-id]} _]
  (->> {:number number
        :place-id place-id}
       #?(:cljs (stringer/track-event "phone_consult_cta_clicked"))))

(defmethod fx/perform-effects e/external-redirect-phone
  [_ _ {:keys [number]} _ _]
  #?(:cljs
     (set! (.-location js/window) (share-links/phone-link number))))

(defmethod t/transition-state e/phone-consult-cta-poll
  [_ _ {:keys [number]} state]
  (-> state
      (assoc :polling-consult true)))

(defmethod fx/perform-effects e/phone-consult-cta-poll
  [_ _ {:keys [number]} _ state]
  #?(:cljs (api/fetch-cms2 [:phoneConsultCta] identity))
  (handle-later e/phone-consult-cta-poll {} (* 1000 60 5)))

(defmethod trk/perform-track e/phone-consult-cta-impression
  [_ _ {:keys [number place-id]} _]
  (->> {:number number
        :place-id place-id}
       #?(:cljs (stringer/track-event "external-redirect-phone"))))

(defdynamic-component component
  (did-mount
   [this]
   (let [{:keys [place-id]} (c/get-props this)]
     (publish e/phone-consult-cta-impression {:number   support-phone-number
                                              :place-id place-id})))
  (render
   [this]
   (c/html
    (let [{:keys [message-rich-text released place-id shop-or-omni in-omni?] :as data} (c/get-props this)]
      (when (and released
                 ;; shop-or-omni (shop = true, omni = false)
                 (= shop-or-omni (not in-omni?)))
        [:a.block.black.m1.border.p4.center.black
         (utils/fake-href e/phone-consult-cta-click
                          {:number   support-phone-number
                           :place-id place-id})
         (map cms-dynamic-content/build-hiccup-tag (:content message-rich-text))
         (when (seq (:order/items data))
           (str "Ref: " (->> data :waiter/order :number)))
         [:div.flex.justify-center
          (ui/button-small-primary {} "Call Now")]])))))
