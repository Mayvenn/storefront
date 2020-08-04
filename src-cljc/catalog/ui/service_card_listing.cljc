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

(defn service->card
  [data
   {:as                          service
    mayvenn-install-discountable :promo.mayvenn-install/discountable}]
  (cond
    (contains? mayvenn-install-discountable true) ;; Free services
    (horizontal-direct-to-cart-card/query data service)

    :else
    (vertical-direct-to-cart-card/query data service)))

(c/defcomponent ^:private service-cards-empty-state
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
  [{:keys [service-cards subsection-key] primary-title :title/primary} _ {:keys [id]}]
  [:div
   {:id id :data-test id}
   (when primary-title
     [:div.canela.title-2.center.mt8.mb2 primary-title])
   [:div.flex.flex-wrap
    (for [card service-cards]
      ^:inline (card->component card))]])

(c/defcomponent organism
  [{:keys [id subsections title no-cards? loading-products?]} _ _]
  (when id
    [:div.px2.pb4
     (cond
       loading-products? [:div.col-12.my8.py4.center (ui/large-spinner {:style {:height "4em"}})]

       no-cards? (c/build service-cards-empty-state {} {})

       :else (mapv (fn build [{:as subsection :keys [subsection-key]}]
                     (c/build service-list-subsection-component
                              subsection
                              (c/component-id (str "subsection-" subsection-key))))
                   subsections))]))

(defn query
  [app-state category matching-services]
  (let [category-selector (:subsections/category-selector category)
        subsection-facet-options (->> (get-in app-state keypaths/v2-facets)
                                      (filter (comp #{category-selector} :facet/slug))
                                      first
                                      :facet/options
                                      (maps/index-by :option/slug))
        servicing-stylist (get-in app-state adventure.keypaths/adventure-servicing-stylist)
        subsections       (->> matching-services
                               (map #(assoc %
                                            :stylist-provides-service
                                            (stylist-filters/stylist-provides-service servicing-stylist %)))
                               (group-by (if category-selector
                                           (comp first category-selector)
                                           (constantly :no-subsections)))
                               (maps/map-values #(sort-by (juxt (comp not :stylist-provides-service) :sort/value) %))
                               (map (fn [[subsection-key services]]
                                      (let [facet (get subsection-facet-options subsection-key)]
                                        {:title/primary  (:option/name facet)
                                         :subsection-key subsection-key
                                         :service-cards  (map #(service->card app-state %) services)}))))
        no-cards? (empty? subsections)]
    {:id                "service-card-listing"
     :subsections       subsections
     :no-cards? no-cards?
     :loading-products? (and no-cards?
                             (utils/requesting? app-state (conj request-keys/get-products
                                                                (skuers/essentials category))))}))
