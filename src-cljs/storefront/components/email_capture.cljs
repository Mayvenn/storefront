(ns storefront.components.email-capture
  (:require [sablono.core :refer-macros [html]]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.component :as component]
            [storefront.platform.component-utils :as utils]))

(defn component [{:keys [email field-errors focused]} owner _]
  (let [on-close (utils/send-event-callback events/control-email-captured-dismiss)]
    (component/create
     (html
      (ui/modal {:on-close on-close
                 :bg-class "bg-darken-4"}
                [:div.bg-white.rounded.p4
                 (ui/modal-close {:on-close on-close :data-test "dismiss-email-capture"})
                 [:form.col-12.flex.flex-column.items-center
                  {:on-submit (utils/send-event-callback events/control-email-captured-submit)}
                  [:div.h3.navy.bold.mb2 "Become an Insider"]
                  [:p.h5.dark-gray.mb2
                   "Want exclusive offers and first access to products? Sign up for our email alerts below!"]
                  [:div.col-12.border-top.border-light-gray ui/nbsp]
                  (ui/text-field {:errors   (get field-errors ["email"])
                                  :keypath  keypaths/captured-email
                                  :focused  focused
                                  :label    "Your Email Address"
                                  :name     "email"
                                  :required true
                                  :type     "email"
                                  :value    email})
                  [:div.col-12.my2 (ui/submit-button "Submit")]
                  [:p.h6.dark-gray.mb2
                   "By signing up, you agree to receive Mayvenn emails and promotions. "
                   "You can unsubscribe at any time. See our Privacy Policy for details."]]])))))

(defn query [data]
  {:email        (get-in data keypaths/captured-email)
   :field-errors (get-in data keypaths/field-errors)
   :focused      (get-in data keypaths/ui-focus)})

(defn built-component [data opts]
  (component/build component (query data) opts))

