(ns storefront.components.cart
  (:require [storefront.components.utils :as utils]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.events :as events]
            [storefront.taxons :refer [default-taxon-path]]
            [clojure.string :as string]
            [storefront.components.order-summary :refer [display-order-summary]]
            [storefront.state :as state]))

(defn shopping-link-attrs [data]
  (when-let [path (default-taxon-path data)]
    (utils/route-to data
                    events/navigate-category
                    {:taxon-path path})))

(defn display-variant-options [option-value]
  [:div.line-item-attr
   [:span.cart-label (str (:option_type_presentation option-value) ": ")]
   [:span.cart-value (:presentation option-value)]])

(defn display-line-item [line-item]
  (let [variant (:variant line-item)]
    [:div.line-item
     [:img {:src (-> variant :images first :small_url)}]
     [:div.line-item-detail.interactive
      [:h4 [:a (variant :name)]]
      (map display-variant-options (:option_values variant))
      [:div.line-item-attr.item-form.price
       [:span.cart-label "Price:"]
       [:span.cart-value (:single_display_amount line-item)]]
      [:div.line-item-attr.item-form.subtotal
       [:span.cart-label "Subtotal:"]
       [:span.cart-value (:display_amount line-item)]]
      [:div.quantity
       [:div.quantity-selector
        [:div.minus [:a.pm-link {:href "#" :FIXME "on-click"} "-"]]
        [:input.quantity-selector-input.line_item_quantity
         {:type "text" :min 1 :FIXME "state" :value (:quantity line-item)}]
        [:div.plus [:a.pm-link {:href "#" :FIXME "on-click"} "+"]]]]
      [:a.delete {:href "#" :FIXME "on-click"} "Remove"]]
     [:div {:style {:clear "both"}}]]))

(defn display-full-cart [data]
  (let [cart (get-in data state/order-path)]
    [:div
     [:form#update-cart
      [:div.inside-cart-form
       [:div.cart-items
        [:div.cart-line-items
         (map display-line-item (:line_items cart))]
        [:div.coupon-cart
         [:h4 "Have a Coupon Code?"]
         [:div.coupon-container
          [:label "Enter a coupon code:"]
          [:input.coupon-code-input {:type "text" :name "coupon-code"}]]
         [:input.primary.button#update-button {:type "submit" :name "update" :value "Update"}]]
        [:div.order-summary-cart
         (display-order-summary cart)
         [:input.button.checkout.primary#checkout-link
          {:type "submit" :value "Checkout" :name "checkout"}]]]]]
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
      (if (get-in data state/user-order-id-path)
        (when-let [cart (get-in data state/order-path)]
          (if (> (-> cart :line_items count) 0)
            (display-full-cart data)
            (display-empty-cart data)))
        (display-empty-cart data))]
     [:div.home-actions-top
      [:div.guarantee]
      [:div.free-shipping-action]
      [:div.keep-shopping
       [:a.full-link (shopping-link-attrs data)]]]])))
