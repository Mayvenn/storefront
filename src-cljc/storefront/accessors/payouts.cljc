(ns storefront.accessors.payouts)

(defn cash-out-eligible? [payout-method]
  ;; TODO: this overloads payout-method to be either
  ;;
  ;;       - PayoutMethod class from diva (eg - Mayvenn::PaypalPayoutMethod)
  ;;       - or a chosen payout method string (eg - paypal)
  ;;
  ;;       ideally, this should be two separate functions, but each function
  ;;       should be co-located to facilitate easier updating of cash-out
  ;;       eligible payout methods.
  (boolean (#{"Mayvenn::GreenDotPayoutMethod"
              "Mayvenn::PaypalPayoutMethod"
              "green_dot"
              "paypal"}
            (or (:type payout-method)
                payout-method))))
