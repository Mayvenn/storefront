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
  (let [errors  (get-in app-state keypaths/field-errors)
        focused (get-in app-state keypaths/ui-focus)
        email   (get-in app-state keypaths/captured-email)]
    {:modal-close/event                  events/control-email-captured-dismiss
     :pre-title/content                  (ui/ucare-img {:width "24px"} ;; Mayvenn logo
                                                       "6620eab6-ca9b-4400-b8e5-d6ec00654dd3")
     :monstrous-title/copy               ["Buy Now," "Pay Later."]
     :subtitle/copy                      [[:div.mtb1 "with"]
                                          [:div.ml2.mtp6 {:style {:width  "124px"
                                                                  :height "23px"}}
                                           ^:inline (svg/quadpay-logo)]]
     :description/copy                   [:div.mt1
                                          "Buy hair with "
                                          [:span.purple "0%"]
                                          " interest over "
                                          [:div [:span.purple "4"]
                                           " installments."]]
     :single-field-form/callback         events/control-email-captured-submit
     :single-field-form/field-data       {:errors    (get errors ["email"])
                                          :keypath   keypaths/captured-email
                                          :focused   focused
                                          :label     "Your E-Mail Address"
                                          :name      "email"
                                          :type      "email"
                                          :value     email
                                          :data-test "email-input"}
     :single-field-form/button-data      {:title        "Shop Now"
                                          :color-kw     :color/teal
                                          :height-class :large
                                          :data-test    "email-input-submit"}}))

(defmethod popup/component :email-capture-quadpay
  [query-data _ _]
  (component/create
   [:div
    (component/build popup.organisms/organism query-data _)]))
