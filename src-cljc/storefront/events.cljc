(ns storefront.events
  (:require [storefront.macros :refer [defpath]]))

(defpath app-start)
(defpath app-stop)
(defpath app-restart)

(defpath domain)
(defpath order-completed)
(defpath order-placed)

(defpath order-remove-promotion)
(defpath order-remove-freeinstall-line-item)

(defpath external-redirect-welcome)
(defpath external-redirect-info-page)
(defpath external-redirect-sms)
(defpath external-redirect-paypal-setup)
(defpath external-redirect-quadpay-checkout)
(defpath external-redirect-google-maps)
(defpath external-redirect-phone)
(defpath external-redirect-typeform-recommend-stylist)

(defpath stringer-browser-identified)
(defpath module-loaded)

(defpath redirect)
(defpath go-to-navigate)
(defpath navigate)

(defpath navigate-home)

(defpath navigate-info)
(defpath navigate-info-certified-stylists)
(defpath navigate-info-about-our-hair)

(defpath navigate-category)
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
(defpath navigate-added-to-cart)
(defpath navigate-shared-cart)
(defpath navigate-store-gallery)
(defpath navigate-gallery-edit)
(defpath navigate-gallery-appointments)
(defpath navigate-gallery-image-picker)
(defpath navigate-gallery-photo)

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

(defpath navigate-not-found)
(defpath navigate-shop-by-look)
(defpath navigate-shop-by-look-details)

(defpath navigate-stylist)
(defpath navigate-stylist-dashboard)
(defpath navigate-stylist-dashboard-earnings)
(defpath navigate-stylist-dashboard-order-details)
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

(defpath navigate-checkout)
(defpath navigate-checkout-sign-in)
(defpath navigate-checkout-returning-or-guest)
(defpath navigate-checkout-address)
(defpath navigate-checkout-payment)
(defpath navigate-checkout-confirmation)
(defpath navigate-checkout-processing)
(defpath navigate-checkout-add)

(defpath navigate-about-mayvenn-install)

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
(defpath browser-back)

(defpath stripe)
(defpath stripe-success-create-token)
(defpath stripe-failure-create-token)
(defpath stripe-component-mounted)
(defpath stripe-component-will-unmount)

(defpath ensure-sku-ids)

(defpath control)
(defpath control-change-state)
(defpath control-focus)
(defpath control-blur)

(defpath control-design-system-popup-show)
(defpath control-design-system-popup-dismiss)

(defpath control-install-landing-page-look-back)
(defpath control-install-landing-page-toggle-accordion)
(defpath control-install-consult-stylist-sms)

(defpath poll-stylist-portrait)
(defpath poll-gallery)

(defpath control-show-gallery-photo-delete-modal)
(defpath control-cancel-editing-gallery)
(defpath control-edit-gallery)
(defpath control-delete-gallery-image)

(defpath control-voucher-redeem)
(defpath control-voucher-scan)
(defpath control-voucher-qr-redeem)

(defpath control-menu-expand)
(defpath control-menu-collapse-all)

(defpath control-menu-expand-hamburger)

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

(defpath control-add-sku-to-bag)
(defpath control-suggested-add-to-bag)

(defpath control-create-order-from-customized-look)
(defpath control-create-order-from-look)
(defpath control-initalize-cart-from-look)
(defpath control-shared-cart-checkout-clicked)
(defpath control-shared-cart-paypal-checkout-clicked)
(defpath control-shared-cart-edit-cart-clicked)
(defpath control-shared-cart-pick-your-stylist-clicked)
(defpath clear-shared-cart-redirect)

(defpath control-cart-update-coupon)
(defpath control-cart-line-item-inc)
(defpath control-cart-line-item-dec)
(defpath control-cart-remove)
(defpath control-cart-share-show)
(defpath control-pick-stylist-button)
(defpath control-show-addon-service-menu)
(defpath control-addon-service-menu-dismiss)
(defpath control-addon-checkbox)
(defpath control-change-stylist)
(defpath control-remove-stylist)
(defpath share-stylist)

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
(defpath control-checkout-add-checked)
(defpath control-checkout-add-continued)

