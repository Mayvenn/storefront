(ns storefront.components.email-capture
  (:require [storefront.accessors.experiments :as experiments]
            [mayvenn.concept.email-capture :as concept]
            [storefront.browser.scroll :as scroll]
            [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.events :as e]
            [storefront.keypaths :as k]
            [storefront.platform.messages :as messages
             :refer [handle-message]
             :rename {handle-message publish}]
            [storefront.platform.component-utils :as utils]))

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

(defn text-field [{:email-capture.text-field/keys [id placeholder focused keypath errors email]}]
  [:div.mx-auto.mb3
   (ui/text-field {:errors    (get errors ["email"])
                   :keypath   keypath
                   :focused   focused
                   :label     placeholder
                   :name      "email"
                   :required  true
                   :type      "email"
                   :value     email
                   :class     "col-12 bg-white"
                   :data-test id})])

(defn cta
  [{:email-capture.cta/keys [value id]}]
  [:div.mb5 (ui/submit-button-medium value {:data-test id})])

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
        (ui/ctf-img
         {:max-width-px 500
          :url          url}
         {:title title
          :class "col-12"
          :style {:vertical-align "bottom"}
          :alt   description})))
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
         (text-field data)
         (cta data)]]
       hr-divider
       (fine-print fine-print-lead-in)])]))

(c/defdynamic-component contentful-driven-template
  (did-mount
   [this]
   (let [{:as data
          :email-capture/keys [trigger-id variation-description template-content-id]} (c/get-props this)]
     (scroll/disable-body-scrolling)
     (publish e/control-menu-collapse-all)
     (publish e/biz|email-capture|deployed {:trigger-id            trigger-id
                                            :variation-description variation-description
                                            :template-content-id   template-content-id})))
  (will-unmount
   [this]
   (scroll/enable-body-scrolling))
  (render
   [this]
   (let [{:keys               [id]
          :email-capture/keys [content-type]
          :as                 data} (c/get-props this)
         template                   (case content-type
                                      "emailModalTemplate" email-capture-modal-template-1
                                      nil)]
     (if template
       (ui/modal
        {:close-attrs (apply utils/fake-href (:email-capture.dismiss/target data))
         :col-class   "col-12 col-5-on-tb col-4-on-dt flex justify-center"
         :bg-class    "bg-darken-4"}
        (template data))
       (js/console.error (str "Content-type not found: " content-type))))))

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
    (when matcher (js/console.error (str "No matching content/type for matcher " (pr-str matcher))))))
;; TODO: there are other path mathers not accounted for yet.

(defn contentful-driven-query [app-state]
  (let [cms-modal-data          (get-in app-state k/cms-email-modal)
        long-timer-started      (get-in app-state concept/long-timer-started-keypath)
        short-timer-starteds    (get-in app-state concept/short-timer-starteds-keypath)
        chosen-modal            (->> cms-modal-data
                                   vals
                                   (filter #(matcher-matches? app-state (-> % :email-modal-trigger :matcher)))
                                   first)
        errors                  (get-in app-state (conj k/field-errors ["email"]))
        focused                 (get-in app-state k/ui-focus)
        textfield-keypath       concept/textfield-keypath
        email                   (get-in app-state textfield-keypath)
        content                 (:email-modal-template chosen-modal)
        trigger-id              (-> chosen-modal :email-modal-trigger :trigger-id)
        template-content-id     (:template-content-id content)
        in-no-modal-experiment? (or (experiments/quiz-results-email-offer-discount? app-state)
                                    (experiments/quiz-results-email-send-look? app-state))

        variation-description   (-> chosen-modal :description)]
    (when (and trigger-id
               template-content-id
               (not long-timer-started)
               (->> trigger-id (get short-timer-starteds) not))
      (when-not (and in-no-modal-experiment?
                     (= trigger-id "adv-quiz-email-capture"))
        {:id                                    "email-capture"
         :email-capture/trigger-id              trigger-id
         :email-capture/variation-description   variation-description
         :email-capture/template-content-id     template-content-id
         :email-capture/content-type            (:content/type content)
         :email-capture.dismiss/target          [e/biz|email-capture|dismissed {:trigger-id            trigger-id
                                                                                :variation-description variation-description
                                                                                :template-content-id   template-content-id}]
         :email-capture.submit/target           [e/biz|email-capture|captured {:trigger-id            trigger-id
                                                                               :variation-description variation-description
                                                                               :template-content-id   template-content-id
                                                                               :email                 email}]
         :email-capture.design/background-color (:background-color content)
         :email-capture.design/close-x-color    (:close-xcolor content)
         :email-capture.copy/title              (:title content)
         :email-capture.copy/subtitle           (:subtitle content)
         :email-capture.copy/supertitle         (:supertitle content)
         :email-capture.copy/fine-print-lead-in (:fine-print-lead-in content)
         :email-capture.cta/id                  "email-capture-submit"
         :email-capture.cta/value               (:cta-copy content)
         :email-capture.text-field/id           "email-capture-input"
         :email-capture.text-field/placeholder  (:email-input-field-placeholder-copy content)
         :email-capture.text-field/focused      focused
         :email-capture.text-field/keypath      textfield-keypath
         :email-capture.text-field/errors       errors
         :email-capture.text-field/email        email
         :email-capture.photo/url               (-> content :hero-image :file :url)
         :email-capture.photo/title             (-> content :hero-image :title)
         :email-capture.photo/description       (-> content :hero-image :description)}))))

(defn ^:export built-component [app-state opts]
  (when-let [data (contentful-driven-query app-state)]
    (c/build contentful-driven-template data opts)))
