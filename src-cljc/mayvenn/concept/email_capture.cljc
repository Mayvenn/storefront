(ns mayvenn.concept.email-capture
  (:require #?@(:cljs
                [[storefront.browser.cookie-jar :as cookie-jar]
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

(def model-keypath [:models :email-capture])
(def textfield-keypath (conj model-keypath :textfield))
(def phonefield-keypath (conj model-keypath :phone))
(def timer-keypath (conj model-keypath :timer?))
(def toggle-keypath (conj model-keypath :open?))
(def long-timer-started-keypath (conj model-keypath :long-timer-started?))
(def short-timer-starteds-keypath (conj model-keypath :short-timer-starteds?))

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
             :let [cookie-id "1pv"]]
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
           (assoc-in short-timer-starteds-keypath (->> (all-trigger-ids app-state)
                                                       (map (fn [trigger-id]
                                                              [trigger-id (cookie-jar/retrieve-email-capture-short-timer-started?
                                                                           "1pv"
                                                                           cookie)]))
                                                       (into {})))))))

(defmethod fx/perform-effects e/biz|email-capture|captured
  [_ _ {:keys [hdyhau email shipping-address]} state _]
  (when-not
      ;; if getting hdyhau, don't set timer until after hdyhau is collected
   hdyhau
    #?(:cljs
       (cookie-jar/save-email-capture-long-timer-started (get-in state k/cookie)))
    (publish e/biz|email-capture|timer-state-observed)))

(defmethod fx/perform-effects e/biz|email-capture|dismissed
  [_ _ {:keys [id trigger-id]} state _]
  (if (get-in state toggle-keypath)
    (publish e/biz|email-capture|toggle|dismissed)
    (do
      #?(:cljs
         (cookie-jar/save-email-capture-short-timer-started "1pv" (get-in state k/cookie)))
      (publish e/biz|email-capture|timer-state-observed))))

(defmethod t/transition-state e/hdyhau-email-capture-submitted
  [_ _ _ app-state]
  (-> app-state
      (assoc-in [:models :hdyhau :submitted] true)))

(defmethod t/transition-state e/email-modal-opened
  [_ _ _ app-state]
  (assoc-in app-state toggle-keypath true))

(defmethod t/transition-state e/biz|email-capture|toggle|dismissed
  [_ _ _ app-state]
  (assoc-in app-state toggle-keypath false))

(defmethod fx/perform-effects e/hdyhau-email-capture-submitted
  [_ _ data state _]
  (publish e/biz|hdyhau-capture|captured data))

(defmethod fx/perform-effects e/biz|hdyhau-capture|captured
  [_ _ _ state _]
  #?(:cljs
     (cookie-jar/save-email-capture-long-timer-started (get-in state k/cookie)))
  (publish e/biz|email-capture|timer-state-observed))

;;; TRACKING

#?(:cljs
   (defmethod trk/perform-track e/biz|email-capture|deployed
     [_ events {:keys [trigger-id variation-description template-content-id]} app-state]
     (stringer/track-event "email_capture-deploy" {:email-capture-id      trigger-id
                                                   :variation-description variation-description
                                                   :template-content-id   template-content-id})))

#?(:cljs
   (defmethod trk/perform-track e/biz|email-capture|toggle|deployed
     [_ events {:keys [trigger-id variation-description template-content-id]} app-state]
     (stringer/track-event "email_capture-toggled-deploy" {:email-capture-id      trigger-id
                                                           :variation-description variation-description
                                                           :template-content-id   template-content-id})))

#?(:cljs
   (defmethod trk/perform-track e/biz|email-capture|captured
     [_ event {:keys [trigger-id variation-description template-content-id details]} app-state]
     (let [no-errors?     (empty? (get-in app-state k/errors))
           captured-email (get-in app-state textfield-keypath)
           captured-phone (get-in app-state phonefield-keypath)
           persona        (get-in app-state k/models-persona)]
       (when no-errors?
         (stringer/identify {:email captured-email})
         (stringer/track-event "email_capture-capture"
                               (merge
                                (when persona {:persona persona})
                                {:email-capture-id      trigger-id
                                 :variation-description variation-description
                                 :template-content-id   template-content-id
                                 :email                 captured-email
                                 :details               details
                                 :test-variations       (get-in app-state k/features)
                                 :store-slug            (get-in app-state k/store-slug)
                                 :store-experience      (get-in app-state k/store-experience)
                                 :account-profile       (get-in app-state k/account-profile)}))
         (google-analytics/track-generate-lead (get-in app-state k/user-ecd))))))

#?(:cljs
   (defmethod trk/perform-track e/biz|sms-capture|captured
     [_ event {:keys [trigger-id variation-description template-content-id phone]} app-state]
     (let [no-errors?     (empty? (get-in app-state k/errors))
           captured-email (get-in app-state textfield-keypath)
           captured-phone (get-in app-state phonefield-keypath)]
       (when no-errors?
         (stringer/track-event "sms_capture-capture"
                               {:email-capture-id       trigger-id
                                :variation-description  variation-description
                                :template-content-id    template-content-id
                                :email                  captured-email
                                :phone                  captured-phone
                                :phone-marketing-opt-in (boolean phone)
                                :test-variations        (get-in app-state k/features)
                                :store-slug             (get-in app-state k/store-slug)
                                :store-experience       (get-in app-state k/store-experience)
                                :account-profile        (get-in app-state k/account-profile)})))))

#?(:cljs
   (defmethod trk/perform-track e/biz|email-capture|dismissed
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