(defpath control-footer-email-submit)
(defpath control-footer-email-on-focus)

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

;; OLD PICKER
(defpath control-product-detail-picker-open)
(defpath control-product-detail-picker-close)
(defpath control-product-detail-picker-option-select)
(defpath control-product-detail-picker-option-quantity-select)

;; NEW PICKER
(defpath control-pdp-picker-option-select)
(defpath control-pdp-picker-open)
(defpath control-pdp-picker-close)

(defpath control-look-detail-picker-option-select)
(defpath control-look-detail-picker-open)
(defpath control-look-detail-picker-close)

(defpath initialize-look-details)

(defpath video-played)

(defpath snap)

(defpath debounced-event-initialized)
(defpath debounced-event-enqueued)

(defpath api)
(defpath api-start)
(defpath api-end)
(defpath api-abort)

(defpath api-success)
(defpath api-success-cache)

(defpath api-success-states)

(defpath api-success-v3-products)
(defpath api-success-v3-products-for-nav)
(defpath api-success-v3-products-for-browse)
(defpath api-success-v3-products-for-details)
(defpath api-success-v3-products-for-stylist-filters)
(defpath api-success-get-skus)

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

(defpath stylist-dashboard-cash-out-commit)
(defpath api-success-cash-out-commit)
(defpath api-success-cash-out-status)
(defpath api-success-cash-out-failed)
(defpath api-success-cash-out-complete)
(defpath api-success-user-stylist-service-menu-fetch)
(defpath api-success-user-stylist-offered-services)

(defpath api-success-fetch-stylists-matching-filters)

(defpath api-success-fetch-cms-keypath)

(defpath api-success-decrease-quantity)
(defpath api-success-add-sku-to-bag)
(defpath api-success-suggested-add-to-bag)
(defpath api-success-remove-from-bag)
(defpath api-success-get-order)
(defpath api-success-get-completed-order)
(defpath api-success-get-saved-cards)
(defpath api-success-sms-number)

(defpath api-success-fetch-shared-cart-matched-stylist)
(defpath api-success-shared-cart-create)
(defpath api-success-shared-cart-fetch)
(defpath api-success-shared-carts-fetch)

(defpath shared-cart-error-matched-stylist-not-eligible)
(defpath flow|shared-cart-stylist|resulted)

(defpath api-success-assign-servicing-stylist)

(defpath api-success-update-order)
(defpath api-success-update-order-from-shared-cart)
(defpath api-success-update-order-update-address)
(defpath api-success-update-order-update-guest-address)
(defpath api-success-update-order-update-cart-payments)
(defpath api-success-update-order-update-shipping-method)
(defpath api-success-update-order-add-promotion-code)
(defpath api-success-update-order-add-service-line-item)
(defpath api-success-update-order-remove-promotion-code)
(defpath api-success-update-order-place-order)
(defpath api-success-update-order-proceed-to-quadpay)
(defpath api-success-promotions)

(defpath api-success-store-gallery-fetch)
(defpath api-success-store-gallery-reorder)

(defpath api-success-stylist-gallery)
(defpath api-success-stylist-gallery-fetch)
(defpath api-success-stylist-gallery-append)
(defpath api-success-stylist-gallery-delete)
(defpath api-success-stylist-gallery-fetch-v2)
(defpath api-success-stylist-gallery-delete-v2)
(defpath api-success-stylist-gallery-reorder-v2)

(defpath api-success-shipping-methods)

(defpath api-success-get-static-content)

(defpath api-success-voucher-redemption)

(defpath api-success-shared-stylist)

(defpath api-failure)
(defpath api-failure-no-network-connectivity)
(defpath api-failure-bad-server-response)
(defpath api-failure-errors)
(defpath api-failure-errors-invalid-promo-code)
(defpath api-failure-pending-promo-code)
(defpath api-failure-order-not-created-from-shared-cart)
(defpath api-failure-shared-stylist)
(defpath api-failure-voucher-redemption)
(defpath api-failure-shared-cart)

