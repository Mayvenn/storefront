(ns email-verification.core
  (:require #?@(:cljs [[storefront.api :as api]
                       [storefront.history :as history]
                       [storefront.browser.cookie-jar :as cookie-jar]])
            [storefront.components.ui :as ui]
            [storefront.config :as config]
            [storefront.platform.component-utils :as utils]
            [storefront.events :as e]
            [storefront.effects :as effects]
            [storefront.keypaths :as k]
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
     [:p.my8.center "Having Trouble? Contact us at "
      (ui/link :link/email :a {} "help@mayvenn.com")
      " or "
      (ui/link :link/phone :a.inherit-color {} config/support-phone-number)
      "."]]))

(defn query [app-state]
  (let [user                  (get-in app-state k/user)
        initiated?        (-> app-state (get-in k/navigation-message) second :query-params :stsm (= "init-success"))]
    (when (and (:id user)
               (not (:verified-at user)))
      {:email-verification/id                           "email-verification"
       :email-verification/email-address                (:email user)
       :email-verification--cta/copy                    (str (if initiated?
                                                               "Resend"
                                                               "Send") " Verification Email")
       :email-verification--cta/spinning?               (utils/requesting? app-state request-keys/email-verification-initiate)
       :email-verification--cta/disabled?               initiated?})))

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

(defmethod effects/perform-effects e/api-success-email-verification-initiate
  [_ _ _ _ app-state]
  #?(:cljs (apply history/enqueue-redirect (get-in app-state k/navigation-message) {:query-params {:stsm "init-success"}})))

(defmethod transitions/transition-state e/api-failure-email-verification-verify
  [_ _ {:keys [user] :as args} app-state]
  (assoc-in app-state k/flash-now-failure-message "An error has occurred, please try again."))

(defmethod transitions/transition-state e/api-success-email-verification-verify
  [_ _ {:keys [user] :as args} app-state]
  (-> app-state
      (assoc-in k/flash-now-success-message "Your email was successfully verified.")
      (assoc-in k/user-verified-at (:verified-at user))))

(defmethod effects/perform-effects e/api-success-email-verification-verify
  [_ _ user _ app-state]
  #?(:cljs (cookie-jar/save-user (get-in app-state k/cookie)
                                 (get-in app-state k/user))))
