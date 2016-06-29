(ns storefront.components.flash
  (:require [clojure.string :as string]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.components.ui :as ui]
            [storefront.keypaths :as keypaths]))

(defn- field->human-name [key]
  (get {"billing-address" "Billing Address"
        "shipping-address" "Shipping Address"
        "address1" "Street Address"
        "address2" "Street Address (cont'd)"
        "first-name" "First Name"
        "last-name" "Last Name"
        "firstname" "First Name"
        "lastname" "Last Name"
        "reset_password_token" "The reset password link"}
       key
       key))

(defn error-box [box-opts body]
  [:div.orange.bg-orange.border.border-orange.rounded.light.letter-spacing-1
   [:div.px2.py1.bg-lighten-5.rounded box-opts
    [:div.img-error-icon.bg-no-repeat.bg-contain.right
     {:style {:width "1.25rem" :height "1.25rem"}}]
    body]])

(defn component [{:keys [success failure validation-errors validation-message]} _ _]
  (component/create
   [:div
    (cond
      (seq validation-errors)
      (ui/narrow-container
       (error-box
        {:data-test "flash-error"}
        [:ul.m0.ml1.px2
         (for [[field-index [field errors]] (map-indexed vector (sort-by first validation-errors))
               [error-index error] (map-indexed vector errors)]
           (let [field-names (map field->human-name (string/split (name field) #"\."))
                 name (string/capitalize (string/join " " field-names))]
             [:li.mr3 {:key (str field-index "-" error-index)} (str name " " error)]))]))

      (or validation-message failure)
      (ui/narrow-container
       (error-box
        {:data-test "flash-error"}
        [:ul.m0.ml1.px2
         [:li.mr3 (or validation-message failure)]]))

      ;; TODO: ensure error messages are cleared when success flash is assoced-in
      success [:div.flash.success {:data-test "flash-success"} success])]))

(defn query [data]
  {:success            (get-in data keypaths/flash-success-message)
   :failure            (get-in data keypaths/flash-failure-message)
   :validation-errors  (get-in data keypaths/validation-errors-details)
   :validation-message (get-in data keypaths/validation-errors-message)})
