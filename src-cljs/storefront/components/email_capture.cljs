(ns storefront.components.email-capture
  (:require [storefront.accessors.experiments :as experiments]
            [mayvenn.concept.awareness :as awareness]
            [mayvenn.concept.email-capture :as concept]
            [mayvenn.concept.funnel :as funnel]
            [storefront.browser.scroll :as scroll]
            [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.events :as e]
            [storefront.keypaths :as k]
            [storefront.transitions :as t]
            [storefront.platform.messages :as messages
             :refer [handle-message]
             :rename {handle-message publish}]
            [storefront.platform.component-utils :as utils]
            [storefront.effects :as fx]))

(defn m-header [id close-dialog-href]
  [:div.flex.justify-between.items-center.p2.bg-white
   [:div]
   [:div.flex.justify-center.pt1
    ^:inline (svg/mayvenn-logo {:width "52px" :height "30px"})]
   (ui/modal-close {:data-test   (str id "-dismiss")
                    :class       "fill-black stroke-black"
                    :close-attrs close-dialog-href})])

(def hr-divider
  [:hr.border-top.border-gray.col-12.m0
   {:style {:border-bottom 0 :border-left 0 :border-right 0}}])

(defn email-field [{:email-capture.email-field/keys [id placeholder focused keypath email]}]
  [:div.mx-auto.mb3
   (ui/text-field {:keypath   keypath
                   :focused   focused
                   :label     placeholder
                   :name      "email"
                   :required  true
                   :type      "email"
                   :value     email
                   :class     "col-12 bg-white"
                   :data-test id})])

(defn phone-field [{:email-capture.phone-field/keys [id placeholder focused keypath phone]}]
  [:div.mx-auto.mb3
   (ui/text-field {:keypath   keypath
                   :focused   focused
                   :label     placeholder
                   :name      "phone"
                   :required  false
                   :type      "tel"
                   :value     phone
                   :class     "col-12 bg-white"
                   :data-test id})])

(defn cta
  [{:email-capture.cta/keys [value id]}]
  [:div.mb2 (ui/submit-button-medium value {:data-test id})])

(def sms-fine-print
  [:div.left-align.mb3
   {:style {:font "12px/17px 'Proxima Nova', Arial, sans-serif"}}
   [:div.bold.py1 "Message & data rates may apply. Message frequency varies. Reply HELP for help or STOP to cancel. See Terms & Privacy Policy for more details."]
   [:div "By submitting my phone number, I’m signing an agreement to permit Mayvenn to text me recurring automated marketing promotions, surveys and personalized messages using the number I entered above. I understand these texts may be sent using an automatic telephone dialing system or other automated system for the selection and dialing of numbers and that I am not required to consent to receive these texts or sign this agreement as a condition of any purchase."]])

(defn fine-print
  [prefix]
  [:div.px2.pt4.pb6
   {:style {:color "#6b6b6b"
            :font  "12px/17px 'Proxima Nova', Arial, sans-serif"}}
   prefix
   " For further information, please read our "
   [:a.p-color (utils/route-to e/navigate-content-tos) "Terms"]
   " and "
   [:a.p-color (utils/route-to e/navigate-content-privacy) "Privacy Policy"]
   ". Unsubscribe anytime."])

(defn fine-print-template-2
  [prefix]
  [:div.px2.pt2.pb4
   {:style {;:color "#6b6b6b"
            :font  "12px/17px 'Proxima Nova', Arial, sans-serif"}}
   prefix
   " For further information, please read our "
   [:a.p-color (utils/route-to e/navigate-content-tos) "Terms"]
   " and "
   [:a.p-color (utils/route-to e/navigate-content-privacy) "Privacy Policy"]
   ". Unsubscribe anytime."])

