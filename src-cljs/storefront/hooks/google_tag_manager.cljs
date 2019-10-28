(ns storefront.hooks.google-tag-manager)

(defn ^:private track
  [data]
  (when (.hasOwnProperty js/window "dataLayer")
    (.push js/dataLayer (clj->js data))))

(defn track-placed-order
  [{:keys [total number]}]
  (track {:event            "orderPlaced"
          :transactionTotal total
          :transactionId    number}))

(defn track-email-capture-capture
  [{:keys [email]}]
  (track {:event        "emailCapture"
          :emailAddress email}))
