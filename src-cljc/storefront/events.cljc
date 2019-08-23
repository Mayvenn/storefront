(ns ^:figwheel-always storefront.events
  (:require [storefront.macros :refer [defpath]]))

(defpath app-start)
(defpath app-stop)
(defpath app-restart)

(defpath domain)
(defpath order-completed)
(defpath order-placed)

(defpath order-remove-promotion)

(defpath external-redirect-welcome)
(defpath external-redirect-freeinstall)
(defpath initiate-redirect-freeinstall-from-menu)
(defpath external-redirect-sms)
(defpath external-redirect-paypal-setup)
(defpath external-redirect-telligent)
(defpath external-redirect-quadpay-checkout)
(defpath external-redirect-google-maps)
(defpath external-redirect-phone)
(defpath external-redirect-typeform-recommend-stylist)

(defpath module-loaded)

(defpath redirect)
(defpath navigate)

(defpath navigate-home)

(defpath navigate-info)
(defpath navigate-info-certified-stylists)
(defpath navigate-info-about-our-hair)
(defpath navigate-info-how-it-works)

(defpath navigate-category)
(defpath navigate-category-update)
(defpath navigate-legacy-product-page)
(defpath navigate-legacy-named-search)
(defpath navigate-legacy-ugc-named-search)
(defpath navigate-product-details)
(defpath navigate-sign-in)
(defpath navigate-sign-out)
(defpath navigate-sign-up)
(defpath navigate-forgot-password)
(defpath navigate-reset-password)
(defpath navigate-force-set-password)
(defpath navigate-cart)
(defpath navigate-shared-cart)
(defpath navigate-store-gallery)
(defpath navigate-gallery-edit)
(defpath navigate-gallery-image-picker)

(defpath navigate-content)
(defpath navigate-content-help)
(defpath navigate-content-about-us)
(defpath navigate-content-guarantee)
(defpath navigate-content-privacy)
(defpath navigate-content-tos)
(defpath navigate-content-ugc-usage-terms)
(defpath navigate-content-voucher-terms)
(defpath navigate-content-program-terms)
(defpath navigate-content-our-hair)

(defpath navigate-design-system)
(defpath navigate-design-system-color)
(defpath navigate-design-system-buttons)
(defpath navigate-design-system-spacing)
(defpath navigate-design-system-form-fields)
(defpath navigate-design-system-navigation)
(defpath navigate-design-system-navigation-tab1)
(defpath navigate-design-system-navigation-tab3)
(defpath navigate-design-system-progress)
(defpath navigate-design-system-carousel)
(defpath navigate-design-system-classic)
(defpath navigate-design-system-adventure)
(defpath navigate-design-system-ui)

(defpath navigate-order-complete)
(defpath navigate-need-match-order-complete)
(defpath navigate-not-found)
(defpath navigate-shop-by-look)
(defpath navigate-shop-by-look-details)

(defpath navigate-stylist)
(defpath navigate-stylist-dashboard)
(defpath navigate-stylist-dashboard-earnings)
(defpath navigate-stylist-dashboard-order-details)
(defpath navigate-stylist-dashboard-referrals)
(defpath navigate-stylist-dashboard-bonus-credit)
(defpath navigate-stylist-dashboard-cash-out-begin)
(defpath navigate-stylist-dashboard-cash-out-pending)
(defpath navigate-stylist-dashboard-cash-out-success)
(defpath navigate-stylist-dashboard-balance-transfer-details)

(defpath navigate-stylist-share-your-store)

(defpath navigate-stylist-account)
(defpath navigate-stylist-account-profile)
(defpath navigate-stylist-account-portrait)
(defpath navigate-stylist-account-password)
(defpath navigate-stylist-account-payout)
(defpath navigate-stylist-account-social)

(defpath navigate-voucher)
(defpath navigate-voucher-redeem)
(defpath navigate-voucher-redeemed)

(defpath navigate-account)
(defpath navigate-account-manage)
(defpath navigate-account-referrals)

(defpath navigate-friend-referrals)
(defpath navigate-friend-referrals-freeinstall)

(defpath navigate-checkout)
(defpath navigate-checkout-sign-in)
(defpath navigate-checkout-returning-or-guest)
(defpath navigate-checkout-address)
(defpath navigate-checkout-payment)
(defpath navigate-checkout-confirmation)
(defpath navigate-checkout-processing)
(defpath navigate-adventure-checkout-wait)

(defpath navigate-mayvenn-made)

(defpath menu-traverse-root)
(defpath menu-traverse-ascend)
(defpath menu-traverse-descend)
(defpath menu-traverse-out)

(defpath menu-home)
(defpath menu-list)

(defpath navigation-save)
(defpath navigation-undo)
(defpath navigation-redo)
(defpath stash-nav-stack-item)
(defpath control-navigate)
(defpath browser-navigate)

