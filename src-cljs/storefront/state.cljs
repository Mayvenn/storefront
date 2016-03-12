(ns storefront.state
  (:require [storefront.events :as events]
            [clojure.string :as string]
            [storefront.browser.cookie-jar :as cookie-jar]))

(defn get-store-subdomain []
  (first (string/split (.-hostname js/location) #"\.")))

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
   :ship-to-billing-address true
   :credit-card-name ""
   :credit-card-number ""
   :credit-card-expiration ""
   :credit-card-ccv ""
   :selected-shipping-method {}
   :use-store-credits false})

(defn initial-state []
  (let [cookie (cookie-jar/make-cookie)]
    {:history nil
     :cookie cookie
     :routes []
     :optimizely {:variations #{}}

     :session-id (cookie-jar/force-session-id cookie)
     :community-url nil
     :get-satisfaction-login? false

     :user (cookie-jar/retrieve-login cookie)
     :order (cookie-jar/retrieve-current-order cookie)
     :pending-promo-code (cookie-jar/retrieve-pending-promo-code cookie)
     :promotions []
     :past-orders {}
     :my-order-ids nil

     :store {:store_slug (get-store-subdomain)}
     :taxons []
     :taxon-product-order {}
     :products {}
     :states []
     :shipping-methods []
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
                               :lifetime-total nil
                               :available-credit nil
                               :bonuses []}
               :referral-program {:bonus-amount nil
                                  :next-amount nil
                                  :earning-amount nil
                                  :lifetime-total nil
                                  :referrals []}
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
          :taxon-images {:closures  ["/images/style_images/closures/1.png"
                                     "/images/style_images/closures/2.png"
                                     "/images/style_images/closures/3.png"
                                     "/images/style_images/closures/4.png"
                                     "/images/style_images/closures/5.png"
                                     "/images/style_images/closures/6.png"]
                         (keyword "deep wave") ["/images/style_images/deep_wave/1.png"
                                                "/images/style_images/deep_wave/2.png"
                                                "/images/style_images/deep_wave/3.png"
                                                "/images/style_images/deep_wave/4.png"
                                                "/images/style_images/deep_wave/5.png"
                                                "/images/style_images/deep_wave/6.png"]
                         (keyword "loose wave") ["/images/style_images/loose_wave/1.png"
                                                 "/images/style_images/loose_wave/2.png"
                                                 "/images/style_images/loose_wave/3.png"
                                                 "/images/style_images/loose_wave/4.png"
                                                 "/images/style_images/loose_wave/5.png"
                                                 "/images/style_images/loose_wave/6.png"]
                         (keyword "body wave") ["/images/style_images/body_wave/1.png"
                                                "/images/style_images/body_wave/2.png"
                                                "/images/style_images/body_wave/3.png"
                                                "/images/style_images/body_wave/4.png"
                                                "/images/style_images/body_wave/5.png"
                                                "/images/style_images/body_wave/6.png"]
                         :blonde  ["/images/style_images/blonde/1.png"
                                   "/images/style_images/blonde/2.png"
                                   "/images/style_images/blonde/3.png"
                                   "/images/style_images/blonde/4.png"
                                   "/images/style_images/blonde/5.png"
                                   "/images/style_images/blonde/6.png"
                                   "/images/style_images/blonde/7.png"
                                   "/images/style_images/blonde/8.png"]
                         :curly  ["/images/style_images/curly/1.png"
                                  "/images/style_images/curly/2.png"
                                  "/images/style_images/curly/3.png"
                                  "/images/style_images/curly/4.png"
                                  "/images/style_images/curly/5.png"
                                  "/images/style_images/curly/6.png"]
                         :straight  ["/images/style_images/straight/1.png"
                                     "/images/style_images/straight/2.png"
                                     "/images/style_images/straight/3.png"
                                     "/images/style_images/straight/4.png"
                                     "/images/style_images/straight/5.png"
                                     "/images/style_images/straight/6.png"]}
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
          :past-order-id nil
          :menu-expanded false
          :account-menu-expanded false
          :selected-stylist-stat :next-payout

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
          :loaded-reviews false
          :loaded-stripe false
          :loaded-facebook false
          :loaded-places false
          :loaded-talkable false}}))
