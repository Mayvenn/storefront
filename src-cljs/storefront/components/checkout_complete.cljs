(ns storefront.components.checkout-complete
  (:require [adventure.keypaths :as adv-keypaths]
            [storefront.components.svg :as svg]
            api.orders
            [storefront.accessors.experiments :as experiments]
            [storefront.events :as events]
            [storefront.component :as component :refer [defcomponent]]
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

(defn ^:private divider []
  (component/html
   [:hr.border-top.border-gray.col-12.m0
    {:style {:border-bottom 0
             :border-left 0
             :border-right 0}}]))

(defn guest-sign-up [sign-up-data]
  [:section.center.mt3.col-11.mx-auto
   [:h1.canela.title-2.mt4
    "Sign Up"]
   [:p.col-10.col-9-on-tb-dt.proxima.content-3.mt3.mx-auto
    "Take advantage of express checkout, order tracking, and more when you sign up."]

   [:div.col-10.col-9-on-tb-dt.mx-auto.mt6
    (when (:facebook-loaded? sign-up-data)
      (ui/button-medium-facebook-blue
       {:on-click  (utils/send-event-callback events/control-facebook-sign-in)
        :data-test "facebook-button"}
       [:div.mx-auto
        (svg/button-facebook-f {:width "7px" :height "14px"})
        [:span.ml2 "Facebook Sign Up"]]))]

   [:div.mx-auto.col-10.col-9-on-tb-dt.pb2.flex.items-center.justify-between.mt3
    ^:inline (divider)
    [:span.proxima.content-3.px2 "or"]
    ^:inline (divider)]

   [:p.canela.title-3.mt2.mb3
    "Create an Account"]
   (sign-up/form sign-up-data
                 {:sign-up-text "Create my account"})])

(defcomponent component
  [{:keys [guest? sign-up-data]} _ _]
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
      (guest-sign-up sign-up-data))]))

(defn servicing-stylist-card-title-molecule
  [{:stylist-card.title/keys [id primary secondary]}]
  (component/html
   [:div
    (when id
      (list
       [:div.content-3.mbn1 {:key secondary} secondary]
       [:div.content-1
        {:data-test id
         :key       primary}
        primary]))]))

(defn servicing-stylist-card-thumbnail-molecule
  "We want ucare-ids here but we do not have them"
  [{:stylist-card.thumbnail/keys [id ucare-id]}]
  (component/html
   [:div
    (when id
      (ui/circle-picture
       {:width "72px"}
       (ui/square-image {:resizable-url ucare-id} 72)))]))

(defn servicing-stylist-phone-molecule
  [{:phone-link/keys [phone-number]}]
  (when phone-number
    (ui/link :link/phone
             :a.inherit-color
             {:data-test "stylist-phone"
              :class     "block flex items-center content-3"}
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
     [:div.px2.flex-grow
      (servicing-stylist-card-title-molecule data)
      (ui.molecules/svg-star-rating-molecule data)
      (servicing-stylist-phone-molecule data)]]))

(defn matched-component-message-molecule
  [{:matched-component.message/keys [id title body]}]
  (when id
    [:div.center.proxima
     [:div.content-2 title]
     [:div.content-3.center.my2.px2
      body]]))

(def get-inspired-cta
  [:div.py2
   [:h3.bold "In the meantime…"]
   [:h4.py2 "Get inspired for your appointment"]
   [:div.py2
    (ui/button-large-primary {:href  "https://www.instagram.com/explore/tags/mayvennfreeinstall/"
                              :class "bold"}
                             "View #MayvennFreeInstall")]])

(defn matched-with-servicing-stylist-component [{:matched-component.message/keys [id] :as queried-data}]
  [:div.bg-white.rounded.px4.py3
   {:data-test id}
   (matched-component-message-molecule queried-data)
   [:div.my2 (servicing-stylist-card-molecule queried-data)]
   (let [{:matched-component.cta/keys [id label target]} queried-data]
     (when id
       [:div.col-10.my2.mx-auto
        (ui/button-large-primary
         (merge (apply utils/route-to target)
                {:data-test id}) label)]))])

(defcomponent adventure-component
  [{:keys              [sign-up-data guest?] :as data
    matched-component? :matched-component.message/id} _ _]
  (ui/narrow-container
   [:div.p3 {:style {:min-height "95vh"}}
    [:div.center
     [:div.mt5.mb2.canela.title-2 {:data-test "checkout-success-message"}
      "Thank you for your order!"]
     [:div.col-10.mx-auto.proxima.content-2
      (copy "We've received your order and will contact you as soon as your package is shipped.")]]

    [:div.py2.mx-auto.white.border-bottom
     {:style {:border-width "0.5px"}}]
    (if matched-component?
      (matched-with-servicing-stylist-component data)
      get-inspired-cta)
    (when guest?
      (guest-sign-up sign-up-data))]))

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
                                              [:span.nowrap (formatters/phone-number customer-phone)]
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
        :matched-component.message/body  [:div.flex.col-12.justify-center.content-3
                                          [:div.left-align
                                           (for [label ["Licensed Salon Stylist"
                                                        "Mayvenn Certified"
                                                        "In your area"]] 
                                             [:div.mb1
                                              (svg/check-mark {:class "black"
                                                               :style {:width  10
                                                                       :height 10}})
                                              [:span.pl2 label]])]]
        :matched-component.cta/id        (when match-via-web? "pick-a-stylist")
        :matched-component.cta/label     "Pick a Stylist"
        :matched-component.cta/target    [events/navigate-adventure-stylist-results-post-purchase]}))))

(defn ^:export built-component [data opts]
  (let [{:as queried-data :keys [show-match-component?]} (query data)]
    (if show-match-component?
      (component/build adventure-component queried-data opts)
      (component/build component queried-data opts))))
