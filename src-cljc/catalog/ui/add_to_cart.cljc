(ns catalog.ui.add-to-cart
  (:require #?@(:cljs [[storefront.hooks.quadpay :as quadpay]])
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(defn cta-molecule
  [{:cta/keys                       [id label target spinning? disabled?]
    disabled-explanation-id         :cta-disabled-explanation/id
    disabled-explanation-primary    :cta-disabled-explanation/primary
    disabled-explanation-cta-label  :cta-disabled-explanation/cta-label
    disabled-explanation-cta-target :cta-disabled-explanation/cta-target}]
  (when (and id label target)
    [:div
     (ui/button-large-primary
      (merge
       {:data-test id
        :spinning? (boolean spinning?)
        :disabled? (boolean disabled?)}
       #?(:clj {:disabled? true})
       (apply utils/fake-href target))
      (component/html [:div.flex.items-center.justify-center.inherit-color label]))
     (when disabled-explanation-id
       [:div.flex.flex-column.items-center
        [:div.red.m2.content-3
         {:data-test disabled-explanation-id}
         disabled-explanation-primary]
        (ui/button-medium-underline-primary
         (merge (when disabled-explanation-cta-target
                  (apply utils/route-to disabled-explanation-cta-target))
                {:class "m5"})
         disabled-explanation-cta-label)])]))

(defn add-to-cart-incentive-block-molecule
  [{:add-to-cart.incentive-block/keys
    [id callout message link-id link-label link-target footnote]}]
  (when id
    [:div.py2.mb2.bg-refresh-gray.px3
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

(defn add-to-cart-quadpay-molecule
  [{:add-to-cart.quadpay/keys [price loaded?]}]
  #?(:cljs
     [:div
      (component/build quadpay/component
                       {:quadpay/order-total price
                        :quadpay/show?       loaded?
                        :quadpay/directive   :just-select}
                       nil)]))

(defcomponent organism
  "Add to Cart organism"
  [data _ _]
  [:div
   (add-to-cart-incentive-block-molecule data)
   [:div.px3.py1
    (cta-molecule data)
    (add-to-cart-quadpay-molecule data)]])
