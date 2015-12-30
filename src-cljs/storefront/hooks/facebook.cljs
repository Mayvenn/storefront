(ns storefront.hooks.facebook
  (:require [storefront.browser.tags :refer [text-tag insert-body-top]]
            [storefront.messages :refer [send]]
            [storefront.events :as events]
            [storefront.config :as config]))

(def ^:private tag-class "facebook-tag")
(def async-load-sdk-js
  ;; TODO: fbAsyncInit would be the right place to tell storefront that FB is
  ;; loaded, and that the login button should be enabled.  It would
  ;; also be the place to run FB.getLoginStatus if we wanted to show
  ;; the logged-in experience on app load.
  (str "
  window.fbAsyncInit = function() {
    FB.init({
      appId      : '" config/facebook-app-id "',
      xfbml      : false,
      version    : 'v2.5'
    });
  };

  (function(d, s, id){
     var js, fjs = d.getElementsByTagName(s)[0];
     if (d.getElementById(id)) {return;}
     js = d.createElement(s); js.id = id;
     js.src = \"//connect.facebook.net/en_US/sdk.js\";
     fjs.parentNode.insertBefore(js, fjs);
   }(document, 'script', 'facebook-jssdk'));
"))

(defn insert []
  ;; TODO: could this be insert-body-bottom? Facebook recommends
  ;; adding it at the top of the page, but that probably only applies
  ;; if you load it before anything else on the page.  Dynamic
  ;; insertion, even at the top of the page, happens after page load.
  (insert-body-top (text-tag async-load-sdk-js tag-class)))

(defn start-log-in [app-state]
  (js/FB.login (fn [response]
                 (let [response (js->clj response :keywordize-keys true)]
                   (if (:authResponse response)
                     (send app-state events/facebook-success-sign-in response)
                     (send app-state events/facebook-failure-sign-in response))))
               (clj->js {:scope "public_profile,email"})))
