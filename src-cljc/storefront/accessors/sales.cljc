(ns storefront.accessors.sales
  (:require [spice.date :as date]))

(defn voucher-expired?
  [sale]
  (let [{:keys [voucher-redeemed-at
                voucher-fulfilled-at
                voucher-expiration-date]} sale]
    (and (not voucher-redeemed-at)
         voucher-expiration-date
         (date/after? (date/now)
                      (date/to-datetime voucher-expiration-date)))))

(defn sale-status
  "Maps a sale to a keyword representing the state of the sale.

  The possible states are as follows:
  | Sale status     | Meaning                                                               |
  |-----------------|-----------------------------------------------------------------------|
  | :sale/pending   | The sale corresponds to an order that has been placed but not shipped |
  | :sale/returned  | The sale corresponds to an order that has been returned               |
  | :sale/shipped   | The sale corresponds to an order that has been shipped                |
  | :sale/unknown   | Somehow this sale corresponds to an order that never shipped?         |"
  [sale]
  (let [{:keys [placed-at
                shipped-at
                returned-at]} sale]
    (cond
      shipped-at  :sale/shipped
      returned-at :sale/returned
      placed-at   :sale/pending
      :otherwise  :sale/unknown)))

(defn voucher-status
  "Maps a sale to a keyword representing the state of the voucher on the sale.

  The possible states are as follows:
  | voucher status       | Meaning                                                                                  |
  |----------------------|------------------------------------------------------------------------------------------|
  | :voucher/pending     | The order has not shipped yet, therefore we don't know if there will be a voucher or not |
  | :voucher/redeemed    | The voucher has already been used and cannot be used again                               |
  | :voucher/expired     | The voucher is past its expiration date and cannot be used.                              |
  | :voucher/active      | The voucher has been fulfilled (provisioned to the order) and is ready for use           |
  | :voucher/transferred | The voucher has been redeemed by another stylist.                                        |
  | :voucher/none        | There is no associated voucher for this sale                                             |"
  [sale]
  (let [{:keys [shipped-at
                voucher-redeemed-at
                voucher-fulfilled-at
                voucher-expiration-date
                voucher]} sale]
    (cond
      (not shipped-at)        :voucher/pending
      voucher-redeemed-at     :voucher/redeemed
      (voucher-expired? sale) :voucher/expired
      voucher-fulfilled-at    :voucher/active
      :otherwise              :voucher/none)))

(def voucher-status->copy
  {:voucher/pending  "pending"
   :voucher/redeemed "redeemed"
   :voucher/expired  "expired"
   :voucher/active   "active"
   :voucher/none     "none"})

(def sale-status->copy
  {:sale/pending "processing"
   :sale/return  "returned"
   :sale/shipped "shipped"
   :sale/unknown "error"})

(def status->copy
  (merge voucher-status->copy sale-status->copy))
