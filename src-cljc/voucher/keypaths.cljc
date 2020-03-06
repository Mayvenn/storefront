(ns voucher.keypaths)

(def voucher [:voucher])
(def voucher-response (conj voucher :response)) ; GROT: After vaqum deploy
(def voucher-redeemed-response (conj voucher :redeemed-response)) ; NOTE: this is the replacement for the above.

(def eight-digit-code (conj voucher :eight-digit-code))
(def scanning? (conj voucher :scanning?))
(def scanned-code (conj voucher :scanned-code))
