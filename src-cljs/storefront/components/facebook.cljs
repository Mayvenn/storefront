(ns storefront.components.facebook
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.utils :as utils]
            [storefront.events :as events]
            [storefront.hooks.facebook :as facebook]
            [storefront.config :as config]
            [storefront.keypaths :as keypaths]))

(defn- redesigned-button [loaded? click-event]
  (if loaded?
    [:.btn.btn-large.bg-fb-blue.col-12.border.rounded-1
     {:on-click (utils/send-event-callback click-event)}
     [:.flex.items-center.justify-center.white.items-center
      [:img.mr2 {:src "/images/FacebookWhite.png" :width 29 :height 29}]
      [:.h3.py1 "Sign in with Facebook"]]]
    [:div {:style {:height "3.25rem"}}]))

(defn redesigned-sign-in-button [loaded?]
  (redesigned-button loaded? events/control-facebook-sign-in))

;; TODO: delete me after experiements/three-steps-redesign? is removed
(defn- button [loaded? click-event]
  (if loaded?
    [:button.fb-login-button
     {:on-click (utils/send-event-callback click-event)}
     [:div.fb-login-wrapper
      [:img {:src "/images/FacebookWhite.png" :width 29 :height 29}]
      [:div.fb-login-content "Sign in with Facebook"]]]
    [:div.fb-filler]))

;; TODO: delete me after experiements/three-steps-redesign? is removed
(defn sign-in-button [loaded?]
  (button loaded? events/control-facebook-sign-in))

(defn reset-button [loaded?]
  (button loaded? events/control-facebook-reset))

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
        {:dangerouslySetInnerHTML {:__html (goog.string/format
                                            "<div class='fb-messengerbusinesslink' messenger_app_id='%s' state=\"{'data': {'user_id': %d}}\"></div>"
                                            config/sonar-facebook-app-id
                                            user-id)}}]))))

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
        {:dangerouslySetInnerHTML {:__html (goog.string/format
                                            "<div class='fb-messengertoggle' messenger_app_id='%s' token='%s'></div>"
                                            config/sonar-facebook-app-id
                                            messenger-token)}}]))))

(defn messenger-business-opt-in-component [{:keys [user-id messenger-token loaded-facebook?]} _]
  (om/component
   (html
    [:div
     (when messenger-token
       (om/build opt-out-component {:messenger-token messenger-token
                                    :loaded-facebook? loaded-facebook?}))
     (om/build opt-in-component {:user-id user-id
                                 :loaded-facebook? loaded-facebook?})])))

(defn query [data]
  {:user-id (get-in data keypaths/user-id)
   :messenger-token (get-in data keypaths/user-messenger-token)
   :loaded-facebook? (get-in data keypaths/loaded-facebook)})
