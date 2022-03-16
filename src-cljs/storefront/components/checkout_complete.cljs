(ns storefront.components.checkout-complete
  (:require [storefront.components.svg :as svg]
            api.orders
            [storefront.accessors.experiments :as experiments]
            [storefront.events :as events]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.formatters :as formatters]
            [storefront.components.sign-up :as sign-up]
            [storefront.components.ui :as ui]
            [storefront.accessors.stylists :as stylists]
            [storefront.accessors.sites :as sites]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            ui.molecules
            [mayvenn.concept.booking :as booking]))

(defn ^:private divider []
  (component/html
   [:hr.border-top.border-gray.col-12.m0
    {:style {:border-bottom 0
             :border-left 0
             :border-right 0}}]))

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
     (let [{:matched-component.cta/keys [id label target]} queried-data]
       (when id
         [:div.col-10.my2.mx-auto
          (ui/button-large-primary
           (merge (apply utils/route-to target)
                  {:data-test id}) label)]))]))

(defcomponent guest-sign-up
  [{:guest-sign-up/keys [id sign-up-data]
    :as data} _ _]
  (when id
    [:section.center.mt3.col-11.mx-auto
     [:h1.canela.title-2.mt4 "Sign Up"]
     [:p.col-10.col-9-on-tb-dt.proxima.content-3.my3.mx-auto
      "Create a Mayvenn.com account below and enjoy faster checkout, order history, and more."]
     (sign-up/form sign-up-data {:sign-up-text "Create my account"
                                 :hide-email?  true})]))


(defcomponent component
  [{:thank-you/keys [primary secondary]
    :as data} _ _]
  (ui/narrow-container
   [:div.p3 {:style {:min-height "95vh"}}
    [:div.center
     [:div.mt5.mb2.canela.title-1 {:data-test "checkout-success-message"} "Thank You"]
     [:div.proxima.content-2 primary]
     (when secondary
       [:div.proxima.content-2.mt4.red secondary])]

    [:div.py2.mx-auto.white.border-bottom
     {:style {:border-width "0.5px"}}]
    (matched-with-servicing-stylist-component data)
    (component/build guest-sign-up data nil)]))

(defn shop-query [data]
  (let [{completed-waiter-order :waiter/order}                             (api.orders/completed data)
        {service-items :services/items}                                    (api.orders/services data completed-waiter-order)
        {:mayvenn.concept.booking/keys [selected-date selected-time-slot]} (booking/<- data)
        appointment-selected                                               (and selected-date selected-time-slot)
        easy-booking?                                                      (experiments/easy-booking? data)]
    (when (seq service-items)
      (merge
       {:thank-you/primary
        (str "We've received your order and a Mayvenn Concierge representative will contact you to "
             (if (and easy-booking? appointment-selected)
               "confirm your"
               "make an")
             " appointment within 3 business days.")}))))

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
           :guest-sign-up/sign-up-data (sign-up/query data)})))

      shop?
      (merge (shop-query data)))))

(defn ^:export built-component [data opts]
  (component/build component (query data) opts))
