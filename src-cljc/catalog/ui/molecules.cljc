(ns catalog.ui.molecules
  (:require [storefront.component :as component]
            [storefront.components.money-formatters :as mf]
            [storefront.platform.reviews :as review-component]
            [storefront.platform.component-utils :as utils]))

(defn product-title
  "TODO empty state"
  [{:title/keys [id primary secondary]}]
  [:div {:data-test id}
   [:h3.black.medium.titleize {:item-prop "name"} primary]
   [:div.medium secondary]])

(defn ^:private item-price
  [price]
  (when price
    [:span {:item-prop "price"} (mf/as-money price)]))

(defn price-block
  [{:price-block/keys [primary secondary]}]
  [:div.right-align
   {:item-prop  "offers"
    :item-scope ""
    :item-type  "http://schema.org/Offer"}
   (when-let [primary-formatted (item-price primary)]
     [:div
      [:div.bold primary-formatted]
      [:div.h8.dark-gray secondary]])])

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

(defn product-description [{:product-description/keys
                            [description
                             colors
                             weights
                             materials
                             summary
                             stylist-exclusives-family
                             hair-family
                             learn-more-nav-event]} _ _]
  (component/create
   (when (seq description)
     [:div.border.border-light-gray.border-width-2.m3.py3.px4.rounded
      [:div.h4.medium.shout "Description"]
      [:div {:item-prop "description"}
       (when (or colors weights materials)
         (let [attrs (->> [["Color" colors]
                           ["Weight" weights]
                           ["Material" materials]]
                          (filter second))
               ;;This won't work if we have 5 possible attrs
               size (str "col-" (/ 12 (count attrs)))]
           (into [:div.clearfix.mxn1.my3]
                 (for [[title value] attrs]
                   [:dl.col.m0.inline-block.dark-gray {:class size}
                    [:dt.mx1.shout.h7 title]
                    [:dd.mx1.ml0.h5 value]]))))
       (when (seq summary)
         [:div.my2.dark-gray
          [:h3.mbp3.h6 "Includes:"]
          [:ul.list-reset.h5.medium
           (for [[idx item] (map-indexed vector summary)]
             [:li.mbp3 {:key (str "item-" idx)} item])]])
       [:div.h5.dark-gray.light
        (for [[idx item] (map-indexed vector description)]
          [:div.mt2 {:key (str "product-description-" idx)} item])
        (when (and learn-more-nav-event
                   (not (or (contains? hair-family "seamless-clip-ins")
                            (contains? hair-family "tape-ins")
                            (contains? stylist-exclusives-family "kits"))))
          [:a.block.navy.mt2.medium
           (utils/route-to learn-more-nav-event)
           "Learn more about our hair"])]]])))
