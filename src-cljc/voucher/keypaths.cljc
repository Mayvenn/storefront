(ns voucher.keypaths)

(def voucher [:voucher])
(def voucher-response (conj voucher [:response]))

(def eight-digit-code (conj voucher :eight-digit-code))
(def scanning? (conj voucher :scanning?))
(def scanned-code (conj voucher :scanned-code))
