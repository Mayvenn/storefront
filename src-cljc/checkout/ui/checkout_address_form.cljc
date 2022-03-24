(ns checkout.ui.checkout-address-form
  (:require #?@(:cljs
                [[storefront.components.places :as places]])
            [storefront.component :as c :refer [defcomponent]]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.events :as e]
            [storefront.components.ui :as ui]))

(defn ^:private address-names
  [address-first-name address-last-name]
  (when (:id address-first-name)
    [:div.col-12
     (ui/text-field-group address-first-name address-last-name)]))

(defn ^:private address-email
  [address-email]
  (when (:id address-email)
    (ui/text-field address-email)))

(defn ^:private address-phone
  [address-phone]
  (when (:id address-phone)
    (ui/text-field address-phone)))

(defn ^:private address-address1
  [address-address1]
  (when (:id address-address1)
    #?@(:cljs [(c/build places/component address-address1)])))

(defn ^:private address-address2-and-zipcode
  [address-address2 address-zipcode]
  (when (:id address-zipcode)
    [:div.col-12
     (ui/text-field-group address-address2 address-zipcode)]))

(defn ^:private address-city
  [address-city]
  (when (:id address-city)
    (ui/text-field address-city)))

(defn ^:private address-state
  [address-state]
  (when (:id address-state)
    (ui/select-field address-state)))

(defn ^:private shipping-address
  [{:checkout-address-title/keys [primary]
    :keys [shipping-address-first-name shipping-address-last-name
           shipping-address-email
           shipping-address-phone
           shipping-address-address1
           shipping-address-address2 shipping-address-zipcode
           shipping-address-city
           shipping-address-state]}]
  [:div.flex.flex-column.items-center.col-12
   {:key "shipping-address"}
   [:div.col-12.my1.proxima.title-3.shout.bold primary]
   (address-names shipping-address-first-name shipping-address-last-name)
   (address-email shipping-address-email)
   (address-phone shipping-address-phone)
   (address-address1 shipping-address-address1)
   (address-address2-and-zipcode shipping-address-address2 shipping-address-zipcode)
   (address-city shipping-address-city)
   (address-state shipping-address-state)])

(defn billing-checkbox
  [billing-address-checkbox]
  [:div.col-12.my1
   [:label.py1
    [:div.mr1
     (ui/check-box
      billing-address-checkbox)]]])

(defn ^:private billing-address
    [{:checkout-address-title/keys [secondary]
      :keys [billing-address-checkbox
             billing-address-first-name billing-address-last-name
             billing-address-phone
             billing-address-address1
             billing-address-address2 billing-address-zipcode
             billing-address-city
             billing-address-state]}]
    [:div.flex.flex-column.items-center.col-12
     {:key "billing-address"}
     [:div.col-12.my1.proxima.title-3.shout.bold secondary]
     (billing-checkbox billing-address-checkbox)
     (address-names billing-address-first-name billing-address-last-name)
     (address-phone billing-address-phone)
     (address-address1 billing-address-address1)
     (address-address2-and-zipcode billing-address-address2 billing-address-zipcode)
     (address-city billing-address-city)
     (address-state billing-address-state)])

(c/defcomponent ^:private opt-in
  [{:keys [id label copy value keypath]} _ _]
  (when id
    [:div.flex.items-center.col-12.flex-column-on-mb.my3-on-mb
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
     [:span.content-2.col-10.col-12-on-tb-dt.center-align-on-mb.mtn2.mt0-on-tb-dt
      label]]))

(c/defcomponent opt-in-section
  [{:opt-in-legalese/keys [terms-nav privacy-nav]
    :as                   options}
   _ _]
  [:div.flex.flex-column.col-12.mb2
   [:div.col-12.my1.proxima.title-3.shout.bold "Would you like to receive text notifications from us?"]
   [:span.content-3 
    "Message & data rates may apply. Message frequency varies. Reply HELP for help or STOP to cancel."
    " See "
    [:a.underline.p-color (apply utils/route-to terms-nav) "Terms"]
    " & "
    [:a.underline.p-color (apply utils/route-to privacy-nav) "Privacy Policy"]
    " for more details. "]
   ;; TODO(jeff): seems like a good use for spice.maps/with, but some compile error occurs when importing that namespace
   (let [{:transactional-opt-in/keys [id label copy value keypath]}
         options]
     (c/build opt-in {:id          id
                      :label       label
                      :copy        copy
                      :value       value
                      :keypath     keypath
                      :terms-nav   terms-nav
                      :privacy-nav privacy-nav}))
   ;; TODO(jeff): seems like a good use for spice.maps/with, but some compile error occurs when importing that namespace
   (let [{:marketing-opt-in/keys [id label copy value keypath]}
         options]
     (when id
       (c/build opt-in {:id          id
                        :label       label
                        :copy        copy
                        :value       value
                        :keypath     keypath
                        :terms-nav   terms-nav
                        :privacy-nav privacy-nav})))])


(defn ^:private continue-to-payment
  [{:continue-to-pay-cta/keys [spinning? label data-test id]}]
  (when id
    [:div.my2.col-12.col-8-on-tb-dt.mx-auto
     (ui/submit-button label {:spinning? spinning?
                              :data-test data-test})]))

(c/defcomponent organism
  [{:keys [become-guest?] :as data} _ _]
  [:div.m-auto.col-8-on-tb-dt
   [:div.p3
    [:form.col-12.flex.flex-column.items-center
     {:on-submit (utils/send-event-callback e/control-checkout-update-addresses-submit
                                            {:become-guest? become-guest?})
      :data-test "address-form"}
     (shipping-address data)
     (billing-address data)
     (c/build opt-in-section data)
     (continue-to-payment data)]]])
