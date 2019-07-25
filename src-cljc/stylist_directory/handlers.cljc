(ns stylist-directory.handlers
  (:require [stylist-directory.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.transitions :as transitions]))

(defmethod transitions/transition-state events/api-success-fetch-stylist-details
  [_ _ {:keys [stylist]} app-state]
  (-> app-state
      (assoc-in (conj keypaths/stylists (:stylist-id stylist)) stylist)))