;; In Contentful, this is labeled as:
;; "Email Modal: Template 1" (content type id: emailModalTemplate)
(defn email-capture-modal-template-1
  [{:keys [id] :as data}]
  (c/html
   [:div.flex.flex-column
    {:data-test (str id "-modal")}
    (m-header id (apply utils/fake-href (:email-capture.dismiss/target data)))
    (let [{:email-capture.photo/keys [url title description]} data]
      (when (seq url)
        (ui/aspect-ratio 4 3
                         (ui/img
                          {:max-size     500
                           :src          url
                           :title        title
                           :class        "col-12"
                           :style        {:vertical-align "bottom"}
                           :alt          description}))))
    (let [{:email-capture.copy/keys [title subtitle supertitle fine-print-lead-in]} data]
      [:div.p4.black
       {:class (:email-capture.design/background-color data)}
       [:form.col-12.center.px1
        {:on-submit (apply utils/send-event-callback (:email-capture.submit/target data))}
        [:div.mb2
         [:div.title-2.proxima.shout supertitle]
         [:div.title-1.canela.p-color title]
         [:div.title-2.proxima subtitle]]
        [:div.px3
         (email-field data)
         (cta data)]]
       hr-divider
       (fine-print fine-print-lead-in)])]))

;; In Contentful, this is from emailModalTemplate2
(defn email-capture-modal-template-2
  [{:keys [id] :as data}]
  (c/html
   [:div.flex.flex-column
    {:data-test (str id "-modal")}
    (let [{:email-capture.photo/keys [url title description]} data]
      (when (seq url)
        (ui/aspect-ratio 4 3
                         (ui/img
                          {:max-size     500
                           :src          url
                           :title        title
                           :class        "col-12"
                           :style        {:vertical-align "bottom"}
                           :alt          description}))))
    (let [{:email-capture.copy/keys [title subtitle supertitle fine-print-lead-in]} data]
      [:div.p4.black
       {:class (:email-capture.design/background-color data)}
       [:form.col-12.center.p1.pb3
        {:on-submit (apply utils/send-event-callback (:email-capture.submit/target data))}
        [:div.mb2
         [:div.title-2.proxima.shout supertitle]
         [:div.title-1.canela.p-color title]
         [:div.title-2.proxima subtitle]]
        [:div.px3
         (email-field data)
         (phone-field data)
         sms-fine-print
         (cta data)
         (ui/button-small-underline-black
          {:on-click   (apply utils/send-event-callback (:email-capture.dismiss/target data))
           :aria-label "no thanks"}
          "No Thanks")]]
       hr-divider
       (fine-print-template-2 fine-print-lead-in)])]))

(defn email-capture-modal-template-hdyhau
  [data]
  (c/html
   [:div.bg-cool-gray.p3.center.fixed.bottom-0
    (let [{:email-capture.hdyhau/keys [form title subtitle target]} data]
      [:form.content-3.col-9.mx-auto
       {:on-submit (apply utils/send-event-callback target)}
       [:div.pb2.content-2.bold title]
       [:div.pb2.content-3.bold subtitle]
       [:div
        (for [{:keys [label keypath value]} form]
          [:div {:key label}
           (ui/check-box {:label    label
                          :keypath  keypath
                          :value    value
                          :boxsize "10px"})])]
       (ui/submit-button-medium "Submit" {:disabled? (every? (comp not :value) form)})
       (ui/button-small-underline-black
        {:on-click   (apply utils/send-event-callback (:email-capture.dismiss/target data))
         :aria-label "skip"}
        "Skip")])]))

