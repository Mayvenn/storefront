(ns email-verification.core
  (:require #?@(:cljs [[storefront.api :as api]
                       [storefront.history :as history]
                       [storefront.accessors.auth :as auth]
                       [storefront.browser.cookie-jar :as cookie-jar]])
            [storefront.components.ui :as ui]
            [storefront.config :as config]
            [storefront.platform.component-utils :as utils]
            [storefront.events :as e]
            [storefront.effects :as effects]
            [storefront.utils.query :as query]
            [storefront.keypaths :as keypaths]
            [storefront.transitions :as transitions]
            [storefront.component :as component]
            [storefront.request-keys :as request-keys]
            [storefront.components.flash :as flash]
            [clojure.string :as string]))

(component/defcomponent template
  [{:email-verification/keys      [id email-address]
    :email-verification--cta/keys [spinning? disabled? copy]
    :as                           data} _ _]
  (when id
    [:div {:key id}
     [:div.center.mb4.content-3 "To continue, please verify your email address"]
     [:div.center.bold.mb6 {:data-test "email-address"} email-address]
     [:div.mb4 (ui/button-medium-primary (merge (utils/fake-href e/biz|email-verification|initiated {})
                                                {:spinning? spinning?
                                                 :data-test "email-verification-initiate"
                                                 :disabled? disabled?}) copy)]
     (let [{:email-verification--status-message/keys [fail-copy success-copy]} data]
       [(when fail-copy (flash/error-box {} fail-copy))
        (when success-copy (flash/success-box {} success-copy))])
     [:p.my8.center "Having Trouble? Contact us at "
      (ui/link :link/email :a {} "help@mayvenn.com")
      " or "
      (ui/link :link/phone :a.inherit-color {} config/support-phone-number)
      "."]]))

;; NOTE: verification success should never land you on this page, so it doesn't need a message here
(def status-messages
  {"init-fail"     "An error has occurred, please try again."
   "init-success"  "Great! Please check your email to verify your account."
   "verif-fail"    "An error has occurred, please try again."
   "verif-success" "Your email was successfully verified."})

(defn query [app-state]
  (let [user                  (get-in app-state k/user)
        status-message        (-> app-state (get-in k/navigation-message) second :query-params :stsm)
        ;; If the flash is showing, it's because we got here from sign-in and the user is already prompted to check their email.
        flash-prompt-showing? (seq (get-in app-state k/flash-now-success-message))]
    (when (and (:id user)
               (not (:verified-at user)))
      {:email-verification/id                           "email-verification"
       :email-verification/email-address                (:email user)
       :email-verification--status-message/fail-copy    (when (and status-message (string/ends-with? status-message "fail"))
                                                          (get status-messages status-message))
       :email-verification--status-message/success-copy (when (and status-message (string/ends-with? status-message "success"))
                                                          (get status-messages status-message))
       :email-verification--cta/copy                    (str (if flash-prompt-showing?
                                                               "Resend"
                                                               "Send") " Verification Email")
       :email-verification--cta/spinning?               (utils/requesting? app-state request-keys/email-verification-initiate)
       :email-verification--cta/disabled?               (= "init-success" status-message)})))

(defn built-component
  [app-state opts]
  [:div.p4
   (component/build template (query app-state) opts)])

(defmethod effects/perform-effects e/navigate-account-email-verification
  [_ _ _ _ app-state]
  ;; Note: navigate-account redirects to sign-in if user is signed out
  #?(:cljs
     (when (get-in app-state k/user-verified-at)
       (history/enqueue-redirect e/navigate-home))))

(defmethod effects/perform-effects e/biz|email-verification|initiated
  [_ _ _ _ app-state]
  #?(:cljs (api/email-verification-initiate (get-in app-state k/user-token)
                                            (get-in app-state k/user-id))))

(defmethod effects/perform-effects e/biz|email-verification|verified
  [_ _ {:keys [evt]} _ app-state]
  #?(:cljs (api/email-verification-verify (get-in app-state k/user-token)
                                          (get-in app-state k/user-id)
                                          evt)))

(defn set-messaging
  [current-nav-message status-message-id]
  [(first current-nav-message)
   (update (second current-nav-message) :query-params #(-> %
                                                           (dissoc :evt)
                                                           (assoc :stsm status-message-id)))])

(defmethod effects/perform-effects e/api-failure-email-verification-initiate
  [_ _ _ _ app-state]
  #?(:cljs (apply history/enqueue-redirect (set-messaging (get-in app-state k/navigation-message) "init-fail"))))

(defmethod effects/perform-effects e/api-success-email-verification-initiate
  [_ _ _ _ app-state]
  #?(:cljs (apply history/enqueue-redirect (set-messaging (get-in app-state k/navigation-message) "init-success"))))

(defmethod effects/perform-effects e/api-failure-email-verification-verify
  [_ _ _ _ app-state]
  #?(:cljs (apply history/enqueue-redirect (set-messaging (get-in app-state k/navigation-message) "verif-fail"))))

(defmethod transitions/transition-state e/api-success-email-verification-verify
  [_ _ {:keys [user] :as args} app-state]
  (-> app-state
      #_(assoc-in k/flash-now-success-message "Email address verified!")
      #_(assoc-in k/flash-now-failure-message nil)
      (assoc-in k/user-verified-at (:verified-at user))))

(defmethod effects/perform-effects e/api-success-email-verification-verify
  [_ _ user _ app-state]
  #?(:cljs (cookie-jar/save-user (get-in app-state k/cookie)
                                 (get-in app-state k/user)))
  #?(:cljs (apply history/enqueue-redirect (set-messaging (get-in app-state k/navigation-message) "verif-success"))))