(defpath flash)
(defpath flash-show)
(defpath flash-dismiss)
(defpath flash-show-success)
(defpath flash-show-failure)
(defpath flash-later-show-success)
(defpath flash-later-show-failure)

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
(defpath clear-features)
(defpath bucketed-for)

(defpath reviews)
(defpath reviews-component-mounted)
(defpath reviews-component-will-unmount)

(defpath inserted)
(defpath inserted-stringer)
(defpath inserted-convert)
(defpath inserted-facebook)
(defpath inserted-google-maps)
(defpath inserted-quadpay)
(defpath inserted-stripe)
(defpath inserted-uploadcare)
(defpath inserted-kustomer)

(defpath did-insert-convert)

(defpath autocomplete-update-address)

(defpath checkout-address)
(defpath checkout-address-place-changed)
(defpath checkout-address-component-mounted)
(defpath checkout-initiated-mayvenn-checkout)
(defpath checkout-initiated-paypal-checkout)
(defpath checkout-order-rejected)
(defpath checkout-order-cleared-for-mayvenn-checkout)
(defpath checkout-order-cleared-for-paypal-checkout)
(defpath checkout-add-flow-completed)

(defpath contentful-api-success-fetch-homepage)

(defpath uploadcare-api-success-upload-portrait)
(defpath uploadcare-api-success-upload-gallery)

(defpath uploadcare-api-failure)

(defpath video-component-mounted)
(defpath video-component-unmounted)

(defpath popup-hide)
(defpath popup-show)
(defpath popup-show-consolidated-cart-free-install)
(defpath control-consolidated-cart-free-install-dismiss)

(defpath popup-show-length-guide)
(defpath popup-hide-length-guide)

(defpath popup-show-wigs-customization)
(defpath control-wigs-customization-dismiss)

(defpath add-sku-to-bag)
(defpath bulk-add-sku-to-bag)
(defpath add-servicing-stylist-and-sku)

(defpath cart-swap-popup-show)
(defpath control-cart-swap-popup-dismiss)
(defpath control-cart-swap-popup-confirm)

(defpath image-picker-component-mounted)
(defpath image-picker-component-will-unmount)

(defpath browse-addon-service-menu-button-enabled)

(defpath sign-out)

(defpath viewed-sku)
(defpath api-success-v2-skus-for-related-addons)

(defpath faq-section-selected)
(defpath tabbed-information-tab-selected)

(defpath voucher-camera-permission-denied)

(defpath browser-fullscreen-enter)
(defpath browser-fullscreen-exit)


;;SECTION v2 Control


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
(defpath navigate-v2-stylist-dashboard-payout-rates)

;; CUSTOMER
(defpath navigate-customer-orders)

(defpath initialize-product-details)

;; ADVENTURE
(defpath navigate-adventure)
(defpath navigate-adventure-find-your-stylist)
(defpath navigate-adventure-top-stylist)
(defpath navigate-adventure-stylist-results)
(defpath navigate-adventure-stylist-gallery)
(defpath navigate-adventure-match-success)
(defpath navigate-adventure-stylist-profile)

(defpath adventure-address-component-mounted)
(defpath control-adventure-stylist-salon-address-clicked)
(defpath control-adventure-stylist-phone-clicked)
(defpath control-adventure-stylist-gallery-open)
(defpath control-adventure-stylist-gallery-close)
(defpath clear-selected-location)
(defpath control-adventure-location-submit)

(defpath control-toggle-promo-code-entry)

(defpath api-success-fetch-matched-stylist)
(defpath api-success-fetch-matched-stylists)
(defpath api-success-fetch-stylist-reviews)
(defpath api-failure-fetch-stylist-reviews)
(defpath control-fetch-stylist-reviews)
(defpath adventure-stylist-search-results-displayed)
(defpath stylist-results-address-component-mounted)
(defpath stylist-results-address-selected)
(defpath stylist-results-update-location-from-address)

