(ns catalog.ui.service-card-listing
  (:require api.current
            [catalog.skuers :as skuers]
            [catalog.ui.horizontal-direct-to-cart-card :as horizontal-direct-to-cart-card]
            [catalog.ui.vertical-direct-to-cart-card :as vertical-direct-to-cart-card]
            clojure.string
            [spice.selector :as selector]
            [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]
            [stylist-matching.search.accessors.filters :as stylist-filters]))

(defn service->card
  [data
   {:as                          service
    mayvenn-install-discountable :promo.mayvenn-install/discountable}]
  (if (contains? mayvenn-install-discountable true) ;; Free services
    (horizontal-direct-to-cart-card/query data service)
    (vertical-direct-to-cart-card/query data service)))

(defn card->component
  [{:as       card
    card-type :card/type}]
  (case card-type
    :vertical-direct-to-cart-card   (vertical-direct-to-cart-card/organism card)
    :horizontal-direct-to-cart-card (horizontal-direct-to-cart-card/organism card)))

(c/defcomponent ^:private service-list-subsection-component
  [{:keys [service-cards subsection-key] primary-title :title/primary} _ {:keys [id]}]
  [:div.mb7
   {:id        id
    :data-test id}
   (when primary-title
     [:div.canela.title-2.center.mb2 primary-title])
   [:div.flex.flex-wrap
    (for [card service-cards]
      ^:inline (card->component card))]])

(c/defcomponent organism
  [{:keys [id subsections loading-products?]} _ _]
  (when id
    [:div.px2.pb4
     (if loading-products?
       [:div.col-12.my8.py4.center (ui/large-spinner {:style {:height "4em"}})]
       (mapv (fn build [{:as subsection :keys [subsection-key]}]
               (c/build service-list-subsection-component
                        subsection
                        (c/component-id (str "subsection-" subsection-key))))
             subsections))]))

(def ^:private select
  (partial selector/match-all {:selector/strict? true}))

(defn ^:private subsections-query
  [data {:subsections/keys [subsection-selectors]} services]
  (if (seq subsection-selectors)
    (keep
     (fn [{:subsection/keys [title selector]}]
       (when-let [service-cards (->> (select selector services)
                                     (mapv (partial service->card data))
                                     (sort-by :sort/value)
                                     not-empty)]
         {:service-cards  service-cards
          :subsection-key (clojure.string/replace title #" " "-")
          :title/primary  title}))
     subsection-selectors)
    [{:service-cards  (->> services
                           (mapv (partial service->card data))
                           (sort-by :sort/value))
      :subsection-key :no-subsections}]))

(defn query
  [app-state category matching-services]
  (let [current-stylist (:diva/stylist (api.current/stylist app-state))
        subsections     (->> matching-services
                             (map #(assoc %
                                          :stylist-provides-service
                                          (stylist-filters/stylist-provides-service? current-stylist %)))
                             (subsections-query app-state category))]
    {:id                "service-card-listing"
     :subsections       subsections
     :loading-products? (utils/requesting? app-state (conj request-keys/get-products
                                                           (skuers/essentials category)))}))
