(ns catalog.ui.product-card-listing
  (:require [catalog.skuers :as skuers]
            [catalog.ui.product-card :as product-card]
            clojure.set
            [spice.maps :as maps]
            [spice.selector :as selector]
            [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]))

(defn subsections-query
  [facets
   {:keys [subsections/category-selector subsections]}
   products-matching-criteria
   data]
  (let [subsection-facet-options (when category-selector
                                   (->> facets
                                        (filter (comp #{category-selector} :facet/slug))
                                        first
                                        :facet/options
                                        (maps/index-by :option/slug)))
        subsection-order         (->> (map-indexed vector subsections)
                                      (into {})
                                      clojure.set/map-invert)]
    (->> products-matching-criteria
         (group-by (if category-selector
                     (comp first category-selector)
                     (constantly :no-subsections)))
         (sequence
          (comp
           (map (fn [[subsection-key products]]
                  {:title/primary (:option/name (get subsection-facet-options subsection-key))
                   :products       products
                   :subsection-key subsection-key}))
           (map #(update % :products (partial map (partial product-card/query data))))
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

(c/defcomponent ^:private product-list-subsection-component
  [{:keys [product-cards subsection-key] primary-title :title/primary} _ {:keys [id]}]
  [:div
   {:id id :data-test id}
   (when primary-title
     [:div.canela.title-2.center.mt8.mb2 primary-title])
   [:div.flex.flex-wrap
    (map product-card/organism product-cards)]])

(c/defcomponent organism
  [{:keys [subsections title all-product-cards loading-products?]} _ _]
  [:div.px2.pb4
   (cond
     loading-products?          [:div.col-12.my8.py4.center (ui/large-spinner {:style {:height "4em"}})]

     (empty? all-product-cards) (c/build product-cards-empty-state {} {})

     :else                      (mapv (fn build [{:as subsection :keys [subsection-key]}]
                                        (c/build product-list-subsection-component
                                                 subsection
                                                 (c/component-id (str "subsection-" subsection-key))))
                                      subsections))])

(defn query
  [app-state category products selections]
  (let [products-matching-category (selector/match-all {:selector/strict? true}
                                                       (merge
                                                        (skuers/electives category)
                                                        (skuers/essentials category))
                                                       products)
        products-matching-criteria (selector/match-all {:selector/strict? true}
                                                       (merge
                                                        (skuers/essentials category)
                                                        selections)
                                                       products-matching-category)
        facets                     (maps/index-by :facet/slug (get-in app-state keypaths/v2-facets))
        subsections                (subsections-query
                                    (vals facets)
                                    category
                                    products-matching-criteria
                                    app-state)]
    {:subsections       subsections
     :all-product-cards (mapcat :product-cards subsections)
     :loading-products? (utils/requesting? app-state (conj request-keys/get-products
                                                           (skuers/essentials category)))}))
