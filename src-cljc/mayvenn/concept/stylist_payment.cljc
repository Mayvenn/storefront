(ns mayvenn.concept.stylist-payment
  "
  Stylist Pay Pilot

  Caveats: This only handles one stylist payment at a time, :current
  "
  (:require [storefront.events :as e]
            [storefront.keypaths :as k]
            [storefront.platform.messages
             :as messages
             :refer [handle-message] :rename {handle-message publish}]
            #?@(:cljs
                [[storefront.api :as api]
                 [storefront.hooks.stripe :as stripe]])
            [storefront.transitions :as t]
            [storefront.effects :as fx]
            [mayvenn.visual.tools :refer [with]])) ;; TODO(corey) maybe this is just visual?

(def k-current (conj k/models-stylist-payments :current))

;; records

;;; schema
;;; amount
;;; note
;;; cardholder - name
;;; token
;;; stylist-id
;;; payment-id
;;; state #{reset,prepared,requested,sent,failed}

(defn <- [state id]
  (-> state
      (get-in (conj k/models-stylist-payments id))
      (update :stylist-payment/amount * 100)))

;; behavior

;;; event: Reset
;;; <- initialized, via navigation
(defmethod t/transition-state
  e/stylist-payment|reset
  [_ _ {:stylist/keys [id]} state]
  (assoc-in state
            k-current
            {:state      "reset"
             :stylist/id id
             :idem/id    (str #?(:cljs (random-uuid)))}))

;;; event: Prepared
;;; <- payment info from customer
;;; -> acquiring token
(defmethod t/transition-state
  e/stylist-payment|prepared
  [_ _ _ state]
  (assoc-in state
            (conj k-current :state)
            "prepared"))

(defmethod fx/perform-effects
  e/stylist-payment|prepared
  [_ _ _ _ state]
  (let [cardholder  (->> (<- state :current)
                         (with :cardholder))
        card-element (get-in state k/stripe-card-element)]
    (publish e/stripe|create-token|requested
             {:on/success e/stylist-payment|requested
              :on/failure e/stylist-payment|failed
              :card-holder  cardholder
              :card-element card-element})))

;;; event: Requested
;;; <- token acquired
;;; -> acquiring charge
(defmethod t/transition-state
  e/stylist-payment|requested
  [_ _ create-token-result state]
  ;; TODO(corey) pare down what is saved
  (let [token (:token create-token-result)]
    (prn token)
    (-> state
        (assoc-in (conj k-current :state)
                  "requested")
        (assoc-in (conj k-current :stripe/source)
                  (:id token))
        (assoc-in (conj k-current :stripe/description)
                  (->> (:card token)
                       ((juxt :brand :funding :last4))
                       (interpose " ")
                       (apply str))))))

(defmethod fx/perform-effects
  e/stylist-payment|requested
  [_ _ create-token-result _ state]
  (let [session-id      (get-in state k/session-id)
        stylist-payment (<- state :current)]
    #?(:cljs
       (api/send-stylist-payment
        session-id
        {:amount         (:stylist-payment/amount stylist-payment)
         :note           (:stylist-payment/note stylist-payment)
         :source         (:stripe/source stylist-payment)
         :stylist-id     (:stylist/id stylist-payment)
         :idempotent-key (:idem/id stylist-payment)}
        e/stylist-payment|sent
        e/stylist-payment|failed))))

;;; event: Failed
;;; <- token failure
;;; <- charge failure
(defmethod t/transition-state
  e/stylist-payment|failed
  [_ _ result state]
  (cond-> state
    (= :error (:failure result))
    (assoc-in (conj k/errors :field-errors ["card-error"] :long-message)
              (-> result :response :body :error-message))
    :always
    (assoc-in (conj k-current :state) "failed")))

;;; event: Sent
;;; <- charge acquired
;;; -> receipt display
(defmethod t/transition-state
  e/stylist-payment|sent
  [_ _ charge-result state]
  (cond-> state
    (:command/success charge-result)
    (assoc-in (conj k-current :state) "sent")))

;;; TODO(corey) this needs a new home
(defmethod fx/perform-effects
  e/stripe|create-token|requested
  [_ _ {:on/keys [success failure]
        :keys [cardholder card-element]} _ _]
  #?(:cljs
     (stripe/create-token* card-element cardholder
                           success failure)))
