(ns storefront.components.checkout-complete
  (:require [adventure.stylist-matching.stylist-results :as stylist-results]
            [adventure.keypaths :as adv-keypaths]
            [adventure.components.profile-card :as profile-card]
            [adventure.components.card-stack :as card-stack]
            [storefront.assets :as assets]
            [storefront.transitions :as transitions]
            [storefront.events :as events]
            [storefront.effects :as effects]
            [storefront.api :as api]
            [storefront.component :as component]
            [storefront.components.checkout-returning-or-guest :as checkout-returning-or-guest]
            [storefront.components.formatters :as formatters]
            [storefront.components.facebook :as facebook]
            [storefront.components.sign-up :as sign-up]
            [storefront.components.ui :as ui]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.stylists :as stylists]
            [storefront.accessors.orders :as orders]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]
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

(defn servicing-stylist-component
  [phone-number servicing-stylist]
  [:div {:data-test "matched-with-stylist"}
   [:div.py4.h3.bold
    "Chat with your Stylist"]
   [:div.h5.line-height-3.center
    "After your order ships, a group SMS will be sent to "
    (if phone-number
      [:span.bold.nowrap (formatters/phone-number phone-number)]
      "you")
    " and your stylist, "
    [:span.nowrap {:data-test "servicing-stylist-name"}
     (stylists/->display-name servicing-stylist)]
    ", to make an appointment."]
   [:div.bg-white.px1.my4.mxn2.rounded.p3
    (component/build profile-card/component (profile-card/stylist-profile-card-data servicing-stylist) nil)]])

(defn need-match
  [matched-stylists]
  (let [match-via-web? (seq matched-stylists)]
    [:div
     [:div.py4.h3.bold
      "Let's match you with a Certified Mayvenn Stylist!"]
     [:div.h5.line-height-3
      {:data-test (str "to-be-matched" (if match-via-web? "-via-web" "-via-phone"))}
      (if match-via-web?
        "If you don’t love the install, we’ll pay for you to get it taken down and redone. It’s a win-win!"
        (copy "A Mayvenn representative will contact you soon to help select a"
              "Certified Mayvenn Stylist with the following criteria:"))]
     [:div
      [:ul.col-10.h6.list-img-purple-checkmark.py4.left-align.mx6
       (mapv (fn [txt] [:li.pl1.mb1 txt])
             ["Licensed Salon Stylist"
              "Mayvenn Certified"
              "In your area"])]]
     (when match-via-web?
       (ui/teal-button (merge (utils/route-to events/navigate-adventure-matching-stylist-wait-post-purchase)
                              {:data-test "pick-a-stylist"}) "Pick a stylist"))]))

(def get-inspired-cta
  [:div.py2
   [:h3.bold "In the meantime…"]
   [:h4.py2 "Get inspired for your appointment"]
   [:div.py2
    (ui/teal-button {:href  "https://www.instagram.com/explore/tags/mayvennfreeinstall/"
                     :class "bold"}
                    "View #MayvennFreeInstall")]])

(defn adventure-component
  [{:keys [servicing-stylist matched-stylists phone-number need-match?]} _ _]
  (component/create
   [:div.bg-lavender.white {:style {:min-height "95vh"}}
    (ui/narrow-container
     [:div.center
      [:div.col-11.mx-auto.py4
       [:div
        [:div.h5.medium.py1
         {:data-test "checkout-success-message"}
         "Thank you for your order!"]
        [:div.h5.line-height-3
         (copy "We've received your order and will be processing it right away."
               "Once your order ships we will send you an email confirmation.")]]

       [:div.py2.mx-auto.white.border-bottom
        {:style {:border-width "0.5px"}}]
       (cond servicing-stylist (servicing-stylist-component phone-number servicing-stylist)
             need-match?       (need-match matched-stylists)
             :else             get-inspired-cta)]])]))

(defn query
  [data]
  (let [{install-applied?  :mayvenn-install/applied?
         dtc?              :order/dtc?
         servicing-stylist :mayvenn-install/stylist} (api.orders/completed data)
        show-match-component?                        (and install-applied? dtc?)]
    {:guest?                (not (get-in data keypaths/user-id))
     :show-match-component? show-match-component?
     :need-match?           (and show-match-component?
                                 (empty? servicing-stylist))
     :matched-stylists      (get-in data adv-keypaths/adventure-matched-stylists)
     :sign-up-data          (sign-up/query data)
     :servicing-stylist     servicing-stylist
     :phone-number          (-> data
                                (get-in keypaths/completed-order)
                                :shipping-address
                                :phone)}))

(defn ^:export built-component [data opts]
  (let [{:as queried-data :keys [show-match-component?]} (query data)]
    (if show-match-component?
      (component/build adventure-component queried-data opts)
      (component/build component queried-data opts))))
