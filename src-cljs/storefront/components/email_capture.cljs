(ns storefront.components.email-capture
  (:require [clojure.spec.alpha :as s]
            [storefront.accessors.experiments :as experiments]
            [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.components.popup :as popup]
            [storefront.components.svg :as svg]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [popup.organisms]))

(s/def ::hiccup-html (s/and vector? #(keyword? (first %))))
(s/def :capture/email string?)
(s/def :form/errors (s/map-of string? string?))
(s/def :form/focused boolean?)

(s/def ::href string?)
(s/def ::on-click fn?)
(s/def :handler/close-dialog (s/keys :req-un {::href ::on-click}))

(s/def :handler/submit fn?)

(s/def ::data
  (s/keys :req [:handler/close-dialog :handler/submit
                :form/errors :form/focused]))

(s/fdef component
  :args (s/cat :query-data ::data :owner any? :props any?)
  :ret vector?)

(def close-dialog-href (utils/fake-href events/control-email-captured-dismiss))
(def submit-callback (utils/send-event-callback events/control-email-captured-submit))

(defmethod popup/query :email-capture
  [app-state]
  {:capture/email (get-in app-state keypaths/captured-email)
   :form/errors   (get-in app-state keypaths/field-errors)
   :form/focused  (get-in app-state keypaths/ui-focus)})

(defmethod popup/component :email-capture
  [{:capture/keys [call-to-action email] :form/keys [errors focused]} _ _]
  (component/create
   (ui/modal
    {:close-attrs close-dialog-href
     :col-class   "col-11 col-5-on-tb col-4-on-dt flex justify-center"
     :bg-class    "bg-darken-4"}
    [:div.flex.flex-column.bg-cover.bg-top.bg-email-capture
     {:style {:max-width "400px"}}
     [:div.flex.justify-end
      (ui/big-x {:data-test "dismiss-email-capture"
                 :attrs     close-dialog-href})]
     [:div {:style {:height "110px"}}]
     [:div.px4.pt1.py3.m4.bg-lighten-4
      [:form.col-12.flex.flex-column.items-center {:on-submit submit-callback}
       call-to-action
       [:div.col-12.mx-auto
        (ui/text-field {:errors    (get errors ["email"])
                        :keypath   keypaths/captured-email
                        :focused   focused
                        :label     "Your E-Mail Address"
                        :name      "email"
                        :required  true
                        :type      "email"
                        :value     email
                        :class     "col-12 center"
                        :data-test "email-input"})
        (ui/submit-button "Sign Up Now"
                          {:data-test "email-input-submit"})]]]])))

(defmethod popup/query :email-capture-quadpay
  [app-state]
  (popup.organisms/query app-state))

(defmethod popup/component :email-capture-quadpay
  [{:as           args
    :capture/keys [email email-capture-quadpay-new?]
    :form/keys    [errors focused]} _ _]
  (if email-capture-quadpay-new?
    (popup.organisms/organism args _ _)
    (component/create
     (ui/modal
      {:close-attrs close-dialog-href
       :col-class   "col-11 col-5-on-tb col-4-on-dt flex justify-center"}
      [:div.flex.flex-column.bg-cover.bg-top.bg-email-capture
       {:style {:max-width "400px"}}
       [:div.flex.justify-end
        (ui/big-x {:data-test "dismiss-email-capture"
                   :attrs     close-dialog-href})]
       [:div {:style {:height "200px"}}]
       [:div.px4.pt1.py3.m4
        [:form.col-12.flex.flex-column.items-center {:on-submit submit-callback}
         [:div.col-12.mx-auto
          (ui/text-field {:errors    (get errors ["email"])
                          :keypath   keypaths/captured-email
                          :focused   focused
                          :label     "Your E-Mail Address"
                          :name      "email"
                          :required  true
                          :type      "email"
                          :value     email
                          :class     "col-12 center bold"
                          :data-test "email-input"})
          (ui/submit-button "Sign Up Now"
                            {:color-kw     :color/quadpay
                             :height-class "py1"
                             :class        "h6 bold mt1"
                             :data-test    "email-input-submit"})]]]]))))
