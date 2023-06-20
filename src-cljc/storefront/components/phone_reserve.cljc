(ns storefront.components.phone-reserve
  (:require [storefront.effects :as fx]
            [storefront.trackings :as trk]
            [storefront.events :as e]
            [storefront.platform.messages
             :as messages
             :refer [handle-message handle-later] :rename {handle-message publish}]
            #?(:cljs [storefront.hooks.stringer :as stringer])))

(def monfort "+1 (469) 216-5724")

(defmethod fx/perform-effects e/phone-reserve-cta-click
  [_ _ {:keys [number]} _ _]
  (publish e/external-redirect-phone {:number number}))

(defmethod trk/perform-track e/phone-reserve-cta-click
  [_ _ {:keys [number retail-location]} _]
  (->> {:number          number
        :retail-location retail-location}
       #?(:cljs (stringer/track-event "phone_reserve_cta_clicked"))))

(defmethod trk/perform-track e/phone-reserve-cta-impression
  [_ _ {:keys [number retail-location]} _]
  (->> {:number          number
        :retail-location retail-location}
       #?(:cljs (stringer/track-event "phone_reserve_cta_impression"))))
