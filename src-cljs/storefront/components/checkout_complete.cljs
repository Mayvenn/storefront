(ns storefront.components.checkout-complete
  (:require [adventure.stylist-matching.stylist-results :as stylist-results]
            [adventure.keypaths :as adv-keypaths]
            [adventure.components.profile-card :as profile-card]
            [storefront.components.svg :as svg]
            api.orders
            [spice.core :as spice]
            [storefront.assets :as assets]
            [storefront.transitions :as transitions]
            [storefront.events :as events]
            [storefront.effects :as effects]
            [storefront.component :as component]
            [storefront.components.checkout-returning-or-guest :as checkout-returning-or-guest]
            [storefront.components.formatters :as formatters]
            [storefront.components.facebook :as facebook]
            [storefront.components.sign-up :as sign-up]
            [storefront.components.ui :as ui]
            [storefront.accessors.stylists :as stylists]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            ui.molecules
            [clojure.string :as string]))

(defn copy [& sentences]
  (string/join " " sentences))

(defn component
  [{:keys [guest? sign-up-data]} _ _]
  (component/create
   (ui/narrow-container
    [:div.p3
     [:section.mx4-on-tb-dt.center
      (ui/ucare-img {:class "mx-auto"
                     :height "55px"
                     :width  "55px"} "a0520c74-b76d-4c4e-97f5-791c9e4ae213")
      [:h1
       {:data-test "checkout-success-message"}
       "Thank you for your order!"]
      [:p
       (copy "We've received your order and will be processing it right away."
             "Once your order ships we will send you an email confirmation.")]]
     (when guest?
       [:div.mt3
        [:section.center
         (ui/ucare-img {:class "mx-auto"
                        :height "55px"
                        :width  "55px"} "962e39be-3d22-4f23-a9b3-d1623ac7c1d7")
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

(defn servicing-stylist-card-title-molecule
  [{:stylist-card.title/keys [id primary secondary]}]
  (component/html
   [:div
    (when id
      (list
       [:div.h6.dark-gray.mbn1 {:key secondary} secondary]
       [:div.h3
        {:data-test id
         :key       primary}
        primary]))]))

(defn servicing-stylist-card-thumbnail-molecule
  "We want ucare-ids here but we do not have them"
  [{:stylist-card.thumbnail/keys [id ucare-id]}]
  (component/html
   (when id
     (ui/circle-picture
      {:width "72px"}
      (ui/square-image {:resizable-url ucare-id} 72)))))

(defn servicing-stylist-phone-molecule
  [{:phone-link/keys [phone-number]}]
  (when phone-number
    (ui/link :link/phone
             :a.dark-gray
             {:data-test "stylist-phone"
              :class     "block flex items-center h6"}
             (svg/phone {:style {:width  "9px"
                                 :height "16px"}
                         :class "mr1"})
             phone-number)))

(defn servicing-stylist-card-molecule
  [{:stylist-card/keys [id] :as data}]
  (when id
    [:div.flex.px6.py4.bg-white.top-lit.rounded
     [:div.flex.justify-center.items-center
      (servicing-stylist-card-thumbnail-molecule data)]
     [:div.medium.px2.flex-grow
      (servicing-stylist-card-title-molecule data)
      (ui.molecules/svg-star-rating-molecule data)
      (servicing-stylist-phone-molecule data)]]))

(defn matched-component-message-molecule
  [{:matched-component.message/keys [id title body]}]
  (when id
    [:div.center
     [:div.h5.medium title]
     [:div.h6.dark-gray.center.my2.px2
      body]]))

(def get-inspired-cta
  [:div.py2
   [:h3.bold "In the meantime…"]
   [:h4.py2 "Get inspired for your appointment"]
   [:div.py2
    (ui/teal-button {:href  "https://www.instagram.com/explore/tags/mayvennfreeinstall/"
                     :class "bold"}
                    "View #MayvennFreeInstall")]])

(defn matched-with-servicing-stylist-component [{:matched-component.message/keys [id] :as queried-data}]
  [:div.bg-too-light-lavender.rounded.px4.py3
   {:data-test id}
   (matched-component-message-molecule queried-data)
   [:div.my2 (servicing-stylist-card-molecule queried-data)]
   (let [{:matched-component.cta/keys [id label target]} queried-data]
     (when id
       [:div.col-10.my2.mx-auto
        (ui/teal-button
         (merge (apply utils/route-to target)
                {:data-test id}) label)]))])


(defn adventure-component
  [{:keys              [sign-up-data guest?] :as data
    matched-component? :matched-component.message/id} _ _]
  (component/create
   (ui/narrow-container
    [:div.p3 {:style {:min-height "95vh"}}
     [:div.center
      [:div.mt5.h1.mb2 {:data-test "checkout-success-message"}
       "Thank you for your order!"]
      [:div.h5.dark-gray.col-10.mx-auto
       (copy "We've received your order and will contact you as soon as your package is shipped.")]]

     [:div.py2.mx-auto.white.border-bottom
      {:style {:border-width "0.5px"}}]
     (if matched-component?
       (matched-with-servicing-stylist-component data)
       get-inspired-cta)
     (when guest?
       [:div.mt3
        [:section.center
         [:img {:src    "//ucarecdn.com/962e39be-3d22-4f23-a9b3-d1623ac7c1d7/-/format/auto/profile"
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


(defn query
  [data]

  (let [{install-applied?  :mayvenn-install/applied?
         dtc?              :order/dtc?
         servicing-stylist :mayvenn-install/stylist} (api.orders/completed data)
        show-match-component?                        (and install-applied? dtc?)
        need-match?                                  (and show-match-component?
                                                          (empty? servicing-stylist))
        customer-phone                               (-> data
                                                         (get-in keypaths/completed-order)
                                                         :shipping-address
                                                         :phone)
        matched-stylists                             (get-in data adv-keypaths/adventure-matched-stylists)
        match-via-web?                               (seq matched-stylists)]
    (cond-> {:guest?                (not (get-in data keypaths/user-id))
             :show-match-component? show-match-component?
             :need-match?           need-match?
             :matched-stylists      (get-in data adv-keypaths/adventure-matched-stylists)
             :sign-up-data          (sign-up/query data)
             :servicing-stylist     servicing-stylist
             :phone-number          customer-phone}

      (and show-match-component? (not need-match?))
      (merge
       (let [stylist-display-name (stylists/->display-name servicing-stylist)]
         {:matched-component.message/id    "servicing-stylist-name"
          :matched-component.message/title [:span.flex.items-center.justify-center
                                            (svg/chat-bubble {:height "14px"
                                                              :class  "mr1"
                                                              :width  "15px"})
                                            (str "Chat with " stylist-display-name)]
          :matched-component.message/body  [:span
                                            "A group text message has been sent to "
                                            (if customer-phone
                                              [:span.medium.black.nowrap (formatters/phone-number customer-phone)]
                                              "you")
                                            " and your stylist, "
                                            [:span.nowrap {:data-test "servicing-stylist-name"}
                                             stylist-display-name]]

          :stylist-card/id                 "stylist-card"
          :stylist-card.thumbnail/id       "portrait"
          :stylist-card.thumbnail/ucare-id (-> servicing-stylist :portrait :resizable-url)
          :rating/value                    (:rating servicing-stylist)
          :stylist-card.title/id           "stylist-name"
          :stylist-card.title/primary      stylist-display-name
          :stylist-card.title/secondary    (-> servicing-stylist :salon :name)
          :phone-link/phone-number         (some-> servicing-stylist :address :phone formatters/phone-number-parens)}))

      (and show-match-component? need-match?)
      (merge
       {:matched-component.message/id    (str "to-be-matched" (if match-via-web? "-via-web" "-via-phone"))
        :matched-component.message/title [:span.flex.items-center.justify-center
                                          (svg/chat-bubble {:height "14px"
                                                            :class  "mr1"
                                                            :width  "15px"})
                                          "Let's match you with a stylist!"]
        :matched-component.message/body  [:div.flex.justify-center.col-12
                                          (into [:ul.h6.list-img-purple-checkmark.left-align]
                                                (map (fn [txt] [:li.pl1.mb1 txt]))
                                                ["Licensed Salon Stylist"
                                                 "Mayvenn Certified"
                                                 "In your area"])]
        :matched-component.cta/id        (when match-via-web? "pick-a-stylist")
        :matched-component.cta/label     "Pick a Stylist"
        :matched-component.cta/target    [events/navigate-adventure-matching-stylist-wait-post-purchase]}))))

(defn ^:export built-component [data opts]
  (let [{:as queried-data :keys [show-match-component?]} (query data)]
    (if show-match-component?
      (component/build adventure-component queried-data opts)
      (component/build component queried-data opts))))
