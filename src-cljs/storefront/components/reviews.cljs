(ns storefront.components.reviews
  (:require [sablono.core :refer-macros [html]]
            [om.core :as om]
            [storefront.accessors.taxons :as taxons]
            [storefront.events :as events]
            [storefront.hooks.experiments :as experiments]
            [storefront.messages :refer [send]]
            [storefront.routes :as routes]
            [storefront.keypaths :as keypaths]
            [storefront.utils.query :as query]))


(defn representative-product-id-for-taxon
  "The bundle-builder shows data for many products, but yotpo can show
  reviews for only one product at a time.  For now, we just pick one product that represents the whole taxon."
  [data]
  (comment
    "The current taxon in focus is stored in the browse-taxon-query keypath,
    but as a selector to retrieve the entire taxon using the `query` combinators.
    As we only need the taxon name, this pulls the slug out and discards the
    selection function and returns the representative product id for use with
    Yotpo.")
  (let [taxon (keyword (first (vals (get-in data keypaths/browse-taxon-query))))]
    (get {:straight 13
          :loose-wave 1
          :body-wave 3
          :deep-wave 11
          :curly 9
          :blonde 15
          :closures 28}
         taxon)))

(defn get-product-id [data]
  (if (experiments/display-variation data "bundle-builder")
    (representative-product-id-for-taxon data)
    (:id (query/get (get-in data keypaths/browse-product-query)
                    (vals (get-in data keypaths/products))))))

(defn reviews-component [data owner]
  (reify
    om/IDidMount
    (did-mount [_] (send data events/reviews-component-mounted))
    om/IWillUnmount
    (will-unmount [_] (send data events/reviews-component-will-unmount))
    om/IRender
    (render [_]
      (html
       (let [product-id (get-product-id data)]
         [:div.product-reviews
          [:div.yotpo.yotpo-main-widget
           {:data-product-id product-id
            :data-url (apply routes/path-for @data
                             (get-in data keypaths/navigation-message))}]])))))

(defn reviews-summary-component [data owner]
  (reify
    om/IDidMount
    (did-mount [_] (send data events/reviews-component-mounted))
    om/IWillUnmount
    (will-unmount [_] (send data events/reviews-component-will-unmount))
    om/IRender
    (render [_]
      (html
       (let [product-id (get-product-id data)]
         [:div.product-reviews-summary
          [:div.yotpo.bottomLine.star-summary
           {:data-product-id product-id
            :data-url (apply routes/path-for @data
                             (get-in data keypaths/navigation-message))}]
          [:div.yotpo.QABottomLine.question-summary
           {:data-product-id product-id}]])))))
