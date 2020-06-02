(ns catalog.ui.product-card-listing
  (:require [catalog.skuers :as skuers]
            [catalog.ui.product-card :as product-card]
            [catalog.ui.vertical-direct-to-cart-card :as vertical-direct-to-cart-card]
            [catalog.ui.horizontal-direct-to-cart-card :as horizontal-direct-to-cart-card]
            clojure.set
            [spice.maps :as maps]
            [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]))

(defn product->card
  [data {:as                          product
         catalog-department           :catalog/department
         mayvenn-install-discountable :promo.mayvenn-install/discountable}]
  (cond
    (and (contains? catalog-department "service")
         (contains? mayvenn-install-discountable true)) ;; Free services
    (horizontal-direct-to-cart-card/query data product)

    (and (contains? catalog-department "service")
         (contains? mayvenn-install-discountable false))
    (vertical-direct-to-cart-card/query data product)

    :else
    (product-card/query data product)))

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
    :product                        (product-card/organism card)
    :vertical-direct-to-cart-card   (vertical-direct-to-cart-card/organism card)
    :horizontal-direct-to-cart-card (horizontal-direct-to-cart-card/organism card)))

;; See HACKY(jeff) note below
(def product-card-height (atom 0))

(c/defcomponent ^:private product-list-subsection-component
  [{:keys        [product-cards subsection-key server-render?]
    :screen/keys [seen?]
    primary-title :title/primary}
   _
   {:keys [id]}]
  [:section
   {:id id :data-test id}
   (when primary-title
     [:div.canela.title-2.center.mt8.mb2 primary-title])
   (if (or seen? (and (nil? seen?) server-render?))
     [:div.flex.flex-wrap
      (for [card product-cards]
        ^:inline (card->component card))]

     ;; HACKY(jeff): We need to employ some evil state / DOM querying to get the height of a product card
     ;;              So we can properly estimate the height a section takes.
     ;;
     ;;              This is broken down into several parts:
     ;;
     ;;               1. Use some state to store the height of an individual product card for future reference
     ;;               2. Compute height using card height by calculating the number of rows needed
     ;;               3. Render a dummy product card and attach a ref handler to capture its height
     ;;
     ;; WHY? This allows us to not render all the product cards unless that
     ;; section is on screen, saving page speed performance.

     [:div ;; I am the spacer
      {:style {:width "100%"
               ;; estimate the amount of scroll height we need
               :height
               (let [w #?(:cljs js/window.innerWidth
                          :clj  425)
                     items-per-row (if (>= w 1000)
                                     3
                                     2)]
                 (str (double
                       (* (+ (int (/ (count product-cards) items-per-row))
                             (if (not= 0 (mod (count product-cards) items-per-row))
                               1
                               0))
                          (+
                           10
                           (max @product-card-height
                                ;; this constant came from subsection height / num elements
                                ;; on large mobile layout
                                188.652307692))))
                      "px"))}}
      [:div.flex.flex-wrap ;; I'm here to hide the dummy card that's used for measurements
       (merge
        {:style {:visibility "hidden"}}
        #?(:cljs {:ref (fn [element]
                         (when element
                           (reset! product-card-height (.-height (.getBoundingClientRect (.-firstChild element))))))}))
       ^:inline (product-card/organism (first product-cards))]])])

(c/defcomponent organism
  [{:keys [subsections title no-product-cards? loading-products?]} _ _]
  [:div.px2.pb4
   (cond
     loading-products? [:div.col-12.my8.py4.center (ui/large-spinner {:style {:height "4em"}})]

     no-product-cards? (c/build product-cards-empty-state {} {})

     :else             (for [[i {:as subsection :keys [subsection-key]}] (map-indexed vector subsections)]
                         ^:inline (ui/screen-aware product-list-subsection-component
                                                   (assoc subsection :server-render? (< i 2))
                                                   (c/component-id (str "subsection-" subsection-key)))))])

(defn query
  [app-state category products-matching-filter-selections]
  (let [facets                     (maps/index-by :facet/slug (get-in app-state keypaths/v2-facets))
        subsections                (subsections-query
                                    (vals facets)
                                    category
                                    products-matching-filter-selections
                                    app-state)]
    {:subsections       subsections
     :no-product-cards? (empty? (mapcat :product-cards subsections))
     :loading-products? (utils/requesting? app-state (conj request-keys/get-products
                                                           (skuers/essentials category)))}))
