(ns voucher.keypaths)

(def voucher [:voucher])
(def voucher-redeemed-response (conj voucher :redeemed-response))

(def eight-digit-code (conj voucher :eight-digit-code))
(def scanning? (conj voucher :scanning?))
(def scanned-code (conj voucher :scanned-code))
