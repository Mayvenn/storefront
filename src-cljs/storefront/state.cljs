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
          :cart {:quantities {}}
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

(def event-ch-path [:event-ch])

(def history-path [:history])
(def cookie-path [:cookie])
(def routes-path [:routes])

(def user-path [:user])
(def user-email-path (conj user-path :email))
(def user-token-path (conj user-path :token))
(def user-store-slug-path (conj user-path :store-slug))
(def user-id-path (conj user-path :id))
(def user-order-token-path (conj user-path :order-token))
(def user-order-id-path (conj user-path :order-id))

(def order-path [:order])

(def store-path [:store])
(def store-slug-path (conj store-path :store_slug))

(def taxons-path [:taxons])
(def products-path [:products])
(def states-path [:states])
(def sms-number-path [:sms-number])

(def ui-path [:ui])
(def navigation-event-path (conj ui-path :navigation-event))
(def browse-taxon-query-path (conj ui-path :browse-taxon-query))
(def browse-product-query-path (conj ui-path :browse-product-query))
(def browse-variant-query-path (conj ui-path :browse-variant-query))
(def browse-variant-quantity-path (conj ui-path :browse-variant-quantity))
(def browse-recently-added-variants-path (conj ui-path :browse-recently-added-variants))
(def menu-expanded-path (conj ui-path :menu-expanded))
(def account-menu-expanded-path (conj ui-path :account-menu-expanded))

(def checkout-current-step-path (conj ui-path :checkout-current-step))
(def checkout-selected-shipping-method-id (conj ui-path :checkout-selected-shipping-method-id))

(def sign-in-path (conj ui-path :sign-in))
(def sign-in-email-path (conj sign-in-path :email))
(def sign-in-password-path (conj sign-in-path :password))
(def sign-in-remember-path (conj sign-in-path :remember-me))

(def sign-up-path (conj ui-path :sign-up))
(def sign-up-email-path (conj sign-up-path :email))
(def sign-up-password-path (conj sign-up-path :password))
(def sign-up-password-confirmation-path (conj sign-up-path :password-confirmation))

(def forgot-password-path (conj ui-path :forgot-password))
(def forgot-password-email-path (conj forgot-password-path :email))

(def reset-password-path (conj ui-path :reset-password))
(def reset-password-password-path (conj reset-password-path :password))
(def reset-password-password-confirmation-path (conj reset-password-path :password-confirmation))
(def reset-password-token-path (conj reset-password-path :token))

(def manage-account-path (conj ui-path :manage-account))
(def manage-account-email-path (conj manage-account-path :email))
(def manage-account-password-path (conj manage-account-path :password))
(def manage-account-password-confirmation-path (conj manage-account-path :password-confirmation))

(def cart-path (conj ui-path :cart))
(def cart-quantities-path (conj cart-path :quantities))

(def checkout-path (conj ui-path :checkout))
(def checkout-billing-address-path (conj checkout-path :billing-address))
(def checkout-billing-address-firstname-path (conj checkout-billing-address-path :firstname))
(def checkout-billing-address-lastname-path (conj checkout-billing-address-path :lastname))
(def checkout-billing-address-address1-path (conj checkout-billing-address-path :address1))
(def checkout-billing-address-address2-path (conj checkout-billing-address-path :address2))
(def checkout-billing-address-city-path (conj checkout-billing-address-path :city))
(def checkout-billing-address-state-path (conj checkout-billing-address-path :state_id))
(def checkout-billing-address-zip-path (conj checkout-billing-address-path :zipcode))
(def checkout-billing-address-phone-path (conj checkout-billing-address-path :phone))
(def checkout-billing-address-save-my-address-path (conj checkout-billing-address-path :save-my-address))
(def checkout-shipping-address-path (conj checkout-path :shipping-address))
(def checkout-shipping-address-firstname-path (conj checkout-shipping-address-path :firstname))
(def checkout-shipping-address-lastname-path (conj checkout-shipping-address-path :lastname))
(def checkout-shipping-address-address1-path (conj checkout-shipping-address-path :address1))
(def checkout-shipping-address-address2-path (conj checkout-shipping-address-path :address2))
(def checkout-shipping-address-city-path (conj checkout-shipping-address-path :city))
(def checkout-shipping-address-state-path (conj checkout-shipping-address-path :state_id))
(def checkout-shipping-address-zip-path (conj checkout-shipping-address-path :zipcode))
(def checkout-shipping-address-phone-path (conj checkout-shipping-address-path :phone))
(def checkout-shipping-address-use-billing-address-path (conj checkout-shipping-address-path :use-billing-address))

(def flash-path (conj ui-path :flash))
(def flash-success-path (conj flash-path :success))
(def flash-success-message-path (conj flash-success-path :message))
(def flash-success-nav-path (conj flash-success-path :navigation))

(def billing-address-path [:billing-address])

(def shipping-address-path [:shipping-address])

(def stylist-path [:stylist])

(def stylist-sales-rep-email-path (conj stylist-path :sales-rep-email))

(def stylist-commissions-path (conj stylist-path :commissions))
(def stylist-commissions-rate-path (conj stylist-commissions-path :rate))
(def stylist-commissions-next-amount-path (conj stylist-commissions-path :next-amount))
(def stylist-commissions-paid-total-path (conj stylist-commissions-path :paid-total))
(def stylist-commissions-new-orders-path (conj stylist-commissions-path :new-orders))
(def stylist-commissions-payouts-path (conj stylist-commissions-path :payouts))


(def stylist-bonus-credit-path (conj stylist-path :bonus-credits))
(def stylist-bonus-credit-bonus-amount-path (conj stylist-bonus-credit-path :bonus-amount))
(def stylist-bonus-credit-earning-amount-path (conj stylist-bonus-credit-path :earning-amount))
(def stylist-bonus-credit-commissioned-revenue-path (conj stylist-bonus-credit-path :commissioned-revenue))
(def stylist-bonus-credit-total-credit-path (conj stylist-bonus-credit-path :total-credit))
(def stylist-bonus-credit-available-credit-path (conj stylist-bonus-credit-path :available-credit))
(def stylist-bonus-credit-bonuses-path (conj stylist-bonus-credit-path :bonuses))

(def stylist-referral-program-path (conj stylist-path :referral-program))
(def stylist-referral-program-bonus-amount-path (conj stylist-referral-program-path :referral-program-bonus-amount))
(def stylist-referral-program-earning-amount-path (conj stylist-referral-program-path :referral-program-earning-amount))
(def stylist-referral-program-total-amount-path (conj stylist-referral-program-path :referral-program-total-amount))
(def stylist-referral-program-referrals-path (conj stylist-referral-program-path :referral-program-referrals))
