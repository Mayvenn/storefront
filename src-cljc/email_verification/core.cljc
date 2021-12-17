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
            [storefront.request-keys :as request-keys]))

(component/defcomponent template
  [{:email-verification/keys [id email-address]
    :email-verification--cta/keys [spinning?]
    :as data} _ _]
  (when id
    [:div {:key id}
     [:div.center.mb4.content-3 "To continue, please verify your email address"]
     [:div.center.bold.mb6 email-address]
     [:div.mb4 (ui/button-medium-primary (merge (utils/fake-href events/biz|email-verification|initiated {})
                                                {:spinning? spinning?}) "Send verification email")]
     ;; TODO: finish or remove
     [:p.my8.center "Having Trouble? Contact us at "
      (ui/link :link/email :a {} "help@mayvenn.com")
      " or "
      (ui/link :link/phone :a.inherit-color {} config/support-phone-number)
      "."]]))

(defn query [app-state]
  (let [user (get-in app-state keypaths/user)]
    (when (and (:id user)
               (not (:verified-at user)))
      (merge {:email-verification/id            "email-verification"
              :email-verification/email-address (:email user)}
             (when (utils/requesting? app-state request-keys/email-verification-initiate)
               {:email-verification--cta/spinning? true})))))

;; TODO: make sure to debounce both the initiated and verified requests
(defmethod effects/perform-effects events/biz|email-verification|initiated
  [_ _ _ _ app-state]
  #?(:cljs (api/email-verification-initiate (get-in app-state keypaths/user-token)
                                            (get-in app-state keypaths/user-id))))

(defmethod effects/perform-effects events/biz|email-verification|verified
  [_ _ {:keys [evt]} _ app-state]
  #?(:cljs (api/email-verification-verify (get-in app-state keypaths/user-token)
                                          (get-in app-state keypaths/user-id)
                                          evt)))

(defmethod transitions/transition-state events/api-failure-email-verification-initiate
  [_ _ _ app-state]
  (assoc-in app-state keypaths/flash-now-failure-message "An error has occurred, please try again."))

(defmethod transitions/transition-state events/api-success-email-verification-initiate
  [_ _ _ app-state]
  (-> app-state
      (assoc-in keypaths/flash-now-success-message "Email sent. Please check your inbox and follow the link.")
      (assoc-in keypaths/flash-now-failure-message nil)))

(defmethod transitions/transition-state events/api-failure-email-verification-verify
  [_ _ _ app-state]
  (assoc-in app-state keypaths/flash-now-failure-message "An error has occurred, please try again."))

(defmethod transitions/transition-state events/api-success-email-verification-verify
  [_ _ {:keys [user] :as args} app-state]
  (-> app-state
      (assoc-in keypaths/flash-now-success-message "Email address verified!")
      (assoc-in keypaths/flash-now-failure-message nil)
      (assoc-in keypaths/user-verified-at (:verified-at user))))

(defmethod effects/perform-effects events/api-success-email-verification-verify
  [_ _ user _ app-state]
  #?(:cljs (cookie-jar/save-user (get-in app-state keypaths/cookie)
                                 (get-in app-state keypaths/user))))
