(ns mayvenn.concept.email-capture
  (:require #?@(:cljs
                [storefront.frontend-trackings
                 [storefront.browser.cookie-jar :as cookie-jar]])
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
            [storefront.trackings :as trk]))

(def never-show-on-these-pages
  #{e/navigate-cart

    e/navigate-checkout-address
    e/navigate-checkout-payment
    e/navigate-checkout-confirmation

    ;; The modal links to these, so we don't show on these pages (instead of dismissing the modal).
    e/navigate-content-tos
    e/navigate-content-privacy})

(def adventure-and-quiz-pages
  #{e/navigate-adventure
    e/navigate-shopping-quiz})

(def email-capture-configs
  {"first-pageview-email-capture" {:nav-events-forbid (set/union never-show-on-these-pages adventure-and-quiz-pages)}
   "example-email-capture" {:nav-events-allow adventure-and-quiz-pages}})

(def model-keypath [:models :email-capture])
(def textfield-keypath (conj model-keypath :textfield))
(def capture-observed-at-keypath (conj model-keypath :capture-observed-at))
(def dismissal-observed-ats-keypath (conj model-keypath :dismissal-observed-ats))

(defn <-trigger [id app-state]
  (let [{:keys [nav-events-forbid nav-events-allow]} (get email-capture-configs id)
        {:keys [capture-observed-at
                dismissal-observed-ats]} (get-in app-state model-keypath)
        captured?              (boolean capture-observed-at)
        dismissed?             (boolean (get dismissal-observed-ats id))
        nav-event              (get-in app-state k/navigation-event)
        location-approved?     (if nav-events-allow
                                 (some #(routes/sub-page? [nav-event {}] [% {}])
                                       nav-events-allow)
                                 (not-any? #(routes/sub-page? [nav-event {}] [% {}])
                                           nav-events-forbid))]
    {:id                 id
     :captured?          captured?
     :dismissed?         dismissed?
     :location-approved? location-approved?
     :displayable?       (and (not captured?)
                              (not dismissed?)
                              location-approved?)}))

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

(defmethod t/transition-state e/biz|email-capture|deployed
    [_ _ _ state]
    ;; TODO: tracking
  )

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
(defmethod t/transition-state e/biz|email-capture|capture-observed ;;consider "inferred"
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
