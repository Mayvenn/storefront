(ns storefront.components.facebook
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.utils :as utils]
            [storefront.events :as events]
            [storefront.hooks.facebook :as facebook]
            [storefront.config :as config]
            [storefront.keypaths :as keypaths]))

(defn- button [data click-event]
  (if (get-in data keypaths/loaded-facebook)
    [:button.fb-login-button
     {:on-click (utils/send-event-callback click-event)}
     [:div.fb-login-wrapper
      [:img {:src "/images/FacebookWhite.png" :width 29 :height 29}]
      [:div.fb-login-content "Sign in with Facebook"]]]
    [:div.fb-filler]))

(defn sign-in-button [data]
  (button data events/control-facebook-sign-in))

(defn reset-button [data]
  (button data events/control-facebook-reset))

(defn opt-in-component [{:keys [user-id loaded-facebook?]} _]
  (reify
    om/IDidMount
    (did-mount [_]
      (when loaded-facebook?
        (facebook/reparse-xfbml)))
    om/IRender
    (render [_]
      (html
       [:div
        ;; Sablono only supports known attributes, but FB insists on using their own.
        [:div
         {:dangerouslySetInnerHTML {:__html (goog.string/format
                                             "<div class='fb-messengerbusinesslink' messenger_app_id='%s' state=\"{'data': {'user_id': %d}}\"></div>"
                                             config/sonar-facebook-app-id
                                             user-id)}}]]))))

(defn opt-out-component [{:keys [messenger-token loaded-facebook?]} _]
  (reify
    om/IDidMount
    (did-mount [_]
      (when loaded-facebook?
        (facebook/reparse-xfbml)))
    om/IRender
    (render [_]
      (html
       [:div
        ;; Sablono only supports known attributes, but FB insists on using their own.
        [:div
         {:dangerouslySetInnerHTML {:__html (goog.string/format
                                             "<div class='fb-messengertoggle' messenger_app_id='%s' token='%s'></div>"
                                             config/sonar-facebook-app-id
                                             messenger-token)}}]]))))

(defn messenger-business-opt-in [{:keys [user-id messenger-token loaded-facebook?]} _]
  (om/component
   (html
    [:div
     (om/build opt-out-component {:messenger-token messenger-token
                                  :loaded-facebook? loaded-facebook?})
     (om/build opt-in-component {:user-id user-id
                                 :loaded-facebook? loaded-facebook?})])))
