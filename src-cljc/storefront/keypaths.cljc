(ns storefront.keypaths
  (:require [clojure.string :as string]))

(def cookie [:cookie])

(def features [:features])
(def welcome-url [:welcome-url])
(def telligent-community-url [:telligent-community-url])

(def session-id [:session-id])

(def app-version [:app-version])

(def user [:user])
(def user-email (conj user :email))
(def user-token (conj user :user-token))
(def user-store-slug (conj user :store-slug))
(def user-id (conj user :id))
(def user-total-available-store-credit (conj user :total-available-store-credit))

(def order [:order])
(def order-total (conj order :total))
(def order-promotion-codes (conj order :promotion-codes))
(def order-cart-payments (conj order :cart-payments))
(def order-cart-payments-store-credit (conj order-cart-payments :store-credit))
(def order-cart-payments-store-credit-amount (conj order-cart-payments-store-credit :amount))
(def order-cart-payments-paypal (conj order-cart-payments :paypal))
(def order-cart-payments-paypal-redirect-url (conj order-cart-payments-paypal :redirect-url))
(def order-cart-payments-stripe (conj order-cart-payments :stripe))
(def order-cart-payments-affirm (conj order-cart-payments :affirm))
(def order-shipping-address (conj order :shipping-address))
(def order-token (conj order :token))
(def order-number (conj order :number))
(def order-user (conj order :user))
(def order-user-id (conj order-user :id))
(def order-user-email (conj order-user :email))

(def stripe-card-element [:stripe :card-element])

(def completed-order [:completed-order])
(def completed-order-number (conj completed-order :number))

(def pending-promo-code [:pending-promo-code])
(def promotions [:promotions])

(def store [:store])
(def store-slug (conj store :store_slug))
(def store-stylist-id (conj store :stylist_id))
(def store-gallery-images (conj store :gallery :images))

(def named-searches [:named-searches])
(def products [:products])
;;TODO use this instead of initial-sku-sets
(def sku-sets [:sku-sets])
(def skus [:skus])

(def db [:db])
(def db-images (conj db :images))

(def categories [:categories])
(def facets [:facets])
(def states [:states])
(def shipping-methods [:shipping-methods])
(def sms-number [:sms-number])
(def api-cache [:api-cache])

(def ugc [:ugc])
(def ugc-images (conj ugc :images))
(def ugc-albums (conj ugc :albums))

(def ui [:ui])
(def ui-focus (conj ui :focus))

(def static (conj ui :static))
(def static-id (conj static :id))
(def static-content (conj static :content))

(def stylist-banner-hidden (conj ui :stylist-banner-hidden?))
(def api-requests (conj ui :api-requests))
(def redirecting? (conj ui :redirecting?))
(def return-navigation-message (conj ui :return-navigation-message))
(def navigation-message (conj ui :navigation-message))
(def navigation-event (conj navigation-message 0))
(def navigation-args (conj navigation-message 1))
(def navigation-stashed-stack-item (conj ui :navigation-stashed-stack-item))
(def navigation-undo-stack (conj ui :navigation-undo-stack))
(def navigation-redo-stack (conj ui :navigation-redo-stack))
(def browse-named-search-query (conj ui :browse-named-search-query))
(def browse-variant-quantity (conj ui :browse-variant-quantity))
(def browse-sku-quantity (conj ui :browse-sku-quantity))
(def browse-recently-added-variants (conj ui :browse-recently-added-variants))
(def browse-recently-added-skus (conj ui :browse-recently-added-skus))
(def saved-bundle-builder-options (conj ui :saved-bundle-builder-options))
(def menu-expanded (conj ui :menu-expanded))
(def account-menu-expanded (conj ui :account-menu-expanded))
(def store-info-expanded (conj ui :store-info-expanded))
(def shop-menu-expanded (conj ui :shop-menu-expanded))
(def popup (conj ui :popup))
(def header-menus #{account-menu-expanded
                    shop-menu-expanded
                    store-info-expanded})
(def menus (conj header-menus menu-expanded))
(def ui-ugc-category-popup-offset (conj ui :popup-ugc-category-offset))

(def expanded-commission-order-id (conj ui :expanded-commission-order-id))

(def bundle-builder (conj ui :bundle-builder))
(def product-details (conj ui :product-details))
(def product-details-url-sku-code (conj product-details :url-sku-code))
(def product-details-sku-set-id (conj product-details :sku-set/id))

(def category-filters-for-nav (conj ui :category-filters :nav))
(def category-filters-for-browse (conj ui :category-filters :browse))

(def captured-email (conj ui :captured-email))

