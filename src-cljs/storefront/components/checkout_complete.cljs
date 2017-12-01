(ns storefront.components.checkout-complete
  (:require [storefront.assets :as assets]
            [storefront.components.facebook :as facebook]
            [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.components.sign-up :as sign-up]
            [storefront.keypaths :as keypaths]
            [clojure.string :as string]
            [spice.date :as date]))

(defn copy [& sentences]
  (string/join " " sentences))

(defn component
  [{:keys [guest? sign-up-data]} _]
  (component/create
   (ui/narrow-container
    [:div.p3
     [:section.mx4-on-tb-dt.center
      [:img {:src (assets/path "/images/icons/success.png")
             :height "55px"
             :width "55px"}]
      [:h1
       {:data-test "checkout-success-message"}
       "Thank you for your order!"]
      [:p
       (copy "We've received your order and will be processing it right away."
             "Once your order ships we will send you an email confirmation.")]]
     (when guest?
       [:div.mt3
        [:section.center
         [:img {:src (assets/path "/images/icons/profile.png")
                :height "55px"
                :width "55px"}]
         [:h1 "Create an account"]
         [:p.h5 "Take advantage of express checkout, order tracking, and more when you sign up."]

         [:p.h5.py2 "Sign in with Facebook to link your account."]
         [:div.col-12.col-6-on-tb-dt.mx-auto
          (facebook/narrow-sign-in-button (:facebook-loaded? sign-up-data))]

         [:p.h5.py4 "Or create a Mayvenn.com account"]
         (sign-up/form sign-up-data {:sign-up-text "Create my account"})]])])))

(defn query [data]
  {:guest?        (not (get-in data keypaths/user-id))
   :sign-up-data  (sign-up/query data)})

(defn built-component [data opts]
  (component/build component (query data) opts))
