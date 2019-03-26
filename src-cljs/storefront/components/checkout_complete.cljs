(ns storefront.components.checkout-complete
  (:require [adventure.stylist-results :as stylist-results]
            [adventure.keypaths :as adv-keypaths]
            [storefront.assets :as assets]
            [storefront.transitions :as transitions]
            [storefront.events :as events]
            [storefront.effects :as effects]
            [storefront.api :as api]
            [storefront.component :as component]
            [storefront.components.formatters :as formatters]
            [storefront.components.facebook :as facebook]
            [storefront.components.sign-up :as sign-up]
            [storefront.components.ui :as ui]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.accessors.experiments :as experiments]
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

(defn servicing-stylist-component
  [phone-number servicing-stylist]
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
    (stylist-card servicing-stylist)]])

(defn need-match-via-web-component
  [matched-stylists]
  [:div {:data-test "to-be-matched"}
   [:div.py4.h3.bold
    "Let's match you with a Certified Mayvenn Stylist!"]
   [:div.h5.line-height-3
    "If you don’t love the install, we’ll pay for you to get it taken down and redone. It’s a win-win!"]
   [:div
    [:ul.col-10.h6.list-img-purple-checkmark.py4.left-align.mx6
     (mapv (fn [txt] [:li.pl1.mb1 txt])
           ["Licensed Salon Stylist"
            "Mayvenn Certified"
            "In your area"])]]
   (ui/teal-button {} "Pick a stylist")])

(def need-match-via-phone-component
  [:div {:data-test "to-be-matched"}
   [:div.py4.h3.bold
    "Let's match you with a Certified Mayvenn Stylist!"]
   [:div.h5.line-height-3
    (copy "A Mayvenn representative will contact you soon to help select a"
          "Certified Mayvenn Stylist with the following criteria:")]
   [:div
    [:ul.col-10.h6.list-img-purple-checkmark.py4.left-align.mx6
     (mapv (fn [txt] [:li.pl1.mb1 txt])
           ["Licensed Salon Stylist"
            "Mayvenn Certified"
            "In your area"])]]])

(def get-inspired-cta
  [:div.py2
   [:h3.bold "In the meantime…"]
   [:h4.py2 "Get inspired for your appointment"]
   [:div.py2
    (ui/teal-button {:href  "https://www.instagram.com/explore/tags/mayvennfreeinstall/"
                     :class "bold"}
                    "View #MayvennFreeInstall")]])

(defn adventure-component
  [{:keys [servicing-stylist matched-stylists phone-number match-post-purchase? need-match?]} _ _]
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
       (cond servicing-stylist                                             (servicing-stylist-component phone-number servicing-stylist)
             (and need-match? match-post-purchase? (seq matched-stylists)) (need-match-via-web-component matched-stylists)
             need-match?                                                   need-match-via-phone-component
             :else                                                         get-inspired-cta)]])]))

(defmethod effects/perform-effects events/api-fetch-geocode
  [_ event _ _ app-state]
  (let [choices                                        (get-in app-state adventure.keypaths/adventure-choices)
        {:keys [address1 address2 city state zipcode]} (:shipping-address (get-in app-state keypaths/completed-order))
        params                                         (clj->js {"address" (string/join " " [address1 address2 (str city ",") state zipcode])
                                                                 "region"  "US"})]
    (. (js/google.maps.Geocoder.)
       (geocode params
                (fn [results status]
                  (let [location (some-> results
                                         (js->clj :keywordize-keys true)
                                         first
                                         :geometry
                                         :location
                                         .toJSON
                                         (js->clj :keywordize-keys true))]
                    (if location
                      (api/fetch-stylists-within-radius (get-in app-state keypaths/api-cache)
                                                        {:latitude     (:lat location)
                                                         :longitude    (:lng location)
                                                         :radius       "10mi"
                                                         :install-type (:install-type choices)
                                                         :choices      choices}
                                                        #(handle-message events/api-success-fetch-stylists-within-radius-post-purchase %))
                      events/api-failure-fetch-geocode)))))))

(defmethod transitions/transition-state events/api-success-fetch-matched-stylist
  [_ event {:keys [stylist]} app-state]
  (assoc-in app-state
            adv-keypaths/adventure-servicing-stylist stylist))

(defn query
  [data]
  (let [freeinstall?      (= "freeinstall" (get-in data keypaths/store-slug))
        servicing-stylist (get-in data adv-keypaths/adventure-servicing-stylist)]
    {:guest?               (not (get-in data keypaths/user-id))
     :freeinstall?         freeinstall?
     :match-post-purchase? (experiments/adv-match-post-purchase? data)
     :need-match?          (and freeinstall?
                                (empty? servicing-stylist))
     :matched-stylists     (get-in data adv-keypaths/adventure-matched-stylists)
     :sign-up-data         (sign-up/query data)
     :servicing-stylist    servicing-stylist
     :phone-number         (-> data
                               (get-in keypaths/completed-order)
                               :shipping-address
                               :phone)}))

(defn built-component
  [data opts]
  (let [{:as queried-data :keys [freeinstall?]} (query data)]
    (if freeinstall?
      (component/build adventure-component queried-data opts)
      (component/build component queried-data opts))))
