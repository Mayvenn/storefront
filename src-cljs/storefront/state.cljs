(ns storefront.state
  (:require [storefront.events :as events]
            [storefront.config :as config]
            [clojure.string :as string]
            [storefront.browser.cookie-jar :as cookie-jar]))

(def initial-checkout-state
  {:as-guest false
   :guest-email ""
   :billing-address {:first-name ""
                     :last-name ""
                     :address1 ""
                     :address2 ""
                     :city ""
                     :state ""
                     :zipcode ""
                     :phone ""}
   :shipping-address {:first-name ""
                      :last-name ""
                      :address1 ""
                      :address2 ""
                      :city ""
                      :state ""
                      :zipcode ""
                      :phone ""}
   :save-my-addresses true
   :save-my-addresses-no-op true
   :bill-to-shipping-address true
   :credit-card-name ""
   :credit-card-save true
   :credit-card-selected-id nil
   :selected-shipping-method {}
   :use-store-credits false})

(def initial-cart-state
  {:coupon-code ""
   :source nil
   :paypal-redirect false})

(def empty-referral {:fullname ""
                     :phone ""
                     :email ""})

(def initial-stylist-state
  {:sales-rep-email nil
   :stats {:previous-payout {:amount 0 :date nil}
           :next-payout {:amount 0}
           :lifetime-payouts {:amount 0}}
   :commissions {:rate nil
                 :page 0
                 :pages nil
                 :history (sorted-set-by
                           (fn [a b]
                             (let [date-and-id (juxt :commission_date :id)]
                               (compare (date-and-id b) (date-and-id a)))))}
   :bonus-credits {:bonus-amount nil
                   :earning-amount nil
                   :commissioned-revenue nil
                   :lifetime-total nil
                   :available-credit nil
                   :bonuses []
                   :page 0
                   :pages nil}
   :referral-program {:bonus-amount nil
                      :earning-amount nil
                      :lifetime-total nil
                      :referrals []
                      :page 0
                      :pages nil}
   :referrals [empty-referral]
   :manage-account {:email nil
                    :id nil
                    :birth-date nil
                    :address {:firstname nil
                              :lastname nil
                              :address1 nil
                              :address2 nil
                              :city nil
                              :state_id nil
                              :country_id nil
                              :zipcode nil
                              :phone nil}}})

(defn initial-state []
  (let [cookie (cookie-jar/make-cookie)]
    {:cookie cookie
     :features #{}

     :session-id (cookie-jar/force-session-id cookie)

     :user (cookie-jar/retrieve-login cookie)
     :order (cookie-jar/retrieve-current-order cookie)
     :completed-order nil
     :pending-promo-code (cookie-jar/retrieve-pending-promo-code cookie)
     :promotions []
     :db {:skus #{}
          :images #{}}

     :store {}
     :named-searches []
     :products {}
     :states []
     :shipping-methods []
     :sms-number "34649"
     :stylist initial-stylist-state
     :api-cache {}
     :ugc {}

     :category-filters {}

     :billing-address {:firstname ""
                       :lastname ""
                       :address1 ""
                       :address2 ""
                       :city ""
                       :state_id 0
                       :zipcode ""
                       :phone ""}
     :shipping-address {:firstname ""
                        :lastname ""
                        :address1 ""
                        :address2 ""
                        :city ""
                        :state_id 0
                        :zipcode ""
                        :phone ""}

     :environment "development"

     :experiments {:bucketed #{}
                   :manual config/manual-experiments}

     :leads {:stylist {:password ""
                       :referred false
                       :referrers-phone ""
                       :address1 ""
                       :address2 ""
                       :city ""
                       :zip ""
                       :state ""
                       :licensed false
                       :payout-method "venmo"
                       :venmo-phone ""
                       :paypal-email ""
                       :slug ""}}

     :ui {:api-requests []
          :navigation-message [events/navigate-home {}]
          :return-navigation-message [events/navigate-home {}]
          :errors {}
          :browse-named-search-query {}
          :browse-variant-quantity 1
          :browse-recently-added-variants []
          :menu-expanded false
          :account-menu-expanded false
          :store-info-expanded false
          :popup nil
          :stylist-banner-hidden? false
          :show-apple-pay? false

          :expanded-commission-order-id #{nil}
          :email-capture-session (cookie-jar/retrieve-email-capture-session cookie)

          :sign-in {:email ""
                    :password ""}
          :sign-up {:email ""
                    :password ""}
          :forgot-password {:email ""}
          :reset-password {:password ""
                           :token ""}
          :manage-account {:email ""
                           :password ""}
          :cart initial-cart-state
          :checkout initial-checkout-state
          :flash {:now   {:success {:message nil}
                          :failure {:message nil}}
                  :later {:success {:message nil}
                          :failure {:message nil}}}
          :review-components-count 0
          :static nil
          :loaded {:reviews false
                   :stripe {:v2 false
                            :v3 false}
                   :facebook false
                   :places false
                   :talkable false}}}))
