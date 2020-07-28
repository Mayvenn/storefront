(ns catalog.ui.service-card-listing
  (:require adventure.keypaths
            [catalog.skuers :as skuers]
            [catalog.ui.vertical-direct-to-cart-card :as vertical-direct-to-cart-card]
            [catalog.ui.horizontal-direct-to-cart-card :as horizontal-direct-to-cart-card]
            clojure.set
            [spice.maps :as maps]
            [storefront.accessors.orders :as orders]
            [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]
            [stylist-matching.search.accessors.filters :as stylist-filters]))

(defn product->card
  [data {:as                          product
         mayvenn-install-discountable :promo.mayvenn-install/discountable}]
  (cond
    (contains? mayvenn-install-discountable true) ;; Free services
    (horizontal-direct-to-cart-card/query data product)

    :else
    (vertical-direct-to-cart-card/query data product)))

(defn stylist-provides-service
  [stylist service-product]
  (->> service-product
       :selector/sku-ids
       first
       stylist-filters/service-sku-id->service-menu-key
       (get (:service-menu stylist))
       boolean))

(defn service-subsection-query
  [stylist stylist-provides-service?]
  (if stylist
    (if stylist-provides-service?
      {:title/primary (str "Available with " (:store-nickname stylist))}
      {:title/primary       "Other Free Mayvenn Services"
       :title/secondary     "Mayvenn Services that are not available with your stylist"
       :title.action/target []
       :title.action/copy   "change stylist"})
    {:title/primary "Available Services"}))

(defn subsections-query
  [facets
   {:keys [subsections]}
   products-matching-criteria
   data]
  (let [subsection-order          (->> (map-indexed vector subsections)
                                       (into {})
                                       clojure.set/map-invert)
        servicing-stylist         (get-in data adventure.keypaths/adventure-servicing-stylist)
        hide-stylist-subsections? (-> (get-in data keypaths/order)
                                      orders/service-line-items
                                      empty?)]
    (->> products-matching-criteria
         (map #(assoc % :stylist-provides-service (or hide-stylist-subsections? (stylist-provides-service servicing-stylist %))))
         (group-by :stylist-provides-service)

         (into []
               (comp
                (map (fn [[subsection-key products]]
                       (merge
                        {:products       products
                         :subsection-key subsection-key}
                        (service-subsection-query (when (not hide-stylist-subsections?) servicing-stylist) subsection-key))))
                (map #(update % :products (partial map (partial product->card data))))
                (map #(clojure.set/rename-keys % {:products :product-cards}))
                (map #(update % :product-cards (partial sort-by :sort/value)))))
         (sort-by (comp subsection-order :subsection-key)))))

(c/defcomponent ^:private product-cards-empty-state
  [_ _ _]
  [:div.col-12.my8.py4.center
   [:p.h1.py4 "😞"]
   [:p.h2.py6 "Sorry, we couldn’t find any matches."]
   [:p.h4.mb10.pb10
    [:a.p-color (utils/fake-href events/control-category-option-clear) "Clear all filters"]
    " to see more hair."]])

(defn card->component
  [{:as       card
    card-type :card/type}]
  (case card-type
    :vertical-direct-to-cart-card   (vertical-direct-to-cart-card/organism card)
    :horizontal-direct-to-cart-card (horizontal-direct-to-cart-card/organism card)))

(c/defcomponent ^:private service-list-subsection-component
  [{:keys [product-cards subsection-key] primary-title :title/primary} _ {:keys [id]}]
  [:div
   {:id id :data-test id}
   (when primary-title
     [:div.canela.title-2.center.mt8.mb2 primary-title])
   [:div.flex.flex-wrap
    (for [card product-cards]
      ^:inline (card->component card))]])

(c/defcomponent organism
  [{:keys [subsections title no-product-cards? loading-products?]} _ _]
  [:div.px2.pb4
   (cond
     loading-products? [:div.col-12.my8.py4.center (ui/large-spinner {:style {:height "4em"}})]

     no-product-cards? (c/build product-cards-empty-state {} {})

     :else             (mapv (fn build [{:as subsection :keys [subsection-key]}]
                               (c/build service-list-subsection-component
                                        subsection
                                        (c/component-id (str "subsection-" subsection-key))))
                             subsections))])

(defn query
  [app-state category products-matching-filter-selections]
  (let [facets            (maps/index-by :facet/slug (get-in app-state keypaths/v2-facets))
        subsections       (subsections-query
                                    (vals facets)
                                    category
                                    products-matching-filter-selections
                                    app-state)
        no-product-cards? (empty? (mapcat :product-cards subsections))]
    {:subsections       subsections
     :no-product-cards? no-product-cards?
     :loading-products? (and no-product-cards?
                             (utils/requesting? app-state (conj request-keys/get-products
                                                                (skuers/essentials category))))}))
