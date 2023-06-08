(ns storefront.components.phone-consult
  (:require [storefront.accessors.experiments :as experiments]
            [storefront.component :as c :refer [defcomponent]]
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
            #?(:cljs [storefront.hooks.stringer :as stringer])))

(def support-phone-number "+1 (888) 562-7952")

(defmethod fx/perform-effects e/phone-consult-cta-click
  [_ _ {:keys [number]} _ _]
  (publish e/external-redirect-phone {:number support-phone-number}))

(defmethod trk/perform-track e/phone-consult-cta-click
  [_ _ {:keys [number place-id]} _]
  (->> {:number number
        :place-id place-id}
       #?(:cljs (stringer/track-event "external-redirect-phone"))))

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
  [_ _ {:keys [number]} _]
  (->> {:number number}
       #?(:cljs (stringer/track-event "external-redirect-phone"))))

(defcomponent component
  [{:keys [message-rich-text released place-id] :as data} owner _]
  (when released
    [:a.block.black.m1.border.p4.center.black
     (utils/fake-href e/phone-consult-cta-click
                      {:number support-phone-number
                       :place-id place-id})
     (map cms-dynamic-content/build-hiccup-tag (:content message-rich-text))
     (when (seq (:order/items data))
       (str "Ref: " (->> data :waiter/order :number)))]))
