(ns storefront.accessors.payouts)

(defn cash-out-eligible? [payout-method]
  (boolean (= "Mayvenn InstaPay" (:name payout-method))))
