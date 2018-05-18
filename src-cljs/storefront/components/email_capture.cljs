(ns storefront.components.email-capture
  (:require [sablono.core :refer [html]]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.component :as component]
            [storefront.platform.component-utils :as utils]
            [storefront.accessors.experiments :as experiments]))

(defn ^:private email-capture-control-modal [{:keys [email field-errors focused test-variation]} owner]
  (let [close-attrs (utils/fake-href events/control-email-captured-dismiss)]
    (ui/modal {:close-attrs close-attrs
               :col-class   "col-11 col-5-on-tb col-4-on-dt"
               :bg-class    "bg-darken-4"}
              [:div.flex.flex-column.bg-cover.bg-top.bg-email-capture
               [:div.flex.justify-end
                (ui/big-x {:data-test "dismiss-email-capture" :attrs close-attrs})]
               [:div {:style {:height "110px"}}]
               [:div.p4.m3.bg-lighten-4
                [:form.col-12.flex.flex-column.items-center
                 {:on-submit (utils/send-event-callback events/control-email-captured-submit {:test-variation test-variation})}
                 [:h1.bold.teal.mb0.center {:style {:font-size "36px"}} "You're Flawless"]
                 [:p.h5.mb1 "Make sure your hair is too"]
                 [:p.h5.mb2.line-height-2.center
                  "Sign up now for exclusive discounts, stylist-approved hair tips, and first access to new products."]
                 (ui/text-field {:errors   (get field-errors ["email"])
                                 :keypath  keypaths/captured-email
                                 :focused  focused
                                 :label    "Your E-Mail Address"
                                 :name     "email"
                                 :required true
                                 :type     "email"
                                 :value    email
                                 :class    "center"})
                 [:div.col-12.col-6-on-tb-dt.mx-auto (ui/submit-button "Sign Up Now")]]]])))

(defn component [data owner _]
  (component/create
   (html
    (case (:test-variation data)
      "email-capture-control" (email-capture-control-modal data owner)
      (email-capture-control-modal data owner)))))  ;; Once convert starts bucketing, change default to nil

(defn query [data]
  {:email          (get-in data keypaths/captured-email)
   :field-errors   (get-in data keypaths/field-errors)
   :focused        (get-in data keypaths/ui-focus)
   :test-variation (experiments/email-capture-test-variation data)})

(defn built-component [data opts]
  (component/build component (query data) opts))
