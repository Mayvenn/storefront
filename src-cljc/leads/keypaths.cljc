(ns leads.keypaths)

(def root [:leads])

(def call-slot-options (conj root :call-slot-options))
(def eastern-offset    (conj root :eastern-offset))
(def offset            (conj root :offset))
(def tz-abbreviation   (conj root :timezone-abbreviation))

;; Working lead
(def lead              (conj root :lead))
(def lead-first-name   (conj lead :first-name))
(def lead-last-name    (conj lead :last-name))
(def lead-phone        (conj lead :phone))
(def lead-email        (conj lead :email))
(def lead-call-slot    (conj lead :call-slot))

;; Lead specific data
(def lead-utm-campaign (conj lead :utm-campaign))
(def lead-utm-content  (conj lead :utm-content))
(def lead-utm-medium   (conj lead :utm-medium))
(def lead-utm-source   (conj lead :utm-source))
(def lead-utm-term     (conj lead :utm-term))
(def lead-id           (conj lead :id))
(def lead-tracking-id  (conj lead :tracking-id))
(def lead-flow-id      (conj lead :flow-id))
(def lead-step-id      (conj lead :step-id))

(def stylist                 (conj root :stylist))
(def stylist-first-name      (conj stylist :first-name))
(def stylist-last-name       (conj stylist :last-name))
(def stylist-phone           (conj stylist :phone))
(def stylist-email           (conj stylist :email))
(def stylist-password        (conj stylist :password))
(def stylist-referred        (conj stylist :referred))
(def stylist-referrers-phone (conj stylist :referrers-phone))
(def stylist-address1        (conj stylist :address1))
(def stylist-address2        (conj stylist :address2))
(def stylist-city            (conj stylist :city))
(def stylist-state           (conj stylist :state))
(def stylist-zip             (conj stylist :zip))
(def stylist-birthday        (conj stylist :birthday))
(def stylist-licensed        (conj stylist :licensed))
(def stylist-payout-method   (conj stylist :payout-method))
(def stylist-venmo-phone     (conj stylist :venmo-phone))
(def stylist-paypal-email    (conj stylist :paypal-email))
(def stylist-slug            (conj stylist :slug))

;; Remote replicas for current session
;; TODO should this be standardized across storefront?
(def remotes           (conj root :remotes))
(def remote-lead       (conj remotes :lead))
(def remote-lead-id    (conj remote-lead :id))
(def remote-user-token (conj remotes :user-token))

(def remote-user-id    (conj remotes :user-id))
(def onboarding-status (conj root :onboarding-status))
