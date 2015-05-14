(ns storefront.components.stylist.referrals
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.stylist.nav :refer [stylist-dashboard-nav-component]]
            [storefront.components.formatters :as f]
            [storefront.state :as state]))

(defn stylist-referrals-component [data owner]
  (om/component
   (html
    [:div
     [:h2.header-bar-heading.referrals "Referrals"]

     (om/build stylist-dashboard-nav-component data)

     [:div.dashboard-content
      [:a#email-referral.dashboard-summary {:href "mailto:FIXME" :target "_top"}
       [:figure.email-icon]
       "Email us a new referral"
       [:figure.right-arrow-icon]]

      [:div#money-rules
       [:div.gold-money-box]
       [:div.money-rule-details
        [:p
         "Earn "
         "FIXME"
         " in bonus credit when each stylist makes their first "
         "FIXME"]]]

      [:div.my-referrals
       [:h4.dashboard-details-header "My Referrals"]
       [:div.solid-line-divider]

       [:div.emphasized-banner
        [:span.emphasized-banner-header "Total Referral Bonuses"]
        [:span.emphasized-banner-value "FIXME"]]

       [:div.loose-table-row
        [:div.left-content
         [:p.chopped-content "FIXME"]
         ["FIXME"]]]]]])))

