(ns storefront.keypaths
  (:require [clojure.string :as string]))

(def cookie [:cookie])
(def scheme [:scheme])
(def modules [:modules])

(def cms [:cms])
(def cms-ugc-collection (conj cms :ugc-collection))
(def cms-ugc-collection-all-looks (conj cms-ugc-collection :all-looks))
(def cms-homepage (conj cms :homepage))
(def cms-homepage-hero (conj cms-homepage :hero))
(def cms-advertised-promo (conj cms :advertisedPromo))
(def cms-advertised-promo-text (conj cms-advertised-promo :advertised-text))
(def cms-advertised-promo-uri (conj cms-advertised-promo :uri))

(def cms-faq (conj cms :faq))

(def features [:features])
(def welcome-url [:welcome-url])

(def session-id [:session-id])

(def app-version [:app-version])

(def user [:user])
(def user-email (conj user :email))
(def user-token (conj user :user-token))
(def user-store-slug (conj user :store-slug))
(def user-store-id (conj user :store-id))
(def user-verified-at (conj user :verified-at))
(def user-stylist-experience (conj user :stylist-experience))
(def user-stylist-service-menu (conj user :service-menu))
(def user-stylist-offered-services (conj user :offered-services))
(def user-stylist-portrait (conj user :stylist-portrait))
(def user-stylist-gallery-images (conj user :stylist-gallery-images))
(def user-stylist-gallery-posts (conj user :stylist-gallery-posts))
(def user-stylist-gallery-initial-posts-ordering (conj user :stylist-gallery-initial-posts-ordering))
(def user-stylist-gallery-new-posts-ordering (conj user :stylist-gallery-new-posts-ordering))

(def user-id (conj user :id))
(def user-must-set-password (conj user :must-set-password))
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
(def order-cart-payments-quadpay (conj order-cart-payments :quadpay))
(def order-shipping-address (conj order :shipping-address))
(def order-token (conj order :token))
(def order-number (conj order :number))
(def order-servicing-stylist-id (conj order :servicing-stylist-id))
(def order-user (conj order :user))
(def order-user-id (conj order-user :id))
(def order-user-email (conj order-user :email))
(def order-phone-marketing-opt-in (conj order :phone-marketing-opt-in))

(def stripe-card-element [:stripe :card-element])

(def completed-order [:completed-order])
(def completed-order-number (conj completed-order :number))
(def completed-order-token (conj completed-order :token))
(def completed-order-servicing-stylist-id (conj completed-order :servicing-stylist-id))

(def order-history [:order-history])

(def pending-promo-code [:pending-promo-code])
(def promotions [:promotions])

(def store [:store])
(def store-slug (conj store :store-slug))
(def store-nickname (conj store :store-nickname))
(def store-stylist-id (conj store :stylist-id))
(def store-gallery-images (conj store :gallery :images))
(def store-features (conj store :features))
(def store-experience (conj store :experience))
(def store-service-menu (conj store :service-menu))

(def catalog [:catalog])
(def v2-products (conj catalog :products))
(def v2-skus (conj catalog :skus))
(def v2-facets (conj catalog :facets))
(def v2-images (conj catalog :images))

;; TODO: have this become the singular shared carts db (including look details &
;; the /c/:shared-cart-id/cart page)
(def v1-looks-shared-carts (conj catalog :looks-shared-carts))

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

(def api-requests (conj ui :api-requests))
(def redirecting? (conj ui :redirecting?))
(def stringer-browser-id (conj ui :stringer-browser-id))
(def kustomer-conversation-id (conj ui :kustomer-conversation-id))
(def return-navigation-message (conj ui :return-navigation-message))
(def navigation-uri (conj ui :navigation-uri))
(def navigation-message (conj ui :navigation-message))
(def navigation-event (conj navigation-message 0))
(def navigation-args (conj navigation-message 1))
(def navigation-query-params (conj navigation-args :query-params))
(def navigation-stashed-stack-item (conj ui :navigation-stashed-stack-item))
(def navigation-undo-stack (conj ui :navigation-undo-stack))
(def navigation-redo-stack (conj ui :navigation-redo-stack))
(def browse-variant-quantity (conj ui :browse-variant-quantity))
(def browse-sku-quantity (conj ui :browse-sku-quantity))
(def menu-expanded (conj ui :menu-expanded))
(def account-menu-expanded (conj ui :account-menu-expanded))
(def store-info-expanded (conj ui :store-info-expanded))
(def shop-a-la-carte-menu-expanded (conj ui :shop-a-la-carte-menu-expanded))
(def shop-looks-menu-expanded (conj ui :shop-looks-menu-expanded))
(def shop-bundle-sets-menu-expanded (conj ui :shop-bundle-sets-menu-expanded))
(def flyout-stuck-open? (conj ui :flyout-stuck-open?))
(def popup (conj ui :popup))
(def header-menus #{account-menu-expanded
                    shop-a-la-carte-menu-expanded
                    shop-looks-menu-expanded
                    store-info-expanded
                    shop-bundle-sets-menu-expanded})
