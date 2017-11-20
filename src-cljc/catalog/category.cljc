(ns catalog.category
  (:require
   #?(:cljs [storefront.component :as component]
      :clj  [storefront.component-shim :as component])
   [catalog.categories :as categories]
   [catalog.product-card :as product-card]
   [storefront.components.ui :as ui]
   [storefront.events :as events]
   [storefront.transitions :as transitions]
   [storefront.effects :as effects]
   [storefront.keypaths :as keypaths]
   [storefront.platform.component-utils :as utils]
   [storefront.platform.messages :as messages]
   [storefront.accessors.experiments :as experiments]
   [storefront.accessors.auth :as auth]
   [storefront.request-keys :as request-keys]
   [catalog.products :as products]
   [catalog.keypaths]
   [catalog.skuers :as skuers]
   [spice.maps :as maps]
   [catalog.selector :as selector]
   [clojure.set :as set]))

(defn filter-tabs [{:keys [selector/essentials selector/electives]}
                   facets
                   product-cards
                   selections
                   open-panel]
  (let [product-count    (count product-cards)
        selections-count (->> (apply dissoc selections essentials)
                              (map (comp count val))
                              (apply +))]
    [:div.py4
     (when (seq electives)
       [:div
        [:div.pb1.flex.justify-between
         [:p.h6.dark-gray (case selections-count
                            0 "Filter by:"
                            1 "1 filter applied:"
                            (str selections-count " filters applied:"))]
         [:p.h6.dark-gray (str product-count " Item" (when (not= 1 product-count) "s"))]]
        (into [:div.border.h6.border-teal.rounded.flex.center]
              (map-indexed
               (fn [idx elective]
                 (let [facet (elective facets)
                       selected? (= open-panel elective)
                       title (:facet/name facet)]
                   [:a.flex-auto.x-group-item.rounded-item
                    (assoc
                     (if selected?
                       (utils/fake-href events/control-category-panel-close)
                       (utils/fake-href events/control-category-panel-open {:selected elective}))
                     :data-test (str "filter-" (name elective))
                     :key elective
                     :class (if selected? "bg-teal white" "dark-gray"))
                    [:div.border-teal.my1
                     {:class (when-not (zero? idx) "border-left")}
                     title]]))
               electives))])]))

(defn filter-panel [facets represented-options selections open-panel]
  [:div.px1
   (for [options (->> facets
                      open-panel
                      :facet/options
                      (sort-by :option/order)
                      (partition-all 4))]
     [:div.flex-on-tb-dt.justify-around
      (for [option options]
        (let [selected?    (contains? (open-panel selections)
                                      (:option/slug option))
              slug         (:option/slug option)
              represented? (contains? (open-panel represented-options) slug)]
          ;;{:keys [slug label represented? selected?]}
          [:div.py1.mr4
           {:key       (str "filter-option-" slug)
            :data-test (str "filter-option-" slug)}
           (ui/check-box {:label     [:span
                                      (when (categories/new-facet? [open-panel slug])
                                        [:span.mr1.teal "NEW"])
                                      (:option/name option)]
                          :value     selected?
                          :disabled  (not represented?)
                          :on-change #(let [event-handler (if selected?
                                                            events/control-category-option-unselect
                                                            events/control-category-option-select)]
                                        (messages/handle-message event-handler
                                                                 {:facet  open-panel
                                                                  :option slug}))})]))])
   [:div.clearfix.mxn3.px1.py4.hide-on-tb-dt
    [:div.col.col-6.px3
     (ui/teal-ghost-button
      (merge (utils/fake-href events/control-category-options-clear)
             {:data-test "filters-clear-all"})
      "Clear all")]
    [:div.col.col-6.px3
     (ui/teal-button
      (merge (utils/fake-href events/control-category-panel-close)
             {:data-test "filters-done"})
      "Done")]]])

(defn hero-section [category]
  [:h1
   (let [{:keys [mobile-url file-name desktop-url alt]} (-> category :images :hero)]
     (when (and mobile-url desktop-url)
       [:picture
        [:source {:media   "(min-width: 750px)"
                  :src-set (str desktop-url "-/format/auto/" file-name " 1x")}]
        [:img.block.col-12 {:src (str mobile-url "-/format/auto/" file-name)
                            :alt alt}]]))])

(defn copy-section [category]
  [:div.mt6.mb2 [:p.py6.max-580.mx-auto.center (-> category :copy :description)]])

(defn product-cards-empty-state [loading?]
  [:div.col-12.my8.py4.center
   (if loading?
     (ui/large-spinner {:style {:height "4em"}})
     [:div
      [:p.h1.py4 "😞"]
      [:p.h2.dark-gray.py6 "Sorry, we couldn’t find any matches."]
      [:p.h4.dark-gray.mb10.pb10
       [:a.teal (utils/fake-href events/control-category-options-clear) "Clear all filters"]
       " to see more hair."]])])

