(ns storefront.components.flash
  (:require [clojure.string :as string]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.keypaths :as keypaths]))

(defn- field->human-name [key]
  (get {"billing-address"       "Billing Address"
        "shipping-address"      "Shipping Address"
        "address1"              "Street Address"
        "address2"              "Street Address (cont'd)"
        "first-name"            "First Name"
        "last-name"             "Last Name"
        "firstname"             "First Name"
        "lastname"              "Last Name"
        "reset_password_token"  "The reset password link"
        "password_confirmation" "Password confirmation"}
       key
       key))

(def success-img
  (component/html (svg/adjustable-check {:class "stroke-green" :width "1.25rem" :height "1.25rem"})))

(def error-img
  (component/html [:div.img-error-icon.bg-no-repeat.bg-contain {:style {:width "1.25rem" :height "1.25rem"}}]))

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

(defn component [{:keys [success failure validation-errors validation-message]} _ _]
  (component/create
   (when (or success failure validation-message (seq validation-errors))
     (ui/narrow-container
      (cond
        (seq validation-errors)
        (error-box
         {:data-test "flash-error"}
         [:ul.m0.ml1.px2
          (for [[field-index [field errors]] (map-indexed vector (sort-by first validation-errors))
                [error-index error]          (map-indexed vector errors)]
            (let [field-names (map field->human-name (string/split (name field) #"\."))
                  name        (string/capitalize (string/join " " field-names))]
              [:li {:key (str field-index "-" error-index)} name " " error]))])

        (or validation-message failure)
        (error-box
         {:data-test "flash-error"}
         [:ul.m0.ml1.px2 [:li (or validation-message failure)]])

        success
        (success-box
         {:data-test "flash-success"}
         [:ul.m0.ml1.px2 [:li success]]))))))

(defn query [data]
  {:success            (get-in data keypaths/flash-success-message)
   :failure            (get-in data keypaths/flash-failure-message)
   :validation-errors  (get-in data keypaths/validation-errors-details)
   :validation-message (get-in data keypaths/validation-errors-message)})
