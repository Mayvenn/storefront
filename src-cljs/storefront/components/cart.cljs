(ns storefront.components.cart
  (:require [storefront.components.utils :as utils]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.events :as events]
            [storefront.accessors.taxons :refer [default-taxon-path]]
            [clojure.string :as string]
            [storefront.components.order-summary :refer [display-order-summary display-line-items]]
            [storefront.request-keys :as request-keys]
            [storefront.keypaths :as keypaths]
            [storefront.util.query :as query]))

(defn shopping-link-attrs [data]
  (when-let [path (default-taxon-path data)]
    (utils/route-to data
                    events/navigate-category
                    {:taxon-path path})))

(defn cart-update-pending? [data]
  (let [request-key-prefix (comp vector first :request-key)]
    (some #(query/get % (get-in data keypaths/api-requests))
          [{:request-key request-keys/checkout-cart}
           {:request-key request-keys/update-coupon}
           {request-key-prefix request-keys/update-line-item}
           {request-key-prefix request-keys/delete-line-item}])))

(defn display-full-cart [data owner]
  (let [cart (get-in data keypaths/order)]
    [:div
     [:div#update-cart
      [:div.inside-cart-form
       [:div.cart-items
        [:div.cart-line-items
         (display-line-items data cart true)]
        [:div.cart-bottom
         [:form
          {:on-submit (utils/send-event-callback data events/control-cart-update-coupon)}
          [:div.coupon-cart
           [:h4 "Have a Coupon Code?"]
           [:div.coupon-container
            [:label "Enter a coupon code:"]
            [:input.coupon-code-input
             (merge
              (utils/change-text data owner keypaths/cart-coupon-code)
              {:type "text"
               :name "coupon-code"})]]
           [:div.primary.button#update-button
            (let [spinning (query/get {:request-key request-keys/update-coupon}
                                      (get-in data keypaths/api-requests))]
              {:type "submit"
               :name "update"
               :class (when spinning "saving")
               :disabled spinning
               :on-click (utils/send-event-callback data events/control-cart-update-coupon)})
            "Apply Coupon Code"]]]
         [:form
          {:on-submit (utils/send-event-callback data events/control-checkout-cart-submit)}
          [:div.order-summary-cart
           (display-order-summary cart)
           [:input.button.checkout.primary#checkout-link
            {:type "submit"
             :value "Checkout"
             :name "checkout"
             :disabled (when (cart-update-pending? data) "disabled")
             :on-click (utils/send-event-callback data events/control-checkout-cart-submit)}]]]]]]]
     [:a.cart-continue.continue.button.gray
      (shopping-link-attrs data)
      "Continue shopping"]]))

(defn display-empty-cart [data]
  [:div
   [:p.empty-cart-message "OH NO!"]
   [:figure.empty-bag]
   [:p
    [:a.button.primary.continue.empty-cart
     (shopping-link-attrs data)
     "Let's Fix That"]]])

(defn cart-component [data owner]
  (om/component
   (html
    [:div
     [:div.cart-container
      (let [cart (get-in data keypaths/order)]
        (if (:number cart)
          (when (:channel cart)
            (if (> (-> cart :line_items count) 0)
              (display-full-cart data owner)
              (display-empty-cart data)))
          (display-empty-cart data)))]
     [:div.home-actions-top
      [:div.guarantee]
      [:div.free-shipping-action]
      [:div.keep-shopping
       [:a.full-link (shopping-link-attrs data)]]]])))
