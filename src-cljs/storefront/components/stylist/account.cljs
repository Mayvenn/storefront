(ns storefront.components.stylist.account
  (:require [clojure.string :as string]
            [spice.maps :as maps]
            [storefront.component :as component]
            [storefront.components.stylist.account.payout :as account.payout]
            [storefront.components.stylist.account.password :as account.password]
            [storefront.components.stylist.account.profile :as account.profile]
            [storefront.components.stylist.account.social :as account.social]
            [storefront.components.tabs :as tabs]
            [storefront.components.ui :as ui]
            [storefront.accessors.credit-cards :as cc]
            [storefront.effects :as effects]
            [storefront.platform.messages :as messages]
            [storefront.transitions :as transitions :refer [transition-state]]
            [storefront.hooks.uploadcare :as uploadcare]
            [storefront.hooks.spreedly :as spreedly]
            [storefront.hooks.stringer :as stringer]
            [storefront.hooks.exception-handler :as exception-handler]
            [storefront.api :as api]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]
            [adventure.keypaths :as adv-keypaths]))

(defn uploadcare-photo [{:keys [status] :as portrait} saving?]
  [:a.navy
   (merge (utils/route-to events/navigate-stylist-account-portrait)
          {:data-test "change-photo-link"})
   (when saving?
     [:div.absolute
      (ui/large-spinner {:class "relative"
                         :style {:width "130px" :height "130px"
                                 :top   "-12px" :left   "-12px"}})])
   [:div.mx-auto.circle.border.mb2.content-box
    {:style {:width "100px" :height "100px" :border-width "3px"}
     :class (if saving? "border-light-gray" "border-teal")}
    [:div.circle.border-light-gray.border.content-box.border-width-2.overflow-hidden {:style {:width "96px" :height "96px"}}
     (ui/circle-picture {:width        "96px"
                         :overlay-copy (case status
                                         "pending" [:span.white.medium "Approval Pending"]
                                         "rejected" [:span.error.bold.h6 "Try a different image"]
                                         nil)}
                        (ui/square-image portrait 96))]]
   "Change Photo"])

(defn store-credit [available-credit]
  [:div.mb3
   [:div.medium.mb1 "Store Credit"]
   [:div.teal.h0 (when available-credit (ui/big-money available-credit))]
   [:div.mb1 ui/nbsp]])

(defn component [{:keys [fetching?
                         portrait-saving?
                         current-nav-event
                         portrait
                         available-credit
                         profile
                         password
                         commission
                         social
                         loaded-uploadcare?]} owner opts]
  (component/create
   [:div.bg-white.dark-gray
    [:div.container.p2.m-auto.overflow-hidden
     [:div.flex.justify-center.items-center.center
      [:div
       (when loaded-uploadcare?
         (uploadcare-photo portrait portrait-saving?))]

      [:div.ml3
       (store-credit available-credit)]]

     [:div.bg-light-gray.mt3.mxn2 ;; Oppose padding on page
      (component/build tabs/component {:selected-tab current-nav-event}
                       {:opts {:tab-refs ["profile" "password" "payout" "social"]
                               :labels   ["Profile" "Password" "Payout" "Social"]
                               :tabs     [events/navigate-stylist-account-profile
                                          events/navigate-stylist-account-password
                                          events/navigate-stylist-account-payout
                                          events/navigate-stylist-account-social]}})]

     (if fetching?
       [:div.my3.h2 ui/spinner]
       [:div.my3
        (condp = current-nav-event
          events/navigate-stylist-account-profile
          (component/build account.profile/component profile opts)

          events/navigate-stylist-account-password
          (component/build account.password/component password opts)

          events/navigate-stylist-account-payout
          (component/build account.payout/component commission opts)

          events/navigate-stylist-account-social
          (component/build account.social/component social opts)

          nil)])]]))

(defn query [data]
  {:fetching?           (utils/requesting? data request-keys/get-stylist-account)
   :portrait-saving?    (utils/requesting? data request-keys/update-stylist-account-portrait)
   :current-nav-event   (get-in data keypaths/navigation-event)
   :portrait            (get-in data (conj keypaths/stylist-manage-account :portrait))
   :available-credit    (get-in data keypaths/user-total-available-store-credit)
   :profile             (account.profile/query data)
   :password            (account.password/query data)
   :commission          (account.payout/query data)
   :social              (account.social/query data)
   :loaded-uploadcare?  (get-in data keypaths/loaded-uploadcare)})

(defn ^:export built-component [data opts]
  (component/build component (query data) opts))

