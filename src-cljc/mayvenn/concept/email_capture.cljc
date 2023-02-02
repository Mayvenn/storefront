(ns mayvenn.concept.email-capture
  (:require #?@(:cljs
                [[storefront.browser.cookie-jar :as cookie-jar]
                 [storefront.hooks.facebook-analytics :as facebook-analytics]
                 [storefront.hooks.google-analytics :as google-analytics]
                 [storefront.hooks.stringer :as stringer]])
            [clojure.set :as set]
            [mayvenn.concept.awareness :as awareness]
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

    e/navigate-mayvenn-stylist-pay

    ;; Auth pages
    e/navigate-reset-password
    e/navigate-force-set-password
    e/navigate-forgot-password
    e/navigate-sign

    ;; The modal links to these, so we don't show on these pages (instead of dismissing the modal).
    e/navigate-content-sms
    e/navigate-content-tos
    e/navigate-content-privacy
    e/navigate-content-privacyv1
    e/navigate-content-privacyv2})

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
(def phonefield-keypath (conj model-keypath :phone))
(def long-timer-started-keypath (conj model-keypath :long-timer-started?))
(def short-timer-starteds-keypath (conj model-keypath :short-timer-starteds?))

(defn email-modal-trigger-id->cookie-id
  [trigger-id]
  (or (:cookie-id (email-capture-configs trigger-id))
      trigger-id))

(defn all-trigger-ids [app-state]
  (map (comp :trigger-id :email-modal-trigger)
       (vals (get-in app-state k/cms-email-modal))))

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

