(ns storefront.hooks.google-tag-manager)

(defn track-placed-order
  [{:keys [total number]}]
  (when (.hasOwnProperty js/window "dataLayer")
    (.push js/dataLayer (clj->js {:event            "orderPlaced"
                                  :transactionTotal total
                                  :transactionId    number}))))

(defn track-email-capture-capture
  [{:keys [email]}]
  (when (.hasOwnProperty js/window "dataLayer")
    (.push js/dataLayer (clj->js {:event        "emailCapture"
                                  :emailAddress email}))))
