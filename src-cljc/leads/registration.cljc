(ns leads.registration
  (:require #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.components.ui :as ui]
            [storefront.assets :as assets]
            [storefront.platform.carousel :as carousel]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [clojure.string :as string]))

(defn ^:private component [data owner opts]
  (component/create
   (let [error                 nil
         errors                {}
         focused-keypath       nil
         anti-forgery-token    nil
         referred              false
         first-name            ""
         last-name             ""
         phone                 ""
         email                 ""
         password              ""
         password-confirmation ""
         referrers-phone       ""
         address1              ""
         address2              ""
         city                  ""
         zip                   ""
         state                 {}
         states                [{}]
         birthday              ""
         is-licensed           ""
         payout-method         ""
         venmo-phone           ""
         paypal-email          ""
         slug                  ""]
     [:div.bg-teal.white
      [:div.max-580.mx-auto
       (when error
         [:p {:class "bg-danger registration-flash"} error])
       [:div.registration-header
        [:div.center.mb3
         [:img {:src (assets/path "/images/leads/logo-knockout.png")}]
         [:h1.mb3 "Join The Mayvenn Movement"]
         "Mayvenn empowers hairstylists to sell directly to their"
         " clients, build their brands, and grow their income. Apply"
         " today! We''ll set you up with a FREE online store, plus:"]
        [:ul.ml1
         [:li "15% commission earned from each sale you make through your Mayvenn store"]
         [:li "Access to Mayvenn's exclusive community for hairstylists and beauty professionals"]
         [:li "Personalized marketing content provided every month to help you promote your business"]
         [:li "A personal sales rep for you, plus the industry’s best customer support for your clients"]]]
       [:div.bg-white.black.p4.my4
        [:div.center.pb2 "Ready to join the movement?"
         [:br]
         "Let's get started."]
        [:form {:action "" :method "POST", :role "form"}
         ;; TODO(FIXME)
         [:input {:type "hidden", :name "__anti-forgery-token", :value anti-forgery-token}]
         (ui/text-field-group
          {:type     "text"
           :label    "First Name *"
           :id       "first_name"
           :name     "first-name"
           :required true
           :errors   (:first-name errors)
           :keypath  keypaths/leads-ui-registration-first-name
           :focused  focused-keypath
           :value    first-name}
          {:type     "text"
           :label    "Last Name *"
           :id       "last_name"
           :name     "last-name"
           :required true
           :errors   (:last-name errors)
           :keypath  keypaths/leads-ui-registration-last-name
           :focused  focused-keypath
           :value    last-name})
         (ui/text-field {:data-test "phone"
                         :errors    (:phone errors)
                         :id        "phone"
                         :keypath   keypaths/leads-ui-registration-phone
                         :focused   focused-keypath
                         :label     "Mobile Phone Number *"
                         :name      "phone"
                         :required  true
                         :type      "tel"
                         :value     phone})
         (ui/text-field {:data-test "email"
                         :errors    (:email errors)
                         :id        "email"
                         :keypath   keypaths/leads-ui-registration-email
                         :focused   focused-keypath
                         :label     "Email"
                         :name      "email"
                         :required  true
                         :type      "email"
                         :value     email})
         (ui/text-field {:data-test "password"
                         :errors    (:password errors)
                         :id        "password"
                         :keypath   keypaths/leads-ui-registration-password
                         :focused   focused-keypath
                         :label     "Password *"
                         :name      "password"
                         :required  true
                         :type      "password"
                         :value     password})
         (ui/text-field {:data-test "password-confirmation"
                         :errors    (:password-confirmation errors)
                         :id        "password-confirmation"
                         :keypath   keypaths/leads-ui-registration-password-confirmation
                         :focused   focused-keypath
                         :label     "Re-type Password *"
                         :name      "password-confirmation"
                         :required  true
                         :type      "password"
                         :value     password-confirmation})
         (ui/check-box {:data-test "referred"
                        :keypath   keypaths/leads-ui-registration-referred
                        :label     "Yes, somebody referred me to Mayvenn"
                        :value     referred})
         (when referred
           (ui/text-field {:data-test "referrers-phone"
                           :errors    (:referrers-phone errors)
                           :id        "referrers-phone"
                           :keypath   keypaths/leads-ui-registration-referrers-phone
                           :focused   focused-keypath
                           :label     "Referrer's Mobile Phone Number *"
                           :name      "referrers-phone"
                           :type      "referrers-phone"
                           :value     referrers-phone}))
         [:div.pb3
          [:div.center.pb3 "Next."
           [:br]
           "We'll need some contact information."]
          (ui/text-field {:data-test "address1"
                          :errors    (:address1 errors)
                          :id        "address1"
                          :keypath   keypaths/leads-ui-registration-address1
                          :focused   focused-keypath
                          :label     "Street Address *"
                          :name      "address1"
                          :required  true
                          :type      "text"
                          :value     address1})
          (ui/text-field {:data-test "address2"
                          :errors    (:address2 errors)
                          :id        "address2"
                          :keypath   keypaths/leads-ui-registration-address2
                          :focused   focused-keypath
                          :label     "Apt or Suite #"
                          :name      "address2"
                          :required  true
                          :type      "text"
                          :value     address2})
          (ui/text-field {:data-test "city"
                          :errors    (:city errors)
                          :id        "city"
                          :keypath   keypaths/leads-ui-registration-city
                          :focused   focused-keypath
                          :label     "City *"
                          :name      "city"
                          :required  true
                          :type      "text"
                          :value     city})
          [:div
           [:div.col.col-6.pr1
            (ui/select-field {:data-test   "state"
                              :errors      (:state errors)
                              :id          :state
                              :keypath     keypaths/leads-ui-registration-state
                              :focused     focused-keypath
                              :label       "State *"
                              :options     states
                              :placeholder "State *"
                              :required    true
                              :value       "State *"})]
           [:div.col.col-6.pl1
            (ui/text-field {:data-test "zip"
                            :errors    (:zip errors)
                            :id        "zip"
                            :keypath   keypaths/leads-ui-registration-zip
                            :focused   focused-keypath
                            :label     "ZIP code *"
                            :name      "zip"
                            :required  true
                            :type      "text"
                            :value     zip})]]

          [:div.flex.flex-column.items-center.col-12
           (ui/text-field {:data-test "birth-date"
                           :errors    (:birthday errors)
                           :id        "birth-date"
                           :keypath   keypaths/leads-ui-registration-birthday
                           :focused   focused-keypath
                           :label     "Birthday"
                           :name      "birth-date"
                           :required  true
                           :type      "date"
                           :value     birthday})]]
         [:div.pb2.center
          [:div.pb2 "Are you a licensed hair stylist?"]
          [:label.mr1
           [:input {:type    "radio"
                    :name    "is-licensed"
                    :id      "is_licensed_yes"
                    :value   "yes"
                    :checked (= is-licensed "yes")}]
           " Yes"]
          [:label
           [:input
            {:type    "radio"
             :name    "is-licensed"
             :id      "is_licensed_no"
             :value   "no"
             :checked (= is-licensed "no")}]
           " No"]]
         [:div.pb3.center
          [:div.pb3
           [:div.pb2 "Our stylists get paid every week. "
            [:br]
            "How would you like to receive your commissions?"]
           [:label.mr1
            [:input {:type       "radio"
                     :name       "payout-method"
                     :id         "payout_method_venmo"
                     :value      "venmo"
                     :data-shows "#venmo_phone_group"
                     :data-hides ".payout-field"
                     :checked    (= payout-method "venmo")}]
            " Venmo"]
           [:label.mr1
            [:input {:type       "radio"
                     :name       "payout-method"
                     :id         "payout_method_paypal"
                     :value      "paypal"
                     :data-shows "#paypal_email_group"
                     :data-hides ".payout-field"
                     :checked    (= payout-method "paypal")}]
            " Paypal"]
           [:label
            [:input {:type       "radio"
                     :name       "payout-method"
                     :id         "payout_method_check"
                     :value      "check"
                     :data-hides ".payout-field"
                     :checked    (= payout-method "check")}]
            " Check"]]
          (ui/text-field {:data-test "venmo-phone"
                          :errors    (:venmo-phone errors)
                          :id        "venmo-phone"
                          :keypath   keypaths/leads-ui-registration-venmo-phone
                          :focused   focused-keypath
                          :label     "Venmo phone number *"
                          :name      "venmo-phone"
                          :required  true
                          :type      "tel"
                          :value     venmo-phone})

          (ui/text-field {:data-test "paypal-email"
                          :errors    (:paypal-email errors)
                          :id        "paypal-email"
                          :keypath   keypaths/leads-ui-registration-paypal-email
                          :focused   focused-keypath
                          :label     "Paypal email address *"
                          :name      "paypal-email"
                          :required  true
                          :type      "tel"
                          :value     paypal-email})]

         [:div.pb2.center
          [:div.pb2 "Almost done!"
           [:br]
           "What would you like to name your store?"]

          [:div.flex
           [:div.flex-auto
            (ui/text-field {:data-test     "slug"
                            :wrapper-class "rounded-left"
                            :errors        (:slug errors)
                            :id            "slug"
                            :keypath       keypaths/leads-ui-registration-slug
                            :focused       focused-keypath
                            :label         (if (empty? slug) "yourstorename" "Store Name")
                            :name          "slug"
                            :required      true
                            :type          "tel"
                            :value         slug})]
           [:div.rounded.rounded-right.border.border-gray.bg-light-gray.x-group-item.p2.floating-label-height
            ".mayvenn.com"]]]
         [:div.py3.border-top.border-gray
          (ui/submit-button "Submit registration" {:class "h2"})]]
        [:div.center.h5.bold.dark-gray
         [:a.inherit-color.text-decoration-none {:href "https://shop.mayvenn.com"} "Go back to shop Mayvenn Hair"]]]]])))

(defn ^:private query [data]
  {})

(defn built-component [data opts]
  (component/build component (query data) opts))
