(ns storefront.state
  (:require [storefront.events :as events]
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
   :credit-card-number ""
   :credit-card-expiration ""
   :credit-card-ccv ""
   :selected-shipping-method {}
   :use-store-credits false})

(defn initial-state []
  (let [cookie (cookie-jar/make-cookie)]
    {:cookie cookie
     :optimizely {:variations #{}}

     :session-id (cookie-jar/force-session-id cookie)
     :get-satisfaction-login? false

     :user (cookie-jar/retrieve-login cookie)
     :order (cookie-jar/retrieve-current-order cookie)
     :pending-promo-code (cookie-jar/retrieve-pending-promo-code cookie)
     :promotions []

     :store (js->clj js/store :keywordize-keys true)
     :taxons []
     :products {}
     :states []
     :shipping-methods []
     :sms-number nil
     :stylist {:sales-rep-email nil
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
               :manage-account {:email nil
                                :id 10
                                :birth-date-1i nil
                                :birth-date-2i nil
                                :birth-date-3i nil
                                :address {:firstname nil
                                          :lastname nil
                                          :address1 nil
                                          :address2 nil
                                          :city nil
                                          :state_id nil
                                          :country_id nil
                                          :zipcode nil
                                          :phone nil}}}
     :api-cache {}

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

     :ui {:api-requests []
          :navigation-message [events/navigate-home {}]
          :return-navigation-message [events/navigate-home {}]
          :previous-navigation-message []
          :validation-errors {:error-message nil
                              :details {}}
          :browse-taxon-query nil
          :browse-product-query nil
          :browse-variant-query nil
          :browse-variant-quantity 1
          :browse-recently-added-variants []
          :menu-expanded false
          :account-menu-expanded false
          :store-info-expanded false

          :expanded-commission-order-id #{nil}

          :sign-in {:email ""
                    :password ""
                    :remember-me true}
          :sign-up {:email ""
                    :password ""
                    :password-confirmation ""}
          :forgot-password {:email ""}
          :reset-password {:password ""
                           :password-confirmation ""
                           :token ""}
          :manage-account {:email ""
                           :password ""
                           :password-confirmation ""}
          :cart {:quantities {}
                 :coupon-code ""
                 :paypal-redirecting false}
          :checkout initial-checkout-state
          :flash {:success {:message nil
                            :navigation []}
                  :failure {:message nil
                            :navigation []}}
          :review-components-count 0
          :places-enabled true
          :loaded {:reviews false
                   :stripe false
                   :facebook false
                   :places false
                   :talkable false
                   :optimizely false}}}))