(def menus (conj header-menus menu-expanded))

(def expanded-commission-order-id (conj ui :expanded-commission-order-id))

(def product-details (conj ui :product-details))
(def product-details-url-sku-code (conj product-details :url-sku-code))
(def product-details-information-tab (conj product-details :information-tab))

(def phone-capture-session (conj ui :phone-capture-session))

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

(def force-set-password (conj ui :force-set-password))
(def force-set-password-password (conj force-set-password :password))

(def manage-account (conj ui :manage-account))
(def manage-account-email (conj manage-account :email))
(def manage-account-password (conj manage-account :password))

(def shared-cart (conj ui :shared-cart))
(def shared-cart-current (conj shared-cart :current))
(def shared-cart-creator (conj shared-cart :creator))
(def shared-cart-url (conj shared-cart :url))
(def shared-cart-id (conj shared-cart :id))
(def shared-cart-redirect (conj shared-cart :redirect))

(def selected-album-keyword (conj ui :selected-album-keyword))
(def selected-look-id (conj ui :selected-look-id))

(def cart (conj ui :cart))
(def cart-coupon-code (conj cart :coupon-code))

(def cart-paypal-redirect (conj cart :paypal-redirect))

(def cart-recently-added-skus (conj cart :recently-added-skus))

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
(def checkout-phone-marketing-opt-in (conj checkout :phone-marketing-opt-in))

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

(def stylist-gallery (conj ui :gallery))
(def stylist-gallery-reorder-mode (conj stylist-gallery :reorder-mode))
(def stylist-gallery-currently-dragging-post (conj stylist-gallery :currently-dragging-post))

(def billing-address [:billing-address])

(def shipping-address [:shipping-address])

(def stylist [:stylist])

(def stylist-service-menu (conj stylist :service-menu))

(def stylist-manage-account (conj stylist :manage-account))
(def stylist-manage-account-chosen-payout-method (conj stylist-manage-account :chosen-payout-method))
(def stylist-manage-account-green-dot-payout-attributes (conj stylist-manage-account :green-dot-payout-attributes))
(def stylist-manage-account-green-dot-card-selected-id (conj stylist-manage-account :green-dot :card-selected-id))
(def stylist-portrait-status (conj stylist-manage-account :portrait :status))
(def stylist-portrait-url    (conj stylist-manage-account :portrait :resizable-url))

(def stylist-payout-stats (conj stylist :payout-stats))
(def stylist-payout-stats-previous-payout (conj stylist-payout-stats :previous-payout))
(def stylist-payout-stats-next-payout (conj stylist-payout-stats :next-payout))
(def stylist-payout-stats-lifetime-stats (conj stylist-payout-stats :lifetime-stats))
(def stylist-payout-stats-initiated-payout (conj stylist-payout-stats :initiated-payout))

(def stylist-earnings (conj stylist :earnings))
(def stylist-earnings-balance-transfers (conj stylist-earnings :balance-transfers))
(def stylist-earnings-balance-transfer-details-id (conj stylist-earnings :balance-transfer-details-id))

(def stylist-cash-out-status-id (conj stylist :cash-out-stylist-id))
(def stylist-cash-out-balance-transfer-id (conj stylist :cash-out-balance-transfer-id))

(def current-category-id (conj ui :current-category-id))
(def current-traverse-nav (conj ui :current-traverse-nav))
(def current-traverse-nav-id (conj current-traverse-nav :id))
(def current-traverse-nav-menu-type (conj current-traverse-nav :menu-type))

