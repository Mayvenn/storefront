(ns storefront.accessors.sales
  (:require [spice.date :as date]
            [storefront.accessors.orders :as orders]))

(defn voucher-expired?
  [sale]
  (let [{:keys [voucher-redeemed-at
                voucher-fulfilled-at
                voucher-expiration-date]} sale]
    (and (not voucher-redeemed-at)
         voucher-expiration-date
         (date/after? (date/now)
                      (date/to-datetime voucher-expiration-date)))))

(defn order-returned? [{:as sale :keys [order]}]
  (let [{:keys [shipments returns]} order
        return-quantity             (->> (mapcat :line-items returns)
                                         (map :quantity)
                                         (reduce + 0))
        shipped-quantity            (->> (mapcat orders/product-items-for-shipment shipments)
                                         (map :quantity)
                                         (reduce + 0))]
    (or (every? #(= "canceled" %) (map :state shipments))
        (= return-quantity shipped-quantity))))

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
      (order-returned? sale) :sale/returned
      shipped-at             :sale/shipped
      placed-at              :sale/pending
      :otherwise             :sale/unknown)))

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
  | :voucher/returned    | The order has been returned by the customer.                                             |
  | :voucher/none        | There is no associated voucher for this sale                                             |"
  [sale]
  (let [{:keys [shipped-at
                voucher-redeemed-at
                voucher-fulfilled-at
                voucher-expiration-date
                voucher]} sale]
    (cond
      (order-returned? sale)  :voucher/returned
      (not shipped-at)        :voucher/pending
      voucher-redeemed-at     :voucher/redeemed
      (voucher-expired? sale) :voucher/expired
      voucher-fulfilled-at    :voucher/active
      :otherwise              :voucher/none)))

(def voucher-status->copy
  {:voucher/pending     "processing"
   :voucher/redeemed    "redeemed"
   :voucher/expired     "expired"
   :voucher/active      "active"
   :voucher/transferred "transferred"
   :voucher/none        "none"
   :voucher/returned    "returned"
   nil                  "--"})

(def voucher-status->description
  {:voucher/pending     "The voucher is being processed and will be active once the shipment is in transit."
   :voucher/redeemed    nil
   :voucher/returned    "Orders that qualified for a voucher and have been returned result in a returned voucher."
   :voucher/expired     "This voucher has expired and is no longer redeemable. Please contact customer service if you have more questions."
   :voucher/active      "Active voucher have not yet been redeemed."
   :voucher/transferred "Vouchers are transferred when a voucher-eligable order is placed through your store but the associated voucher is redeemed by another Mayvenn stylist."
   :voucher/none        "Orders must include 3 bundles (closures and frontals count towards the 3) to qualify for a voucher."})

(def sale-status->copy
  {:sale/pending  "processing"
   :sale/returned "returned"
   :sale/shipped  "shipped"
   :sale/unknown  "error"})

(def status->copy
  (merge voucher-status->copy sale-status->copy))
