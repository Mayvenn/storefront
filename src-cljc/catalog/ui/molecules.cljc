(ns catalog.ui.molecules
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.components.money-formatters :as mf]
            [storefront.components.ui :as ui]
            [storefront.platform.reviews :as review-component]
            [storefront.platform.component-utils :as utils]))

(defn product-title
  "TODO empty state"
  [{:title/keys [id primary secondary]}]
  [:div {:data-test id}
   [:h3.proxima.title-2.shout {:item-prop "name"} primary]
   [:div.medium secondary]])

(defn ^:private item-price
  [price]
  (when price
    [:span.proxima.content-2 {:item-prop "price"} (mf/as-money price)]))

(defn price-block
  [{:price-block/keys [primary secondary]}]
  [:div.right-align
   {:item-prop  "offers"
    :item-scope ""
    :item-type  "http://schema.org/Offer"}
   (when-let [primary-formatted (item-price primary)]
     [:div
      primary-formatted
      [:div.proxima.content-3 secondary]])])

(defn yotpo-reviews-summary
  [{:yotpo-reviews-summary/keys [product-title product-id data-url]}]
  [:div
   (when product-id
     [:div.h6
      {:style {:min-height "18px"}}
      (component/build review-component/reviews-summary-dropdown-experiment-component
                       {:yotpo-data-attributes
                        {:data-name       product-title
                         :data-product-id product-id
                         :data-url        data-url}})])])

(defcomponent product-description
  [{:product-description/keys
    [description
     colors
     weights
     materials
     summary
     stylist-exclusives-family
     hair-family
     learn-more-nav-event]} _ _]
  (when (seq description)
    [:div.border.border-width-2.m3.p4.border-cool-gray
     [:div.light.canela.title-2 "Description"]
     [:div {:item-prop "description"}
      (when (or colors weights materials)
        (let [attrs (->> [["Color" colors]
                          ["Weight" weights]
                          ["Material" materials]]
                         (filter second))
               ;;This won't work if we have 5 possible attrs
              size (str "col-" (/ 12 (count attrs)))]
          (into [:div.clearfix.mxn1.mt2.mb4]
                (for [[title value] attrs]
                  [:dl.col.m0.inline-block {:class size}
                   [:dt.mx1.shout.proxima.title-3 title]
                   [:dd.mx1.ml0.proxima.content-2 value]]))))
      (when (seq summary)
        [:div.my2
         [:h3.mbp3.h6 "Includes:"]
         [:ul.list-reset.h5.medium
          (for [[idx item] (map-indexed vector summary)]
            [:li.mbp3 {:key (str "item-" idx)} item])]])
      (for [[idx item] (map-indexed vector description)]
        [:div.mt2 {:key (str "product-description-" idx)} item])
      (when (and learn-more-nav-event
                 (not (or (contains? hair-family "seamless-clip-ins")
                          (contains? hair-family "tape-ins")
                          (contains? stylist-exclusives-family "kits"))))
        [:div.mt4.mb2
         (ui/button-medium-underline-black
          (utils/route-to learn-more-nav-event)
          "Learn more about our hair")])]]))
