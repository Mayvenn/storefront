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

     :user (cookie-jar/retrieve-login cookie)
     :order nil
     :promotions []

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
                                  :referrals []}}

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

     :ui {:navigation-event events/navigate-home
          :browse-taxon-query nil
          :browse-product-query nil
          :browse-variant-query nil
          :browse-variant-quantity 1
          :browse-recently-added-variants []
          :menu-expanded false
          :account-menu-expanded false
          :checkout-current-step "address"
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
                                        :use-billing-address true}}
          :flash {:success {:message nil
                            :navigation []}}
          :checkout-selected-shipping-method-id 0}}))
