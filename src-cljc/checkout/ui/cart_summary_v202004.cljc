(ns checkout.ui.cart-summary-v202004
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.components.ui :as ui]
            [checkout.ui.molecules :as checkout.M]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [ui.molecules :as ui.M]))

(defcomponent organism
  [{:cart-summary/keys [id lines]
    :as                query
    :keys              [promo-field-data]} owner _]
  [:div {:data-test id}
   [:div.py1.bg-refresh-gray.px4
    [:h2.title-2.proxima.my2.hide-on-mb "Order Summary"]
    [:table.col-12
     [:tbody
      (for [line lines]
        ^:inline (checkout.M/cart-summary-line-molecule line))]]

    (when-let [{:keys [text-input-attrs button-attrs submit-target]} promo-field-data]
      [:form.my2
       {:on-submit (apply utils/send-event-callback submit-target)}
       ^:inline (ui.M/field-reveal-molecule promo-field-data)
       (when (and text-input-attrs button-attrs)
         ^:inline (ui/input-group text-input-attrs
                                  button-attrs))])

    ^:inline (checkout.M/freeinstall-informational query)]

   [:div.pt2.px4.bg-white-on-mb
    ^:inline (checkout.M/cart-summary-total-line query)
    ^:inline (checkout.M/cart-summary-total-incentive query)]])
