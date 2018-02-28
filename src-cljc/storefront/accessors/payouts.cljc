(ns storefront.accessors.payouts)

(defn cash-out-eligible? [payout-method]
  (boolean (#{"green_dot" "paypal"} (:slug payout-method))))
