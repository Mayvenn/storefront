(ns storefront.keypaths)

(def cookie [:cookie])

(def optimizely [:optimizely])
(def optimizely-variations [:optimizely :variations])

(def session-id [:session-id])

(def app-version [:app-version])

(def user [:user])
(def user-email (conj user :email))
(def user-token (conj user :user-token))
(def user-store-slug (conj user :store-slug))
(def user-id (conj user :id))
(def user-total-available-store-credit (conj user :total-available-store-credit))
(def user-messenger-token (conj user :messenger-token))

(def order [:order])
(def order-total (conj order :total))
(def order-promotion-codes (conj order :promotion-codes))
(def order-cart-payments (conj order :cart-payments))
(def order-cart-payments-store-credit (conj order-cart-payments :store-credit))
(def order-cart-payments-store-credit-amount (conj order-cart-payments-store-credit :amount))
(def order-cart-payments-paypal (conj order-cart-payments :paypal))
(def order-cart-payments-paypal-redirect-url (conj order-cart-payments-paypal :redirect-url))
(def order-cart-payments-stripe (conj order-cart-payments :stripe))
(def order-cart-payments-stripe-source (conj order-cart-payments-stripe :source))
(def order-cart-payments-stripe-amount (conj order-cart-payments-stripe :amount))

(def order-token (conj order :token))
(def order-number (conj order :number))

(def pending-promo-code [:pending-promo-code])
(def promotions [:promotions])

(def store [:store])
(def store-slug (conj store :store_slug))
(def store-stylist-id (conj store :stylist_id))

(def taxons [:taxons])
(def products [:products])
(def states [:states])
(def shipping-methods [:shipping-methods])
(def sms-number [:sms-number])
(def api-cache [:api-cache])

(def ui [:ui])
(def api-requests (conj ui :api-requests))
(def return-navigation-message (conj ui :return-navigation-message))
(def previous-navigation-message (conj ui :previous-navigation-message))
(def navigation-message (conj ui :navigation-message))
(def navigation-event (conj navigation-message 0))
(def navigation-args (conj navigation-message 1))
(def browse-taxon-query (conj ui :browse-taxon-query))
(def browse-variant-quantity (conj ui :browse-variant-quantity))
(def browse-recently-added-variants (conj ui :browse-recently-added-variants))
(def menu-expanded (conj ui :menu-expanded))
(def account-menu-expanded (conj ui :account-menu-expanded))
(def store-info-expanded (conj ui :store-info-expanded))
(def shop-menu-expanded (conj ui :shop-menu-expanded))
(def popup (conj ui :popup))
(def header-menus #{account-menu-expanded
                    shop-menu-expanded
                    store-info-expanded})
(def menus (conj header-menus menu-expanded))

(def places-enabled (conj ui :places-enabled))

(def expanded-commission-order-id (conj ui :expanded-commission-order-id))

(def bundle-builder (conj ui :bundle-builder))

(def sign-in (conj ui :sign-in))
(def sign-in-email (conj sign-in :email))
(def sign-in-password (conj sign-in :password))
(def sign-in-remember (conj sign-in :remember-me))

(def sign-up (conj ui :sign-up))
(def sign-up-email (conj sign-up :email))
(def sign-up-password (conj sign-up :password))
(def sign-up-password-confirmation (conj sign-up :password-confirmation))

(def forgot-password (conj ui :forgot-password))
(def forgot-password-email (conj forgot-password :email))

(def reset-password (conj ui :reset-password))
(def reset-password-password (conj reset-password :password))
(def reset-password-password-confirmation (conj reset-password :password-confirmation))
(def reset-password-token (conj reset-password :token))

(def manage-account (conj ui :manage-account))
(def manage-account-email (conj manage-account :email))
(def manage-account-password (conj manage-account :password))
(def manage-account-password-confirmation (conj manage-account :password-confirmation))

(def shared-cart-url (conj ui :shared-cart-url))

(def cart (conj ui :cart))
(def cart-coupon-code (conj cart :coupon-code))
(def cart-source (conj cart :source))

(def cart-paypal-redirect (conj cart :paypal-redirect))

