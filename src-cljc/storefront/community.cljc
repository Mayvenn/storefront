(ns storefront.community
  (:require #?@(:cljs [[storefront.api :as api]
                       [storefront.browser.cookie-jar :as cookie-jar]
                       [storefront.config :as config]
                       [storefront.keypaths :as keypaths]])
            [storefront.events :as events]
            [storefront.platform.messages :as messages]
            [storefront.effects :as effects]
            [storefront.platform.component-utils :as utils]))

(def community-url
  (merge (utils/fake-href events/control-stylist-community)
         {:data-test "community"}))

(defmethod effects/perform-effects events/control-stylist-community
  [_ _ _ _ app-state]
  #?(:cljs
     (api/telligent-sign-in (get-in app-state keypaths/session-id)
                            (get-in app-state keypaths/user-id)
                            (get-in app-state keypaths/user-token))))

(defmethod effects/perform-effects events/external-redirect-telligent
  [_ _ _ _ app-state]
  #?(:cljs
     (set! (.-location js/window)
           (or (get-in app-state keypaths/telligent-community-url)
               config/telligent-community-url))))

(defmethod effects/perform-effects events/api-success-telligent-login
  [_ _ {:keys [cookie max-age]} _ app-state]
  #?(:cljs
     (cookie-jar/save-telligent-cookie (get-in app-state keypaths/cookie)
                                       cookie
                                       max-age))
  (messages/handle-message events/external-redirect-telligent))

(defn redirect-to-telligent-as-user
  [app-state]
  #?(:cljs
     (api/telligent-sign-in (get-in app-state keypaths/session-id)
                            (get-in app-state keypaths/user-id)
                            (get-in app-state keypaths/user-token)))
  (messages/handle-message events/flash-later-show-success
                           {:message "Redirecting to the stylist community"}))
