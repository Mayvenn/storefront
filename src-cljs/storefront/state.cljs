(ns storefront.state
  (:require [storefront.events :as events]
            [storefront.config :as config]
            [storefront.browser.cookie-jar :as cookie-jar]
            [spice.core :as spice]))

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
  {:coupon-code             ""
   :recently-added-skus     #{}
   :freeinstall-just-added? false
   :source                  nil
   :paypal-redirect         false})

(def empty-referral {:fullname ""
                     :phone ""
                     :email ""})

(def initial-stylist-state
  {:sales-rep-email nil
   :earnings {:rate nil
              :page 0
              :pages nil
              :history []}
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
                              :address-1 nil
                              :address-2 nil
                              :city nil
                              :state-id nil
                              :country-id nil
                              :zipcode nil
                              :phone nil}}})

(def initial-dashboard-state
  {:cash-balance-section-expanded?         false
   :store-credit-balance-section-expanded? false})

(defn initial-state []
  (let [cookie (cookie-jar/make-cookie)]
    {:cookie    cookie
     :modules   #{}
     :adventure {:from-shop-to-freeinstall? (boolean (cookie-jar/retrieve-from-shop-to-freeinstall cookie))
                 :affiliate-stylist-id      (some-> cookie
                                                    cookie-jar/retrieve-affiliate-stylist-id
                                                    :affiliate-stylist-id
                                                    spice/parse-int)}
     :features  #{}
     :scheme    (apply str (drop-last (.-protocol js/location)))

     :session-id (cookie-jar/force-session-id cookie)

     :user               (cookie-jar/retrieve-login cookie)
     :order              (cookie-jar/retrieve-current-order cookie)
     :completed-order    (cookie-jar/retrieve-completed-order cookie)
     :pending-promo-code (cookie-jar/retrieve-pending-promo-code cookie)
     :promotions         []
     :store              {}
     :products           {}
     :states             []
     :shipping-methods   []
     :sms-number         "34649"
     :stylist            initial-stylist-state
     :api-cache          {}
     :ugc                {}

     :billing-address  {:firstname ""
                        :lastname  ""
                        :address1  ""
                        :address2  ""
                        :city      ""
                        :state-id  0
                        :zipcode   ""
                        :phone     ""}
     :shipping-address {:firstname ""
                        :lastname  ""
                        :address1  ""
                        :address2  ""
                        :city      ""
                        :state-id  0
                        :zipcode   ""
                        :phone     ""}

     :environment "development"

     :experiments {:bucketed #{}
                   :manual   config/manual-experiments}

     :ui {:api-requests                   []
          :carousel                       {:certified-stylist-index 0}
          :navigation-message             [events/navigate-home {}]
          :return-navigation-message      [events/navigate-home {}]
          :errors                         {}
          :browse-variant-quantity        1
          :browse-recently-added-variants []
          :menu-expanded                  false
          :account-menu-expanded          false
          :store-info-expanded            false
          :popup                          nil

          :expanded-commission-order-id #{nil}

          :email-capture-session  (cookie-jar/retrieve-email-capture-session cookie)
          :phone-capture-session  (cookie-jar/get-phone-capture-session cookie)
          :dismissed-free-install (cookie-jar/get-dismissed-free-install cookie)

          :dismissed-pick-a-stylist-email-capture (cookie-jar/retrieve-dismissed-pick-a-stylist-email-capture cookie)
          :sign-in                                {:email    ""
                                                   :password ""}
          :sign-up                                {:email    ""
                                                   :password ""}
          :forgot-password                        {:email ""}
          :reset-password                         {:password ""
                                                   :token    ""}
          :manage-account                         {:email    ""
                                                   :password ""}
          :cart                                   initial-cart-state
          :checkout                               initial-checkout-state
          :flash                                  {:now   {:success {:message nil}
                                                           :failure {:message nil}}
                                                   :later {:success {:message nil}
                                                           :failure {:message nil}}}
          :review-components-count                0
          :static                                 nil
          :loaded                                 {:reviews  false
                                                   :stripe   false
                                                   :facebook false
                                                   :places   false}
          :confetti-mode                          "ready"
          :promo-code-entry-open?                 false}

     :v2 {:ui {:dashboard initial-dashboard-state}
          :db {}}}))
