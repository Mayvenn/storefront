(ns checkout.returning-or-guest-v2020-05
  (:require [checkout.ui.secure-checkout :as secure-checkout]
            [checkout.ui.checkout-address-form :as checkout-address-form]
            [storefront.accessors.auth :as auth]
            [storefront.components.checkout-steps :as checkout-steps]
            [storefront.components.checkout-address :as checkout-address]
            [storefront.component :as c :refer [defcomponent]]
            [storefront.components.ui :as ui]
            [storefront.events :as e]
            [storefront.keypaths :as k]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]
            [ui.promo-banner :as promo-banner]
            ))

(def or-separator
  [:div.black.py1.flex.items-center.col-10.mx-auto
   [:div.flex-grow-1.border-bottom.border-gray]
   [:div.h4.mx2 "or"]
   [:div.flex-grow-1.border-bottom.border-gray]])

(defcomponent template
  [{:keys [promo-banner
           secure-checkout
           checkout-steps
           shipping-address
           billing-address
           checkout-address-form]}
   owner _]
  [:div.container
   (c/build promo-banner/sticky-organism promo-banner nil)
   (c/build secure-checkout/organism secure-checkout)
   or-separator
   (c/build checkout-steps/component checkout-steps nil)
   (c/build checkout-address-form/organism checkout-address-form)])

(defn ^:private secure-checkout-query
  [facebook-loaded?]
  {:secure-checkout.title/primary        "Secure Checkout"
   :secure-checkout.title/secondary      "Sign in or checkout as guest. You’ll have an opportunity to create an account after placing your order. "
   :secure-checkout.cta/id               "begin-password-sign-in-button"
   :secure-checkout.cta/value            "Facebook Sign in"
   :secure-checkout.cta/target           e/navigate-checkout-sign-in
   :secure-checkout.facebook-cta/id      "sign-in-facebook"
   :secure-checkout.facebook-cta/loaded? facebook-loaded?})

;; TODO(heather): refactor checkout_steps component so this query looks like the others
(defn ^:private checkout-steps-query
  [nav-event guest?]
  {:current-navigation-event nav-event
   :checkout-title-content   (if guest?
                               [:div.title-2.canela.center.pt5 "Guest Checkout"]
                               [:div.title-1.canela.center.pt5 "Secure Checkout"])})

