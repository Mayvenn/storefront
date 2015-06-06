(ns storefront.state
  (:require [cljs.core.async :refer [chan]]
            [storefront.events :as events]
            [clojure.string :as string]
            [storefront.cookie-jar :as cookie-jar]))

(defn get-store-subdomain []
  (first (string/split (.-hostname js/location) #"\.")))

(defn initial-state []
  (let [cookie (cookie-jar/make-cookie)]
    {:event-ch (chan)

     :history nil
     :cookie cookie
     :routes []

     :session-id (cookie-jar/force-session-id cookie)

     :user (cookie-jar/retrieve-login cookie)
     :order nil
     :promotions []
     :past-orders {}
     :my-order-ids nil

     :store {:store_slug (get-store-subdomain)}
     :taxons []
     :products {}
     :states []
     :sms-number nil
     :stylist {:sales-rep-email nil
               :commissions {:rate nil
                             :next-amount nil
                             :paid-total nil
                             :new-orders []
                             :payouts []}
               :bonus-credits {:bonus-amount nil
                               :earning-amount nil
                               :commissioned-revenue nil
                               :total-credit nil
                               :available-credit nil
                               :bonuses []}
               :referral-program {:bonus-amount nil
                                  :next-amount nil
                                  :earning-amount nil
                                  :total-amount nil
                                  :referrals []}
               :manage-account {:email nil
                                :id 10
                                :birth-date-1i nil
                                :birth-date-2i nil
                                :birth-date-3i nil
                                :password ""
                                :password_confirmation ""
                                :address {:firstname nil
                                          :lastname nil
                                          :address1 nil
                                          :address2 nil
                                          :city nil
                                          :state_id nil
                                          :country_id nil
                                          :zipcode nil
                                          :phone nil}}}

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

     :ui {:navigation-message [events/navigate-home {}]
          :browse-taxon-query nil
          :browse-product-query nil
          :browse-variant-query nil
          :browse-variant-quantity 1
          :browse-recently-added-variants []
          :past-order-id nil
          :menu-expanded false
          :account-menu-expanded false
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
                 :coupon-code ""}
          :checkout {:billing-address {:firstname ""
                                       :lastname ""
                                       :address1 ""
                                       :address2 ""
                                       :city ""
                                       :state_id 0
                                       :zipcode ""
                                       :phone ""
                                       :save-my-address true}
                     :shipping-address {:firstname ""
                                        :lastname ""
                                        :address1 ""
                                        :address2 ""
                                        :city ""
                                        :state_id 0
                                        :zipcode ""
                                        :phone ""
                                        :use-billing-address true}
                     :credit-card-name ""
                     :credit-card-number ""
                     :credit-card-expiration ""
                     :credit-card-ccv ""
                     :selected-shipping-method-id 0}
          :flash {:success {:message nil
                            :navigation []}}}}))
