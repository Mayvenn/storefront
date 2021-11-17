(ns mayvenn.concept.email-capture
  (:require #?@(:cljs
                [[storefront.browser.cookie-jar :as cookie-jar]
                 [storefront.hooks.facebook-analytics :as facebook-analytics]
                 [storefront.hooks.google-tag-manager :as google-tag-manager]
                 [storefront.hooks.stringer :as stringer]])
            [clojure.set :as set]
            [spice.date]
            [storefront.events :as e]
            [storefront.effects :as fx]
            [storefront.keypaths :as k]
            [storefront.platform.messages
             :as messages
             :refer [handle-message]
             :rename {handle-message publish}]
            [storefront.routes :as routes]
            [storefront.transitions :as t]
            [storefront.trackings :as trk]
            [storefront.accessors.experiments :as experiments]))

(def never-show-on-these-pages
  #{e/navigate-cart
    e/navigate-checkout

    ;; Auth pages
    e/navigate-reset-password
    e/navigate-force-set-password
    e/navigate-forgot-password
    e/navigate-sign

    ;; The modal links to these, so we don't show on these pages (instead of dismissing the modal).
    e/navigate-content-tos
    e/navigate-content-privacy
    e/navigate-content-privacyv1})

(def adventure-and-quiz-pages
  #{e/navigate-adventure
    e/navigate-shopping-quiz})

(def email-capture-configs
  {"first-pageview-email-capture" {:cookie-id "1pv"
                                   :nav-events-forbid (set/union never-show-on-these-pages adventure-and-quiz-pages)}
   "adv-quiz-email-capture"       {:cookie-id "adv"
                                   :nav-events-allow adventure-and-quiz-pages}})

(def model-keypath [:models :email-capture])
(def textfield-keypath (conj model-keypath :textfield))
(def long-timer-started-keypath (conj model-keypath :long-timer-started?))
(def short-timer-starteds-keypath (conj model-keypath :short-timer-starteds?))

(defn location-approved? [nav-event email-capture-id]
  (let [{:keys [nav-events-forbid nav-events-allow]} (get email-capture-configs email-capture-id)]
    (if nav-events-allow
      (some #(routes/sub-page? [nav-event {}] [% {}])
            nav-events-allow)
      (not-any? #(routes/sub-page? [nav-event {}] [% {}])
                nav-events-forbid))))

;; TODO refactor to not refer to email-capture-configs twice
(defn location->email-capture-id
  [nav-event]
  (->> email-capture-configs
       keys
       (filter (partial location-approved? nav-event))
       first))

(defn <-trigger [email-capture-id app-state]
  (let [{:keys [long-timer-started?
                short-timer-starteds?]} (get-in app-state model-keypath)
        short-timer-started?            (boolean (get short-timer-starteds? email-capture-id))]
    {:email-capture-id     email-capture-id
     :long-timer-started?  long-timer-started?
     :short-timer-started? short-timer-started?
     :displayable?         (and email-capture-id
                                #?(:cljs (. js/navigator -cookieEnabled)) ; Never show if cookies disabled
                                (not long-timer-started?)
                                (not short-timer-started?))}))

(defn refresh-short-timers [cookie]
  #?(:clj nil
     :cljs
     (doseq [[_ {:keys [cookie-id]}] email-capture-configs]
       ;; These cookies get refreshed on every navigate so that they expire only
       ;; after 30 minutes of inactivity
       (when (cookie-jar/retrieve-email-capture-short-timer-started? cookie-id cookie)
         (cookie-jar/save-email-capture-short-timer-started cookie-id cookie)))))

(defn start-long-timer-if-unstarted [cookie]
  #?(:cljs
     (when-not (cookie-jar/retrieve-email-capture-long-timer-started? cookie)
       (cookie-jar/save-email-capture-long-timer-started cookie))))

(defmethod t/transition-state e/biz|email-capture|reset
  [_ _ _ state]
  (assoc-in state model-keypath {}))

(defmethod t/transition-state e/biz|email-capture|timer-state-observed
  [_ _ _ app-state]
  #?(:cljs
     (let [cookie (get-in app-state k/cookie)]
       (-> app-state
           (assoc-in long-timer-started-keypath (cookie-jar/retrieve-email-capture-long-timer-started? cookie))
           (assoc-in short-timer-starteds-keypath (->> email-capture-configs
                                                       (map (fn [[email-capture-id {:keys [cookie-id]}]]
                                                              [email-capture-id (cookie-jar/retrieve-email-capture-short-timer-started? cookie-id cookie)]))
                                                       (into {})))))))

(defmethod fx/perform-effects e/biz|email-capture|captured
  [_ _ _ state _]
  #?(:cljs
     (cookie-jar/save-email-capture-long-timer-started (get-in state k/cookie)))
  (publish e/biz|email-capture|timer-state-observed))

(defmethod fx/perform-effects e/biz|email-capture|dismissed
  [_ _ {:keys [id]} state _]
  #?(:cljs
     (cookie-jar/save-email-capture-short-timer-started (get-in email-capture-configs [id :cookie-id]) (get-in state k/cookie)))
  (publish e/biz|email-capture|timer-state-observed))

;;; TRACKING

(defmethod trk/perform-track e/biz|email-capture|deployed
  [_ events {:keys [id variant]} app-state]
  #?(:cljs
     (stringer/track-event "email_capture-deploy" {:email-capture-id id
                                                   :variant          variant})))

(defmethod trk/perform-track e/biz|email-capture|captured
  [_ event {:keys [id variant]} app-state]
  #?(:cljs
     (let [no-errors?     (empty? (get-in app-state k/errors))
           captured-email (get-in app-state textfield-keypath)]
       (when no-errors?
         ;; TODO: CONSIDER READDING THESE
         ;; (facebook-analytics/subscribe)
         ;; (google-tag-manager/track-email-capture-capture {:email captured-email})
         (stringer/identify {:email captured-email})
         (stringer/track-event "email_capture-capture"
                               {:email            captured-email
                                :email-capture-id id
                                :variant          variant
                                :test-variations  (get-in app-state k/features)
                                :store-slug       (get-in app-state k/store-slug)
                                :store-experience (get-in app-state k/store-experience)})))))

(defmethod trk/perform-track e/biz|email-capture|dismissed
  [_ events {:keys [id variant]} app-state]
  #?(:cljs
     (stringer/track-event "email_capture-dismiss" {:email-capture-id id
                                                    :variant          variant})))