(defpath stripe)
(defpath stripe-success-create-token)
(defpath stripe-failure-create-token)
(defpath stripe-component-mounted)
(defpath stripe-component-will-unmount)

(defpath ensure-sku-ids)

(defpath control-stylist-gallery-open)
(defpath control-stylist-gallery-close)

(defpath control-freeinstall-ugc-modal-open)
(defpath control-freeinstall-ugc-modal-close)

(defpath control)
(defpath control-change-state)
(defpath control-focus)
(defpath control-blur)

(defpath control-email-captured)
(defpath control-email-captured-dismiss)
(defpath control-email-captured-submit)

(defpath control-design-system-popup-show)
(defpath control-design-system-popup-dismiss)

(defpath control-free-install)
(defpath control-free-install-shop-looks)

(defpath control-install-landing-page-look-back)
(defpath control-install-landing-page-toggle-accordion)
(defpath control-install-consult-stylist-sms)

(defpath control-stylist-community)
(defpath poll-stylist-portrait)
(defpath poll-gallery)
(defpath control-cancel-editing-gallery)
(defpath control-edit-gallery)
(defpath control-delete-gallery-image)

(defpath control-voucher-redeem)
(defpath control-voucher-scan)
(defpath control-voucher-qr-redeem)

(defpath control-menu-expand)
(defpath control-menu-collapse-all)

(defpath control-menu-expand-hamburger)
(defpath control-mayvenn-made-hero-clicked)

(defpath control-sign-in-submit)
(defpath control-sign-out)
(defpath control-sign-up-submit)
(defpath control-forgot-password-submit)
(defpath control-reset-password-submit)
(defpath control-force-set-password-submit)
(defpath control-account-profile-submit)

(defpath control-facebook-sign-in)
(defpath control-facebook-reset)

(defpath control-browse-variant)
(defpath control-add-to-bag)

(defpath control-add-sku-to-bag)
(defpath control-suggested-add-to-bag)

(defpath control-create-order-from-shared-cart)

(defpath control-cart-add-freeinstall-coupon)
(defpath control-cart-update-coupon)
(defpath control-cart-line-item-inc)
(defpath control-cart-line-item-dec)
(defpath control-cart-remove)
(defpath control-cart-share-show)

(defpath control-popup-hide)

(defpath control-counter-inc)
(defpath control-counter-dec)

(defpath control-checkout-cart-submit)
(defpath control-checkout-cart-paypal-setup)
(defpath control-checkout-update-addresses-submit)
(defpath control-checkout-shipping-method-select)
(defpath control-checkout-payment-method-submit)
(defpath control-checkout-choose-payment-method-submit)
(defpath control-checkout-payment-select)
(defpath control-checkout-remove-promotion)
(defpath control-checkout-confirmation-submit)
(defpath control-checkout-quadpay-confirmation-submit)

(defpath control-stylist-account-profile-submit)
(defpath control-stylist-account-password-submit)
(defpath control-stylist-account-commission-submit)
(defpath control-stylist-account-social-submit)

(defpath control-stylist-dashboard-cash-out-begin)
(defpath control-stylist-dashboard-cash-out-commit)

(defpath control-commission-order-expand)

(defpath control-category-panel-open)
(defpath control-category-panel-close)

(defpath control-category-option)
(defpath control-category-option-select)
(defpath control-category-option-unselect)
(defpath control-category-option-clear)

(defpath control-product-detail-picker-open)
(defpath control-product-detail-picker-close)
(defpath control-product-detail-picker-option-select)
(defpath control-product-detail-picker-option-quantity-select)

(defpath video-played)

(defpath snap)

(defpath api)
(defpath api-start)
(defpath api-end)
(defpath api-abort)

(defpath api-success)
(defpath api-success-cache)

(defpath api-success-states)

(defpath api-success-v2-products)
(defpath api-success-v2-products-for-nav)
(defpath api-success-v2-products-for-browse)
(defpath api-success-v2-products-for-details)

(defpath api-success-facets)

(defpath api-success-one-time-auth)
(defpath api-success-auth)
(defpath api-success-auth-sign-in)
(defpath api-success-auth-sign-up)
(defpath api-success-auth-reset-password)

(defpath api-success-forgot-password)

(defpath api-success-account)
(defpath api-success-manage-account)
(defpath api-success-force-set-password)
(defpath api-success-stylist-account)
(defpath api-success-stylist-account-portrait)
(defpath api-success-stylist-account-profile)
(defpath api-success-stylist-account-password)
(defpath api-success-stylist-account-commission)
(defpath api-success-stylist-account-social)
(defpath api-success-stylist-balance-transfer-details)
(defpath api-success-stylist-next-payout)
(defpath api-success-stylist-payout-stats)
(defpath api-success-stylist-payout-stats-cash-out)
(defpath api-success-cash-out-commit)
(defpath api-success-cash-out-status)
(defpath api-success-cash-out-failed)
(defpath api-success-cash-out-complete)
(defpath api-success-stylist-referral-program)
(defpath api-success-user-stylist-service-menu-fetch)
(defpath api-success-send-stylist-referrals)
(defpath api-partial-success-send-stylist-referrals)

