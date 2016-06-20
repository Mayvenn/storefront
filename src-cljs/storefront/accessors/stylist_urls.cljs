(ns storefront.accessors.stylist-urls)

(def store-url
  (str (-> js/window .-location .-protocol) "//"
       (-> js/window .-location .-host)))