;;;;;;;;;;;;;;;; not the best place to put, but it's at least out of main module ;;;;;;;;;;;;;;;;;;;;;

(defn clear-field-errors [app-state]
  (assoc-in app-state keypaths/errors {}))

(defmethod effects/perform-effects events/navigate-stylist-account [_ event args _ app-state]
  (let [user-token (get-in app-state keypaths/user-token)
        user-id    (get-in app-state keypaths/user-id)
        stylist-id (get-in app-state keypaths/user-store-id)]
    (when (and user-token stylist-id)
      (uploadcare/insert)
      (spreedly/insert)
      (api/get-states (get-in app-state keypaths/api-cache))
      (api/get-stylist-account user-id user-token stylist-id))))

(defmethod effects/perform-effects events/control-stylist-account-profile-submit [_ _ args _ app-state]
  (let [session-id      (get-in app-state keypaths/session-id)
        stylist-id      (get-in app-state keypaths/user-store-id)
        user-id         (get-in app-state keypaths/user-id)
        user-token      (get-in app-state keypaths/user-token)
        stylist-account (dissoc (get-in app-state keypaths/stylist-manage-account)
                                :green-dot-payout-attributes)]
    (api/update-stylist-account session-id user-id user-token stylist-id stylist-account
                                events/api-success-stylist-account-profile)))

(defmethod effects/perform-effects events/control-stylist-account-password-submit [_ _ args _ app-state]
  (let [session-id      (get-in app-state keypaths/session-id)
        stylist-id      (get-in app-state keypaths/user-store-id)
        user-id         (get-in app-state keypaths/user-id)
        user-token      (get-in app-state keypaths/user-token)
        stylist-account (dissoc (get-in app-state keypaths/stylist-manage-account)
                                :green-dot-payout-attributes)]
    (when (empty? (get-in app-state keypaths/errors))
      (api/update-stylist-account session-id user-id user-token stylist-id stylist-account
                                  events/api-success-stylist-account-password))))

(defn reformat-green-dot [greendot-attributes]
  (let [{:keys [expiration-date card-number] :as attributes}
        (select-keys greendot-attributes [:expiration-date
                                          :card-number
                                          :card-first-name
                                          :card-last-name
                                          :postalcode])]
    (when (seq card-number)
      (let [[month year] (cc/parse-expiration (str expiration-date))]
        (-> attributes
            (dissoc :expiration-date)
            (assoc :expiration-month month)
            (assoc :expiration-year year)
            (update :card-number (comp string/join cc/filter-cc-number-format str)))))))

(defmethod effects/perform-effects events/spreedly-frame-tokenized [_ _ {:keys [token payment]} _ app-state]
  (let [session-id      (get-in app-state keypaths/session-id)
        stylist-id      (get-in app-state keypaths/user-store-id)
        user-id         (get-in app-state keypaths/user-id)
        user-token      (get-in app-state keypaths/user-token)
        stylist-account (-> (get-in app-state keypaths/stylist-manage-account)
                            (assoc :green-dot-payout-attributes {:expiration-month (:month payment)
                                                                 :expiration-year  (:year payment)
                                                                 :card-token       token
                                                                 :card-first-name  (:first_name payment)
                                                                 :card-last-name   (:last_name payment)
                                                                 :postalcode       (:zip payment)})
                            maps/deep-remove-nils)]
    (api/update-stylist-account session-id user-id user-token stylist-id stylist-account
                                events/api-success-stylist-account-commission)))

(defmethod effects/perform-effects events/control-stylist-account-commission-submit [_ _ args _ app-state]
  (let [payout-method   (get-in app-state keypaths/stylist-manage-account-chosen-payout-method)
        session-id      (get-in app-state keypaths/session-id)
        stylist-id      (get-in app-state keypaths/user-store-id)
        user-id         (get-in app-state keypaths/user-id)
        user-token      (get-in app-state keypaths/user-token)
        stylist-account (-> (get-in app-state keypaths/stylist-manage-account)
                            (update :green-dot-payout-attributes reformat-green-dot)
                            maps/deep-remove-nils)]
    (if (= "green_dot" payout-method)
      (let [payout-attributes (get-in app-state (conj keypaths/stylist-manage-account :green-dot-payout-attributes))
            [month year]      (cc/parse-expiration (str (:expiration-date payout-attributes)))]
        (spreedly/tokenize (get-in app-state keypaths/spreedly-frame)
                           {:first-name (:card-first-name payout-attributes)
                            :last-name  (:card-last-name payout-attributes)
                            :exp-month  month
                            :exp-year   (cc/pad-year year)
                            :zip        (:postalcode payout-attributes)}))
      (api/update-stylist-account session-id user-id user-token stylist-id stylist-account
                                  events/api-success-stylist-account-commission))))