(c/defdynamic-component template
  (did-mount
   [this]
   (let [{:as                 data
          :email-capture/keys [trigger-id variation-description template-content-id]} (c/get-props this)]
     (scroll/disable-body-scrolling)
     (publish e/control-menu-collapse-all)
     (publish e/funnel|acquisition|prompted
              {:method                       :email-modal/trigger
               :email-modal/template-id      template-content-id
               :email-modal/test-description variation-description
               :email-modal/trigger-id       trigger-id})
     (publish e/biz|email-capture|deployed
              {:trigger-id            trigger-id
               :variation-description variation-description
               :template-content-id   template-content-id})))
  (will-unmount
   [this]
   (scroll/enable-body-scrolling))
  (render
   [this]
   (let [{:keys               [id]
          :email-capture/keys [content-type]
          :email-capture.hdyhau/keys [title]
          :as                 data} (c/get-props this)
         template                   (cond title
                                          email-capture-modal-template-hdyhau

                                          (= content-type "emailModalTemplate2")
                                          email-capture-modal-template-2

                                          :else email-capture-modal-template-1)]
     (if template
       (ui/modal
        {:close-attrs (apply utils/fake-href (:email-capture.dismiss/target data))
         :col-class   "col-12 col-5-on-tb col-4-on-dt flex justify-center"
         :bg-class    "bg-darken-4"}
        (template data))
       (js/console.error (str "Content-type not found: " content-type))))))

(defmethod fx/perform-effects e/email-modal-submitted
  [_ _ {:keys [email-modal values]} _ state]
  (let [{{:keys [template-content-id hdyhau]} :email-modal-template
         variation-description                :description
         {:keys [trigger-id]}                 :email-modal-trigger}
        email-modal

        email-address (get values "email-capture-input")
        phone-number  (get values "phone-capture-input")]
    ;; TODO Identify with backend and publish after success
    (publish e/funnel|acquisition|succeeded
             {:prompt {:method                       :email-modal/trigger
                       :email-modal/trigger-id       trigger-id
                       :email-modal/template-id      template-content-id
                       :email-modal/test-description variation-description}
              :action {:auth.email/id email-address}})
    (publish e/biz|email-capture|captured
             {:trigger-id            trigger-id
              :variation-description variation-description
              :template-content-id   template-content-id
              :hdyhau                hdyhau
              :email                 email-address
              :phone                 phone-number})))

(defmethod fx/perform-effects e/homepage-email-submitted
  [_ _ {:keys [email-modal values]} _ _]
  (let [{{:keys [template-content-id]} :email-modal-template
         variation-description         :description
         {:keys [trigger-id]}          :email-modal-trigger}
        email-modal

        email-address (get values "email-capture-input")]
    ;; TODO Identify with backend and publish after success
    (publish e/funnel|acquisition|succeeded
             {:prompt {:method                       :email-modal/trigger
                       :email-modal/trigger-id       trigger-id
                       :email-modal/template-id      template-content-id
                       :email-modal/test-description variation-description}
              :action {:auth.email/id email-address}})
    (publish e/biz|email-capture|captured
             {:trigger-id            trigger-id
              :variation-description variation-description
              :template-content-id   template-content-id
              :email                 email-address})))

(defmethod t/transition-state e/email-modal-submitted
  [_ _ {:keys [email-modal]} app-state]
  (if (and (:hdyhau (:email-modal-template email-modal))
           (experiments/hdyhau-email-capture? app-state))
    (assoc-in app-state k/show-hdyhau true)
    (assoc-in app-state k/show-hdyhau false)))

(defmethod t/transition-state e/homepage-email-submitted
  [_ _ _ app-state]
  (assoc-in app-state k/homepage-email-submitted true))

(defmethod fx/perform-effects e/email-modal-dismissed
  [_ _ {:keys [email-modal]} _ _]
  (let [{{:keys [template-content-id]} :email-modal-template
         variation-description         :description
         {:keys [trigger-id]}          :email-modal-trigger}
        email-modal]
    (publish e/funnel|acquisition|failed
             {:prompt {:method                       :email-modal/trigger
                       :email-modal/trigger-id       trigger-id
                       :email-modal/template-id      template-content-id
                       :email-modal/test-description variation-description}})
    (publish e/biz|email-capture|dismissed
             {:trigger-id            trigger-id
              :variation-description variation-description
              :template-content-id   template-content-id})))

(defn shuffled-hdyhau-options
  []
  (shuffle awareness/hdyhau))

