(ns leads.registration
  (:require #?@(:clj [[storefront.component-shim :as component]]
                :cljs [[storefront.component :as component]
                       [storefront.api :as api]
                       [storefront.history :as history]])
            [storefront.components.ui :as ui]
            [storefront.assets :as assets]
            [storefront.platform.carousel :as carousel]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.effects :as effects]
            [storefront.transitions :as transitions]
            [storefront.platform.component-utils :as utils]
            [clojure.string :as string]
            [storefront.effects :as effects]
            [storefront.platform.messages :as messages]))

(defn ^:private sign-up-section
  [{:keys [first-name last-name phone email password field-errors]} focused]
  [:div
   (ui/text-field-group {:type     "text"
                         :label    "First Name *"
                         :id       "first_name"
                         :name     "first-name"
                         :required true
                         :errors   (get field-errors ["first-name"])
                         :keypath  keypaths/leads-ui-sign-up-first-name
                         :focused  focused
                         :value    first-name}
                        {:type     "text"
                         :label    "Last Name *"
                         :id       "last_name"
                         :name     "last-name"
                         :required true
                         :errors   (get field-errors ["last-name"])
                         :keypath  keypaths/leads-ui-sign-up-last-name
                         :focused  focused
                         :value    last-name})
   (ui/text-field {:data-test "phone"
                   :errors    (get field-errors ["phone"])
                   :id        "phone"
                   :keypath   keypaths/leads-ui-sign-up-phone
                   :focused   focused
                   :label     "Mobile Phone Number *"
                   :name      "phone"
                   :required  true
                   :type      "tel"
                   :value     phone})
   (ui/text-field {:data-test "email"
                   :errors    (get field-errors ["email"])
                   :id        "email"
                   :keypath   keypaths/leads-ui-sign-up-email
                   :focused   focused
                   :label     "Email"
                   :name      "email"
                   :required  true
                   :type      "email"
                   :value     email})
   (ui/text-field {:data-test "password"
                   :errors    (get field-errors ["password"])
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
  [{:keys [referred referrers-phone field-errors]} focused]
  [:div
   (ui/check-box {:data-test "referred"
                  :keypath   keypaths/leads-ui-registration-referred
                  :label     "Yes, somebody referred me to Mayvenn"
                  :value     referred})
   (when referred
     (ui/text-field {:data-test "referrers-phone"
                     :errors    (get field-errors ["referrers-phone"])
                     :id        "referrers-phone"
                     :keypath   keypaths/leads-ui-registration-referrers-phone
                     :focused   focused
                     :label     "Referrer's Mobile Phone Number *"
                     :name      "referrers-phone"
                     :type      "referrers-phone"
                     :value     referrers-phone}))])

(defn ^:private contact-section
  [{:keys [address1 address2 city zip state field-errors]} states focused]
  [:div.pb3
   [:div.center.pb3 "Next."
    [:br]
    "We'll need some contact information."]
   (ui/text-field {:data-test "address1"
                   :errors    (get field-errors ["address1"])
                   :id        "address1"
                   :keypath   keypaths/leads-ui-registration-address1
                   :focused   focused
                   :label     "Street Address *"
                   :name      "address1"
                   :required  true
                   :type      "text"
                   :value     address1})
   (ui/text-field {:data-test "address2"
                   :errors    (get field-errors ["address2"])
                   :id        "address2"
                   :keypath   keypaths/leads-ui-registration-address2
                   :focused   focused
                   :label     "Apt or Suite #"
                   :name      "address2"
                   :required  false
                   :type      "text"
                   :value     address2})
   (ui/text-field {:data-test "city"
                   :errors    (get field-errors ["city"])
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
                       :errors      (get field-errors ["state"])
                       :id          :state
                       :keypath     keypaths/leads-ui-registration-state
                       :focused     focused
                       :label       "State *"
                       :options     states
                       :placeholder "State *"
                       :required    true
                       :value       state})]
    [:div.col.col-6.pl1
     (ui/text-field {:data-test "zip"
                     :errors    (get field-errors ["zip"])
                     :id        "zip"
                     :keypath   keypaths/leads-ui-registration-zip
                     :focused   focused
                     :label     "ZIP code *"
                     :name      "zip"
                     :required  true
                     :type      "text"
                     :value     zip})]]])

(defn ^:private stylist-details-section
  [{:keys [birthday licensed? payout-method venmo-phone paypal-email slug field-errors]} focused]
  [:div
   [:div.flex.flex-column.items-center.col-12
    (ui/text-field {:data-test "birth-date"
                    :errors    (get field-errors ["birthday"])
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
    (ui/radio-group {:group-name    "licensed?"
                     :checked-value licensed?
                     :keypath       keypaths/leads-ui-registration-licensed?}
                    [{:id "yes" :label "Yes" :value true}
                     {:id "no" :label "No" :value false}])]
   [:div.pb3.center
    [:div.pb3
     [:div.pb2 "Our stylists get paid every week. "
      [:br]
      "How would you like to receive your commissions?"]
     (ui/radio-group {:group-name    "payout-method"
                      :keypath       keypaths/leads-ui-registration-payout-method
                      :checked-value payout-method
                      :required      true}
                     [{:id "venmo" :label "Venmo" :value "venmo"}
                      {:id "paypal" :label "Paypal" :value "paypal"}
                      {:id "check" :label "Check" :value "check"}])]
    (case payout-method
      "venmo"  (ui/text-field {:data-test "venmo-phone"
                               :errors    (get field-errors ["venmo-phone"])
                               :id        "venmo-phone"
                               :keypath   keypaths/leads-ui-registration-venmo-phone
                               :focused   focused
                               :label     "Venmo phone number *"
                               :name      "venmo-phone"
                               :required  true
                               :type      "tel"
                               :value     venmo-phone})
      "paypal" (ui/text-field {:data-test "paypal-email"
                               :errors    (get field-errors ["paypal-email"])
                               :id        "paypal-email"
                               :keypath   keypaths/leads-ui-registration-paypal-email
                               :focused   focused
                               :label     "Paypal email address *"
                               :name      "paypal-email"
                               :required  true
                               :type      "email"
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
                      :errors        (get field-errors ["slug"])
                      :id            "slug"
                      :keypath       keypaths/leads-ui-registration-slug
                      :focused       focused
                      :label         (if (empty? slug) "yourstorename" "Store Name")
                      :name          "slug"
                      :required      true
                      :type          "text"
                      :value         slug})]
     [:div.rounded.rounded-right.border.border-gray.bg-light-gray.x-group-item.p2.floating-label-height
      ".mayvenn.com"]]]] )

(defn ^:private component
  [{:keys [error focused sign-up referral contact stylist-details states]} owner opts]
  (component/create
   [:div.bg-teal.white.pb4
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
      [:form {:action    ""
              :method    "POST"
              :role      "form"
              :on-submit (utils/send-event-callback events/leads-control-self-registration-submit {})}
       (sign-up-section sign-up focused)
       (referral-section referral focused)
       (contact-section contact states focused)
       (stylist-details-section stylist-details focused)

       [:div.py3.border-top.border-gray
        (ui/submit-button "Submit registration" {:class "h2"})]]
      [:div.center.h5.bold.dark-gray
       [:a.inherit-color.text-decoration-none {:href "https://shop.mayvenn.com"} "Go back to shop Mayvenn Hair"]]]]]))

