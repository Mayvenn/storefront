(ns storefront.events
  #?(:cljs (:require [storefront.macros :refer-macros [defpath]]))
  #?(:clj (:require [storefront.macros :refer [defpath]])))

(defpath app-start)
(defpath app-stop)
(defpath app-restart)

(defpath domain)
(defpath order-completed)

(defpath external-redirect-welcome)
(defpath external-redirect-community)
(defpath external-redirect-paypal-setup)

(defpath navigate)
(defpath navigate-home)
(defpath navigate-category)
(defpath navigate-categories)
(defpath navigate-product)
(defpath navigate-sign-in)
(defpath navigate-getsat-sign-in)
(defpath navigate-sign-up)
(defpath navigate-forgot-password)
(defpath navigate-reset-password)
(defpath navigate-cart)
(defpath navigate-shared-cart)

(defpath navigate-content)
(defpath navigate-content-help)
(defpath navigate-content-about-us)
(defpath navigate-content-guarantee)
(defpath navigate-content-privacy)
(defpath navigate-content-tos)

(defpath navigate-style-guide)
(defpath navigate-style-guide-color)
(defpath navigate-style-guide-buttons)
(defpath navigate-style-guide-spacing)
(defpath navigate-style-guide-form-fields)
(defpath navigate-style-guide-navigation)
(defpath navigate-style-guide-navigation-tab1)
(defpath navigate-style-guide-navigation-tab3)
(defpath navigate-style-guide-progress)
(defpath navigate-style-guide-carousel)

(defpath navigate-order-complete)
(defpath navigate-not-found)
(defpath navigate-shop-by-look)

(defpath navigate-stylist)
(defpath navigate-stylist-dashboard)
(defpath navigate-stylist-dashboard-commissions)
(defpath navigate-stylist-dashboard-referrals)
(defpath navigate-stylist-dashboard-bonus-credit)

(defpath navigate-stylist-account)
(defpath navigate-stylist-account-profile)
(defpath navigate-stylist-account-password)
(defpath navigate-stylist-account-commission)
(defpath navigate-stylist-account-social)

(defpath navigate-account)
(defpath navigate-account-manage)
(defpath navigate-account-referrals)

(defpath navigate-friend-referrals)

(defpath navigate-checkout)
(defpath navigate-checkout-sign-in)
(defpath navigate-checkout-address)
(defpath navigate-checkout-payment)
(defpath navigate-checkout-confirmation)

(defpath apple-pay-availability)

(defpath stripe-success-create-token)
(defpath stripe-failure-create-token)

(defpath control)
(defpath control-change-state)
(defpath control-focus)
(defpath control-blur)

(defpath control-email-captured-dismiss)
(defpath control-email-captured-submit)

(defpath control-stylist-commissions-fetch)
(defpath control-stylist-referrals-fetch)
(defpath control-stylist-bonuses-fetch)

(defpath control-menu-expand)
(defpath control-menu-collapse-all)

(defpath control-sign-in-submit)
(defpath control-sign-out)
(defpath control-sign-up-submit)
(defpath control-forgot-password-submit)
(defpath control-reset-password-submit)
(defpath control-account-profile-submit)

(defpath control-facebook-sign-in)
(defpath control-facebook-reset)

(defpath control-bundle-option-select)

(defpath control-browse-variant)
(defpath control-add-to-bag)

(defpath control-create-order-from-shared-cart)

(defpath control-cart-update-coupon)
(defpath control-cart-line-item-inc)
(defpath control-cart-line-item-dec)
(defpath control-cart-remove)
(defpath control-cart-share-show)
(defpath control-essence-offer-details)

(defpath control-popup-hide)
(defpath control-popup-show-refer-stylists)
(defpath control-popup-ugc-category)

(defpath control-counter-inc)
(defpath control-counter-dec)

(defpath control-checkout-as-guest-submit)
(defpath control-checkout-cart-submit)
(defpath control-checkout-cart-paypal-setup)
(defpath control-checkout-update-addresses-submit)
(defpath control-checkout-shipping-method-select)
(defpath control-checkout-payment-method-submit)
(defpath control-checkout-remove-promotion)
(defpath control-checkout-confirmation-submit)

(defpath control-stylist-referral-submit)
(defpath control-stylist-referral-remove)
(defpath control-stylist-referral-add-another)

(defpath control-stylist-banner-close)

(defpath control-stylist-account-photo-pick)
(defpath control-stylist-account-profile-submit)
(defpath control-stylist-account-password-submit)
(defpath control-stylist-account-commission-submit)
(defpath control-stylist-account-social-submit)

(defpath control-commission-order-expand)

(defpath control-play-video)

(defpath api)
(defpath api-start)
(defpath api-end)
(defpath api-abort)

(defpath api-success)
(defpath api-success-cache)

(defpath api-success-products)
(defpath api-success-states)

(defpath api-success-auth)
(defpath api-success-auth-sign-in)
(defpath api-success-auth-sign-up)
(defpath api-success-auth-reset-password)

(defpath api-success-forgot-password)

(defpath api-success-account)
(defpath api-success-manage-account)
(defpath api-success-stylist-account)
(defpath api-success-stylist-account-photo)
(defpath api-success-stylist-account-profile)
(defpath api-success-stylist-account-password)
(defpath api-success-stylist-account-commission)
(defpath api-success-stylist-account-social)
(defpath api-success-stylist-stats)
(defpath api-success-stylist-commissions)
(defpath api-success-stylist-bonus-credits)
(defpath api-success-stylist-referral-program)
(defpath api-success-send-stylist-referrals)
(defpath api-partial-success-send-stylist-referrals)

(defpath api-success-add-to-bag)
(defpath api-success-remove-from-bag)
(defpath api-success-get-order)
(defpath api-success-get-completed-order)
(defpath api-success-get-saved-cards)
(defpath api-success-sms-number)

(defpath api-success-shared-cart)

(defpath api-success-update-order)
(defpath api-success-update-order-from-shared-cart)
(defpath api-success-update-order-update-address)
(defpath api-success-update-order-update-guest-address)
(defpath api-success-update-order-update-cart-payments)
(defpath api-success-update-order-update-shipping-method)
(defpath api-success-update-order-modify-promotion-code)
(defpath api-success-update-order-add-promotion-code)
(defpath api-success-update-order-remove-promotion-code)
(defpath api-success-update-order-place-order)
(defpath api-success-promotions)

(defpath api-success-shipping-methods)

(defpath api-success-get-static-content)

(defpath api-failure)
(defpath api-failure-no-network-connectivity)
(defpath api-failure-bad-server-response)
(defpath api-failure-errors)
(defpath api-failure-pending-promo-code)
(defpath api-failure-stylist-account-photo-too-large)
(defpath api-failure-order-not-created-from-shared-cart)

(defpath flash)
(defpath flash-show)
(defpath flash-dismiss)
(defpath flash-show-success)
(defpath flash-show-failure)

(defpath referral-thank-you-show)
(defpath referral-thank-you-hide)

(defpath added-to-bag)

(defpath facebook)
(defpath facebook-success-sign-in)
(defpath facebook-success-reset)
(defpath facebook-failure-sign-in)
(defpath facebook-email-denied)

(defpath convert)
(defpath enable-feature)

(defpath reviews)
(defpath reviews-component-mounted)
(defpath reviews-component-will-unmount)

(defpath talkable)
(defpath talkable-offer-shown)

(defpath inserted)
(defpath inserted-places)
(defpath inserted-facebook)
(defpath inserted-stripe)
(defpath inserted-reviews)
(defpath inserted-talkable)
(defpath inserted-convert)

(defpath autocomplete-update-address)

(defpath checkout-address)
(defpath checkout-address-place-changed)
(defpath checkout-address-component-mounted)

(defpath pixlee-api-success-fetch-mosaic)
(defpath pixlee-api-success-fetch-named-search-album-ids)
(defpath pixlee-api-success-fetch-named-search-album)

(defpath video-component-mounted)
(defpath video-component-unmounted)
(defpath show-email-popup)
