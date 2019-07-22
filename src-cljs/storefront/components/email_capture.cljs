(ns storefront.components.email-capture
  (:require [clojure.spec.alpha :as s]
            [storefront.accessors.experiments :as experiments]
            [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.components.popup :as popup]
            [storefront.components.svg :as svg]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]))

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
  {:capture/email          (get-in app-state keypaths/captured-email)
   :form/errors            (get-in app-state keypaths/field-errors)
   :form/focused           (get-in app-state keypaths/ui-focus)})

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
  {:capture/email-capture-quadpay-new? (experiments/email-capture-quadpay-new? app-state)
   :capture/email                      (get-in app-state keypaths/captured-email)
   :form/errors                        (get-in app-state keypaths/field-errors)
   :form/focused                       (get-in app-state keypaths/ui-focus)})



(defmethod popup/component :email-capture-quadpay
  [{:capture/keys [email email-capture-quadpay-new?] :form/keys [errors focused]} _ _]
  (component/create
   (if email-capture-quadpay-new?
     (ui/modal
      {:close-attrs close-dialog-href
       :col-class   "col-11 col-5-on-tb col-4-on-dt flex justify-center"}
      [:div.flex.flex-column.bg-cover.bg-top.bg-white.p2.rounded.col-12
       {:style {:max-width "345px"}}
       [:div.flex.justify-end.mt2.mr1
        (svg/simple-x (merge close-dialog-href
                             {:class     "white"
                              :data-test "dismiss-email-capture"
                              :style     {:width  "18px"
                                          :height "18px"
                                          :color  "gray"}}))]
       [:div.bold.px3
        (ui/ucare-img {:width "24px"} ;; Mayvenn logo
                      "6620eab6-ca9b-4400-b8e5-d6ec00654dd3")
        [:div.h0.line-height-1.pt3
         [:div "Buy Now,"]
         [:div "Pay Later."]]
        [:div.flex.my4
         [:div.h2.mtb1 "with"]
         [:div.ml2.mtp6 {:style {:width  "124px"
                                 :height "23px"}}
          svg/quadpay-logo]]
        [:div.mt1
         "Buy hair with "
         [:span.purple "0%"]
         " interest over "
         [:div [:span.purple "4"]
          " installments."]]]
       [:div.px3.mt10.mb4
        [:form.col-12.flex.flex-column.items-center
         {:on-submit submit-callback}
         [:div.col-12.mx-auto
          (ui/text-field {:errors    (get errors ["email"])
                          :keypath   keypaths/captured-email
                          :focused   focused
                          :label     "Your E-Mail Address"
                          :name      "email"
                          :required  true
                          :type      "email"
                          :value     email
                          :class     "dark-gray h6 bg-light-silver"
                          :data-test "email-input"})
          (ui/submit-button "Shop Now"
                            {:color-kw     :color/teal
                             :height-class "py3"
                             :class        "h3 bold mt1"
                             :data-test    "email-input-submit"})]]]])
     (ui/modal ;; not email-capture-quadpay-new?
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
