(ns storefront.hooks.facebook
  (:require [storefront.browser.tags :refer [insert-tag-with-src]]
            [storefront.messages :refer [send]]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.config :as config]))

(defn init []
  (js/FB.init (clj->js {:appId config/facebook-app-id
                        :xfbml false
                        :version "v2.5"})))

(defn insert [app-state]
  (set! (.-fbAsyncInit js/window)
        (fn []
          (init)
          (send app-state events/facebook-inserted)))
  (insert-tag-with-src "//connect.facebook.net/en_US/sdk.js" "facebook-jssdk"))

(defn check-log-in-permissions [app-state login-response]
  (js/FB.api "/me/permissions"
             (fn [permissions-response]
               (let [permissions-include? (-> permissions-response
                                              (js->clj :keywordize-keys true)
                                              :data
                                              set)]
                 (if (permissions-include? {:permission "email" :status "granted"})
                   (send app-state events/facebook-success-sign-in login-response)
                   (send app-state events/facebook-email-denied))))))

(defn start-log-in [app-state]
  (js/FB.login (fn [response]
                 (let [response (js->clj response :keywordize-keys true)]
                   (if (:authResponse response)
                     (check-log-in-permissions app-state response)
                     (send app-state events/facebook-failure-sign-in))))
               (-> {:scope "public_profile,email"}
                   (merge (when (get-in app-state keypaths/facebook-email-denied)
                            {:auth_type "rerequest"}))
                   clj->js)))
