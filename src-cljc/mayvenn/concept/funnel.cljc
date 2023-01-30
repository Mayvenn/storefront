(ns mayvenn.concept.funnel
  (:require [storefront.effects :as fx]
            [storefront.events :as e]
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

(defmethod fx/perform-effects e/funnel|acquisition|succeeded
  [_ _ {:keys [prompt action]} state _]
  (let [{:keys [trigger-id template-id test-description hdyhau]} (:keys/email-modal prompt)]
    (publish e/biz|email-capture|captured
             {:trigger-id            trigger-id
              :variation-description test-description
              :template-content-id   template-id
              :hdyhau                hdyhau
              :email                 (:auth.email/id action)})
    (when-let [phone-number (:auth.sms/id action)]
      (publish e/biz|sms-capture|captured
               {:trigger-id            trigger-id
                :variation-description test-description
                :template-content-id   template-id
                :phone                 phone-number}))))

;; TODO Build up a model of the funnel in state
