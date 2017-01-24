(ns storefront.components.essence
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.ui :as ui]))

(defn component [_ owner {:keys [on-close]}]
  (om/component
   (html
    (ui/modal {:on-close on-close
               :bg-class "bg-darken-4"}
              [:div.bg-white.rounded.p4
               (ui/modal-close {:on-close on-close})
               [:div.flex.flex-column.items-center.justify-center.py2.dark-gray
                [:h3.navy.mb3 "Offer and Rebate Details"]
                [:p.mb2 "Included with your purchase is a 1 year subscription to ESSENCE magazine ($10 value)."]
                [:p.mb3
                 "Offer valid in the U.S. only. Limit one subscription per Mayvenn customer. "
                 " After your purchase is complete, your name and address will be forwarded to the publisher to fulfill your subscription. "
                 " They will send you a postcard with instructions on how to request a refund, if desired. "
                 " Your first issue will mail 8-12 weeks upon receipt of order."]
                (ui/navy-button {:on-click on-close} "Close")]]))))

(defn built-component [_ opts]
  (om/build component nil opts))
