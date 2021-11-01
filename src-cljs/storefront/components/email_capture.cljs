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

(defn bg-image [{:email-capture.photo/keys [uuid-mob uuid-dsk]}]
  [:div
   [:div.hide-on-mb
    (ui/aspect-ratio 1 1
                     (ui/img {:max-size "500px"
                              :class    "col-12"
                              :style    {:vertical-align "bottom"}
                              :src      uuid-dsk}))]
   [:div.hide-on-tb-dt
    (ui/aspect-ratio 4 3
                     (ui/img {:max-size "500px"
                              :class    "col-12"
                              :style    {:vertical-align "bottom"}
                              :src      uuid-mob}))]])

(defn title [{:email-capture.title/keys [primary]}]
  [:div.proxima.title-1.mb3
   primary])

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

(def fine-print-1
  [:div.px2.pt4
   {:style {:color "#767676"
            :font  "10px/16px 'Proxima Nova', Arial, sans-serif"}}
   "*$35 Off offer is for first-time subscribers only. $200 minimum purchase required. "
   "Mayvenn may choose to modify the promotion at any time. "
   "*I consent to receive Mayvenn marketing content via email. "
   "For further information, please read our "
   [:a.p-color (utils/route-to e/navigate-content-tos) "Terms"]
   " and "
   [:a.p-color (utils/route-to e/navigate-content-privacy) "Privacy Policy"]
   ". Unsubscribe anytime."])

(defn cta-1 [{:email-capture.cta/keys [value id]}]
  [:div.mb5 (ui/submit-button-medium value {:data-test id})])

;; TODO consider using c/build and c/html and stuff
(defn design-1 [{:keys [id]
                 :as   data}]
  [:div.flex.flex-column
   {:data-test (str id "-modal")}
   (m-header id (apply utils/fake-href (:email-capture.dismiss/target data)))
   (bg-image data)
   [:div.p4.black.bg-white
    [:form.col-12.center.px1
     {:on-submit (apply utils/send-event-callback (:email-capture.submit/target data))}
     (title data)
     [:div.px3
      (text-field data)
      (cta-1 data)]]
    hr-divider
    fine-print-1]])

(defn cta-2 [{:email-capture.cta/keys [value id]}]
  [:div.mb5 (ui/submit-button-medium value {:data-test id})])

(def fine-print-2
  [:div.px2.pt4.pb6
   {:style {:color "#6b6b6b"
            :font "12px/17px 'Proxima Nova', Arial, sans-serif"}}
   "*$35 Off offer is for first-time subscribers only. $200 minimum purchase required. "
   "Mayvenn may choose to modify the promotion at any time. "
   "*I consent to receive Mayvenn marketing content via email. "
   "For further information, please read our "
   [:a.p-color (utils/route-to e/navigate-content-tos) "Terms"]
   " and "
   [:a.p-color (utils/route-to e/navigate-content-privacy) "Privacy Policy"]
   ". Unsubscribe anytime."])

(defn design-2 [{:keys [id] :as data}]
  [:div.bg-pale-purple.p4
   {:data-test (str id "-modal")}
   [:div.flex.justify-end
    (ui/modal-close {:data-test   (str id "-dismiss")
                     :class       "fill-black stroke-black"
                     :close-attrs (apply utils/fake-href (:email-capture.dismiss/target data))})]
   [:div.flex.justify-center.my10
    ^:inline (svg/mayvenn-logo {:width "83px" :height "48px"})]
   [:div.center.canela.title-1.mb6 "Looking For A Stylist?"]
   [:div.center.mb6
    {:style {:font "32px/37px 'Proxima Nova'"
             :letter-spacing "0.33px"}}
    "Sign up to receive top-rated stylists near you, promotional offers, and for a limited time, get "
    [:span.p-color "$35 OFF"]
    " for joining."]
   [:form.col-12.center.px4
    {:on-submit (apply utils/send-event-callback (:email-capture.submit/target data))}
    (text-field data)
    (cta-2 data)]
   hr-divider
   fine-print-2])

