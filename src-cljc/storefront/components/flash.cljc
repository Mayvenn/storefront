(ns storefront.components.flash
  (:require [clojure.string :as string]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.keypaths :as keypaths]))

(defn- field-component->human-name [key]
  (get {"billing-address"       "Billing Address"
        "shipping-address"      "Shipping Address"
        "address1"              "Street Address"
        "address2"              "Street Address (cont'd)"
        "first-name"            "First Name"
        "last-name"             "Last Name"
        "firstname"             "First Name"
        "lastname"              "Last Name"
        "reset_password_token"  "The reset password link"
        "password_confirmation" "Password confirmation"
        "payout_method"         "Payout method"}
       key
       key))

(defn- field->human-name [field]
  (->> (string/split (name field) #"\.")
       (map field-component->human-name)
       (string/join " ")
       string/capitalize))

(def flash-line-height "1.25em")

(def success-img
  (component/html [:div (svg/adjustable-check {:class "stroke-green align-middle" :width flash-line-height :height flash-line-height})]))

(def error-img
  (component/html [:div.img-error-icon.bg-no-repeat.bg-contain {:style {:width flash-line-height :height flash-line-height}}]))

(defn success-box [box-opts body]
  [:div.green.bg-green.border.border-green.rounded.light.letter-spacing-1
   [:div.clearfix.px2.py1.bg-lighten-5.rounded box-opts
    [:div.right.ml1 success-img]
    [:div.overflow-hidden body]]])

(defn error-box [box-opts body]
  [:div.orange.bg-orange.border.border-orange.rounded.light.letter-spacing-1
   [:div.clearfix.px2.py1.bg-lighten-5.rounded box-opts
    [:div.right.ml1 error-img]
    [:div.overflow-hidden body]]])

(defn component [{:keys [success failure validation-errors validation-message errors]} _ _]
  (component/create
   (when (or success failure validation-message (seq validation-errors) (seq errors))
     (ui/narrow-container
      (cond
        (seq validation-errors)
        (error-box
         {:data-test "flash-error"}
         (if (and (= 1 (count validation-errors))
                  (= 1 (count (second (first validation-errors)))))
           (let [[field [error]] (first validation-errors)]
             [:div.px2 (field->human-name field) " " error])
           [:ul.m0.ml1.px2
            (for [[field-index [field errors]] (map-indexed vector (sort-by first validation-errors))
                  [error-index error]          (map-indexed vector errors)]
              [:li {:key (str field-index "-" error-index)} (field->human-name field) " " error])]))

        (or validation-message failure)
        (error-box
         {:data-test "flash-error"}
         [:div.px2 {:style {:line-height flash-line-height}}
          (or validation-message failure)])

        (seq errors)
        (error-box
         {:data-test "flash-error"}
         [:div.px2 {:style {:line-height flash-line-height}}
          "Oops! Please fix the errors below."])

        success
        (success-box
         {:data-test "flash-success"}
         [:div.px2 {:style {:line-height flash-line-height}}
          success]))))))

(defn query [data]
  {:success            (get-in data keypaths/flash-success-message)
   :failure            (get-in data keypaths/flash-failure-message)
   :errors             (get-in data keypaths/errors)
   :validation-errors  (get-in data keypaths/validation-errors-details)
   :validation-message (get-in data keypaths/validation-errors-message)})
