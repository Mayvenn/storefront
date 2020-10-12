(ns storefront.components.checkout-complete
  (:require [storefront.components.svg :as svg]
            api.orders
            [storefront.events :as events]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.formatters :as formatters]
            [storefront.components.sign-up :as sign-up]
            [storefront.components.ui :as ui]
            [storefront.accessors.stylists :as stylists]
            [storefront.accessors.sites :as sites]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            ui.molecules))

(defn ^:private divider []
  (component/html
   [:hr.border-top.border-gray.col-12.m0
    {:style {:border-bottom 0
             :border-left 0
             :border-right 0}}]))

(defn servicing-stylist-card-title-molecule
  [{:stylist-card.title/keys [id primary secondary]}]
  (component/html
   [:div
    (when id
      (list
       [:div.content-2.mbn1 {:key secondary} secondary]
       [:div.proxima.title-2.shout
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
              :class     "block flex items-center content-2"}
             phone-number)))

(defn servicing-stylist-card-molecule
  [{:stylist-card/keys [id] :as data}]
  (when id
    [:div.flex.px6.py4.bg-white.top-lit
     [:div.flex.justify-center.items-center
      (servicing-stylist-card-thumbnail-molecule data)]
     [:div.px2.flex-grow
      (servicing-stylist-card-title-molecule data)
      [:span.proxima.title-2
       (ui.molecules/svg-star-rating-molecule data)]
      (servicing-stylist-phone-molecule data)]]))

(defn matched-component-message-molecule
  [{:matched-component.message/keys [id title body]}]
  (when id
    [:div.center
     [:div.shout.proxima.title-2.flex.items-center.justify-center
      (svg/chat-bubble {:class "mr1 fill-p-color" :height "18px" :width "19px"})
      title]
     [:div.content-2.center.my2 body]]))

(defn matched-with-servicing-stylist-component [{:matched-component.message/keys [id] :as queried-data}]
  (when id
    [:div.bg-white.pt8.pb4.px3.bg-refresh-gray
     {:data-test id}
     (matched-component-message-molecule queried-data)
     [:div.my2 (servicing-stylist-card-molecule queried-data)]
     (let [{:matched-component.cta/keys [id label target]} queried-data]
       (when id
         [:div.col-10.my2.mx-auto
          (ui/button-large-primary
           (merge (apply utils/route-to target)
                  {:data-test id}) label)]))]))

(defcomponent facebook-cta [{:facebook-cta/keys [id]} _ _]
  (when id
    (ui/button-medium-facebook-blue
     {:on-click  (utils/send-event-callback events/control-facebook-sign-in)
      :data-test id}
     [:div.mx-auto
      (svg/button-facebook-f {:width "7px" :height "14px"})
      [:span.ml2 "Facebook Sign Up"]])))

(defcomponent guest-sign-up
  [{:guest-sign-up/keys [id sign-up-data]
    :as data} _ _]
  (when id
    [:section.center.mt3.col-11.mx-auto
     [:h1.canela.title-2.mt4
      "Sign Up"]
     [:p.col-10.col-9-on-tb-dt.proxima.content-3.mt3.mx-auto
      "Take advantage of express checkout, order tracking, and more when you sign up."]

     [:div.col-10.col-9-on-tb-dt.mx-auto.mt6
      (component/build facebook-cta data nil)]

     [:div.mx-auto.col-10.col-9-on-tb-dt.pb2.flex.items-center.justify-between.mt3
      ^:inline (divider)
      [:span.proxima.content-3.px2 "or"]
      ^:inline (divider)]

     [:p.canela.title-3.mt2.mb3 "Create an Account"]
     (sign-up/form sign-up-data {:sign-up-text "Create my account"})]))


(defcomponent component
  [{:thank-you/keys [primary]
    :as data} _ _]
  (ui/narrow-container
   [:div.p3 {:style {:min-height "95vh"}}
    [:div.center
     [:div.mt5.mb2.canela.title-1 {:data-test "checkout-success-message"} "Thank You"]
     [:div.proxima.content-2 primary]]

    [:div.py2.mx-auto.white.border-bottom
     {:style {:border-width "0.5px"}}]
    (matched-with-servicing-stylist-component data)
    (component/build guest-sign-up data nil)]))

(defn shop-query [data]
  (let [{completed-waiter-order :waiter/order
         services-only?         :order/services-only?
         customer-phone         :order.shipping/phone} (api.orders/completed data)
        {servicing-stylist    :services/stylist
         service-items        :services/items}         (api.orders/services data completed-waiter-order)]
    (when (seq service-items)
      (merge
       (when services-only?
         {:thank-you/primary "We've received your order and will contact you to make an appointment within 2 business days."} )
       (when-let [stylist-display-name (some-> servicing-stylist not-empty stylists/->display-name)]
         {:matched-component.message/id    "servicing-stylist-name"
          :matched-component.message/title (str "Chat with " stylist-display-name)
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
          :phone-link/phone-number         (some-> servicing-stylist :address :phone formatters/phone-number-parens)})))))

(defn query
  [data]
  (let [shop?  (= :shop (sites/determine-site data))
        guest? (not (get-in data keypaths/user-id))]
    (cond->
        {:thank-you/primary "We've received your order and will contact you as soon as your package is shipped."}

      guest?
      (merge
       (let [sign-up-data (sign-up/query data)]
         (merge
          {:guest-sign-up/id           "guest-sign-up"
           :guest-sign-up/sign-up-data (sign-up/query data)}
          (when (:facebook-loaded? sign-up-data)
            {:facebook-cta/id "facebook-button"}))))

      shop?
      (merge (shop-query data)))))

(defn ^:export built-component [data opts]
  (component/build component (query data) opts))
