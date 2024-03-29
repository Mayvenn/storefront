(ns catalog.ui.product-card-listing
  (:require [api.catalog :refer [select]]
            [catalog.skuers :as skuers]
            [catalog.ui.product-card :as product-card]
            clojure.string
            [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]
            [storefront.utils :as general-utils]
            [storefront.request-keys :as request-keys]
            [mayvenn.visual.lib.call-out-box :as call-out-box]))

(defn ^:private subsections-query
  [data
   {:subsections/keys [subsection-selectors]}
   products-matching-criteria]
  (if (seq subsection-selectors)
    (->> subsection-selectors
         (keep
          (fn [{:subsection/keys [title selector]}]
            (when-let [product-cards (->> products-matching-criteria
                                          (select selector)
                                          (mapv (partial product-card/query data))
                                          (sort-by :sort/value)
                                          not-empty)]
              {:product-cards  product-cards
               :subsection-key (-> title
                                   (clojure.string/replace #" " "-")
                                   clojure.string/lower-case)
               :title/primary  title})))
         vec
         not-empty)
    [{:product-cards  (some->> products-matching-criteria
                               (mapv (partial product-card/query data))
                               (sort-by :sort/value)
                               not-empty)
      :subsection-key :no-subsections}]))

(c/defcomponent ^:private product-list-subsection-component
  [{:keys [product-cards] primary-title :title/primary} _ {:keys [id]}]
  [:div.mb6
   {:id id :data-test id :key id}
   (when primary-title
     [:h2.canela.title-2.center.mb2 primary-title])
   [:div.flex.flex-wrap
    (for [card product-cards]
      ^:inline
      (case (:card/type card)
        :product          (product-card/organism card)
        nil))]])

(c/defcomponent organism
  [{:keys [id subsections loading-products?]} _ _]
  (when id
    [:div.px2.pb4.pt6
     (if loading-products?
       [:div.col-12.center (ui/large-spinner {:style {:height "4em"}})]
       (mapv (fn build [{:as subsection :keys [subsection-key]}]
               (c/build product-list-subsection-component
                        subsection
                        (c/component-id (str "subsection-" subsection-key))))
             subsections))]))

(defn query
  [app-state category products-matching-filter-selections]
  (let [subsections (subsections-query
                     app-state
                     category
                     products-matching-filter-selections)]
    {:id                "product-card-listing"
     :subsections       subsections
     :loading-products? (utils/requesting? app-state (conj request-keys/get-products
                                                           (skuers/essentials< category)))}))