(def editing-gallery? (conj ui :editing-gallery))
(def hide-header? (conj ui :hide-header?))

(def errors (conj ui :errors))
(def field-errors (conj errors :field-errors))
(def error-message (conj errors :error-message))
(def error-code (conj errors :error-code))

(def loaded (conj ui :loaded))
(def loaded-convert (conj loaded :convert))
(def loaded-facebook (conj loaded :facebook))
(def loaded-google-maps (conj loaded :google-maps))
(def loaded-stripe (conj loaded :stripe))
(def loaded-quadpay (conj loaded :quadpay))
(def loaded-uploadcare (conj loaded :uploadcare))

(def loaded-kustomer (conj loaded :kustomer))
;; Kustomer integration requires to be called and completed after tag insertion
(def started-kustomer (conj ui :started :kustomer))

(def loaded-muuri (conj loaded :muuri))
(def initialize-muuri (conj loaded :initialize-muuri))

(def facebook-email-denied (conj ui :facebook-email-denied))

(def footer-email-submitted [:footer :email :submitted])
(def footer-email-ready [:footer :email :ready])
(def footer-email-value [:footer :email :value])

(def experiments [:experiments])
(def experiments-manual (conj experiments :manual))
(def experiments-bucketed (conj experiments :bucketed))

(def environment [:environment])

(def fvlanding (conj ui :fvlanding))

(def v2-root [:v2])

(def v2-ui (conj v2-root :ui))
(def v2-db (conj v2-root :db))

(def v2-ui-dashboard (conj v2-ui :dashboard))
(def v2-ui-dashboard-cash-balance-section-expanded? (conj v2-ui-dashboard :cash-balance-section-expanded?))
(def v2-ui-dashboard-store-credit-section-expanded? (conj v2-ui-dashboard :store-credit-section-expanded?))

(def v2-dashboard (conj v2-db :dashboard))
(def v2-dashboard-stats (conj v2-dashboard :stats))
(def v2-dashboard-sales (conj v2-dashboard :sales))
(def v2-dashboard-sales-current-sale-id (conj v2-dashboard-sales :current-sale-id))
(def v2-dashboard-sales-elements (conj v2-dashboard-sales :elements))
(def v2-dashboard-sales-pagination (conj v2-dashboard-sales :pagination))
(def v2-dashboard-sales-pagination-page (conj v2-dashboard-sales-pagination :page))
(def v2-dashboard-sales-pagination-ordering (conj v2-dashboard-sales-pagination :ordering))
(def v2-dashboard-balance-transfers (conj v2-dashboard :balance-transfers))
(def v2-dashboard-balance-transfers-current-sale-id (conj v2-dashboard-balance-transfers :current-sale-id))
(def v2-dashboard-balance-transfers-elements (conj v2-dashboard-balance-transfers :elements))
(def v2-dashboard-balance-transfers-pagination (conj v2-dashboard-balance-transfers :pagination))
(def v2-dashboard-balance-transfers-pagination-page (conj v2-dashboard-balance-transfers-pagination :page))
(def v2-dashboard-balance-transfers-pagination-ordering (conj v2-dashboard-balance-transfers-pagination :ordering))
(def v2-dashboard-balance-transfers-voucher-popup-visible? (conj v2-dashboard-balance-transfers :voucher-popup-visible?))

(def accordion (conj ui :accordion))

(def faq (conj ui :faq))
(def faq-expanded-section (conj faq :expanded-section))

(def spreedly-frame [:spreedly-frame])

(defn ->str [keypath]
  (string/join "-" (map name keypath)))

(defn ->component-str [keypath]
  (string/replace (->str keypath) #"^navigate-" "page-"))

(def promo-code-entry-open? (conj ui :promo-code-entry-open?))

(def models-root [:models])
(def models-stylists (conj models-root :stylists))
(def models-products (conj models-root :products))
(def models-progressions (conj models-root :progressions))
(def models-questionings (conj models-root :questionings))
(def models-looks-suggestions (conj models-root :looks-suggestions))
(def models-look-selected (conj models-root :look-selected))
(def models-wait (conj models-root :wait))
(def models-follows (conj models-root :follows))
(def models-live-help (conj models-root :live-help))

(def slideout-nav (conj ui :slideout-nav))
(def slideout-nav-selected-tab (conj slideout-nav :selected-tab))
