(ns mayvenn.concept.stylist-payment
  "
  Stylist Pay Pilot

  Caveats: This only handles one stylist payment at a time, :current
  "
  (:require [clojure.string :refer [includes? ends-with?]]
            [storefront.events :as e]
            [storefront.keypaths :as k]
            [storefront.platform.messages
             :as messages
             :refer [handle-message] :rename {handle-message publish}]
            #?@(:cljs
                [[storefront.api :as api]
                 [storefront.hooks.stripe :as stripe]
                 [storefront.hooks.stringer :as stringer]
                 [storefront.browser.scroll :as scroll]])
            [storefront.transitions :as t]
            [storefront.trackings :as tt]
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

(defn ^:private invalid-email?
  [email]
  (not (and (seq email)
            (< 3 (count email))
            (includes? email "@")
            (not (ends-with? email "@")))))

(defn <- [state id]
  (let [{:as stylist-payment
         :stylist-payment/keys [amount email]}
        (-> state
            (get-in (conj k/models-stylist-payments id))
            (update :stylist-payment/amount * 100))]
    (assoc stylist-payment
           :valid (and (number? amount)
                       (not (invalid-email? email))))))

(defn <-tracking
  [state id]
  (-> (<- state id)
      (dissoc :stripe/token)))

;; behavior

;;; event: Reset
;;; <- initialized, via navigation
(defmethod t/transition-state e/stylist-payment|reset
  [_ _ {:stylist/keys [id]} state]
  (assoc-in state
            k-current
            {:state      "reset"
             :stylist/id id
             :idem/id    (str #?(:cljs (random-uuid)))}))

(defmethod tt/perform-track e/stylist-payment|reset
  [_ _ _ state]
  (let [data (merge
              (<-tracking state :current)
              {:session-id (get-in state k/session-id)})]
    #?(:cljs
       (stringer/track-event "stylist_payment_reset"
                             data))))

;;; event: Prepared
;;; <- payment info from customer
;;; -> acquiring token
(defmethod t/transition-state e/stylist-payment|prepared
  [_ _ _ state]
  (assoc-in state
            (conj k-current :state)
            "prepared"))

(defmethod fx/perform-effects e/stylist-payment|prepared
  [_ _ _ _ state]
  (let [cardholder   (->> (<- state :current)
                          (with :cardholder))
        card-element (get-in state k/stripe-card-element)]
    (publish e/stripe|create-token|requested
             {:on/success   e/stylist-payment|requested
              :on/failure   e/stylist-payment|failed
              :cardholder   cardholder
              :card-element card-element})))

(defmethod tt/perform-track e/stylist-payment|prepared
  [_ _ _ state]
  (let [data (merge
              (<-tracking state :current)
              {:session-id (get-in state k/session-id)})]
    #?(:cljs
       (stringer/track-event "stylist_payment_prepared"
                             data))))

;;; event: Requested
;;; <- token acquired
;;; -> acquiring charge
(defmethod t/transition-state e/stylist-payment|requested
  [_ _ create-token-result state]
  (let [token (:token create-token-result)]
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

(defmethod fx/perform-effects e/stylist-payment|requested
  [_ _ create-token-result _ state]
  #?(:cljs
     (scroll/snap-to-top))
  (let [session-id (get-in state k/session-id)
        {:stylist-payment/keys [amount note email phone]
         :as stylist-payment}
        (<- state :current)]
    #?(:cljs
       (api/send-stylist-payment
        session-id
        {:amount         amount
         :payee-email    email
         :payee-phone    phone
         :note           note
         :source         (:stripe/source stylist-payment)
         :stylist-id     (:stylist/id stylist-payment)
         :idempotent-key (:idem/id stylist-payment)}
        e/stylist-payment|sent
        e/stylist-payment|failed))))

(defmethod tt/perform-track e/stylist-payment|requested
  [_ _ _ state]
  (let [data (merge
              (<-tracking state :current)
              {:session-id (get-in state k/session-id)})]
    #?(:cljs
       (stringer/track-event "stylist_payment_requested"
                             data))))

;;; event: Failed
;;; <- token failure
;;; <- charge failure
(defmethod t/transition-state e/stylist-payment|failed
  [_ _ result state]
  (cond-> state
    (= :error (:failure result))
    (assoc-in (conj k/errors :field-errors ["card-error"] :long-message)
              (-> result :response :body :error-message))
    :always
    (assoc-in (conj k-current :state) "failed")))

(defmethod tt/perform-track e/stylist-payment|failed
  [_ _ _ state]
  (let [data (merge
              (<-tracking state :current)
              {:session-id (get-in state k/session-id)})]
    #?(:cljs
       (stringer/track-event "stylist_payment_failed"
                             data))))

;;; event: Sent
;;; <- charge acquired
;;; -> receipt display
(defmethod t/transition-state e/stylist-payment|sent
  [_ _ charge-result state]
  (if-not (:command/success charge-result)
    state
    (-> state
        (assoc-in (conj k-current
                        :stylist-payment/payment-id)
                  (:payment-id charge-result))
        (assoc-in (conj k-current :state)
                  "sent"))))

(defmethod tt/perform-track e/stylist-payment|sent
  [_ _ _ state]
  (let [data (merge
              (<-tracking state :current)
              {:session-id (get-in state k/session-id)})]
    (when (:opt-in data)
      #?(:cljs
         (stringer/identify {:email (:stylist-payment/email data)}))
      #?(:cljs
         (stringer/track-event "email_capture-capture"
                               {:email            (:stylist-payment/email data)
                                :email-capture-id "stylist-pay"
                                :store-slug       (get-in state k/store-slug)
                                :store-experience (get-in state k/store-experience)})))
    #?(:cljs
       (stringer/track-event "stylist_payment_sent"
                             data))))

;;;;;;
;;;;;;

;;; TODO(corey) this needs a new home
(defmethod fx/perform-effects e/stripe|create-token|requested
  [_ _ {:on/keys [success failure]
        :keys [cardholder card-element]} _ _]
  #?(:cljs
     (stripe/create-token* card-element cardholder
                           success failure)))
