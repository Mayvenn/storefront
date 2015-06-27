(ns storefront.reviews
  (:require [storefront.script-tags :refer [src-tag insert-tag-with-callback]]
            [storefront.messages :refer [enqueue-message]]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]))

(def ^:private tag-class "product-review-tag")

(defn insert-reviews [data]
  (when-not (aget (.getElementsByClassName js/document tag-class) 0)
    (insert-tag-with-callback
     (src-tag "//staticw2.yotpo.com/ZmvkoIuVo61VsbHVPaqDPZpkfGm6Ce2kjVmSqFw9/widget.js"
              tag-class)
     #(enqueue-message (get-in data keypaths/event-ch)
                       [events/reviews-inserted]))))

(defn start []
  (when (.hasOwnProperty js/window "yotpo")
    (set! (.-initialized js/yotpo) false)
    (.init js/yotpo)))

(defn stop []
  (when (.hasOwnProperty js/window "yotpo")
    (.pop (.-widgets js/yotpo))
    (.pop (.. js/yotpo -callbacks -ready))))