(def memoize-hdyhau-options
  (memoize shuffled-hdyhau-options))

(defn query [state email-modal]
  (when-let [{{:keys [template-content-id] :as content} :email-modal-template
              variation-description                     :description
              {:keys [trigger-id]}                      :email-modal-trigger}
             email-modal]
    ;; Handle modal inputs/actions
    (let [focused            (get-in state k/ui-focus)
          textfield-keypath  concept/textfield-keypath
          phonefield-keypath concept/phonefield-keypath
          show-hdyhau?       (get-in state k/show-hdyhau)
          email              (get-in state textfield-keypath)
          phone              (get-in state phonefield-keypath)
          hdyhau             (memoize-hdyhau-options)]
      (merge {:id                                    "email-capture"
              :email-capture/trigger-id              trigger-id
              :email-capture/variation-description   variation-description
              :email-capture/template-content-id     template-content-id
              :email-capture/content-type            (:content/type content)
              :email-capture.dismiss/target          [e/email-modal-dismissed {:email-modal email-modal}]
              :email-capture.submit/target           [e/email-modal-submitted {:email-modal email-modal
                                                                               :values      {"email-capture-input" email
                                                                                             "phone-capture-input" phone}}]
              :email-capture.design/background-color (:background-color content)
              :email-capture.design/close-x-color    (:close-xcolor content)
              :email-capture.copy/title              (:title content)
              :email-capture.copy/subtitle           (:subtitle content)
              :email-capture.copy/supertitle         (:supertitle content)
              :email-capture.copy/fine-print-lead-in (:fine-print-lead-in content)
              :email-capture.cta/id                  "email-capture-submit"
              :email-capture.cta/value               (:cta-copy content)
              :email-capture.email-field/id          "email-capture-input"
              :email-capture.email-field/placeholder (:email-input-field-placeholder-copy content)
              :email-capture.email-field/focused     focused
              :email-capture.email-field/keypath     textfield-keypath
              :email-capture.email-field/email       email
              :email-capture.phone-field/id          "email-capture-input"
              :email-capture.phone-field/placeholder (:sms-input-field-placeholder-copy content)
              :email-capture.phone-field/focused     focused
              :email-capture.phone-field/keypath     phonefield-keypath
              :email-capture.phone-field/phone       phone
              :email-capture.photo/url               (-> content :hero-image :file :url)
              :email-capture.photo/title             (-> content :hero-image :title)
              :email-capture.photo/description       (-> content :hero-image :description)}
             (when (and show-hdyhau? (:hdyhau content))
               {:email-capture.hdyhau/title    "Thanks for signing up!"
                :email-capture.hdyhau/subtitle "How did you hear about us? Select all that apply."
                :email-capture.hdyhau/form     (->> hdyhau
                                                    (mapv (fn [[slug label]]
                                                            {:label   label
                                                             :keypath (conj k/models-hdyhau :to-submit slug)
                                                             :value   (get (:to-submit (get-in state k/models-hdyhau)) slug false)})))
                :email-capture.hdyhau/target   [e/hdyhau-email-capture-submitted]})))))

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
    (when matcher (js/console.error (str "No matching content/type for matcher " (pr-str matcher))))))

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

(defn- hidden-email-modal-for-in-situ-email-capture?
  [state {{:keys [trigger-id]} :email-modal-trigger}]
  (= trigger-id "adv-quiz-email-capture"))

(defn ^:export built-component [state opts]
  (let [long-timer   (get-in state concept/long-timer-started-keypath)
        short-timers (get-in state concept/short-timer-starteds-keypath)
        email-modals (triggered-email-modals<- state long-timer short-timers)]
    ;; FIXME indeterminate behavior when multiple triggers are valid and active
    (when-let [email-modal (first email-modals)]
      (when-not (hidden-email-modal-for-in-situ-email-capture? state email-modal)
        (c/build template (query state email-modal) opts)))))