(def checkout (conj ui :checkout))
(def checkout-as-guest (conj checkout :as-guest))
(def checkout-guest-email (conj checkout :guest-email))
(def checkout-bill-to-shipping-address (conj checkout :bill-to-shipping-address))
(def checkout-billing-address (conj checkout :billing-address))
(def checkout-billing-address-first-name (conj checkout-billing-address :first-name))
(def checkout-billing-address-last-name (conj checkout-billing-address :last-name))
(def checkout-billing-address-address1 (conj checkout-billing-address :address1))
(def checkout-billing-address-address2 (conj checkout-billing-address :address2))
(def checkout-billing-address-city (conj checkout-billing-address :city))
(def checkout-billing-address-state (conj checkout-billing-address :state))
(def checkout-billing-address-zip (conj checkout-billing-address :zipcode))
(def checkout-billing-address-phone (conj checkout-billing-address :phone))
(def checkout-shipping-address (conj checkout :shipping-address))
(def checkout-shipping-address-first-name (conj checkout-shipping-address :first-name))
(def checkout-shipping-address-last-name (conj checkout-shipping-address :last-name))
(def checkout-shipping-address-address1 (conj checkout-shipping-address :address1))
(def checkout-shipping-address-address2 (conj checkout-shipping-address :address2))
(def checkout-shipping-address-city (conj checkout-shipping-address :city))
(def checkout-shipping-address-state (conj checkout-shipping-address :state))
(def checkout-shipping-address-zip (conj checkout-shipping-address :zipcode))
(def checkout-shipping-address-phone (conj checkout-shipping-address :phone))
(def checkout-credit-card-name (conj checkout :credit-card-name))
(def checkout-credit-card-number (conj checkout :credit-card-number))
(def checkout-credit-card-expiration (conj checkout :credit-card-expiration))
(def checkout-credit-card-ccv (conj checkout :credit-card-ccv))
(def checkout-selected-shipping-method (conj checkout :shipping-method))
(def checkout-selected-shipping-method-sku (conj checkout-selected-shipping-method :sku))
(def checkout-selected-payment-methods (conj checkout :payment-methods))

(def pending-talkable-order (conj ui :pending-talkable-order))

(def flash (conj ui :flash))
(def flash-success (conj flash :success))
(def flash-success-message (conj flash-success :message))
(def flash-success-nav (conj flash-success :navigation))
(def flash-failure (conj flash :failure))
(def flash-failure-message (conj flash-failure :message))
(def flash-failure-nav (conj flash-failure :navigation))

(def billing-address [:billing-address])

(def shipping-address [:shipping-address])

(def stylist [:stylist])

(def stylist-sales-rep-email (conj stylist :sales-rep-email))

(def stylist-manage-account (conj stylist :manage-account))

(def stylist-stats (conj stylist :stats))
(def stylist-stats-previous-payout (conj stylist-stats :previous-payout))
(def stylist-stats-next-payout (conj stylist-stats :next-payout))
(def stylist-stats-lifetime-payouts (conj stylist-stats :lifetime-payouts))

(def stylist-commissions (conj stylist :commissions))
(def stylist-commissions-rate (conj stylist-commissions :rate))
(def stylist-commissions-history (conj stylist-commissions :history))
(def stylist-commissions-page (conj stylist-commissions :page))
(def stylist-commissions-pages (conj stylist-commissions :pages))

(def stylist-bonuses (conj stylist :bonus-credits))
(def stylist-bonuses-award-amount (conj stylist-bonuses :bonus-amount))
(def stylist-bonuses-milestone-amount (conj stylist-bonuses :earning-amount))
(def stylist-bonuses-progress-to-next-bonus (conj stylist-bonuses :progress-to-next-bonus))
(def stylist-bonuses-lifetime-total (conj stylist-bonuses :lifetime-total))
(def stylist-bonuses-history (conj stylist-bonuses :bonuses))
(def stylist-bonuses-page (conj stylist-bonuses :page))
(def stylist-bonuses-pages (conj stylist-bonuses :pages))

(def stylist-referral-program (conj stylist :referral-program))
(def stylist-referral-program-bonus-amount (conj stylist-referral-program :bonus-amount))
(def stylist-referral-program-earning-amount (conj stylist-referral-program :earning-amount))
(def stylist-referral-program-lifetime-total (conj stylist-referral-program :lifetime-total))
(def stylist-referral-program-referrals (conj stylist-referral-program :referrals))
(def stylist-referral-program-page (conj stylist-referral-program :page))
(def stylist-referral-program-pages (conj stylist-referral-program :pages))

(def stylist-referrals (conj stylist :referrals))

(def validation-errors (conj ui :validation-errors))
(def validation-errors-message (conj validation-errors :error-message))
(def validation-errors-details (conj validation-errors :details))

(def errors (conj ui :errors))

(def review-components-count (conj ui :review-components-count))

(def loaded (conj ui :loaded))
(def loaded-reviews (conj loaded :reviews))
(def loaded-stripe (conj loaded :stripe))
(def loaded-places (conj loaded :places))
(def loaded-facebook (conj loaded :facebook))
(def loaded-talkable (conj loaded :talkable))
(def loaded-optimizely (conj loaded :optimizely))

(def facebook-email-denied (conj ui :facebook-email-denied))

(def get-satisfaction-login? [:get-satisfaction-login?])
