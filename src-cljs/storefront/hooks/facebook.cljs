(ns storefront.hooks.facebook
  (:require [storefront.browser.tags :refer [insert-tag-with-src]]
            [storefront.messages :refer [send]]
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

(defn start-log-in [app-state]
  (js/FB.login (fn [response]
                 (let [response (js->clj response :keywordize-keys true)]
                   (if (:authResponse response)
                     (send app-state events/facebook-success-sign-in response)
                     (send app-state events/facebook-failure-sign-in response))))
               (clj->js {:scope "public_profile,email"})))
