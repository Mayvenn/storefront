(ns catalog.ui.product-card-listing
  (:require [catalog.skuers :as skuers]
            [catalog.ui.product-card :as product-card]
            clojure.set
            clojure.string
            [spice.selector :as selector]
            [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]))

(def ^:private select
  (partial selector/match-all {:selector/strict? true}))

(defn ^:private subsections-query
  [data
   {:subsections/keys [subsection-selectors]}
   products-matching-criteria]
  (if (seq subsection-selectors)
    (keep
     (fn [{:subsection/keys [title selector]}]
       (when-let [product-cards (->> products-matching-criteria
                                     (select selector)
                                     (mapv (partial product-card/query data))
                                     (sort-by :sort/value)
                                     not-empty)]
         {:product-cards  product-cards
          :subsection-key (clojure.string/replace title #" " "-")
          :title/primary  title}))
     subsection-selectors)
    [{:product-cards  (->> products-matching-criteria
                           (mapv (partial product-card/query data))
                           (sort-by :sort/value))
      :subsection-key :no-subsections}]))

(c/defcomponent ^:private product-cards-empty-state
  [_ _ _]
  [:div.col-12.my8.py4.center
   [:p.h1.py4 "😞"]
   [:p.h2.py6 "Sorry, we couldn’t find any matches."]
   [:p.h4.mb10.pb10
    [:a.p-color (utils/fake-href events/control-category-option-clear) "Clear all filters"]
    " to see more hair."]])

(c/defcomponent ^:private product-list-subsection-component
  [{:keys [product-cards] primary-title :title/primary} _ {:keys [id]}]
  [:div.mb6
   {:id id :data-test id :key id}
   (when primary-title
     [:div.canela.title-2.center.mb2 primary-title])
   [:div.flex.flex-wrap
    (for [card product-cards]
      ^:inline (product-card/organism card))]])

(c/defcomponent organism
  [{:keys [id subsections no-product-cards? loading-products?]} _ _]
  (when id
    [:div.px2.pb4.pt6
     (cond
       loading-products? [:div.col-12.center (ui/large-spinner {:style {:height "4em"}})]

       no-product-cards? (c/build product-cards-empty-state {} {})

       :else             (mapv (fn build [{:as subsection :keys [subsection-key]}]
                                 (c/build product-list-subsection-component
                                          subsection
                                          (c/component-id (str "subsection-" subsection-key))))
                               subsections))]))

(defn query
  [app-state category products-matching-filter-selections]
  (let [subsections       (subsections-query
                           app-state
                           category
                           products-matching-filter-selections)
        no-product-cards? (empty? (mapcat :product-cards subsections))]
    {:id                "product-card-listing"
     :subsections       subsections
     :no-product-cards? no-product-cards?
     :loading-products? (and no-product-cards?
                             (utils/requesting? app-state (conj request-keys/get-products
                                                                (skuers/essentials category))))}))
