(ns storefront.components.stylist.manage-account
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [clojure.string :refer [join capitalize]]
            [storefront.components.formatters :as f]
            [storefront.components.utils :as utils]
            [storefront.components.facebook :as facebook]
            [storefront.components.validation-errors :as validation-errors]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defn- field-id [field-path]
  (let [field-name (name (last field-path))
        model-name (name (first field-path))
        model-attributes (if (not= field-name model-name)
                           (str model-name "_attributes_") "")]
    (str "mayvenn_stylist_" model-attributes field-name)))

(defn- input-field
  [data owner field-path & {:keys [field-type label required placeholder]
                            :or {required false
                                 field-type "string"}}]
  (let [keypath          (into keypaths/stylist-manage-account field-path)
        field-name       (last field-path)
        field-class-name (str "mayvenn_stylist_" (join "_" (map name field-path)))
        field-type       (or field-type "string")
        required         (if required "required" "optional")
        css-classes      (fn [& classes]
                           (join " " (vec (concat [field-type required] classes))))]
    (html
     [:.input {:class (css-classes field-class-name)}
      (when-not placeholder
        [:label
         {:class (css-classes "control-label" "top-padded-input") :for (field-id field-path)}
         (list (if (= required "required") [:abbr {:title "required"} "*"])
               " "
               (or label (capitalize (name field-name))))])
      [:input
       (merge (utils/change-text data owner keypath)
              {:id (field-id field-path)
               :class (css-classes)
               :type (if (= "string" field-type) "text" field-type)
               :name field-name}
              (when placeholder {:placeholder placeholder}))]])))

(defn- state-options [data]
  (let [states (get-in data keypaths/states)]
    (map (fn [{:keys [id name]}]
           [:option {:key id :value id} name])
         states)))

(defn- year-options []
  (map (fn [year]
         [:option {:key year :value year} year])
       (range 1900 (inc (.getFullYear (js/Date.))))))

(def ^:private month-options
  (let [names [:January :February :March :April :May :June :July
               :August :September :October :November :December]]
    (map-indexed
     (fn [idx month]
       (let [month-name (name month)]
         [:option {:key month-name :value (inc idx)}
          month-name]))
     names)))

(def ^:private day-options
  (map (fn [day]
         [:option {:key day :value day} day])
       (range 1 (inc 31))))

(defn- payout-method-radio [data payout-method payout-label]
  (let [payout-method (name payout-method)
        keypath (conj keypaths/stylist-manage-account :chosen_payout_method)
        field-id (str "mayvenn_stylist_chosen_payout_method_" payout-method)]
    [:li
     [:input
      (merge {:id field-id
              :name "mayvenn_stylist[chosen_payout_method]"
              :type "radio"}
             (utils/change-radio data keypath payout-method))]
     [:label {:for field-id} payout-label]]))

