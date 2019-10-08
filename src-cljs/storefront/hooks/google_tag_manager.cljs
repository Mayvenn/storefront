(ns storefront.hooks.google-tag-manager)

(defn track-placed-order
  [{:keys [total number]}]
  (when (.hasOwnProperty js/window "dataLayer")
    (.push js/window.dataLayer (clj->js {:event "orderPlaced"
                                         :transactionTotal total
                                         :transactionId number}))))
