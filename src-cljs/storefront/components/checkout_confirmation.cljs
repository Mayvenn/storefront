(ns storefront.components.checkout-confirmation
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.components.utils :as utils]
            [storefront.components.checkout-steps :refer [checkout-step-bar]]
            [storefront.components.counter :refer [counter-component]]
            [storefront.components.order-summary :refer [display-order-summary]]
            [clojure.string :as string]
            [cljs.core.async :refer [put!]]))

(defn field [name value & [classes]]
  [:div.line-item-attr {:class classes}
   [:span.cart-label name]
   [:span.cart-value value]])

(defn display-line-item [app-state line-item]
  [:div.line-item
   [:a
    (if (zero? (get-in line-item [:variant :images]))
      [:img]
      [:img {:src (str "http://localhost:3000/" (get-in line-item [:variant :images 0 :large_url]))
             :alt (get-in line-item [:variant :name])}])]

   [:div.line-item-detail
    [:h4 [:a (utils/route-to app-state events/navigate-product {:product-path (get-in line-item [:variant :slug])})
          (get-in line-item [:variant :name])]]

    (field "Length:" (string/replace (get-in line-item [:variant :options_text]) #"Length:" ""))
    (field "Quantity:" (:quantity line-item))
    (field "Price:" (:single_display_amount line-item))
    (field "Subtotal:" (:display_amount line-item))]
   [:div {:style {:clear :both}}]])

(defn checkout-confirmation-component [data owner]
  (om/component
   (html
    [:div#checkout
     (checkout-step-bar data)
     [:div.row
      [:div.checkout-form-wrapper
       [:form.edit_order
        {:method "POST"
         :on-submit (utils/enqueue-event data events/control-checkout-payment-method-submit)}

        [:div.checkout-container

         (map (partial display-line-item data)
              (:line_items (get-in data keypaths/order)))

         (display-order-summary (get-in data keypaths/order))

         [:div.form-buttons.pay-for-order
          [:input.continue.button.primary
           {:type "submit" :name "commit" :value "Pay for order"}]]]]]]])))
