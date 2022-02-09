(ns mayvenn.concept.hard-session
  (:require #?@(:cljs
                [[storefront.browser.cookie-jar :as cookie-jar]])
            [clojure.set :as set]
            [spice.date]
            [storefront.events :as e]
            [storefront.effects :as fx]
            [storefront.keypaths :as k]
            [storefront.accessors.auth :as auth]
            [storefront.platform.messages
             :as messages
             :refer [handle-later
                     handle-message]
             :rename {handle-later publish-later
                      handle-message publish}]
            [storefront.routes :as routes]
            [storefront.transitions :as t]
            [storefront.trackings :as trk]
            [storefront.accessors.experiments :as experiments]))

(def timeout-period (* 30 60 1000))

(def model-keypath [:models :hard-session])
(def token-keypath (conj model-keypath :token))
(def timeout-keypath (conj model-keypath :timeout))

(def ^:private hard-session-pages
  #{e/navigate-account-manage})

(defn requires-hard-session? [navigation-event]
  (contains? hard-session-pages navigation-event))

(defn signed-in [app-state]
  #?(:cljs
     (assoc (auth/signed-in app-state)
            ::token (cookie-jar/retrieve-hard-session (get-in app-state k/cookie)))))

(defn allow?
  "Determines whether a given user has access to a a given page requiring login.
  Additionally, this respects hard-sessions and pages that require them."
  [app-state nav-event]
  (let [auth-data (signed-in app-state)]
    (and (not= :guest (::auth/as auth-data)) ; => true
         (or (not (requires-hard-session? nav-event))
             (some? (::token auth-data))))))

(defmethod fx/perform-effects e/biz|hard-session|begin
  [_ _ args state _]
  (publish e/biz|hard-session|token|set (select-keys args [:token]))
  (publish e/biz|hard-session|timeout|initialized))

(defmethod fx/perform-effects e/biz|hard-session|end
  [_ _ args state _]
  (publish e/biz|hard-session|token|clear)
  (publish e/biz|hard-session|timeout|clear))

(defmethod fx/perform-effects e/biz|hard-session|timeout|initialized
  [_ _ _ state _]
  #?(:cljs
     (publish e/biz|hard-session|timeout|started
              {:timeout (publish-later e/biz|hard-session|timeout|triggered
                                       {}
                                       timeout-period)})))

(defmethod fx/perform-effects e/biz|hard-session|token|set
  [_ _event args app-state _prev-app-state]
  #?(:cljs
     (cookie-jar/save-hard-session (get-in app-state k/cookie)
                                   {:hard-session-token (:token args)})))

(defmethod t/transition-state e/biz|hard-session|token|set
  [_ _event {:keys [token]} app-state]
  (assoc-in app-state token-keypath token))

(defmethod fx/perform-effects e/biz|hard-session|token|clear
  [_ _event _args app-state _prev-app-state]
  #?(:cljs
     (cookie-jar/clear-hard-session (get-in app-state k/cookie))))

(defmethod t/transition-state e/biz|hard-session|token|clear
  [_ _event _args app-state]
  (assoc-in app-state token-keypath nil))
