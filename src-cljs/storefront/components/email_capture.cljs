(ns storefront.components.email-capture
  (:require [mayvenn.concept.email-capture :as concept]
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
     {:on-submit (apply utils/send-event-callback (:email-capture.submit/target data))
      :data-test (:email-capture.submit/id data)}
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

(defn design-2 [{:keys [id] :as   data}]
  [:div.bg-pale-purple.p4
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
    {:on-submit (apply utils/send-event-callback (:email-capture.submit/target data))
     :data-test (:email-capture.submit/id data)}
    (text-field data)
    (cta-2 data)]
   hr-divider
   fine-print-2])

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
              :email-capture.submit/id              "email-capture-submit"
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
               {:email-capture.cta/value              "Sign Up"}

               ;; ELSE
               nil)))))

(defn ^:export built-component [app-state opts]
  (when-let [data (query app-state)]
    (c/build template data opts)))
