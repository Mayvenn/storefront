(ns storefront.components.email-capture
  (:require [clojure.spec.alpha :as s]
            [storefront.accessors.experiments :as experiments]
            [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]))

(s/def ::hiccup-html (s/and vector? #(keyword? (first %))))
(s/def :capture/call-to-action ::hiccup-html)
(s/def :capture/email string?)
(s/def :form/errors (s/map-of string? string?))
(s/def :form/focused boolean?)

(s/def ::href string?)
(s/def ::on-click fn?)
(s/def :handler/close-dialog (s/keys :req-un {::href ::on-click}))

(s/def :handler/submit fn?)

(s/def ::data
  (s/keys :req [:capture/call-to-action :capture/email
                :handler/close-dialog :handler/submit
                :form/errors :form/focused]))

(s/fdef component
  :args (s/cat :query-data ::data :owner any? :props any?)
  :ret vector?)

(def discount-cta
  [:div.center.line-height-3
   [:h1.bold.teal.mb2 {:style {:font-size "36px"}} "Get 35% Off!"]
   [:p.h5.m2
    (str "Sign up now and we'll email you a promotion code for "
         "35% off your first order of 3 bundles or more.")]])

(def flawless-cta
  [:span
   [:div [:h1.bold.teal.mb0.center {:style {:font-size "36px"}}
          "You're Flawless"]
    [:p.h5.mb1.center "Make sure your hair is too"]]
   [:p.h5.my2.line-height-2.center
    "Sign up now for exclusive discounts, stylist-approved hair "
    "tips, and first access to new products."]])

(defn discount-for?
  "These are the experiences that the discount should be called-to-action"
  [experience]
  (contains? experience #{"mayvenn-classic" "influencer"}))

(defn call-to-action
  "Determine the call-to-action for the current experience"
  [app-state]
  (let [store-experience (get-in app-state keypaths/store-experience)
        discount-cta?    (and (discount-for? store-experience)
                              (not (experiments/the-ville? app-state)))]
    (if discount-cta? discount-cta flawless-cta)))

(defn query
  [app-state]
  {:capture/call-to-action (call-to-action app-state)
   :capture/email          (get-in app-state keypaths/captured-email)
   :handler/close-dialog   (utils/fake-href events/control-email-captured-dismiss)
   :handler/submit         (utils/send-event-callback events/control-email-captured-submit)
   :form/errors            (get-in app-state keypaths/field-errors)
   :form/focused           (get-in app-state keypaths/ui-focus)})

(defn component
  [{:capture/keys [call-to-action email]
    :handler/keys [close-dialog submit]
    :form/keys    [errors focused]} _ _]
  (component/create
   (ui/modal
    {:close-attrs close-dialog
     :col-class   "col-11 col-5-on-tb col-4-on-dt flex justify-center"
     :bg-class    "bg-darken-4"}
    [:div.flex.flex-column.bg-cover.bg-top.bg-email-capture
     {:style {:max-width "400px"}}
     [:div.flex.justify-end
      (ui/big-x {:data-test "dismiss-email-capture"
                 :attrs     close-dialog})]
     [:div {:style {:height "110px"}}]
     [:div.px4.pt1.py3.m4.bg-lighten-4
      [:form.col-12.flex.flex-column.items-center {:on-submit submit}
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