(def account (conj ui :account))
(def account-show-password? (conj account :show-password?))

(def sign-in (conj ui :sign-in))
(def sign-in-email (conj sign-in :email))
(def sign-in-password (conj sign-in :password))

(def sign-up (conj ui :sign-up))
(def sign-up-email (conj sign-up :email))
(def sign-up-password (conj sign-up :password))

(def forgot-password (conj ui :forgot-password))
(def forgot-password-email (conj forgot-password :email))

(def reset-password (conj ui :reset-password))
(def reset-password-password (conj reset-password :password))
(def reset-password-token (conj reset-password :token))

(def manage-account (conj ui :manage-account))
(def manage-account-email (conj manage-account :email))
(def manage-account-password (conj manage-account :password))

(def shared-cart (conj ui :shared-cart))
(def shared-cart-current (conj shared-cart :current))
(def shared-cart-url (conj shared-cart :url))
(def shared-cart-id (conj shared-cart :id))

(def selected-look-id (conj ui :selected-look-id))

(def show-apple-pay? (conj ui :show-apple-pay?))
(def disable-apple-pay-button? (conj ui :disable-apple-pay-button?))

(def cart (conj ui :cart))
(def cart-coupon-code (conj cart :coupon-code))

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
(def checkout-credit-card-save (conj checkout :credit-card-save))
(def checkout-credit-card-existing-cards (conj checkout :credit-card-existing-cards))
(def checkout-credit-card-selected-id (conj checkout :credit-card-selected-id))
(def checkout-selected-shipping-method (conj checkout :shipping-method))
(def checkout-selected-shipping-method-name (conj checkout-selected-shipping-method :name))
(def checkout-selected-shipping-method-sku (conj checkout-selected-shipping-method :sku))
(def checkout-selected-payment-methods (conj checkout :payment-methods))

(def pending-talkable-order (conj ui :pending-talkable-order))

(def flash (conj ui :flash))
(def flash-now (conj flash :now))
(def flash-now-success (conj flash-now :success))
(def flash-now-success-message (conj flash-now-success :message))
(def flash-now-failure (conj flash-now :failure))
(def flash-now-failure-message (conj flash-now-failure :message))
(def flash-later (conj flash :later))
(def flash-later-success (conj flash-later :success))
(def flash-later-success-message (conj flash-later-success :message))
(def flash-later-failure (conj flash-later :failure))
(def flash-later-failure-message (conj flash-later-failure :message))

(def billing-address [:billing-address])

(def shipping-address [:shipping-address])

(def stylist [:stylist])

(def stylist-sales-rep-email (conj stylist :sales-rep-email))

(def stylist-manage-account (conj stylist :manage-account))
(def stylist-manage-account-green-dot-card-selected-id (conj stylist-manage-account :green-dot :card-selected-id))
(def stylist-portrait-status (conj stylist-manage-account :portrait :status))
(def stylist-portrait-url    (conj stylist-manage-account :portrait :resizable_url))

(def stylist-stats (conj stylist :stats))

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

(def current-category-id (conj ui :current-category-id))
(def current-traverse-nav (conj ui :current-traverse-nav))
(def current-traverse-nav-id (conj current-traverse-nav :id))

(def stylist-referrals (conj stylist :referrals))

(def editing-gallery? (conj ui :editing-gallery))

(def errors (conj ui :errors))
(def field-errors (conj errors :field-errors))
(def error-message (conj errors :error-message))
(def error-code (conj errors :error-code))

(def review-components-count (conj ui :review-components-count))

(def affirm-refresh-timeout (concat ui [:affirm :refresh :timeout] ))

(def loaded (conj ui :loaded))
(def loaded-convert (conj loaded :convert))
(def loaded-facebook (conj loaded :facebook))
(def loaded-places (conj loaded :places))
(def loaded-reviews (conj loaded :reviews))
(def loaded-stripe (conj loaded :stripe))
(def loaded-stripe-v2 (conj loaded-stripe :v2))
(def loaded-stripe-v3 (conj loaded-stripe :v3))
(def loaded-talkable (conj loaded :talkable))
(def loaded-uploadcare (conj loaded :uploadcare))

(def facebook-email-denied (conj ui :facebook-email-denied))

(def email-capture-session (conj ui :email-capture-session))

(def experiments [:experiments])
(def experiments-manual (conj experiments :manual))
(def experiments-bucketed (conj experiments :bucketed))

(def environment [:environment])

(defn ->str [keypath]
  (string/join "-" (map name keypath)))

(defn ->component-str [keypath]
  (string/replace (->str keypath) #"^navigate-" "page-"))
