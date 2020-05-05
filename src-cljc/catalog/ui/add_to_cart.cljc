(ns catalog.ui.add-to-cart
  (:require #?@(:cljs [[storefront.hooks.quadpay :as quadpay]])
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(defn cta-molecule
  [{:cta/keys [id label target spinning? disabled?]}]
  (when (and id label target)
    [:div
     (ui/button-large-primary
      (merge
       {:data-test id
        :spinning? (boolean spinning?)
        :disabled? (boolean disabled?)}
       #?(:clj {:disabled? true})
       (apply utils/fake-href target))
      (component/html [:div.flex.items-center.justify-center.inherit-color label]))]))

(defn add-to-cart-incentive-block-molecule
  [{:add-to-cart.incentive-block/keys
    [id callout message link-id link-label link-target footnote]}]
  (when id
    [:div.py2.mb2
     {:class "bg-refresh-gray"}
     [:div.proxima.content-2.line-height-1.bold callout]
     [:div.pl4.pr1
      [:div.proxima.content-3.mb1
       message
       (when link-id
         (ui/button-small-underline-primary
          {:data-test "freeinstall-add-to-cart-info-link"
           :on-click  (apply utils/send-event-callback link-target)}
          link-label))]
      [:div.content-4.dark-gray footnote]]]))

(defcomponent organism
  "Add to Cart organism"
  [data _ _]
  [:div
   (add-to-cart-incentive-block-molecule data)
   [:div.px3.py1
    (cta-molecule data)
    #?(:cljs
       [:div
        (component/build quadpay/component
                         {:quadpay/order-total (:quadpay/price data)
                          :quadpay/show?       (:quadpay/loaded? data)
                          :quadpay/directive   :just-select}
                         nil)])]])