(defpath control-stylist-search-toggle-filter)
(defpath control-stylist-search-reset-filters)
(defpath control-show-stylist-search-filters)
(defpath control-stylist-search-filters-dismiss)

(defpath stylist-search-filter-menu-close)
(defpath initialize-stylist-search-filters)
(defpath control-toggle-stylist-search-filter-section)

(defpath api-success-update-order-remove-servicing-stylist)

(defpath api-shipping-address-geo-lookup)
(defpath api-success-shipping-address-geo-lookup)
(defpath api-failure-shipping-address-geo-lookup)

;; Stripe Payment Button
(defpath stripe-payment-request-button-inserted)
(defpath stripe-payment-request-button-removed)

(defpath cart-interstitial-browse-stylist-mounted)
(defpath cart-interstitial-free-mayvenn-service-tracker-mounted)
(defpath control-cart-interstitial-browse-stylist-cta)
(defpath control-cart-interstitial-view-cart)


;; Spreedly


(defpath loaded-spreedly)
(defpath spreedly-did-mount)
(defpath spreedly-did-unmount)
(defpath spreedly-frame-initialized)
(defpath spreedly-frame-tokenized)

(defpath stick-flyout)
(defpath unstick-flyout)

(defpath flyout-mouse-enter)
(defpath flyout-mouse-away)
(defpath flyout-on-click)
(defpath flyout-click-away)

(defpath escape-key-pressed)

;; data team only

(defpath visual-add-on-services-displayed)

;; Flows

; Clear everything
(defpath flow|stylist-matching|initialized)

(defpath flow|stylist-matching|set-presearch-field)
(defpath flow|stylist-matching|presearch-canceled)
(defpath flow|stylist-matching|presearch-cleared)
(defpath flow|stylist-matching|set-address-field)
(defpath flow|stylist-matching|selected-for-inspection)

; set search params for stylist matching
(defpath flow|stylist-matching|param-ids-constrained)
(defpath flow|stylist-matching|param-address-constrained)
(defpath flow|stylist-matching|param-location-constrained)
(defpath flow|stylist-matching|param-name-presearched)
(defpath flow|stylist-matching|param-name-constrained)
(defpath flow|stylist-matching|param-services-constrained)

; core domain for searching
(defpath flow|stylist-matching|prepared)
(defpath flow|stylist-matching|searched)
(defpath flow|stylist-matching|resulted)
(defpath flow|stylist-matching|matched)
;; HACK:(corey) Breaks mental model
(defpath flow|stylist-matching|unmatched)

(defpath control-stylist-matching-presearch-salon-result-selected)
(defpath control-stylist-matching-presearch-stylist-result-selected)

;; Flow - Looks Filtering

;;; -> Any page with a facet filter (category & look pages)
;;; Should `on-nav` ingest the filters from the query parameters and put them
;;; into app state as the selected filters

;;; -> Closing the panel and closing the subsections of the panel
(defpath flow|facet-filtering|initialized)
;;; -> Redirect to the page with the filter component on it and remove the filters from the query parameters
(defpath flow|facet-filtering|reset)

;;; -> Open & Close the panel
(defpath flow|facet-filtering|panel-toggled)

;;; -> Open & Close a subsection of the panel
(defpath flow|facet-filtering|section-toggled)

;;; -> --Check and uncheck the filters themselves--
;;; -> Redirect to page with filter removing or adding the appropriate filter to the URL
(defpath flow|facet-filtering|filter-toggled)

(defpath shop-by-look|look-selected)

;; TODO more specific name
(defpath cancel-presearch-requests)
;; TODO(corey) think about general solutions to the api handlers problem
(defpath api-success-presearch-name)

(defpath api-success-set-appointment-time-slot)
(defpath api-failure-set-appointment-time-slot)

(defpath api-success-remove-appointment-time-slot)
(defpath api-failure-remove-appointment-time-slot)

;; Flow - Service Addon Selection in the Cart

(defpath flow|cart-service-addons|toggled)

