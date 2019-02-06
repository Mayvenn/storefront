(ns storefront.components.checkout-complete
  (:require [adventure.stylist-results :as stylist-results]
            [adventure.keypaths :as adv-keypaths]
            [storefront.assets :as assets]
            [storefront.transitions :as transitions]
            [storefront.events :as events]
            [storefront.component :as component]
            [storefront.components.formatters :as formatters]
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

(defn stylist-card [servicing-stylist]
  (let [firstname (-> servicing-stylist
                      :address
                      :firstname)
        lastname  (-> servicing-stylist
                      :address
                      :lastname)
        city      (-> servicing-stylist
                      :salon
                      :city)
        state     (-> servicing-stylist
                      :salon
                      :state)
        rating    (:rating servicing-stylist)
        portrait  (-> servicing-stylist
                      :portrait
                      :resizable-url)
        name      (-> servicing-stylist
                      :salon
                      :name)]
    [:div.flex
     [:div.mr2 (ui/circle-picture {:width "104px"} portrait)]
     [:div.flex-grow-1.left-align.dark-gray.h7.line-height-4
      [:div.h3.black.line-height-1 (clojure.string/join  " " [firstname lastname])]
      [:div.pyp2 (ui/star-rating rating)]
      [:div.bold (str city ", " state)]
      [:div name]
      (stylist-results/stylist-detail-line servicing-stylist)]]))

(defn adventure-component
  [{:keys [stylist-store servicing-stylist phone-number]} _ _]
  (component/create
   [:div.bg-lavender.white {:style {:min-height "95vh"}}
    (ui/narrow-container
     [:div.center
      [:div.col-11.mx-auto.py4
       [:div
        [:div.h5.medium.py1
         "Thank you for your order!"]
        [:div.h5.line-height-3
         (copy "We've received your order and will be processing it right away."
               "Once your order ships we will send you an email confirmation.")]]

       [:div.py2.mx-auto.white.border-bottom
        {:style {:border-width "0.5px"}}]

       (if servicing-stylist
         [:div {:data-test "matched-with-stylist"}
          [:div.py4.h3.bold
           "Chat with your Stylist"]
          [:div.h5.line-height-3.center
           "A group text message will be sent to "
           [:span.bold.nowrap (formatters/phone-number phone-number)]
           " and your stylist, "
           [:span.nowrap {:data-test "servicing-stylist-firstname"}
            (-> servicing-stylist
                :address
                :firstname)]
           "."]
          [:div.bg-white.px1.my4.mxn2.rounded.py3
           (stylist-card servicing-stylist)]]

         [:div {:data-test "to-be-matched"}
          [:div.py4.h3.bold
           "Let's match you with a Certified Mayvenn Stylist!"]
          [:div.h5.line-height-3
           (copy "A Mayvenn representative will contact you soon to help select a"
                 "Certified Mayvenn Stylist with the following criteria:")]
          [:div
           [:ul.col-10.h6.list-img-purple-checkmark.py4.left-align.mx6
            (mapv (fn [%] [:li.pl1.mb1 %])
                  ["Licensed Salon Stylist"
                   "Mayvenn Certified"
                   "In your area"])]]])

       [:div.py2
        [:h3.bold "In the meantime…"]
        [:h4.py2 "Get inspired for your appointment"]
        [:div.py2
         (ui/teal-button {:href  "https://www.instagram.com/explore/tags/mayvennfreeinstall/"
                          :class "bold"}
                         "View #MayvennFreeInstall")]]]])]))

(defmethod transitions/transition-state events/api-success-fetch-matched-stylist
  [_ event {:keys [stylist]} app-state]
  (assoc-in app-state
            adv-keypaths/adventure-servicing-stylist stylist))

(defn query
  [data]
  {:guest?            (not (get-in data keypaths/user-id))
   :freeinstall?        (= "freeinstall" (get-in data keypaths/store-slug))
   :sign-up-data      (sign-up/query data)
   :stylist-store     (get-in data keypaths/store)
   :servicing-stylist (get-in data adv-keypaths/adventure-servicing-stylist)
   :phone-number      (-> data
                          (get-in keypaths/completed-order)
                          :shipping-address
                          :phone)})

(defn built-component
  [data opts]
  (let [{:as queried-data :keys [freeinstall?]} (query data)]
    (if freeinstall?
      (component/build adventure-component queried-data opts)
      (component/build component queried-data opts))))