(defn render-product-cards [loading? product-cards]
  [:div.flex.flex-wrap.mxn1
   (if (empty? product-cards)
     (product-cards-empty-state loading?)
     (map product-card/component product-cards))])

(defn ^:private component
  [{:keys [category
           facets
           loading-products?
           selections
           represented-options
           open-panel
           product-cards
           options]} owner opts]
  (component/create
   [:div
    (hero-section category)
    [:div.max-960.col-12.mx-auto.px2-on-mb
     (copy-section category)
     [:div.bg-white.sticky.z1
      ;; The -5px prevents a sliver of the background from being visible above the filters
      ;; (when sticky) on android (and sometimes desktop chrome when using the inspector)
      {:style {:top "-5px"}}
      (if open-panel
        [:div
         [:div.hide-on-tb-dt.px2.z4.fixed.overlay.overflow-auto.bg-white
          (filter-tabs category facets product-cards options open-panel)
          (filter-panel facets represented-options selections open-panel)]
         [:div.hide-on-mb
          (filter-tabs category facets product-cards options open-panel)
          (filter-panel facets represented-options selections open-panel)]]
        [:div
         (filter-tabs category facets product-cards options open-panel)])]
     (render-product-cards loading-products? product-cards)]]))

(defn ^:private query [data]
  (let [category      (categories/current-category data)
        category-skus (selector/strict-query (vals (get-in data keypaths/skus))
                                             (skuers/essentials category))
        selections    (get-in data catalog.keypaths/category-selections)
        products      (->> (selector/strict-query (vals (get-in data keypaths/sku-sets))
                                                  (skuers/essentials category)
                                                  selections
                                                  {:hair/color #{:query/missing}})
                           (filter (fn skus-exist-for-product
                                     [product]
                                     (seq (selector/query category-skus
                                                          (skuers/essentials product)
                                                          selections))))
                           (sort-by #(->> (select-keys (get-in data keypaths/skus) (:selector/skus %))
                                          vals
                                          (apply min-key :price)
                                          :price)))]
    {:category            category
     :represented-options (->> category-skus
                               (map (fn [sku]
                                      (select-keys sku (concat (:selector/essentials category)
                                                               (:selector/electives category)))))
                               maps/into-multimap)
     :facets              (maps/index-by :facet/slug (get-in data keypaths/facets))
     :selections          selections
     :product-cards       (map (partial product-card/query data) products)
     :open-panel          (get-in data catalog.keypaths/category-panel)
     :loading-products?   (utils/requesting? data (conj request-keys/search-sku-sets
                                                        (skuers/essentials category)))}))

(defn built-component [data opts]
  (component/build component (query data) opts))

(defmethod transitions/transition-state events/navigate-category
  [_ event {:keys [catalog/category-id query-params]} app-state]
  (-> app-state
      (assoc-in catalog.keypaths/category-id category-id)
      ;;TODO transform query-params back into proper query format.
      ;;TODO validate that the new query-params are allowed selections (ie keys are in the set of category electives)
      (assoc-in catalog.keypaths/category-selections {})))

#?(:cljs
   (defmethod effects/perform-effects events/navigate-category
     [_ event {:keys [catalog/category-id slug]} _ app-state]
     (let [category   (categories/current-category app-state)
           success-fn #(messages/handle-message events/api-success-sku-sets-for-browse
                                                (assoc % :category-id category-id))]
       (if (auth/permitted-category? app-state category)
         (do
           (storefront.api/fetch-facets (get-in app-state keypaths/api-cache))
           (storefront.api/search-sku-sets (get-in app-state keypaths/api-cache)
                                           (skuers/essentials category)
                                           success-fn))
         (effects/redirect events/navigate-home)))))

(defmethod transitions/transition-state events/api-success-sku-sets-for-browse
  [_ event {:keys [sku-sets skus category-id] :as response} app-state]
  app-state)

(defmethod transitions/transition-state events/control-category-panel-open
  [_ _ {:keys [selected]} app-state]
  (assoc-in app-state catalog.keypaths/category-panel selected))

(defmethod transitions/transition-state events/control-category-panel-close
  [_ _ _ app-state]
  (assoc-in app-state catalog.keypaths/category-panel nil))

(defmethod transitions/transition-state events/control-category-option-select
  [_ _ {:keys [facet option]} app-state]
  (update-in app-state (conj catalog.keypaths/category-selections facet) set/union #{option}))

(defmethod transitions/transition-state events/control-category-option-unselect
  [_ _ {:keys [facet option]} app-state]
  (let [facet-path (conj catalog.keypaths/category-selections facet)
        facet-selections (set/difference (get-in app-state facet-path)
                                         #{option})]
    (if (empty? facet-selections)
      (update-in app-state catalog.keypaths/category-selections dissoc facet)
      (assoc-in app-state facet-path facet-selections))))

(defmethod transitions/transition-state events/control-category-options-clear
  [_ _ _ app-state]
  (assoc-in app-state catalog.keypaths/category-selections {}))