(defn ^:private checkout-address-form-query
  [shipping-address
   billing-address
   bill-to-shipping-address?
   states
   email
   google-maps-loaded?
   field-errors
   focused
   saving?]
  (cond->
      (merge
       #:checkout-address-title {:primary "Shipping Address"
                                 :secondary "Billing Address"}
       {:shipping-address-first-name {:label         "First Name"
                                      :keypath       k/checkout-shipping-address-first-name
                                      :errors        (get field-errors ["shipping-address" "first-name"])
                                      :focused       focused
                                      :value         (:first-name shipping-address)
                                      :id            "shipping-first-name"
                                      :name          "shipping-first-name"
                                      :data-test     "shipping-first-name"
                                      :auto-complete "shipping given-name"
                                      :required      true
                                      :type          "text"
                                      :max-length    24}

        :shipping-address-last-name {:label         "Last Name"
                                     :keypath       k/checkout-shipping-address-last-name
                                     :errors        (get field-errors ["shipping-address" "last-name"])
                                     :focused       focused
                                     :value         (:last-name shipping-address)
                                     :id            "shipping-last-name"
                                     :name          "shipping-last-name"
                                     :data-test     "shipping-last-name"
                                     :auto-complete "shipping family-name"
                                     :required      true
                                     :type          "text"
                                     :max-length    25}

        :shipping-address-email {:label     "Email"
                                 :keypath   k/checkout-guest-email
                                 :errors    (get field-errors ["email"])
                                 :focused   focused
                                 :value     email
                                 :id        "shipping-email"
                                 :name      "shipping-email"
                                 :data-test "shipping-email"
                                 :type      "email"
                                 :required  true}

        :shipping-address-phone {:label     "Mobile Phone"
                                 :keypath   k/checkout-shipping-address-phone
                                 :errors    (get field-errors ["shipping-address" "phone"])
                                 :focused   focused
                                 :value     (:phone shipping-address)
                                 :type      "tel"
                                 :id        "shipping-phone"
                                 :name      "shipping-phone"
                                 :data-test "shipping-phone"
                                 :required  true}



        :shipping-address-address2 {:label         "Apt/Suite"
                                    :keypath       k/checkout-shipping-address-address2
                                    :errors        (get field-errors ["shipping-address" "address2"])
                                    :focused       focused
                                    :value         (:address2 shipping-address)
                                    :id            "shipping-address2"
                                    :name          "shipping-address2"
                                    :data-test     "shipping-address2"
                                    :auto-complete "shipping address-line2"
                                    :type          "text"
                                    :max-length    100}

        :shipping-address-zipcode {:label         "Zipcode"
                                   :keypath       k/checkout-shipping-address-zip
                                   :errors        (get field-errors ["shipping-address" "zipcode"])
                                   :focused       focused
                                   :value         (:zipcode shipping-address)
                                   :id            "shipping-zip"
                                   :name          "shipping-zip"
                                   :data-test     "shipping-zip"
                                   :required      true
                                   :auto-complete "shipping postal-code"
                                   :type          "text"
                                   :max-length    5}

        :shipping-address-city {:label     "City"
                                :keypath   k/checkout-shipping-address-city
                                :errors    (get field-errors ["shipping-address" "city"])
                                :focused   focused
                                :value     (:city shipping-address)
                                :id        "shipping-city"
                                :name      "shipping-city"
                                :data-test "shipping-city"
                                :required  true
                                :type      "text"}

        :shipping-address-state {:label     "State"
                                 :keypath   k/checkout-shipping-address-state
                                 :errors    (get field-errors ["shipping-address" "state"])
                                 :focused   focused
                                 :value     (:state shipping-address)
                                 :options   states
                                 :placeholder "State"
                                 :id        "shipping-state"
                                 :name      "shipping-state"
                                 :data-test "shipping-state"
                                 :required  true}

        :billing-address-checkbox {:type      "checkbox"
                                   :label     "Use same address?"
                                   :id        "use_billing"
                                   :data-test "use-billing"
                                   :value     bill-to-shipping-address?
                                   :keypath   k/checkout-bill-to-shipping-address}
        :continue-to-pay-cta/spinning? saving?
        :continue-to-pay-cta/label     "Continue to Payment"
        :continue-to-pay-cta/data-test "address-form-submit"
        :continue-to-pay-cta/id        "address-form-submit"
        :become-guest?                 true})
    (not bill-to-shipping-address?)
    (merge
     {:billing-address-first-name {:type          "text"
                                   :label         "First Name"
                                   :keypath       k/checkout-billing-address-first-name
                                   :focused       focused
                                   :value         (:first-name billing-address)
                                   :errors        (get field-errors ["billing-address" "first-name"])
                                   :name          "billing-first-name"
                                   :id            "billing-first-name"
                                   :data-test     "billing-first-name"
                                   :auto-complete "billing given-name"
                                   :required      true}
      :billing-address-last-name {:type          "text"
                                  :label         "Last Name"
                                  :keypath       k/checkout-billing-address-last-name
                                  :focused       focused
                                  :value         (:last-name billing-address)
                                  :errors        (get field-errors ["billing-address" "last-name"])
                                  :name          "billing-last-name"
                                  :id            "billing-last-name"
                                  :data-test     "billing-last-name"
                                  :auto-complete "billing family-name"
                                  :required      true}
      :billing-address-phone {:data-test "billing-phone"
                              :errors    (get field-errors ["billing-address" "phone"])
                              :id        "billing-phone"
                              :keypath   k/checkout-billing-address-phone
                              :focused   focused
                              :label     "Mobile Phone"
                              :name      "billing-phone"
                              :required  true
                              :type      "tel"
                              :value     (:phone billing-address)}
      :billing-address-address2 {:type          "text"
                                 :label         "Apt/Suite"
                                 :keypath       k/checkout-billing-address-address2
                                 :focused       focused
                                 :value         (:address2 billing-address)
                                 :errors        (get field-errors ["billing-address" "address2"])
                                 :name          "billing-address2"
                                 :id            "billing-address2"
                                 :auto-complete "billing address-line2"
                                 :data-test     "billing-address2"}
      :billing-address-zipcode {:type           "text"
                                :label         "Zip Code"
                                :keypath       k/checkout-billing-address-zip
                                :focused       focused
                                :value         (:zipcode billing-address)
                                :errors        (get field-errors ["billing-address" "zipcode"])
                                :name          "billing-zip"
                                :id            "billing-zip"
                                :data-test     "billing-zip"
                                :required      true
                                :auto-complete "billing postal-code"
                                :max-length    5
                                :min-length    5
                                :pattern       "\\d{5}"
                                :title         "zip code must be 5 digits"}
      :billing-address-city {:data-test "billing-city"
                             :errors    (get field-errors ["billing-address" "city"])
                             :id        "billing-city"
                             :keypath   k/checkout-billing-address-city
                             :focused   focused
                             :label     "City"
                             :name      "billing-city"
                             :required  true
                             :type      "text"
                             :value     (:city billing-address)}
      :billing-address-state {:data-test   "billing-state"
                              :errors      (get field-errors ["billing-address" "state"])
                              :id          "billing-state"
                              :keypath     k/checkout-billing-address-state
                              :focused     focused
                              :label       "State"
                              :options     states
                              :placeholder "State"
                              :required    true
                              :value       (:state billing-address)}})

    google-maps-loaded?
    (merge
     {:shipping-address-address1 {:label           "Address"
                                  :keypath         k/checkout-shipping-address-address1
                                  :address-keypath k/checkout-shipping-address
                                  :errors          (get field-errors ["shipping-address" "address1"])
                                  :focused?        focused
                                  :value           (:address1 shipping-address)
                                  :id              "shipping-address1"
                                  :data-test       "shipping-address1"
                                  :auto-complete   "shipping address-line1"
                                  :max-length      100}})
    (and google-maps-loaded? (not bill-to-shipping-address?))
    (merge
     {:billing-address-address1  {:label           "Address"
                                  :id              "billing-address1"
                                  :data-test       "billing-address1"
                                  :address-keypath k/checkout-billing-address
                                  :keypath         k/checkout-billing-address-address1
                                  :focused         focused
                                  :errors          (get field-errors ["billing-address" "address1"])
                                  :auto-complete   "billing address-line1"
                                  :value           (:address1 billing-address)}})))

