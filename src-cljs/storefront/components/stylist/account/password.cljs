(ns storefront.components.stylist.account.password
  (:require [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]
            [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.components.facebook-messenger :as facebook]))

(defn component [{:keys [password
                         confirmation]} owner opts]
  (component/create
   [:form {:on-submit
           (utils/send-event-callback events/control-stylist-manage-account-submit)}
    [:div.flex.flex-column.items-center.col-12
     [:h1.h2.light.col-12.my3.center "Update your password"]

     (ui/text-field "New Password"
                    (conj keypaths/stylist-manage-account :user :password)
                    password
                    {:type      "password"
                     :name      "account-password"
                     :id        "account-password"
                     :data-test "account-password"
                     :required  true})

     (ui/text-field "Re-type New Password"
                    (conj keypaths/stylist-manage-account :user :password-confirmation)
                    confirmation
                    {:type      "password"
                     :name      "account-password-confirmation"
                     :id        "account-password-confirmation"
                     :data-test "account-password-confirmation"
                     :required  true})

     [:div.my2.col-12
      (ui/submit-button "Update" {:spinning? false
                                  :data-test "account-form-submit"})]]]))

(defn query [data]
  {:password     (get-in data (conj keypaths/stylist-manage-account :user :password))
   :confirmation (get-in data (conj keypaths/stylist-manage-account :user :password-confirmation))})