(defpath api-success-fetch-geocode)
(defpath api-failure-fetch-geocode)
(defpath api-fetch-stylists-within-radius-pre-purchase)
(defpath api-fetch-stylists-within-radius-post-purchase)
(defpath api-success-fetch-stylists-within-radius)
(defpath api-success-fetch-stylists-within-radius-pre-purchase)
(defpath api-success-fetch-stylists-within-radius-post-purchase)

(defpath api-success-fetch-cms-data)

(defpath api-success-add-to-bag)
(defpath api-success-add-sku-to-bag)
(defpath api-success-suggested-add-to-bag)
(defpath api-success-remove-from-bag)
(defpath api-success-get-order)
(defpath api-success-get-completed-order)
(defpath api-success-get-saved-cards)
(defpath api-success-sms-number)

(defpath api-success-shared-cart-create)
(defpath api-success-shared-cart-fetch)

(defpath api-success-assign-servicing-stylist)
(defpath api-success-assign-servicing-stylist-pre-purchase)
(defpath api-success-assign-servicing-stylist-post-purchase)

(defpath api-success-update-order)
(defpath api-success-update-order-from-shared-cart)
(defpath api-success-update-order-update-address)
(defpath api-success-update-order-update-guest-address)
(defpath api-success-update-order-update-cart-payments)
(defpath api-success-update-order-update-shipping-method)
(defpath api-success-update-order-add-promotion-code)
(defpath api-success-update-order-remove-promotion-code)
(defpath api-success-update-order-place-order)
(defpath api-success-update-order-proceed-to-quadpay)
(defpath api-success-promotions)

(defpath api-success-store-gallery-fetch)

(defpath api-success-stylist-gallery)
(defpath api-success-stylist-gallery-fetch)
(defpath api-success-stylist-gallery-append)
(defpath api-success-stylist-gallery-delete)

(defpath api-success-shipping-methods)

(defpath api-success-get-static-content)

(defpath api-success-telligent-login)

(defpath api-success-voucher-redemption)

(defpath api-failure)
(defpath api-failure-no-network-connectivity)
(defpath api-failure-bad-server-response)
(defpath api-failure-errors)
(defpath api-failure-errors-invalid-promo-code)
(defpath api-failure-pending-promo-code)
(defpath api-failure-order-not-created-from-shared-cart)

(defpath flash)
(defpath flash-show)
(defpath flash-dismiss)
(defpath flash-show-success)
(defpath flash-show-failure)
(defpath flash-later-show-success)
(defpath flash-later-show-failure)

(defpath referral-thank-you-show)
(defpath referral-thank-you-hide)

(defpath added-to-bag)

(defpath save-order)
(defpath clear-order)

(defpath facebook)
(defpath facebook-success-sign-in)
(defpath facebook-success-reset)
(defpath facebook-failure-sign-in)
(defpath facebook-email-denied)

(defpath convert)
(defpath enable-feature)
(defpath bucketed-for)

(defpath reviews)
(defpath reviews-component-mounted)
(defpath reviews-component-will-unmount)

(defpath talkable)
(defpath talkable-offer-shown)

(defpath inserted)
(defpath inserted-stringer)
(defpath inserted-convert)
(defpath inserted-facebook)
(defpath inserted-pixlee)
(defpath inserted-google-maps)
(defpath inserted-quadpay)
(defpath inserted-stripe)
(defpath inserted-talkable)
(defpath inserted-uploadcare)

(defpath stringer-distinct-id-available)

(defpath autocomplete-update-address)

(defpath checkout-address)
(defpath checkout-address-place-changed)
(defpath checkout-address-component-mounted)

(defpath contentful-api-success-fetch-homepage)

(defpath uploadcare-api-success-upload-portrait)
(defpath uploadcare-api-success-upload-gallery)

(defpath uploadcare-api-failure)

(defpath mayvenn-made-gallery-displayed)

(defpath video-component-mounted)
(defpath video-component-unmounted)

(defpath popup-hide)
(defpath popup-show)
(defpath popup-show-email-capture)
(defpath popup-show-free-install)

(defpath image-picker-component-mounted)
(defpath image-picker-component-will-unmount)

(defpath sign-out)

(defpath viewed-sku)

(defpath determine-and-show-popup)

(defpath faq-section-selected)

(defpath voucherify-api-failure)

(defpath voucher-camera-permission-denied)

(defpath browser-fullscreen-enter)
(defpath browser-fullscreen-exit)

