(ns storefront.components.cart
  (:require [storefront.components.utils :as utils]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.events :as events]
            [storefront.taxons :refer [default-taxon-path]]
            [clojure.string :as string]
            [storefront.components.order-summary :refer [display-order-summary]]
            [storefront.components.counter :refer [counter-component]]
            [storefront.keypaths :as keypaths]))

(defn shopping-link-attrs [data]
  (when-let [path (default-taxon-path data)]
    (utils/route-to data
                    events/navigate-category
                    {:taxon-path path})))

(defn display-variant-options [option-value]
  [:div.line-item-attr
   [:span.cart-label (str (:option_type_presentation option-value) ": ")]
   [:span.cart-value (:presentation option-value)]])

(defn display-line-item [data line-item]
  (let [variant (:variant line-item)]
    [:div.line-item
     [:a
      (utils/route-to data events/navigate-product {:product-path (:slug variant)})
      [:img {:src (-> variant :images first :small_url)}]]
     [:div.line-item-detail.interactive
      [:h4
       [:a
        (utils/route-to data events/navigate-product {:product-path (:slug variant)})
        (variant :name)]]
      (map display-variant-options (:option_values variant))
      [:div.line-item-attr.item-form.price
       [:span.cart-label "Price:"]
       [:span.cart-value (:single_display_amount line-item)]]
      [:div.line-item-attr.item-form.subtotal
       [:span.cart-label "Subtotal:"]
       [:span.cart-value (:display_amount line-item)]]
      (om/build counter-component
                data
                {:opts {:path (conj keypaths/cart-quantities (:id line-item))}})
      [:a.delete
       {:href "#"
        :on-click (utils/enqueue-event data
                                       events/control-cart-remove
                                       (select-keys line-item [:id]))}
       "Remove"]]
     [:div {:style {:clear "both"}}]]))

(defn display-full-cart [data]
  (let [cart (get-in data keypaths/order)]
    [:div
     [:form#update-cart
      {:on-submit (utils/enqueue-event data events/control-cart-update)}
      [:div.inside-cart-form
       [:div.cart-items
        [:div.cart-line-items
         (map (partial display-line-item data) (:line_items cart))]
        [:div.coupon-cart
         [:h4 "Have a Coupon Code?"]
         [:div.coupon-container
          [:label "Enter a coupon code:"]
          [:input.coupon-code-input
           (merge
            (utils/update-text data
                               events/control-cart-coupon-change
                               :coupon-code)
            {:type "text"
             :name "coupon-code"
             :value (get-in data keypaths/cart-coupon-code)})]]
         [:input.primary.button#update-button
          {:type "submit" :name "update" :value "Update"}]]
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
      (if (get-in data keypaths/user-order-id)
        (when-let [cart (get-in data keypaths/order)]
          (if (> (-> cart :line_items count) 0)
            (display-full-cart data)
            (display-empty-cart data)))
        (display-empty-cart data))]
     [:div.home-actions-top
      [:div.guarantee]
      [:div.free-shipping-action]
      [:div.keep-shopping
       [:a.full-link (shopping-link-attrs data)]]]])))
