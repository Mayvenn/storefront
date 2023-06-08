(ns storefront.components.phone-consult
  (:require [storefront.accessors.experiments :as experiments]
            [storefront.component :as c :refer [defcomponent]]
            [storefront.components.share-links :as share-links]
            [storefront.effects :as fx]
            [storefront.trackings :as trk]
            [storefront.events :as e]
            [catalog.cms-dynamic-content :as cms-dynamic-content]
            [storefront.platform.component-utils :as utils]
            #?(:cljs [storefront.hooks.stringer :as stringer])))

(def support-phone-number "+1 (888) 562-7952")

(defmethod fx/perform-effects e/external-redirect-phone
  [_ _ {:keys [number]} _ _]
  #?(:cljs
     (set! (.-location js/window) (share-links/phone-link number))))

(defmethod trk/perform-track e/external-redirect-phone
  [_ _ {:keys [number]} _]
  (->> {:number number}
       #?(:cljs (stringer/track-event "external-redirect-phone"))))

(defcomponent component
  [{:keys [message-rich-text released] :as data} owner _]
  (when released
    [:a.block.black.m1.border.p4.center.black
     (utils/fake-href e/external-redirect-phone {:number support-phone-number})