;;SECTION v2

;;SECTION v2 popup
(defpath popup-show-v2-homepage)

;;SECTION v2 Control
(defpath control-v2-homepage-popup-dismiss)
(defpath control-v2-stylist-dashboard-section-toggle)
(defpath control-v2-stylist-dashboard-sales-load-more)
(defpath control-v2-stylist-dashboard-balance-transfers-load-more)
(defpath control-v2-stylist-dashboard-balance-transfers-voucher-popup-set-visible)

;;SECTION v2 Api
;; ;; REQUESTS
(defpath v2-stylist-dashboard-stats-fetch)
(defpath v2-stylist-dashboard-balance-transfers-fetch)
(defpath v2-stylist-dashboard-sales-fetch)

;; ;; SUCCESS
(defpath api-success-v2-stylist-dashboard-stats)
(defpath api-success-v2-stylist-dashboard-balance-transfers)
(defpath api-success-v2-stylist-dashboard-sales)
(defpath api-success-v2-stylist-dashboard-sale)

;;SECTION v2 Navigate
(defpath navigate-v2-stylist-dashboard)
(defpath navigate-v2-stylist-dashboard-payments)
(defpath navigate-v2-stylist-dashboard-orders)

;; CUSTOMER
(defpath navigate-customer-orders)

(defpath initialize-product-details)

;; ADVENTURE
(defpath navigate-adventure)
(defpath navigate-adventure-home)
(defpath navigate-adventure-match-stylist)
(defpath navigate-adventure-find-your-stylist)
(defpath navigate-adventure-shop-hair)
(defpath navigate-adventure-how-shop-hair)
(defpath navigate-adventure-hair-texture)
(defpath navigate-adventure-bundlesets-hair-texture)
(defpath navigate-adventure-a-la-carte-hair-texture)
(defpath navigate-adventure-a-la-carte-hair-color)
(defpath navigate-adventure-a-la-carte-product-list)
(defpath navigate-adventure-select-new-look)
(defpath navigate-adventure-look-detail)
(defpath navigate-adventure-matching-stylist-wait)
(defpath navigate-adventure-matching-stylist-wait-pre-purchase)
(defpath navigate-adventure-matching-stylist-wait-post-purchase)
(defpath navigate-adventure-select-bundle-set)
(defpath navigate-adventure-stylist-results)
(defpath navigate-adventure-stylist-results-pre-purchase)
(defpath navigate-adventure-stylist-results-post-purchase)
(defpath navigate-adventure-stylist-gallery)
(defpath navigate-adventure-out-of-area)
(defpath navigate-adventure-match-success)
(defpath navigate-adventure-match-success-pre-purchase)
(defpath navigate-adventure-match-success-post-purchase)
(defpath navigate-adventure-let-mayvenn-match)
(defpath navigate-adventure-stylist-profile)

(defpath navigate-adventure-product-details)

(defpath control-adventure)
(defpath control-adventure-choice)
(defpath control-adventure-select-stylist)
(defpath control-adventure-select-stylist-pre-purchase)
(defpath control-adventure-select-stylist-post-purchase)
(defpath control-adventure-emailcapture-submit)

(defpath adventure-address-component-mounted)
(defpath control-adventure-stylist-salon-address-clicked)
(defpath control-adventure-stylist-phone-clicked)
(defpath control-adventure-stylist-gallery-open)
(defpath control-adventure-stylist-gallery-close)
(defpath clear-selected-location)
(defpath control-adventure-location-submit)

(defpath control-open-shop-escape-hatch)

(defpath popup-show-adventure-free-install)
(defpath popup-show-adventure-emailcapture)
(defpath control-adventure-free-install-dismiss)

(defpath api-success-fetch-matched-stylist)
(defpath api-success-fetch-stylist-details)
(defpath api-failure-fetch-stylist-details)
(defpath adventure-stylist-search-results-displayed)
(defpath adventure-stylist-search-results-post-purchase-displayed)

(defpath adventure-visitor-identified)

(defpath adventure-fetch-matched-skus)
(defpath api-success-adventure-fetch-skus)
(defpath adventure-fetch-matched-products)
(defpath api-success-adventure-fetch-products)

(defpath adventure-clear-servicing-stylist)
(defpath api-success-adventure-cleared-servicing-stylist)

(defpath api-shipping-address-geo-lookup)
(defpath api-success-shipping-address-geo-lookup)
(defpath api-failure-shipping-address-geo-lookup)

;; Stripe Payment Button
(defpath stripe-payment-request-button-inserted)
(defpath stripe-payment-request-button-removed)


;; Spreedly
(defpath loaded-spreedly)
(defpath spreedly-did-mount)
(defpath spreedly-did-unmount)
(defpath spreedly-frame-initialized)
(defpath spreedly-frame-tokenized)

(defpath escape-key-pressed)
