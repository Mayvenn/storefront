(ns mayvenn.concept.email-capture
  (:require #?@(:cljs
                [[storefront.browser.cookie-jar :as cookie-jar]
                 [storefront.hooks.facebook-analytics :as facebook-analytics]
                 [storefront.hooks.google-tag-manager :as google-tag-manager]
                 [storefront.hooks.stringer :as stringer]])
            [clojure.string :as string]
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

    e/navigate-sign

    ;; The modal links to these, so we don't show on these pages (instead of dismissing the modal).
    e/navigate-content-tos
    e/navigate-content-privacy})

(def adventure-and-quiz-pages
  #{e/navigate-adventure
    e/navigate-shopping-quiz})

(def email-capture-configs
  {"first-pageview-email-capture" {:nav-events-forbid (set/union never-show-on-these-pages adventure-and-quiz-pages)}
   "adv-quiz-email-capture" {:nav-events-allow adventure-and-quiz-pages}})

(def model-keypath [:models :email-capture])
(def textfield-keypath (conj model-keypath :textfield))
(def capture-observed-at-keypath (conj model-keypath :capture-observed-at))
(def dismissal-observed-ats-keypath (conj model-keypath :dismissal-observed-ats))

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
  (let [{:keys [capture-observed-at
                dismissal-observed-ats]} (get-in app-state model-keypath)
        captured?                        (boolean capture-observed-at)
        dismissed?                       (boolean (get dismissal-observed-ats email-capture-id))]
    {:email-capture-id email-capture-id
     :captured?        captured?
     :dismissed?       dismissed?
     :displayable?     (and (not captured?)
                            (not dismissed?)
                            (experiments/in-house-email-capture? app-state))}))

(defn refresh-dismissed-ats [cookie]
  #?(:clj nil
     :cljs
     (doseq [capture-modal-id (keys email-capture-configs)]
       ;; These cookies get refreshed on every navigate so that they expire only
       ;; after 30 minutes of inactivity
       (when-let [dismissed-at (cookie-jar/retrieve-email-capture-dismissed-at capture-modal-id cookie)]
         (cookie-jar/save-email-capture-dismissed-at capture-modal-id cookie dismissed-at)))))

(defmethod t/transition-state e/biz|email-capture|reset
  [_ _ _ state]
  (assoc-in state model-keypath {}))

(defmethod fx/perform-effects e/biz|email-capture|reset
  [_ _ _ state _]
  #?(:cljs
     (when-let [cookie (get-in state k/cookie)]
       (doseq [id (keys email-capture-configs)]
         (when (cookie-jar/retrieve-email-capture-dismissed-at id cookie)
           (publish e/biz|email-capture|dismissal-observed {:id     id
                                                            :reason "cookie"})))
       (when (cookie-jar/retrieve-email-captured-at cookie)
         (publish e/biz|email-capture|capture-observed {:reason "cookie"})))))

(defn now-iso []
  (spice.date/to-iso (spice.date/now)))

(defmethod fx/perform-effects e/biz|email-capture|captured
  [_ _ _ state _]
  #?(:cljs
     (cookie-jar/save-email-captured-at (get-in state k/cookie) (now-iso)))
  (publish e/biz|email-capture|capture-observed {:reason "capture"}))

;; capture-observed = know the email address has been captured, i.e.
;; * an email address has been entered in the modal (see also "captured", below)
;; * em_hash query param has been consumed
;; * user is logged in
(defmethod t/transition-state e/biz|email-capture|capture-observed
  [_ _ _ state]
  (assoc-in state capture-observed-at-keypath (now-iso)))

(defmethod fx/perform-effects e/biz|email-capture|dismissed
  [_ _ {:keys [id]} state _]
  #?(:cljs
     (cookie-jar/save-email-capture-dismissed-at id (get-in state k/cookie) (now-iso)))
  (publish e/biz|email-capture|dismissal-observed {:id id}))

;; dismissal-observed = know the modal has been seen and dismissed
(defmethod t/transition-state e/biz|email-capture|dismissal-observed
  [_ _ {:keys [id]} state]
  (assoc-in state (conj dismissal-observed-ats-keypath id) (now-iso)))

(defmethod trk/perform-track e/biz|email-capture|deployed ; TODO fire
  [_ events {:keys [id]} app-state]
  #?(:cljs
     (stringer/track-event "email_capture-deploy" {:email-capture-id id})))

(defmethod trk/perform-track e/biz|email-capture|captured
  [_ event {:keys [id]} app-state]
  #?(:cljs
     (let [no-errors?     (empty? (get-in app-state k/errors))
           captured-email (get-in app-state textfield-keypath)]
       (when no-errors?
         ;; TODO: CONSIDER READDING THESE
         ;; (facebook-analytics/subscribe)
         ;; (google-tag-manager/track-email-capture-capture {:email captured-email})
         (stringer/track-event "email_capture-capture"
                               {:email            captured-email
                                :email-capture-id id
                                :test-variations  (get-in app-state k/features)
                                :store-slug       (get-in app-state k/store-slug)
                                :store-experience (get-in app-state k/store-experience)})))))

(defmethod trk/perform-track e/biz|email-capture|dismissed
  [_ events {:keys [id]} app-state]
  #?(:cljs
     (stringer/track-event "email_capture-dismiss" {:email-capture-id id})))