(defn ^:private sign-up-query [data]
  {:first-name (get-in data keypaths/leads-ui-sign-up-first-name)
   :last-name  (get-in data keypaths/leads-ui-sign-up-last-name)
   :phone      (get-in data keypaths/leads-ui-sign-up-phone)
   :email      (get-in data keypaths/leads-ui-sign-up-email)
   :password   (get-in data keypaths/leads-ui-registration-password)})

(defn ^:private contact-query [data]
  {:address1 (get-in data keypaths/leads-ui-registration-address1)
   :address2 (get-in data keypaths/leads-ui-registration-address2)
   :city     (get-in data keypaths/leads-ui-registration-city)
   :zip      (get-in data keypaths/leads-ui-registration-zip)
   :state    (get-in data keypaths/leads-ui-registration-state)})

(defn ^:private stylist-details-query [data]
  {:birthday      (get-in data keypaths/leads-ui-registration-birthday)
   :licensed?     (get-in data keypaths/leads-ui-registration-licensed?)
   :payout-method (get-in data keypaths/leads-ui-registration-payout-method)
   :venmo-phone   (get-in data keypaths/leads-ui-registration-venmo-phone)
   :paypal-email  (get-in data keypaths/leads-ui-registration-paypal-email)
   :slug          (get-in data keypaths/leads-ui-registration-slug)})

(defn ^:private query [data]
  (let [field-errors (get-in data keypaths/field-errors)]
    {:focused         (get-in data keypaths/ui-focus)
     :error           ""
     :states          (map (juxt :name :abbr) (get-in data keypaths/states))
     :referral        {:referred        (get-in data keypaths/leads-ui-registration-referred)
                       :referrers-phone (get-in data keypaths/leads-ui-registration-referrers-phone)
                       :field-errors    field-errors}
     :sign-up         (merge (sign-up-query data)
                             {:field-errors field-errors})
     :contact         (merge (contact-query data)
                             {:field-errors field-errors})
     :stylist-details (merge (stylist-details-query data)
                             {:field-errors field-errors})}))

(defn built-component [data opts]
  (component/build component (query data) opts))

(defn handle-referral [{:keys [referred] :as registration}]
  (if referred
    registration
    (-> registration
        (assoc :referred false)
        (dissoc :reffers-phone))))

#?(:cljs
   (defmethod effects/perform-effects events/navigate-leads-registration-details
     [_ _ _ _ app-state]
     (api/get-states (get-in app-state keypaths/api-cache))))

(defmethod effects/perform-effects events/leads-control-self-registration-submit
  [dispatch event args _ app-state]
  #?(:cljs
     (let [{:keys [id step-id] :as lead} (get-in app-state keypaths/leads-lead)
           sign-up                       (get-in app-state keypaths/leads-ui-sign-up)
           registration                  (-> app-state
                                             (get-in keypaths/leads-ui-registration)
                                             (merge (select-keys sign-up [:first-name :last-name :email :phone]))
                                             handle-referral)]
       (api/advance-lead-registration {:lead-id    id
                                       :step-id    step-id
                                       :session-id (get-in app-state keypaths/session-id)
                                       :step-data  {:registration registration}}
                                      (fn [registered-lead]
                                        (js/console.log (clj->js registered-lead))
                                        (messages/handle-message events/api-success-lead-registered {:registered-lead registered-lead}))))))

(defmethod transitions/transition-state events/api-success-lead-registered
  [_ event {:keys [registered-lead]} app-state]
  (assoc-in app-state keypaths/leads-lead registered-lead))

#?(:cljs
   (defmethod effects/perform-effects events/api-success-lead-registered
     [_ event {:keys [registered-lead]} _ app-state]
     (history/enqueue-navigate events/navigate-leads-registration-resolve)))
