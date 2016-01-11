(ns storefront.hooks.facebook
  (:require [storefront.browser.tags :refer [insert-tag-with-src]]
            [storefront.messages :refer [send]]
            [storefront.keypaths :as keypaths]
            [storefront.hooks.experiments :as experiments]
            [storefront.events :as events]
            [storefront.config :as config]))

(defn init []
  (js/FB.init (clj->js {:appId config/facebook-app-id
                        :xfbml false
                        :version "v2.5"})))

(defn insert [app-state]
  (when (and (experiments/facebook? app-state)
             (not (.hasOwnProperty js/window "FB")))
    (set! (.-fbAsyncInit js/window)
          (fn []
            (init)
            (send app-state events/facebook-inserted)))
    (insert-tag-with-src "//connect.facebook.net/en_US/sdk.js" "facebook-jssdk")))

(defn- check-fb-permissions [app-state success-event login-response]
  (js/FB.api "/me/permissions"
             (fn [permissions-response]
               (let [permissions-include? (-> permissions-response
                                              (js->clj :keywordize-keys true)
                                              :data
                                              set)]
                 (if (permissions-include? {:permission "email" :status "granted"})
                   (send app-state success-event login-response)
                   (send app-state events/facebook-email-denied))))))

(defn- fb-login [app-state success-event]
  (js/FB.login (fn [response]
                 (let [response (js->clj response :keywordize-keys true)]
                   (if-not (:authResponse response)
                     (send app-state events/facebook-failure-sign-in)
                     (check-fb-permissions app-state success-event response))))
               (clj->js (merge {:scope "public_profile,email"}
                               (when (get-in app-state keypaths/facebook-email-denied)
                                 {:auth_type "rerequest"})))))

(defn start-log-in [app-state]
  (fb-login app-state events/facebook-success-sign-in))

(defn start-reset [app-state]
  (fb-login app-state events/facebook-success-reset))
