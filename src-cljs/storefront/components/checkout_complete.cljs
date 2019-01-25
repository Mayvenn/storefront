(ns storefront.components.checkout-complete
  (:require [storefront.assets :as assets]
            [storefront.component :as component]
            [storefront.components.facebook :as facebook]
            [storefront.components.sign-up :as sign-up]
            [storefront.components.ui :as ui]
            [storefront.accessors.experiments :as experiments]
            [storefront.keypaths :as keypaths]
            [clojure.string :as string]))

(defn copy [& sentences]
  (string/join " " sentences))

(defn component
  [{:keys [guest? sign-up-data]} _ _]
  (component/create
   (ui/narrow-container
    [:div.p3
     [:section.mx4-on-tb-dt.center
      [:img {:src    (assets/path "/images/icons/success.png")
             :height "55px"
             :width  "55px"}]
      [:h1
       {:data-test "checkout-success-message"}
       "Thank you for your order!"]
      [:p
       (copy "We've received your order and will be processing it right away."
             "Once your order ships we will send you an email confirmation.")]]
     (when guest?
       [:div.mt3
        [:section.center
         [:img {:src    (assets/path "/images/icons/profile.png")
                :height "55px"
                :width  "55px"}]
         [:h1
          "Create an account"]
         [:p.h5
          "Take advantage of express checkout, order tracking, and more when you sign up."]

         [:p.h5.py2
          "Sign in with Facebook to link your account."]
         [:div.col-12.col-6-on-tb-dt.mx-auto
          (facebook/narrow-sign-in-button (:facebook-loaded? sign-up-data))]

         [:p.h5.py4
          "Or create a Mayvenn.com account"]
         (sign-up/form sign-up-data
                       {:sign-up-text "Create my account"})]])])))

(defn adventure-component
  [_ _ _]
  (component/create
   [:div.bg-lavender.white {:style {:min-height "95vh"}}
    (ui/narrow-container
     [:div.center
      [:div.col-10.mx-auto.py4
       [:div
        [:h2.h2.bold.py2
         "Thank you for your order!"]
        [:div.h2.line-height-2
         "We've received your order and will be processing it right away."
         "Once your order ships we will send you an email confirmation."]]

       [:div.py2.mx-auto.white.border-bottom.border-width-1]

       [:div
        [:h1.py4.h1.bold
         "Let's match you with a Certified Mayvenn Stylist!"]
        [:div.h2.line-height-2
         "A Mayvenn representative will contact you soon to help select a "
         "Certified Mayvenn Stylist with the following criteria:"]
        [:div
         [:ul.col-10.h3.purple-checkmark.py4.left-align.mx6
          (mapv (fn [%] [:li.pl1.mb1 %])
                ["Licensed Salon Stylist"
                 "Mayvenn Certified"
                 "In your area"])]]]

       [:div.pt6.pb2
        [:h1.bold "In the meantime…"]
        [:h2.py2 "Get inspired for your appointment"]
        [:div.py2
         (ui/teal-button {:href "https://www.instagram.com/explore/tags/mayvennfreeinstall/"}
                         "View #MayvennFreeInstall")]]]])]))

(defn query
  [data]
  {:guest?       (not (get-in data keypaths/user-id))
   :adventure?   (experiments/adventure? data)
   :sign-up-data (sign-up/query data)})

(defn built-component
  [data opts]
  (let [{:as queried-data :keys [adventure?]} (query data)]
    (if adventure?
      (component/build adventure-component queried-data opts)
      (component/build component queried-data opts))))
