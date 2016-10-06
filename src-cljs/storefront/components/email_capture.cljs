(ns storefront.components.email-capture
  (:require [sablono.core :refer-macros [html]]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.component :as component]
            [storefront.platform.component-utils :as utils]))

(defn component [{:keys [email field-errors focused]} owner {:keys [on-close]}]
  (component/create
   (html
    (ui/modal {:on-close on-close
               :bg-class "bg-darken-4"}
              [:div.bg-white.rounded.p2
               (ui/modal-close {:on-close on-close})
               [:form.col-12.flex.flex-column.items-center
                {:on-submit (utils/send-event-callback events/control-email-captured-submit)}
                [:div.h3.navy.bold.mb2 "Become an Insider"]
                [:p.h5.gray.mb2
                 "Want exclusive offers and first access to products? Sign up for our email alerts below!"]
                [:div.col-12.border-top.border-silver ui/nbsp]
                (ui/text-field {:errors   (get field-errors ["email"])
                                :keypath  keypaths/captured-email
                                :focused  focused
                                :label    "Your Email Address"
                                :name     "email"
                                :required true
                                :type     "email"
                                :value    email})
                [:div.col-12.mtn2.mb2 (ui/submit-button "Submit")]
                [:p.h6.gray.mb2
                 "By signing up, you agree to receive Mayvenn emails and promotions. "
                 "You can unsubscribe at any time. See our Privacy Policy for details."]]]))))

(defn query [data]
  {:email        (get-in data keypaths/captured-email)
   :field-errors (get-in data keypaths/field-errors)
   :focused      (get-in data keypaths/ui-focus)})

(defn built-component [data opts]
  (component/build component (query data) opts))

