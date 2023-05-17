(ns ui.legal
  (:require [storefront.components.ui :as ui]
            [storefront.component :as c :refer [defcomponent]]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.keypaths :as keypaths]
            [storefront.events :as e]))

(defcomponent tos-form-footnote [{:keys [copy-type]} _ _]
  [:p.content-3
   "By submitting this form, I agree to the "
   (if (= copy-type "stylist")
     [:a.underline.p-color (utils/route-to e/navigate-content-program-terms)
      "Stylist Terms & Conditions"]
     [:a.underline.p-color (utils/route-to e/navigate-content-tos)
      "Terms & Conditions"])
   "."])

;; TODO: extract keypaths from this query
(defn opt-in-query [copy-type phone-transactional-opt-in-value phone-marketing-opt-in-value]
  (condp = copy-type
    "stylist"
    {:marketing-opt-in/id          "phone-marketing-opt-in"
     :marketing-opt-in/label       "… text me marketing messages."
     :marketing-opt-in/legal       (str "By selecting “Yes”, I’m signing an agreement to"
                                        " permit Mayvenn to text me recurring automated marketing promotions, surveys and"
                                        " personalized messages using the number I entered above.  I understand these texts may"
                                        " be sent using an automatic telephone dialing system or other automated system for the"
                                        " selection and dialing of numbers and that I am not required to consent to receive these"
                                        " texts or sign this agreement as a condition of any purchase.")
     :marketing-opt-in/value       phone-marketing-opt-in-value
     :marketing-opt-in/keypath     keypaths/reset-password-sms-transactional-optin
     :transactional-opt-in/id      "phone-transactional-opt-in"
     :transactional-opt-in/label   "… text me updates about transactional orders that my customers or I purchase. This includes booking appointments."
     :transactional-opt-in/legal   (str "By selecting “Yes”,"
                                        " I agree that Mayvenn can text me automated messages about my order"
                                        " (e.g. delivery updates and feedback requests)"
                                        " using the number I entered above.")
     :transactional-opt-in/value   phone-transactional-opt-in-value
     :transactional-opt-in/keypath keypaths/reset-password-sms-marketing-optin
     :opt-in-legalese/title        "Would you like to receive text messages from us?"
     :opt-in-legalese/terms-nav    [e/navigate-content-sms]
     :opt-in-legalese/privacy-nav  [e/navigate-content-privacy]}

    "user"
    {:marketing-opt-in/id          "phone-marketing-opt-in"
     :marketing-opt-in/label       "… text me marketing messages."
     :marketing-opt-in/legal       (str "By selecting “Yes”, I’m signing an agreement to"
                                        " permit Mayvenn to text me recurring automated marketing promotions, surveys and"
                                        " personalized messages using the number I entered above.  I understand these texts may"
                                        " be sent using an automatic telephone dialing system or other automated system for the"
                                        " selection and dialing of numbers and that I am not required to consent to receive these"
                                        " texts or sign this agreement as a condition of any purchase.")
     :marketing-opt-in/value       phone-marketing-opt-in-value
     :marketing-opt-in/keypath     keypaths/reset-password-sms-transactional-optin
     :transactional-opt-in/id      "phone-transactional-opt-in"
     :transactional-opt-in/label   "… text me updates about my orders"
     :transactional-opt-in/legal   (str "By selecting “Yes”,"
                                        " I agree that Mayvenn can text me automated messages about my order"
                                        " (e.g. delivery updates and feedback requests)"
                                        " using the number I entered above.")
     :transactional-opt-in/value   phone-transactional-opt-in-value
     :transactional-opt-in/keypath keypaths/reset-password-sms-marketing-optin
     :opt-in-legalese/title        "Would you like to receive text messages from us?"
     :opt-in-legalese/terms-nav    [e/navigate-content-sms]
     :opt-in-legalese/privacy-nav  [e/navigate-content-privacy]}

    {}))

(defcomponent opt-in ;; chances, you probably want to use opt-in-section
  [{:keys [id label legal value keypath]} _ _]
  (when id
    [:div.flex.items-center.col-12.flex-column-on-mb.my3-on-mb.items-start-on-tb-dt
     [:div.h6.my1.py1.mr1.flex
      [:div.mr3
       (ui/radio-section
        {:id           (str id "-yes-radio")
         :name         id
         :data-test    (str id "-yes")
         :checked      (boolean value)
         :on-change    (fn [e] (handle-message e/control-change-state
                                               {:keypath keypath
                                                :value   true}))}
        "Yes")]
      [:div.mr3
       (ui/radio-section
        {:id           (str id "-no-radio")
         :name         id
         :data-test    (str id "-no")
         :checked      (not (boolean value))
         :on-change    (fn [e] (handle-message e/control-change-state
                                               {:keypath keypath
                                                :value   false}))}
        "No")]]
     [:span.content-2.col-10.col-12-on-tb-dt.center-align-on-mb.mtn2.mt0-on-tb-dt.pt3
      label
      [:span.content-3.block.gray-700 legal]]]))

(defcomponent opt-in-section
  [{:opt-in-legalese/keys [title terms-nav privacy-nav]
    :as                   options}
   _ _]
  [:div.flex.flex-column.col-12.mb2
   [:h2.col-12.my1.proxima.title-3.shout.bold (or title "Would you like to receive text notifications from us?")]
   [:span.content-3
    "Message & data rates may apply. Message frequency varies. Reply HELP for help or STOP to cancel."
    " See "
    [:a.underline.p-color (apply utils/route-to terms-nav) "Terms"]
    " & "
    [:a.underline.p-color (apply utils/route-to privacy-nav) "Privacy Policy"]
    " for more details. "]
   ;; TODO(jeff): seems like a good use for spice.maps/with, but some compile error occurs when importing that namespace
   (let [{:transactional-opt-in/keys [id label legal value keypath]}
         options]
     (c/build opt-in {:id          id
                      :label       label
                      :legal       legal
                      :value       value
                      :keypath     keypath
                      :terms-nav   terms-nav
                      :privacy-nav privacy-nav}))
   ;; TODO(jeff): seems like a good use for spice.maps/with, but some compile error occurs when importing that namespace
   (let [{:marketing-opt-in/keys [id label legal value keypath]}
         options]
     (when id
       (c/build opt-in {:id          id
                        :label       label
                        :legal       legal
                        :value       value
                        :keypath     keypath
                        :terms-nav   terms-nav
                        :privacy-nav privacy-nav})))])
