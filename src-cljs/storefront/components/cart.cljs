(ns storefront.components.cart
  (:require [storefront.components.utils :as utils]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.events :as events]
            [storefront.taxons :refer [default-taxon-path]]
            [clojure.string :as string]
            [storefront.components.order-summary :refer [display-order-summary]]
            [storefront.state :as state]))

(defn display-variant-options [option-value]
  [:div.line-item-attr
   [:span.cart-label (str (:option_type_presentation option-value) ": ")]
   [:span.cart-value (:presentation option-value)]])

(defn display-line-item [line-item]
  [:div.line-item
   [:img {:src "/assets/noimage/FIXME.png"}]
   [:div.line-item-detail.interactive
    (let [variant (:variant line-item)]
      [:div
       [:h4 [:a (:name variant)]]
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
       [:a.delete {:href "#" :FIXME "on-click"} "Remove"]])]
   [:div {:style {:clear "both"}}]])

(defn cart-component [data owner]
  (om/component
   (html
    [:div.cart-container
     (when-let [cart (get-in data state/order-path)]
       (if (> (-> cart :line_items count) 0)
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
           {:href "#" :FIXME "on-click"}
           "Continue shopping"]]
         [:div
          [:p.empty-cart-message "OH NO!"]
          [:figure.empty-bag]
          [:p
           [:a.button.primary.continue.empty-cart
            (when-let [path (default-taxon-path data)]
              (utils/route-to data
                              events/navigate-category
                              {:taxon-path path}))
            "Let's Fix That"]]]))])))
