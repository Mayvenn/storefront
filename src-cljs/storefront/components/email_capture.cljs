(ns storefront.components.email-capture
  (:require [sablono.core :refer [html]]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.component :as component]
            [storefront.platform.component-utils :as utils]
            [storefront.accessors.experiments :as experiments]))

(def email-capture-copy
  {"email-capture-bundle-deal" {:header "Exclusive Bundle Deal"
                                :body "Sign up now and we'll email you an exclusive bundle deal plus free shipping."}})

(defn ^:private email-capture-modal [{:keys [email field-errors focused variation-contents test-variation]} owner]
  (let [close-attrs (utils/fake-href events/control-email-captured-dismiss)]
    (ui/modal {:close-attrs close-attrs
               :col-class   "col-11 col-5-on-tb col-4-on-dt flex justify-center"
               :bg-class    "bg-darken-4"}
              [:div.flex.flex-column.bg-cover.bg-top.bg-email-capture
               {:style {:max-width "400px"}}
               [:div.flex.justify-end
                (ui/big-x {:data-test "dismiss-email-capture" :attrs close-attrs})]
               [:div {:style {:height "110px"}}]
               [:div.px4.pt1.py3.m4.bg-lighten-4
                [:form.col-12.flex.flex-column.items-center
                 {:on-submit (utils/send-event-callback events/control-email-captured-submit {:test-variation test-variation})}
                 (if (and (not (nil? test-variation)) ;; Remove nil case once convert starts bucketing
                          (not= "email-capture-control" test-variation))
                   (let [{:keys [header body]} variation-contents]
                     [:div.center.line-height-3
                      [:h1.bold.teal.mb2 {:style {:font-size "36px"}} header]
                      [:p.h5.m2
                       body]])
                   [:span
                    [:div [:h1.bold.teal.mb0.center {:style {:font-size "36px"}} "You're Flawless"]
                     [:p.h5.mb1.center "Make sure your hair is too"]]
                    [:p.h5.my2.line-height-2.center
                     "Sign up now for exclusive discounts, stylist-approved hair
                     tips, and first access to new products."]])
                 [:div.col-12.col-6-on-dt.mx-auto
                  (ui/text-field {:errors   (get field-errors ["email"])
                                  :keypath  keypaths/captured-email
                                  :focused  focused
                                  :label    "Your E-Mail Address"
                                  :name     "email"
                                  :required true
                                  :type     "email"
                                  :value    email
                                  :class    "col-12 center"})
                  (ui/submit-button "Sign Up Now")]]]])))

(defn component [data owner _]
  (component/create
   (html
    (email-capture-modal data owner))))

(defn query [data]
  (let [test-variation (experiments/email-capture-test-variation data)]
    {:email              (get-in data keypaths/captured-email)
     :field-errors       (get-in data keypaths/field-errors)
     :focused            (get-in data keypaths/ui-focus)
     :test-variation     test-variation
     :variation-contents (email-capture-copy test-variation)}))

(defn built-component [data opts]
  (component/build component (query data) opts))
