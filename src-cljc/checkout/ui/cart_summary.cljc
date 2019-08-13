(ns checkout.ui.cart-summary
  (:require [storefront.component :as component]
            [checkout.ui.molecules :as checkout.M]))

(defn organism
  [{:cart-summary/keys [id lines]
    :as query
    :keys [promo-data
           freeinstall-informational-data]} owner _]
  (component/create
   [:div {:data-test id}
    [:div.py1.bg-fate-white.px4
     [:table.col-12
      [:tbody
       (for [line lines]
         (cart-summary-line-molecule line))]]

     (when promo-data
       [:div.h5 (promo-entry promo-data)])

     (when freeinstall-informational-data
       checkout.M/freeinstall-informational)]

    [:div.pt2.px4
     (checkout.M/cart-summary-total-line query)]]))
