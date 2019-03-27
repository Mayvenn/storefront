(ns storefront.hooks.facebook
  (:require [clojure.string :as str]
            [storefront.browser.tags :refer [insert-tag-with-src]]
            [storefront.config :as config]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.messages :refer [handle-message]]))

(defn init []
  (js/FB.init (clj->js {:appId config/facebook-app-id
                        :version "v3.2"})))

(defn insert []
  (when (not (.hasOwnProperty js/window "FB"))
    (set! (.-fbAsyncInit js/window)
          (fn []
            (init)
            (handle-message events/inserted-facebook)))
    (insert-tag-with-src "//connect.facebook.net/en_US/sdk.js" "facebook-jssdk")))

(defn- fb-login [app-state success-event]
  (js/FB.login (fn [response]
                 (let [response (js->clj response :keywordize-keys true)
                       auth (:authResponse response)
                       permissions (-> auth :grantedScopes str (str/split #",") set)]
                   (handle-message (cond
                                     (not auth)                  events/facebook-failure-sign-in
                                     (not (permissions "email")) events/facebook-email-denied
                                     :else                       success-event)
                                   response)))
               (clj->js (merge {:scope "public_profile,email"
                                :return_scopes true}
                               (when (get-in app-state keypaths/facebook-email-denied)
                                 {:auth_type "rerequest"})))))

(defn start-log-in [app-state]
  (fb-login app-state events/facebook-success-sign-in))

(defn start-reset [app-state]
  (fb-login app-state events/facebook-success-reset))
