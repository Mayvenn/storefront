(ns mayvenn.live-help.core
  (:require #?(:cljs [storefront.hooks.kustomer :as kustomer])
            [storefront.accessors.experiments :as experiments]
            [storefront.keypaths :as k]
            [storefront.effects :as fx]
            [storefront.events :as e]))

(defmethod fx/perform-effects e/flow|live-help|reset
  [_ _ _ _ state]
  (when (and (experiments/live-help? state)
             (not (get-in state k/loaded-kustomer)))
    #?(:cljs (kustomer/init))))

(defmethod fx/perform-effects e/flow|live-help|opened
  [_ _ _ _ state]
  (when (experiments/live-help? state)
    #?(:cljs (kustomer/open-conversation))))