;; Flow - Service Addon Selection safety-net post-Cart

(defpath flow|post-cart-service-addons|toggled)

;; Business domains

(defpath biz|current-stylist|selected)
(defpath biz|current-stylist|deselected)

(defpath biz|product|options-elected)

(defpath biz|service-item|addons-replaced)

;; Cache domains

(defpath cache|product|requested)
(defpath cache|product|fetched)

(defpath cache|stylist|requested)
(defpath cache|stylist|fetched)

(defpath cache|current-stylist|requested)

;; shared cart

(defpath biz|shared-cart|hydrated)
(defpath cart-cleared)

;; Adventure Quiz
(defpath navigate-adventure-quiz)

(defpath biz|questioning|reset)
(defpath biz|questioning|answered)
(defpath biz|questioning|submitted)

(defpath flow|wait|begun)
(defpath flow|wait|elapsed)

(defpath biz|follow|defined)

(defpath flow|live-help|reset)
(defpath flow|live-help|opened)

;; suggesting looks (from quiz)
(defpath biz|looks-suggestions|reset)
(defpath biz|looks-suggestions|queried)
(defpath biz|looks-suggestions|resulted)
(defpath biz|looks-suggestions|selected)

;; generalized concept of progress
(defpath biz|progression|reset)
(defpath biz|progression|progressed)

;; Shopping Quiz iterations
(defpath navigate-shopping-quiz)
(defpath navigate-shopping-quiz-unified-freeinstall)
(defpath navigate-shopping-quiz-unified-freeinstall-intro)
(defpath navigate-shopping-quiz-unified-freeinstall-question)
(defpath navigate-shopping-quiz-unified-freeinstall-recommendations)
(defpath navigate-shopping-quiz-unified-freeinstall-summary)
(defpath navigate-shopping-quiz-unified-freeinstall-find-your-stylist)
(defpath navigate-shopping-quiz-unified-freeinstall-top-stylist)
(defpath navigate-shopping-quiz-unified-freeinstall-stylist-results)
(defpath navigate-shopping-quiz-unified-freeinstall-match-success)
(defpath navigate-shopping-quiz-unified-freeinstall-appointment-booking)

;; NOTE
;; Models
;; - form concepts/nouns
;; - composed of actions/verbs that work on that concept
;;
;; Events
;; - form verbs/actions
;; - descriptions, so always in past tense
;;
;; Prepend events with these prefixes to enhance understanding
;;
;; flow| ;; - sequence of user interactions
;; - visual in nature (ui state machine)
;; - the 'noun' is the ui state
;;
;; biz|
;; - independent of a particular user interface
;; - the 'noun' is a business concept e.g. cart, customer
;;
;; cache|
;; - less conceptualization, don't interpret into other models
;; - e.g. data handling from http remotes, 1-1 with APIs
;; - e.g. wraps data stores like cookies
;; - the 'noun' is a store e.g. db, cookies, remote api state

;; Stylist Gallery
(defpath control-stylist-gallery-posts-drag-predicate-loop)
(defpath control-stylist-gallery-posts-drag-began)
(defpath control-stylist-gallery-posts-drag-ended)
(defpath control-stylist-gallery-delete-v2)
(defpath control-stylist-gallery-posts-drag-predicate-initialized)
(defpath stylist-gallery-posts-drag-predicate-loop)
(defpath stylist-gallery-posts-reordered)

(defpath top-stylist-navigation-decided)

;; Appointment Booking TODO consider moving to own namespace
(defpath biz|appointment-booking|initialized)
(defpath biz|appointment-booking|date-selected)
(defpath biz|appointment-booking|time-slot-selected)
(defpath biz|appointment-booking|submitted)
(defpath biz|appointment-booking|skipped)
(defpath biz|appointment-booking|done)

(defpath biz|appointment-booking|navigation-decided)
(defpath control-show-edit-appointment-menu)
(defpath control-dismiss-edit-appointment-menu)

(defpath post-stylist-matched-navigation-decided)

(defpath navigate-adventure-appointment-booking)
