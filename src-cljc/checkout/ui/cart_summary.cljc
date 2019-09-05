(ns checkout.ui.cart-summary
  (:require [storefront.component :as component]
            [checkout.ui.molecules :as checkout.M]
            [ui.molecules :as ui.M]))

(defn organism
  [{:cart-summary/keys [id lines]
    :as query
    :keys [promo-field-data]} owner _]
  (component/create
   [:div {:data-test id}
    [:div.py1.bg-fate-white.px4
     [:table.col-12
      [:tbody
       (for [line lines]
         (checkout.M/cart-summary-line-molecule line))]]

     (when promo-field-data
       [:div.my2
        (ui.M/input-group-field-and-button-molecule promo-field-data)])

     (when (:freeinstall-informational/value query)
       checkout.M/freeinstall-informational)]

    [:div.pt2.px4
     (checkout.M/cart-summary-total-line query)]]))
