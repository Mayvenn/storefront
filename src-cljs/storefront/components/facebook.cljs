(ns storefront.components.facebook
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.platform.component-utils :as utils]
            [storefront.config :as config]
            [storefront.events :as events]
            [storefront.hooks.facebook :as facebook]
            [storefront.keypaths :as keypaths]))

(defn- button [loaded? click-event]
  (if loaded?
    [:.btn.btn-primary.bg-fb-blue.col-12
     {:on-click (utils/send-event-callback click-event)}
     [:.flex.items-center.justify-center.white.items-center
      [:img.mr2 {:src "/images/FacebookWhite.png" :width 29 :height 29}]
      [:.h3.py1 "Sign in with Facebook"]]]
    [:div {:style {:height "3.25rem"}}]))

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
       (om/build opt-out-component {:messenger-token  messenger-token
                                    :loaded-facebook? loaded-facebook?}))
     (om/build opt-in-component {:user-id          user-id
                                 :loaded-facebook? loaded-facebook?})])))

(defn query [data]
  {:user-id          (get-in data keypaths/user-id)
   :messenger-token  (get-in data keypaths/user-messenger-token)
   :loaded-facebook? (get-in data keypaths/loaded-facebook)})
