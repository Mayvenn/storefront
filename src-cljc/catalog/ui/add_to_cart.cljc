(ns catalog.ui.add-to-cart
  (:require #?@(:cljs [[storefront.hooks.quadpay :as quadpay]])
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(defn cta-molecule
  [{:cta/keys [id label target spinning? disabled?]}]
  (when (and id label target)
    (ui/teal-button
     (merge {:data-test id
             :spinning? (boolean spinning?)
             :disabled? (boolean disabled?)}
            (apply utils/fake-href target))
     [:div.flex.items-center.justify-center.inherit-color label])))

(defn add-to-cart-incentive-block-molecule
  [{:add-to-cart.incentive-block/keys [id message link-label link-target footnote icon]}]
  (when id
    (component/html
     [:div.flex.pb1
      [:div.px3.flex.justify-center.pt1
       (ui/ucare-img {:width "18"} icon)]
      [:div.flex.flex-column
       [:div.h7
        [:span.mr1 message]
        [:a.underline.navy.pointer
         {:data-test "freeinstall-add-to-cart-info-link"
          :on-click  (apply utils/send-event-callback link-target)}
         link-label]]
       [:div.dark-silver.h8 footnote]]])))

(defn add-to-cart-background-atom
  [color]
  (when color
    {:class color}))

(defcomponent organism
  "Add to Cart organism"
  [data _ _]
  [:div.px3.pt3.pb1
   (add-to-cart-background-atom (:add-to-cart.background/color data))
   (add-to-cart-incentive-block-molecule data)
   (cta-molecule data)
   #?(:cljs
      [:div
       (component/build quadpay/component
                        {:quadpay/order-total (:quadpay/price data)
                         :quadpay/show?       (:quadpay/loaded? data)
                         :quadpay/directive   :just-select}
                        nil)])])
