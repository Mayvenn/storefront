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
  [:div.flex.justify-between.items-center.p3.bg-white
   [:div]
   [:div.flex.justify-center.pt1
    ^:inline (svg/mayvenn-logo {:width "52px" :height "30px"})]
   (ui/modal-close {:data-test   (str id "-dismiss")
                    :class       "fill-black stroke-black"
                    :close-attrs close-dialog-href})])

(defn bg-image [{:email-capture.photo/keys [uuid]}]
  (ui/img {:max-size "500px"
           :class    "col-12"
           :src      uuid}))

(defn title [{:email-capture.title/keys [primary]}]
  [:div.proxima.mb3
   {:style {:font "900 30px/32px 'Proxima Nova', Arial, sans-serif"
            :letter-spacing "letter-spacing: 0.5px"}}
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
                   :class     "col-12 center bg-white title-3 proxima"
                   :data-test id})])

(defn cta [{:email-capture.cta/keys [value id]}]
      [:div.mb5 (ui/submit-button-medium-black value {:data-test id})])

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
   (let [props             (c/get-props this)
         {:keys [id]
          :as   data}      props
         close-dialog-href (apply utils/fake-href
                                  (:email-capture.dismiss/target data))]
     (ui/modal
      {:close-attrs close-dialog-href
       :col-class   "col-12 col-5-on-tb col-4-on-dt flex justify-center"
       :bg-class    "bg-darken-4"}
      [:div.flex.flex-column
       {:data-test (str id "-modal")}
       (m-header id close-dialog-href)
       (bg-image data)
       [:div.p4.white.bg-p-color
        [:form.col-12.center.px4
         {:on-submit (apply utils/send-event-callback (:email-capture.submit/target data))}
         (title data)
         (text-field data)
         (cta data)]
        hr-divider
        [:div.proxima.content-4.px2.pt4
         {:style {:font "10px/16px 'Proxima Nova', Arial, sans-serif"}}
         "*$35 Off offer is for first-time subscribers only. $200 minimum purchase required. "
         "Mayvenn may choose to modify the promotion at any time. "
         "*I consent to receive Mayvenn marketing content via email. "
         "For further information, please read our "
         [:a.s-color (utils/route-to e/navigate-content-tos) "Terms"]
         " and "
         [:a.s-color (utils/route-to e/navigate-content-privacy) "Privacy Policy"]
         ". Unsubscribe anytime."]]]))))

(defn query [app-state]
  (let [capture-modal-id       "first-pageview-email-capture"
        {:keys [displayable?]} (concept/<-trigger capture-modal-id app-state)
        errors                 (get-in app-state (conj k/field-errors ["email"]))
        focused                (get-in app-state k/ui-focus)
        textfield-keypath      concept/textfield-keypath
        email                  (get-in app-state textfield-keypath)
        id                     "email-capture" ]
    (when displayable?
      {:id                                   id
       :capture-modal-id                     capture-modal-id
       :email-capture.dismiss/target         [e/biz|email-capture|dismissed {:id capture-modal-id}]
       :email-capture.submit/target          [e/biz|email-capture|captured {:id    capture-modal-id
                                                                            :email email}]
       :email-capture.photo/uuid             "1ba0870d-dad8-466a-adc9-0d5ec77c9944"
       :email-capture.title/primary          [:span "Join our email list and get "
                                              [:span
                                               {:style {:color "#97D5CA"}}
                                               "$35 OFF"]
                                              " your first order"]
       :email-capture.text-field/id          (str id "-input")
       :email-capture.text-field/placeholder "ENTER EMAIL ADDRESS"
       :email-capture.text-field/focused     focused
       :email-capture.text-field/keypath     textfield-keypath
       :email-capture.text-field/errors      errors
       :email-capture.text-field/email       email
       :email-capture.cta/id                 (str id "-submit")
       :email-capture.cta/value              "Save me $35 Now"})))

(defn ^:export built-component [app-state opts]
  (let [{:as data :keys [id]} (query app-state)]
    (when id
      (c/build template data opts))))
