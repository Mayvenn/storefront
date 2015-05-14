(ns storefront.components.stylist.nav
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.events :as events]
            [storefront.components.utils :as utils]))

(defn stylist-dashboard-nav-component [data]
  (om/component
   (html
    [:nav.stylist-dashboard-nav
     (utils/link-with-selected data events/navigate-stylist-commissions "Commissions")
     (utils/link-with-selected data events/navigate-stylist-commissions "Bonus Credit")
     (utils/link-with-selected data events/navigate-stylist-referrals "Referrals")])))