(defn refresh-short-timers [cookie trigger-ids]
  #?(:clj nil
     :cljs
     (doseq [trigger-id trigger-ids
             :let [cookie-id (email-modal-trigger-id->cookie-id trigger-id)]]
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

(defmethod t/transition-state e/biz|capture-modal|timer-state-observed
  [_ _ _ app-state]
  #?(:cljs
     (let [cookie (get-in app-state k/cookie)]
       (-> app-state
           (assoc-in long-timer-started-keypath (cookie-jar/retrieve-email-capture-long-timer-started? cookie))
           (assoc-in short-timer-starteds-keypath (->> (all-trigger-ids app-state)
                                                       (map (fn [trigger-id]
                                                              [trigger-id (cookie-jar/retrieve-email-capture-short-timer-started?
                                                                           (email-modal-trigger-id->cookie-id trigger-id)
                                                                           cookie)]))
                                                       (into {})))))))

(defmethod fx/perform-effects e/biz|capture-modal|timer-state-observed
  [_ _ _ state _]
  (publish e/biz|capture-modal|started))

;;; Matchers and Triggers

;; TODO(corey) defmulti?
(defn matcher-matches? [app-state matcher]
  (case (:content/type matcher)
    "matchesAny"              (->> matcher
                                   :must-satisfy-any-match
                                   (map (partial matcher-matches? app-state))
                                   (some true?))
    "matchesAll"              (->> matcher
                                   :must-satisfy-all-matches
                                   (map (partial matcher-matches? app-state))
                                   (every? true?))
    "matchesNot"              (->> matcher
                                   :must-not-satisfy
                                   (matcher-matches? app-state)
                                   not)
    "matchesPath"             (let [cur-path (-> app-state
                                                 (get-in k/navigation-uri)
                                                 :path)]
                                (case (:path-matches matcher)
                                  "starts with"         (clojure.string/starts-with? cur-path (:path matcher))
                                  "contains"            (clojure.string/includes? cur-path (:path matcher))
                                  "exactly matches"     (= cur-path (:path matcher))
                                  "does not start with" (not (clojure.string/starts-with? cur-path (:path matcher)))))
    ;; TODO: Does not provide segmentation for customers who arrive organically. Only deals with UTM params
    ;; which rely on marketing segmentation.
    ;; TODO: With many landfalls, it's easily possible for multiple triggers to apply. There is still no prioritization
    ;; to resolve these conflicts, and the issue becomes more pronounced with the marketing trackers.
    "matchesMarketingTracker" (->> (get-in app-state k/account-profile)
                                   :landfalls
                                   (map :utm-params)
                                   (some #(= (:value matcher) (get % (:tracker-type matcher)))))
    ;; TODO: there are other path matchers not accounted for yet.
    #?(:cljs (when matcher (js/console.error (str "No matching content/type for matcher " (pr-str matcher)))))))

(defn triggered-email-modals<-
  [state long-timer short-timers]
  (when-not long-timer
    (let [modals (vals (get-in state k/cms-email-modal))]
      (->> modals
           ;; Verify modal has trigger, for acceptance env
           (filter #(-> % :email-modal-trigger :trigger-id))
           ;; Verify modal has content, for acceptance env
           (filter #(-> % :email-modal-template :template-content-id))
           ;; Matches trigger
           (filter #(matcher-matches? state (-> % :email-modal-trigger :matcher)))
           ;; Removes triggers under timers
           (remove #(get short-timers (-> % :email-modal-trigger :trigger-id)))
           not-empty))))

(defmethod t/transition-state e/biz|capture-modal|started
  [_ _ _ app-state]
  (let [long-timer       (get-in app-state long-timer-started-keypath)
        short-timers     (get-in app-state short-timer-starteds-keypath)
        query-params     (get-in app-state k/navigation-query-params)
        signed-in?       (get-in app-state k/user-id)
        email-modals     (when-not (or signed-in? (:em_hash query-params))
                           (triggered-email-modals<- app-state long-timer short-timers))
        ;; FIXME indeterminate behavior when multiple triggers are valid and active
        determined-modal (spice.core/spy (first email-modals))]
    (when determined-modal
      (-> app-state
          (assoc-in [:models :modal :started]
                    determined-modal)))))

;; RACE CONDITION: add another event for determining modal, so we can separate these

(defmethod fx/perform-effects e/biz|capture-modal|started
  [_ _ _ state _]
  (publish e/biz|capture-modal|determined))

(defmethod fx/perform-effects e/biz|capture-modal|determined
  [_ _ _ state _]
  (let [started-modal (get-in state [:models :modal :started])]
    (when-not started-modal
      (publish e/modal|finished {:cause :no-matching-modal}))))

(defmethod t/transition-state e/modal|finished
  [_ _ _ app-state]
  (-> app-state
      (update-in [:models :modal] dissoc :started)))

(defmethod fx/perform-effects e/funnel|acquisition|prompted
  [_ _ {:email-modal/keys [template-content-id variation-description trigger-id]} _ _]
  (publish e/biz|email-capture|deployed
           {:trigger-id            trigger-id
            :variation-description variation-description
            :template-content-id   template-content-id}))

(defmethod fx/perform-effects e/biz|email-capture|captured
  [_ _ {:keys [hdyhau]} state _]
  (when-not
      ;; if getting hdyhau, don't set timer until after hdyhau is collected
      (and hdyhau
           (experiments/hdyhau-email-capture? state))
        #?(:cljs
           (cookie-jar/save-email-capture-long-timer-started (get-in state k/cookie)))
        (publish e/biz|capture-modal|timer-state-observed)))

(defmethod fx/perform-effects e/modal|finished
  [_ _ {:keys [id trigger-id cause]} state _]
  (case cause
    :cta
    #?(:cljs
       (do
         (cookie-jar/save-email-capture-long-timer-started (get-in state k/cookie))
         (publish e/biz|capture-modal|timer-state-observed)))

    :dismiss
    #?(:cljs
       (do (cookie-jar/save-email-capture-short-timer-started (email-modal-trigger-id->cookie-id trigger-id)
                                                              (get-in state k/cookie))
           (publish e/biz|capture-modal|timer-state-observed)))

    nil))

(defmethod t/transition-state e/hdyhau-email-capture-submitted
  [_ _ _ app-state]
  (-> app-state
      (assoc-in [:models :hdyhau :submitted] true)))

(defmethod fx/perform-effects e/hdyhau-email-capture-submitted
  [_ _ data state _]
  (publish e/biz|hdyhau-capture|captured data))

(defmethod fx/perform-effects e/biz|hdyhau-capture|captured
  [_ _ _ state _]
  #?(:cljs
     (cookie-jar/save-email-capture-long-timer-started (get-in state k/cookie)))
  (publish e/biz|capture-modal|timer-state-observed))

;;; TRACKING

#?(:cljs
   (defmethod trk/perform-track e/biz|email-capture|deployed
     [_ events {:keys [trigger-id variation-description template-content-id]} app-state]
     (stringer/track-event "email_capture-deploy" {:email-capture-id      trigger-id
                                                   :variation-description variation-description
                                                   :template-content-id   template-content-id})))

#?(:cljs
   (defmethod trk/perform-track e/biz|email-capture|captured
     [_ event {:keys [trigger-id variation-description template-content-id details]} app-state]
     (let [no-errors?     (empty? (get-in app-state k/errors))
           captured-email (get-in app-state textfield-keypath)
           captured-phone (get-in app-state phonefield-keypath)]
       (when no-errors?
         ;; TODO: CONSIDER READDING THESE
         ;; (facebook-analytics/subscribe)
         (stringer/identify {:email captured-email})
         (stringer/track-event "email_capture-capture"
                               {:email-capture-id      trigger-id
                                :variation-description variation-description
                                :template-content-id   template-content-id
                                :email                 captured-email
                                :details               details
                                :test-variations       (get-in app-state k/features)
                                :store-slug            (get-in app-state k/store-slug)
                                :store-experience      (get-in app-state k/store-experience)
                                :account-profile       (get-in app-state k/account-profile)})
         (google-analytics/track-generate-lead)))))

#?(:cljs
   (defmethod trk/perform-track e/biz|sms-capture|captured
     [_ event {:keys [trigger-id variation-description template-content-id]} app-state]
     (let [no-errors?     (empty? (get-in app-state k/errors))
           captured-email (get-in app-state textfield-keypath)
           captured-phone (get-in app-state phonefield-keypath)]
       (when no-errors?
         ;; TODO: CONSIDER READING THESE
         ;; (facebook-analytics/subscribe)
         (stringer/track-event "sms_capture-capture"
                               {:email-capture-id      trigger-id
                                :variation-description variation-description
                                :template-content-id   template-content-id
                                :email                 captured-email
                                :phone                 captured-phone
                                :test-variations       (get-in app-state k/features)
                                :store-slug            (get-in app-state k/store-slug)
                                :store-experience      (get-in app-state k/store-experience)
                                :account-profile       (get-in app-state k/account-profile)})))))

#?(:cljs
   (defmethod trk/perform-track e/modal|finished
     [_ events {:keys [trigger-id variation-description template-content-id]} app-state]
     (stringer/track-event "email_capture-dismiss" {:email-capture-id      trigger-id
                                                    :variation-description variation-description
                                                    :template-content-id   template-content-id})))

#?(:cljs
   (defmethod trk/perform-track e/biz|hdyhau-capture|captured
     [_ event {:keys [hdyhau-options]} app-state]
     (let [hdyhau-to-submit (:to-submit (get-in app-state k/models-hdyhau))
           email            (get-in app-state [:models :email-capture :textfield])
           phone            (get-in app-state [:models :email-capture :phone])]
       (stringer/track-event "hdyhau-answered"
                             (awareness/hdyhau-answered-data hdyhau-to-submit
                                                             hdyhau-options
                                                             {:email email
                                                              :phone phone
                                                              :form  "email-capture"})))))
