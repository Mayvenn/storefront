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

(defn ^:private sign-up-section
  [{:keys [first-name last-name phone email password]} focused errors]
  [:div
   (ui/text-field-group {:type     "text"
                         :label    "First Name *"
                         :id       "first_name"
                         :name     "first-name"
                         :required true
                         :errors   (:first-name errors)
                         :keypath  keypaths/leads-ui-registration-first-name
                         :focused  focused
                         :value    first-name}
                        {:type     "text"
                         :label    "Last Name *"
                         :id       "last_name"
                         :name     "last-name"
                         :required true
                         :errors   (:last-name errors)
                         :keypath  keypaths/leads-ui-registration-last-name
                         :focused  focused
                         :value    last-name})
   (ui/text-field {:data-test "phone"
                   :errors    (:phone errors)
                   :id        "phone"
                   :keypath   keypaths/leads-ui-registration-phone
                   :focused   focused
                   :label     "Mobile Phone Number *"
                   :name      "phone"
                   :required  true
                   :type      "tel"
                   :value     phone})
   (ui/text-field {:data-test "email"
                   :errors    (:email errors)
                   :id        "email"
                   :keypath   keypaths/leads-ui-registration-email
                   :focused   focused
                   :label     "Email"
                   :name      "email"
                   :required  true
                   :type      "email"
                   :value     email})
   (ui/text-field {:data-test "password"
                   :errors    (:password errors)
                   :id        "password"
                   :keypath   keypaths/leads-ui-registration-password
                   :focused   focused
                   :label     "Password *"
                   :name      "password"
                   :required  true
                   :type      "password"
                   :value     password
                   :hint      password})])

(defn ^:private referral-section
  [{:keys [referred referrers-phone]} focused errors]
  [:div
   (ui/check-box {:data-test "referred"
                  :keypath   keypaths/leads-ui-registration-referred
                  :label     "Yes, somebody referred me to Mayvenn"
                  :value     referred})
   (when referred
     (ui/text-field {:data-test "referrers-phone"
                     :errors    (:referrers-phone errors)
                     :id        "referrers-phone"
                     :keypath   keypaths/leads-ui-registration-referrers-phone
                     :focused   focused
                     :label     "Referrer's Mobile Phone Number *"
                     :name      "referrers-phone"
                     :type      "referrers-phone"
                     :value     referrers-phone}))])

(defn ^:private contact-section
  [{:keys [address1 address2 city zip state]} states focused errors]
  [:div.pb3
   [:div.center.pb3 "Next."
    [:br]
    "We'll need some contact information."]
   (ui/text-field {:data-test "address1"
                   :errors    (:address1 errors)
                   :id        "address1"
                   :keypath   keypaths/leads-ui-registration-address1
                   :focused   focused
                   :label     "Street Address *"
                   :name      "address1"
                   :required  true
                   :type      "text"
                   :value     address1})
   (ui/text-field {:data-test "address2"
                   :errors    (:address2 errors)
                   :id        "address2"
                   :keypath   keypaths/leads-ui-registration-address2
                   :focused   focused
                   :label     "Apt or Suite #"
                   :name      "address2"
                   :required  true
                   :type      "text"
                   :value     address2})
   (ui/text-field {:data-test "city"
                   :errors    (:city errors)
                   :id        "city"
                   :keypath   keypaths/leads-ui-registration-city
                   :focused   focused
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
                       :focused     focused
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
                     :focused   focused
                     :label     "ZIP code *"
                     :name      "zip"
                     :required  true
                     :type      "text"
                     :value     zip})]]

   ])

(defn ^:private stylist-details-section
  [{:keys [birthday is-licensed payout-method venmo-phone paypal-email slug]} focused errors]
  [:div
   [:div.flex.flex-column.items-center.col-12
    (ui/text-field {:data-test "birth-date"
                    :errors    (:birthday errors)
                    :id        "birth-date"
                    :keypath   keypaths/leads-ui-registration-birthday
                    :focused   focused
                    :label     "Birthday"
                    :name      "birth-date"
                    :required  true
                    :type      "date"
                    :value     birthday})]
   [:div.pb2.center
    [:div.pb2 "Are you a licensed hair stylist?"]
    (ui/radio-group "licensed?"
                    is-licensed
                    keypaths/leads-ui-registration-is-licensed
                    [{:id "yes" :label "Yes"}
                     {:id "no" :label "No"}])]
   [:div.pb3.center
    [:div.pb3
     [:div.pb2 "Our stylists get paid every week. "
      [:br]
      "How would you like to receive your commissions?"]
     (ui/radio-group "payout-method"
                     payout-method
                     keypaths/leads-ui-registration-payout-method
                     [{:id "venmo" :label "Venmo"}
                      {:id "paypal" :label "Paypal"}
                      {:id "check" :label "Check"}])]
    (case payout-method
      "venmo"  (ui/text-field {:data-test "venmo-phone"
                               :errors    (:venmo-phone errors)
                               :id        "venmo-phone"
                               :keypath   keypaths/leads-ui-registration-venmo-phone
                               :focused   focused
                               :label     "Venmo phone number *"
                               :name      "venmo-phone"
                               :required  true
                               :type      "tel"
                               :value     venmo-phone})
      "paypal" (ui/text-field {:data-test "paypal-email"
                               :errors    (:paypal-email errors)
                               :id        "paypal-email"
                               :keypath   keypaths/leads-ui-registration-paypal-email
                               :focused   focused
                               :label     "Paypal email address *"
                               :name      "paypal-email"
                               :required  true
                               :type      "tel"
                               :value     paypal-email})
      [:span])]
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
                      :focused       focused
                      :label         (if (empty? slug) "yourstorename" "Store Name")
                      :name          "slug"
                      :required      true
                      :type          "tel"
                      :value         slug})]
     [:div.rounded.rounded-right.border.border-gray.bg-light-gray.x-group-item.p2.floating-label-height
      ".mayvenn.com"]]]] )

