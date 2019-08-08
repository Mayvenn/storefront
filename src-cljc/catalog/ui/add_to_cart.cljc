(ns catalog.ui.add-to-cart
  (:require #?@(:cljs [[storefront.hooks.quadpay :as quadpay]])
            [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.platform.component-utils :as utils]))

;; MOLECULES
(defn cta-molecule
  [{:cta/keys [id label target spinning? disabled?]}]
  (when (and id label target)
    (ui/teal-button
     (merge {:data-test id
             :spinning? (boolean spinning?)
             :disabled? (boolean disabled?)}
            (apply utils/fake-href target))
     [:div.flex.items-center.justify-center.inherit-color label]) ))

(defn freeinstall-add-to-cart-block
  [{:freeinstall-add-to-cart-block/keys [message link-label link-target footnote icon show?]}]
  (when show?
    [:div.flex.pb1
     [:div.px3.flex.justify-center.pt1
      (ui/ucare-img {:width "18"} icon)]
     [:div.flex.flex-column
      [:div.h7
       [:span.mr1 message]
       [:a.underline.navy
        (apply utils/send-event-callback link-target)
        link-label]]
      [:div.dark-silver.h8 footnote]]]))


;; ORGANISM
(defn organism
  "Add to Cart organism"
  [data _ _]
  (component/create
   [:div.px3.pt3.pb1
    (when (:freeinstall-add-to-cart-block/show? data)
      {:class "bg-fate-white"})
    (freeinstall-add-to-cart-block data)
    (cta-molecule data)
    #?(:cljs
       [:div
        (component/build quadpay/component
                         {:show?       (:quadpay/loaded? data)
                          :order-total (:quadpay/price data)
                          :directive   [:div.flex.justify-center.items-center
                                        "Just select"
                                        [:div.mx1 {:style {:width "70px" :height "14px"}}
                                         ^:inline (svg/quadpay-logo)]
                                        "at check out."]}
                         nil)])]))
