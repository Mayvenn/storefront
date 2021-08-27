(ns storefront.components.email-capture
  (:require [mayvenn.concept.email-capture :as concept]
            [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [storefront.accessors.experiments :as experiments]
            [storefront.browser.scroll :as scroll]
            [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.components.popup :as popup]
            [storefront.components.svg :as svg]
            [storefront.effects :as fx]
            [storefront.events :as e]
            [storefront.keypaths :as k]
            [storefront.platform.messages :as messages
             :refer [handle-message]
             :rename {handle-message publish}]
            [storefront.platform.component-utils :as utils]
            [popup.organisms]))

(defn ^:private invalid-email? [email]
  (not (and (seq email)
            (< 3 (count email))
            (string/includes? email "@")
            (not (string/ends-with? email "@")))))

(c/defdynamic-component template
  (did-mount
   [this]
   (scroll/disable-body-scrolling)
   (publish e/control-menu-collapse-all))
  (will-unmount
   [this]
   (scroll/enable-body-scrolling))
  (render
   [this]
   (let [props          (c/get-props this)
         {:keys [id
                 dismiss-message
                 submit-message
                 title
                 placeholder-text
                 call-to-action
                 fine-print
                 errors
                 focused
                 text-field-keypath
                 email]} props
         close-dialog-href (apply utils/fake-href dismiss-message)]
     (ui/modal
      {:close-attrs close-dialog-href
       :col-class   "col-11 col-5-on-tb col-4-on-dt flex justify-center"
       :bg-class    "bg-darken-4"}
      [:div.flex.flex-column.bg-cover.bg-top.bg-white

       {:style {:max-width "400px"}
        :data-test   (str id "-modal")}
       [:a.block.flex.justify-end
        (ui/big-x {:data-test (str id "-dismiss")
                   :attrs     close-dialog-href})]
       [:div {:style {:height "110px"}}]
       [:div.px4.pt1.py3.m4.bg-lighten-4
        [:form.col-12.flex.flex-column.items-center
         {:on-submit (apply utils/send-event-callback submit-message)}
         title
         [:div.col-12.mx-auto
          (ui/text-field {:errors    (get errors ["email"])
                          :keypath   text-field-keypath
                          :focused   focused
                          :label     placeholder-text
                          :name      "email"
                          :required  true
                          :type      "email"
                          :value     email
                          :class     "col-12 center"
                          :data-test (str id "-input")})
          (ui/submit-button call-to-action
                            {:data-test (str id "-submit")})]]]
       [:div fine-print]]))))

(defn query [app-state]
  (let [capture-modal-id       "first-pageview-email-capture"
        {:keys [displayable?]} (spice.core/sspy (concept/<-trigger capture-modal-id app-state))
        errors                 (get-in app-state (conj k/field-errors ["email"]))
        focused                (get-in app-state k/ui-focus)
        textfield-keypath      concept/textfield-keypath
        email                  (get-in app-state textfield-keypath)]
    (when displayable?
      {:id                 "email-capture"
       :dismiss-message    [e/biz|email-capture|dismissed {:id capture-modal-id}]
       :submit-message     [e/biz|email-capture|captured {:id    capture-modal-id
                                                          :email email}]
       :title              [:span "Join our email list and get "
                            [:span "$35 OFF"]
                            " your first order"]
       :placeholder-text   "Enter Email Address"
       :call-to-action     "Save me $35 Now"
       :fine-print         [:span "*$35 Off offer is for first-time subscribers only. $200 minimum purchase required. "
                            "Mayvenn may choose to modify the promotion at any time. "
                            "*I consent to receive Mayvenn marketing content via email. "
                            "For further information, please read our "
                            "Terms" ; TODO nav to navigate-content-tos
                            " and "
                            "Privacy Policy" ; TODO nav to navigate-content-privacy
                            ". Unsubscribe anytime."]
       :errors             errors
       :focused            focused
       :text-field-keypath textfield-keypath
       :email              email})))

(defn ^:export built-component [app-state opts]
  (let [{:as data :keys [id]} (query app-state)]
    (when id
      (c/build template data opts))))

;; (defmethod fx/perform-effects e/control-email-capture-dismissed
;;   [_ _ {:keys [nav]} state _]
;;   (publish e/biz|email-capture|dismissed {:reason "nav"}))

;; HOW TO SOLVE THE FOLLOW-LINK-AND-CLOSE-MODAL PROBLEM? OPTIONS
;; 1. Do it like we do popups: if a popup is showing and you nav, close it
;; 2. Add interstitial control event to dismissing with an optional nav argument
;; 3. Make each link a control event that dismissed and navigates
;; 4. Make one control event with nav arg that dismisses and navigates