(defn ^:private component
  [{:keys [error errors focused anti-forgery-token sign-up referral contact stylist-details states]} owner opts]
  (component/create
   [:div.bg-teal.white
    [:div.max-580.mx-auto
     (when error
       [:p.bg-danger.registration-flash error])
     [:div.registration-header
      [:div.center.mb3
       [:img.my2 {:src (assets/path "/images/leads/logo-knockout.png")}]
       [:h1.mb3 "Join The Mayvenn Movement"]
       "Mayvenn empowers hairstylists to sell directly to their"
       " clients, build their brands, and grow their income. Apply"
       " today! We'll set you up with a FREE online store, plus:"]
      [:ul.ml1
       [:li "15% commission earned from each sale you make through your Mayvenn store"]
       [:li "Access to Mayvenn's exclusive community for hairstylists and beauty professionals"]
       [:li "Personalized marketing content provided every month to help you promote your business"]
       [:li "A personal sales rep for you, plus the industry’s best customer support for your clients"]]]
     [:div.bg-white.black.p4.my4
      [:div.center.pb2 "Ready to join the movement?"
       [:br]
       "Let's get started."]
      [:form {:action "" :method "POST" :role "form"}
       ;; TODO(FIXME)
       [:input {:type "hidden" :name "__anti-forgery-token" :value anti-forgery-token}]

       (sign-up-section sign-up focused errors)
       (referral-section referral focused errors)
       (contact-section contact states focused errors)
       (stylist-details-section stylist-details focused errors)

       [:div.py3.border-top.border-gray
        (ui/submit-button "Submit registration" {:class "h2"})]]
      [:div.center.h5.bold.dark-gray
       [:a.inherit-color.text-decoration-none {:href "https://shop.mayvenn.com"} "Go back to shop Mayvenn Hair"]]]]]))

(defn ^:private sign-up-query [data]
  {:first-name            (get-in data keypaths/leads-ui-registration-first-name)
   :last-name             (get-in data keypaths/leads-ui-registration-last-name)
   :phone                 (get-in data keypaths/leads-ui-registration-phone)
   :email                 (get-in data keypaths/leads-ui-registration-email)
   :password              (get-in data keypaths/leads-ui-registration-password)})

(defn ^:private contact-query [data]
  {:address1 (get-in data keypaths/leads-ui-registration-address1)
   :address2 (get-in data keypaths/leads-ui-registration-address2)
   :city     (get-in data keypaths/leads-ui-registration-city)
   :zip      (get-in data keypaths/leads-ui-registration-zip)
   :state    (or (get-in data keypaths/leads-ui-registration-state)
                 "")})

(defn ^:private stylist-details-query [data]
  {:birthday      (get-in data keypaths/leads-ui-registration-birthday)
   :is-licensed   (get-in data keypaths/leads-ui-registration-is-licensed)
   :payout-method (or (get-in data keypaths/leads-ui-registration-payout-method)
                      "venmo")
   :venmo-phone   (get-in data keypaths/leads-ui-registration-venmo-phone)
   :paypal-email  (get-in data keypaths/leads-ui-registration-paypal-email)
   :slug          (get-in data keypaths/leads-ui-registration-slug)})

(defn ^:private query [data]
  {:anti-forgery-token (get-in data keypaths/leads-ui-registration-first-name)
   :focused            (get-in data keypaths/ui-focus)
   :error              ""
   :errors             {}
   :states             (map (juxt :name :abbr) (get-in data keypaths/states))
   :referral           {:referred        (get-in data keypaths/leads-ui-registration-referred)
                        :referrers-phone (get-in data keypaths/leads-ui-registration-referrers-phone)}
   :sign-up            (sign-up-query data)
   :contact            (contact-query data)
   :stylist-details    (stylist-details-query data)})

(defn built-component [data opts]
  (component/build component (query data) opts))
