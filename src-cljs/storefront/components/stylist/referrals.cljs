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





;; <div class="dashboard-content">
;;   <a class="dashboard-summary" id="email-referral" href="mailto:<%= @stylist.sales_rep.email %>?Subject=Referral" target="_top">
;;     <figure class="email-icon"></figure>
;;     Email us a new referral
;;     <figure class="right-arrow-icon"></figure>
;;   </a>
;; 
;;   <div id="money-rules">
;;     <div class="gold-money-box"></div>
;;     <div class="money-rule-details">
;;       <p>Earn <%= Spree::Money.new(Mayvenn::Stylist::REFERRAL_BONUS_AMOUNT, no_cents: true) %> in bonus credit when each stylist makes their first <%= Spree::Money.new(Mayvenn::Stylist::REFERRAL_EARNING_MINIMUM, no_cents: true) %>.</p>
;;     </div>
;;   </div>
;; 
;;   <div class='my-referrals'>
;;     <h4 class="dashboard-details-header">My Referrals</h4>
;;     <div class="solid-line-divider"></div>
;; 
;;     <div class="emphasized-banner">
;;       <span class="emphasized-banner-header">Total Referral Bonuses</span>
;;       <span class="emphasized-banner-value"><%= Spree::Money.new(@referrals.map(&:store_credit).map { |sc| sc.try!(:amount) || 0 }.reduce(&:+) || 0, no_cents: true) %></span>
;;     </div>
;; 
;;     <% @referrals.each do |referral| %>
;;       <div class="loose-table-row">
;;         <div class="left-content">
;;           <p class="chopped-content"><%= referral.referred_stylist.full_name %></p>
;;           <% if referral.store_credit %>
;;             <p class="referral-paid-time"><%= referral.store_credit.created_at.strftime("%m/%d/%Y") %></p>
;;           <% else %>
;;             <div class="referral-progress">
;;               <div class="progress-bar">
;;                 <div class="progress-bar-progress" style="width: <%= referral.percent_complete %>%;"></div>
;;               </div>
;;               <p class="progress-text">Sales so far: <%= Spree::Money.new(referral.referred_stylist.total_commissioned_revenue) %> of <%= Spree::Money.new(Mayvenn::Stylist::REFERRAL_EARNING_MINIMUM, no_cents: true) %></p>
;;             </div>
;;           <% end %>
;;         </div>
;; 
;;         <div class="right-content">
;;           <p class="paid-amount"><%= Spree::Money.new(referral.bonus_due, no_cents: true) %> bonus</p>
;;           <% if referral.store_credit %>
;;             <p class="referral-label paid-label">Paid</p>
;;           <% else %>
;;             <p class="referral-label pending-label">Pending</p>
;;           <% end %>
;;         </div>
;;       </div>
;;     <% end %>
;;    </div>
;; 
;; </div>
