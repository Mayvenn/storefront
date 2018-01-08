(ns storefront.components.email-capture
  (:require [sablono.core :refer [html]]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.component :as component]
            [storefront.platform.component-utils :as utils]))

(defn component [{:keys [email field-errors focused]} owner _]
  (let [close-attrs (utils/fake-href events/control-email-captured-dismiss)]
    (component/create
     (html
      (ui/modal {:close-attrs close-attrs
                 :bg-class    "bg-darken-4"}
                [:div.flex.flex-column
                 {:style {:background-size     "cover"
                          :background-position "top"
                          :background-image    "url('//ucarecdn.com/173e14ac-e55b-49f2-89e6-793051979629/emailmodalheroIMG.png')"}}
                 [:div.flex.justify-end
                  (ui/modal-close-big {:data-test "dismiss-email-capture" :close-attrs close-attrs})]
                 [:div {:style {:height "230"}}]
                 [:div.p4.m3 {:style {:background "white" :opacity 0.8}}
                  [:form.col-12.flex.flex-column.items-center
                   {:on-submit (utils/send-event-callback events/control-email-captured-submit)}
                   [:div.h0.bold.teal.mb0.nowrap "You're Flawless"]
                   [:p.mb2 "Make sure your hair is too"]
                   [:p.mb2.center "Sign up now for exclusive discounts, stylist-approved hair tips, and first access to new products."]
                   (ui/text-field {:errors   (get field-errors ["email"])
                                   :keypath  keypaths/captured-email
                                   :focused  focused
                                   :label    "Your E-Mail Address"
                                   :name     "email"
                                   :required true
                                   :type     "email"
                                   :value    email
                                   :class    "center"})
                   [:div.col-12.col-6-on-tb-dt.mx-auto.my2 (ui/submit-button "Sign Up Now")]]]])))))

(defn query [data]
  {:email        (get-in data keypaths/captured-email)
   :field-errors (get-in data keypaths/field-errors)
   :focused      (get-in data keypaths/ui-focus)})

(defn built-component [data opts]
  (component/build component (query data) opts))