(defn stylist-manage-account-component [data owner]
  (om/component
   (html
    [:div
     [:h2.header-bar-heading "Manage Account"]

     (om/build validation-errors/component (get-in data keypaths/validation-errors-details))

     [:div.manage-account-content
      [:.mx-auto
       [:form.edit_mayvenn_stylist.simple_form
        {:on-submit
         (utils/send-event-callback events/control-stylist-manage-account-submit)}
        [:.profile-info

         [:h4.manage-account-header.no-top-space "Profile Info"]
         [:.solid-line-divider]
         [:.profile-sections

          [:.profile-section

           [:.photo-container
            (if-let [profile-picture-url (get-in data
                                                 (conj keypaths/stylist-manage-account
                                                       :profile_picture_url))]
              [:.profile-photo
               {:style {:background-image (str "url(" profile-picture-url ")")}}]
              [:.empty-profile-picture])
            [:.change-photo-link
             [:span#filename "Change photo"]

             [:input.file-picker#mayvenn_stylist_profile_picture
              (merge (utils/change-file events/control-stylist-profile-picture)
                     {:name "mayvenn_stylist[profile_picture]" :type "file"})]]]

           (input-field data owner [:user :email] :field-type "email")

           [:.input.date.required.mayvenn_stylist_birth_date
            [:label.date.required.control-label.top-padded-input
             {:for "mayvenn_stylist_birth_date_1i"}
             [:abbr {:title "required"} "*"] " Birth Date"]
            [:select#mayvenn_stylist_birth_date_2i.date.required
             (utils/change-text data owner (conj keypaths/stylist-manage-account :birth_date_2i))
             month-options]
            " "
            [:select#mayvenn_stylist_birth_date_3i.date.required
             (utils/change-text data owner (conj keypaths/stylist-manage-account :birth_date_3i))
             day-options]
            " "
            [:select#mayvenn_stylist_birth_date_1i.date.required
             (utils/change-text data owner (conj keypaths/stylist-manage-account :birth_date_1i))
             (year-options)]]

           (input-field data owner [:address :firstname]
                        :label "First Name" :required true)
           (input-field data owner [:address :lastname]
                        :label "Last Name" :required true)]

          [:.profile-section.address

           (input-field data owner [:address :address1]
                        :label "Street Address"
                        :required true)
           (input-field data owner [:address :address2]
                        :label "Street Address 2 (Apartment Or Unit Number)"
                        :required true)
           (input-field data owner [:address :city]
                        :required true)

           [:.input.date.optional.mayvenn_stylist_address_state
            [:label.date.optional.control-label.top-padded-input
             {:for "mayvenn_stylist_address_attributes_state_id"} "State"]
            [:select#mayvenn_stylist_address_attributes_state_id
             (utils/change-text data owner (conj keypaths/stylist-manage-account :address :state_id))
             (state-options data)]]

           (input-field data owner [:address :zipcode])
           (input-field data owner [:address :phone]
                        :field-type "tel" :label "Mobile Phone Number")

           [:.password-reset
            [:.password-fields
             (input-field data owner [:user :password]
                          :field-type "password")
             (input-field data owner [:user :password-confirmation]
                          :field-type "password"
                          :label "Confirm Password")]
            [:p.password-instructions
             "Leave blank to keep the same password."]]]]]

        (let [chosen-keypath (conj keypaths/stylist-manage-account :chosen_payout_method)
              chosen-payout-method (get-in data chosen-keypath)
              original-keypath (conj keypaths/stylist-manage-account :original_payout_method)
              original-payout-method (get-in data original-keypath)]
          [:.select-payout-method
           [:h4.manage-account-header "Commissions"]
           [:.solid-line-divider]
           [:span#payout-method-radio
            [:ul.payout-methods
             (payout-method-radio data :venmo "Venmo")
             (payout-method-radio data :paypal "PayPal")
             (payout-method-radio data :check "Check")
             (when (= original-payout-method "mayvenn_debit")
               (payout-method-radio data :mayvenn_debit "Mayvenn Debit"))]]

           (condp = chosen-payout-method
             "venmo" [:#venmo.payout-method
                      (input-field data owner [:venmo_payout_attributes :phone]
                                   :label "Phone number on Venmo account"
                                   :field-type "tel"
                                   :required true)]

             "paypal" [:#paypal.payout-method
                       (input-field data owner [:paypal_payout_attributes :email]
                                    :label "Email On PayPal Account"
                                    :field-type "email"
                                    :required true)]
             "check" [:#check.payout-method
                      [:p "Checks will mail to the above address"]]
             "mayvenn_debit" [:#mayvenn_debit.payout-method
                              [:p "A prepaid Visa debit card will be mailed to the above address"]]

             nil [:#check.payout-method
                  [:p "Checks will mail to the above address"]])])

        [:.social-media
         [:h4.manage-account-header "Social Media"]
         [:.solid-line-divider]

         [:.social-media-container
          [:figure.instagram.social-media-icon]
          (input-field data owner [:instagram_account]
                       :placeholder "Enter your Instagram username")]

         [:.social-media-container
          [:figure.styleseat.social-media-icon]
          (input-field data owner [:styleseat_account]
                       :placeholder "Enter your StyleSeat username")]]

        [:input.big-button {:name "commit" :type "submit" :value "Update Account"}]]

       (when-let [available-credit (get-in data keypaths/user-total-available-store-credit)]
         [:#store-credit
          [:h4.manage-account-header "Store Credit"]
          [:.solid-line-divider]
          [:div.password-reset
           [:p.password-instructions
            "Available store credit is " (f/as-money available-credit)]]])

       [:div.my3
        (om/build facebook/messenger-business-opt-in-component (facebook/query data))]]]])))