;; Used for Early Access
(def fine-print-3
  [:div.px2.pt4
   {:style {:color "white"
            :font  "10px/16px 'Proxima Nova', Arial, sans-serif"}}
   "Mayvenn may choose to modify the promotion at any time. "
   "*I consent to receive Mayvenn marketing content via email. "
   "For further information, please read our "
   [:a.yellow (utils/route-to e/navigate-content-tos) "Terms"]
   " and "
   [:a.yellow (utils/route-to e/navigate-content-privacy) "Privacy Policy"]
   ". Unsubscribe anytime."])

(defn early-access-main [app-state]
  [:div
   {:style     {:max-width "580px"}
    :data-test "email-capture-modal"}
   [:div.relative
    (ui/ucare-img {:style {:max-width "580px"}} "78759574-51d8-4982-b70f-61d469fe6a4d")
    [:div.absolute
     {:style {:top "1rem" :right "1rem"}}
     (ui/modal-close {:data-test   "email-capture-dismiss"
                      :class       "fill-white stroke-white"
                      :close-attrs (apply utils/fake-href [e/biz|email-capture|dismissed {:id "first-pageview-email-capture"}])})]]
   [:div.white.center.px6.mx-auto.mynp6
    {:style {:background-image "url('//ucarecdn.com/bf5c99de-3829-465d-a066-231dcdc00ac9/-/format/auto/-/quality/lightest/-/resize/580x/')"}}
    [:div.pt6.shout.bold.title-1 "Get it First"]
    [:div.canela.title-1.py2.yellow "Black Friday"]
    [:div.shout.bold.title-1 "Early Subscriber Access"]
    [:div.p3
     [:div.content-2.left-align.px3
      [:div.my2 "Join our email list to get exclusive early access to our Black Friday deals, can't-miss product drops, and so much more."]
      [:div.my2 "Stock up on dreamy hair at our best prices."]]
     (let [email (get-in app-state concept/textfield-keypath)]
       [:form.px3
        {:on-submit (apply utils/send-event-callback [e/biz|email-capture|captured {:id    "first-pageview-email-capture"
                                                                                    :email email}])}
        (ui/text-field {:errors    (get (get-in app-state (conj k/field-errors ["email"])) ["email"])
                        :keypath   concept/textfield-keypath
                        :focused   (get-in app-state k/ui-focus)
                        :label     "Enter Your Email"
                        :name      "email"
                        :required  true
                        :type      "email"
                        :value     email
                        :class     "col-12 bg-white"
                        :data-test "email-capture-input"})
        [:div.my2 (ui/submit-button-medium "Sign Up Now" {:data-test "email-capture-submit"})]])
     fine-print-3]]])

(defn early-access-quiz [app-state]
  [:div
   {:style     {:max-width "580px"}
    :data-test "email-capture-modal"}
   [:div.relative
    (ui/ucare-img {:style {:max-width "580px"}} "4d9d1edb-cc49-4af9-900e-bef610e16675")
    [:div.absolute
     {:style {:top "1rem" :right "1rem"}}
     (ui/modal-close {:data-test   "email-capture-dismiss"
                      :class       "fill-black stroke-black"
                      :close-attrs (apply utils/fake-href [e/biz|email-capture|dismissed {:id "adv-quiz-email-capture"}])})]]
   [:div.white.center.px6.mx-auto.mynp6
    {:style {:background-image "url('//ucarecdn.com/bf5c99de-3829-465d-a066-231dcdc00ac9/-/format/auto/-/quality/lightest/-/resize/580x/')"}}
    [:div.pt6.shout.bold.title-1 "Get it First"]
    [:div.canela.title-1.py2.yellow "Black Friday"]
    [:div.shout.bold.title-1 "Early Subscriber Access"]
    [:div.p3
     [:div.content-2.left-align.px3
      [:div.my2 "Join our email list to get exclusive early access to our Black Friday deals, can't-miss product drops, and so much more."]
      [:div.my2 "Stock up on dreamy hair at our best prices."]]
     (let [email (get-in app-state concept/textfield-keypath)]
       [:form.px3
        {:on-submit (apply utils/send-event-callback [e/biz|email-capture|captured {:id    "adv-quiz-email-capture"
                                                                                    :email email}])}
        (ui/text-field {:errors    (get (get-in app-state (conj k/field-errors ["email"])) ["email"])
                        :keypath   concept/textfield-keypath
                        :focused   (get-in app-state k/ui-focus)
                        :label     "Enter Your Email"
                        :name      "email"
                        :required  true
                        :type      "email"
                        :value     email
                        :class     "col-12 bg-white"
                        :data-test "email-capture-input"})
        [:div.my2 (ui/submit-button-medium "Sign Up Now" {:data-test "email-capture-submit"})]])
     fine-print-3]]])