(defmethod effects/perform-effects events/control-stylist-account-social-submit [_ _ _ _ app-state]
  (let [session-id      (get-in app-state keypaths/session-id)
        stylist-id      (get-in app-state keypaths/user-store-id)
        user-id         (get-in app-state keypaths/user-id)
        user-token      (get-in app-state keypaths/user-token)
        stylist-account (dissoc (get-in app-state keypaths/stylist-manage-account)
                                :green-dot-payout-attributes)]
    (api/update-stylist-account session-id user-id user-token stylist-id stylist-account
                                events/api-success-stylist-account-social)))

(defmethod effects/perform-effects events/uploadcare-api-failure [_ _ {:keys [error error-data]} _ app-state]
  (exception-handler/report error error-data))

(defmethod effects/perform-effects events/navigate-stylist-dashboard [_ event args _ app-state]
  (let [user-token (get-in app-state keypaths/user-token)
        user-id    (get-in app-state keypaths/user-id)
        stylist-id (get-in app-state keypaths/user-store-id)]
    (when (and user-token stylist-id)
      (api/get-stylist-account user-id user-token stylist-id)
      (api/get-stylist-payout-stats
       events/api-success-stylist-payout-stats
       stylist-id user-id user-token))))

(defmethod transition-state events/control-stylist-account-commission-submit [_ event args app-state]
  (let [selected-id (get-in app-state keypaths/stylist-manage-account-green-dot-card-selected-id)
        last-4      (:last-4 (get-in app-state keypaths/stylist-manage-account-green-dot-payout-attributes))]
    (cond-> app-state
      (and (seq last-4) (= selected-id last-4))
      (assoc-in keypaths/stylist-manage-account-green-dot-payout-attributes {:last-4 last-4}))))

(defmethod transition-state events/control-stylist-account-password-submit [_ event args app-state]
  (let [stylist-account       (get-in app-state keypaths/stylist-manage-account)
        password              (-> stylist-account :user :password)
        field-errors          (cond-> {}
                                (> 6 (count password))
                                (merge (group-by :path [{:path ["user" "password"] :long-message "New password must be at least 6 characters"}])))]
    (if (seq field-errors)
      (assoc-in app-state keypaths/errors {:field-errors field-errors :error-code "invalid-input" :error-message "Oops! Please fix the errors below."})
      (clear-field-errors app-state))))

(defmethod transition-state events/api-success-stylist-account
  [_ event {:keys [stylist]} app-state]
  (let [stylist-zipcode (-> stylist :address :zipcode)]
    (-> app-state
        (update-in keypaths/stylist-manage-account merge stylist)
        (update-in (conj keypaths/stylist-manage-account-green-dot-payout-attributes :postalcode) #(or % stylist-zipcode)))))

(defmethod transition-state events/api-success-stylist-account-commission [_ event {:keys [stylist]} app-state]
  (let [green-dot-payout-attributes (some-> stylist :green-dot-payout-attributes (select-keys [:last-4 :payout-timeframe]))]
    (cond-> (update-in app-state keypaths/stylist-manage-account dissoc :green-dot-payout-attributes)
      (= "green_dot" (:chosen-payout-method stylist))
      (-> (assoc-in keypaths/stylist-manage-account-green-dot-card-selected-id (:last-4 green-dot-payout-attributes))
          (assoc-in keypaths/stylist-manage-account-green-dot-payout-attributes green-dot-payout-attributes)))))

(defmethod transition-state events/api-success-stylist-payout-stats
  [_ _ stats app-state]
  (assoc-in app-state keypaths/stylist-payout-stats stats))

(defmethod transition-state events/api-success-stylist-referral-program
  [_ event {:keys [sales-rep-email bonus-amount earning-amount lifetime-total referrals current-page pages]} app-state]
  (-> app-state
      (assoc-in keypaths/stylist-referral-program-bonus-amount bonus-amount)
      (assoc-in keypaths/stylist-referral-program-earning-amount earning-amount)
      (assoc-in keypaths/stylist-referral-program-lifetime-total lifetime-total)
      (update-in keypaths/stylist-referral-program-referrals into referrals)
      (assoc-in keypaths/stylist-referral-program-pages (or pages 0))
      (assoc-in keypaths/stylist-referral-program-page (or current-page 1))
      (assoc-in keypaths/stylist-sales-rep-email sales-rep-email)))
