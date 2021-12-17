(ns email-verification.core
  (:require #?@(:cljs [[storefront.api :as api]
                       [storefront.history :as history]
                       [storefront.accessors.auth :as auth]
                       [storefront.browser.cookie-jar :as cookie-jar]])
            [storefront.components.ui :as ui]
            [storefront.config :as config]
            [storefront.platform.component-utils :as utils]
            [storefront.events :as events]
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
    :email-verification--cta/keys [spinning? disabled?]
    :as                           data} _ _]
  (when id
    [:div {:key id}
     [:div.center.mb4.content-3 "To continue, please verify your email address"]
     [:div.center.bold.mb6 email-address]
     [:div.mb4 (ui/button-medium-primary (merge (utils/fake-href events/biz|email-verification|initiated {})
                                                {:spinning? spinning?
                                                 :disabled? disabled?}) "Send verification email")]
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
  (let [user           (get-in app-state keypaths/user)
        status-message (-> app-state (get-in keypaths/navigation-message) second :query-params :stsm)]
    (when (and (:id user)
               (not (:verified-at user)))
      {:email-verification/id                           "email-verification"
       :email-verification/email-address                (:email user)
       :email-verification--status-message/fail-copy    (when (and status-message (string/ends-with? status-message "fail"))
                                                          (get status-messages status-message))
       :email-verification--status-message/success-copy (when (and status-message (string/ends-with? status-message "success"))
                                                          (get status-messages status-message))
       :email-verification--cta/spinning?               (utils/requesting? app-state request-keys/email-verification-initiate)
       :email-verification--cta/disabled?               (= "init-success" status-message)})))

(defmethod effects/perform-effects events/biz|email-verification|initiated
  [_ _ _ _ app-state]
  #?(:cljs (api/email-verification-initiate (get-in app-state keypaths/user-token)
                                            (get-in app-state keypaths/user-id))))

(defmethod effects/perform-effects events/biz|email-verification|verified
  [_ _ {:keys [evt]} _ app-state]
  #?(:cljs (api/email-verification-verify (get-in app-state keypaths/user-token)
                                          (get-in app-state keypaths/user-id)
                                          evt)))

(defn set-messaging
  [current-nav-message status-message-id]
  [(first current-nav-message)
   (update (second current-nav-message) :query-params #(-> %
                                                           (dissoc :evt)
                                                           (assoc :stsm status-message-id)))])

(defmethod effects/perform-effects events/api-failure-email-verification-initiate
  [_ _ _ _ app-state]
  #?(:cljs (apply history/enqueue-redirect (set-messaging (get-in app-state keypaths/navigation-message) "init-fail"))))

(defmethod effects/perform-effects events/api-success-email-verification-initiate
  [_ _ _ _ app-state]
  #?(:cljs (apply history/enqueue-redirect (set-messaging (get-in app-state keypaths/navigation-message) "init-success"))))

(defmethod effects/perform-effects events/api-failure-email-verification-verify
  [_ _ _ _ app-state]
  #?(:cljs (apply history/enqueue-redirect (set-messaging (get-in app-state keypaths/navigation-message) "verif-fail"))))

(defmethod transitions/transition-state events/api-success-email-verification-verify
  [_ _ {:keys [user] :as args} app-state]
  (-> app-state
      #_(assoc-in keypaths/flash-now-success-message "Email address verified!")
      #_(assoc-in keypaths/flash-now-failure-message nil)
      (assoc-in keypaths/user-verified-at (:verified-at user))))

(defmethod effects/perform-effects events/api-success-email-verification-verify
  [_ _ user _ app-state]
  #?(:cljs (cookie-jar/save-user (get-in app-state keypaths/cookie)
                                 (get-in app-state keypaths/user)))
  #?(:cljs (apply history/enqueue-redirect (set-messaging (get-in app-state keypaths/navigation-message) "verif-success"))))
