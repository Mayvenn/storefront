(ns mayvenn.concept.funnel
  (:require [storefront.events :as e]
            [storefront.platform.messages :refer [handle-message] :rename {handle-message publish}]
            [storefront.trackings :as trk]
            [storefront.transitions :as t]
            #?(:cljs
               [storefront.hooks.stringer :as stringer])))

(defmethod trk/perform-track e/funnel|acquisition|prompted
  [_ _ args _ _]
  #?(:cljs
     (->> (clj->js args :keyword-fn (comp str symbol))
          (stringer/track-event "funnel.acquisition/prompted"))))

(defmethod trk/perform-track e/funnel|acquisition|succeeded
  [_ _ args _ _]
  #?(:cljs
  (->> (clj->js args :keyword-fn (comp str symbol))
       (stringer/track-event "funnel.acquisition/succeeded"))))

(defmethod trk/perform-track e/funnel|acquisition|failed
  [_ _ args _ _]
  #?(:cljs
  (->> (clj->js args :keyword-fn (comp str symbol))
       (stringer/track-event "funnel.acquisition/failed"))))

;; TODO Build up a model of the funnel in state