(defn query [app-state]
  (let [facebook-loaded?          (get-in app-state k/loaded-facebook)
        current-nav-event         (get-in app-state k/navigation-event)
        guest?                    (= :guest (::auth/as (auth/signed-in app-state)))
        shipping-address          (get-in app-state k/checkout-shipping-address)
        billing-address           (get-in app-state k/checkout-billing-address)
        bill-to-shipping-address? (get-in app-state k/checkout-bill-to-shipping-address)
        states                    (map (juxt :name :abbr) (get-in app-state k/states))
        email                     (get-in app-state k/checkout-guest-email)
        google-maps-loaded?       (get-in app-state k/loaded-google-maps)
        field-errors              (get-in app-state k/field-errors)
        focused                   (get-in app-state k/ui-focus)
        saving?                   (utils/requesting? app-state request-keys/update-addresses)]
    {:promo-banner          (promo-banner/query app-state) ;; no app-states
     :secure-checkout       (secure-checkout-query facebook-loaded?)
     :checkout-steps        (checkout-steps-query current-nav-event guest?)
     :checkout-address-form (checkout-address-form-query shipping-address
                                                         billing-address
                                                         bill-to-shipping-address?
                                                         states
                                                         email
                                                         google-maps-loaded?
                                                         field-errors
                                                         focused
                                                         saving?)}))

#_(defn ^:export page [app-state] ;pipedream
  (c/build
   template) )