(c/defdynamic-component template
  (did-mount
   [this]
   (scroll/disable-body-scrolling)
   (publish e/control-menu-collapse-all)
   (publish e/biz|email-capture|deployed {:id (:capture-modal-id (c/get-props this))}))
  (will-unmount
   [this]
   (scroll/enable-body-scrolling))
  (render
   [this]
   (let [{:keys [capture-modal-id]
          :as   data} (c/get-props this)]
     (ui/modal
      {:close-attrs (apply utils/fake-href (:email-capture.dismiss/target data))
       :col-class   "col-12 col-5-on-tb col-4-on-dt flex justify-center"
       :bg-class    "bg-darken-4"}
      ((case capture-modal-id
         "first-pageview-email-capture" design-1
         "adv-quiz-email-capture"       design-2) data)))))

(defn query [app-state]
  (let [nav-event              (get-in app-state k/navigation-event)
        capture-modal-id       (concept/location->email-capture-id nav-event)
        {:keys [displayable?]} (concept/<-trigger capture-modal-id app-state)
        errors                 (get-in app-state (conj k/field-errors ["email"]))
        focused                (get-in app-state k/ui-focus)
        textfield-keypath      concept/textfield-keypath
        email                  (get-in app-state textfield-keypath)]
    (when displayable?
      (merge {:id                                   "email-capture"
              :capture-modal-id                     capture-modal-id
              :email-capture.dismiss/target         [e/biz|email-capture|dismissed {:id capture-modal-id}]
              :email-capture.submit/target          [e/biz|email-capture|captured {:id    capture-modal-id
                                                                                   :email email}]
              :email-capture.cta/id                 "email-capture-submit"
              :email-capture.text-field/id          "email-capture-input"
              :email-capture.text-field/placeholder "Enter Email Address"
              :email-capture.text-field/focused     focused
              :email-capture.text-field/keypath     textfield-keypath
              :email-capture.text-field/errors      errors
              :email-capture.text-field/email       email}
             (case capture-modal-id

               "first-pageview-email-capture"
               {:email-capture.photo/uuid-mob "ef2a5a8b-6da2-4abd-af99-c33d485ac275"
                :email-capture.photo/uuid-dsk "1ba0870d-dad8-466a-adc9-0d5ec77c9944"
                :email-capture.title/primary  [:span "Join our email list and get "
                                               [:span.p-color "$35 OFF"]
                                               " your first order"]
                :email-capture.cta/value      "Sign Up"}

               "adv-quiz-email-capture"
               {:email-capture.cta/value "Sign Up"}

               ;; ENGINEER: Are you adding or altering an email capture modal design? Be sure to let Roman
               ;; know, as he'd like to mirror design changes on OptinMonster (which still show on the Instapages)

               ;; ELSE
               nil)))))

(defn early-access-query [app-state]
  (let [nav-event                (get-in app-state k/navigation-event)
        capture-modal-id         (concept/location->email-capture-id nav-event)]
    {:capture-modal-id capture-modal-id}))

(c/defdynamic-component early-access-template
  (did-mount
   [this]
   (scroll/disable-body-scrolling)
   (publish e/control-menu-collapse-all)
   (publish e/biz|email-capture|deployed {:id (:capture-modal-id (c/get-props this))}))
  (will-unmount
   [this]
   (scroll/enable-body-scrolling))
  (render
   [this]
   (let [{:keys [capture-modal-id]
          :as   data} (c/get-props this)]
     (ui/modal
      {:close-attrs (apply utils/fake-href [e/biz|email-capture|dismissed {:id capture-modal-id}])
       :col-class   "col-12 col-5-on-tb col-4-on-dt flex justify-center"
       :bg-class    "bg-darken-4"}
      ((case capture-modal-id
         "first-pageview-email-capture" early-access-main
         "adv-quiz-email-capture"       early-access-quiz) data)))))

(defn ^:export built-component [app-state opts]
  (when-let [data (query app-state)]
    (if (experiments/early-access? app-state)
      (c/build early-access-template
               (early-access-query app-state)
               opts)
      (c/build template data opts))))
