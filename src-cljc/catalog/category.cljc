(ns catalog.category
  (:require
   #?@(:cljs [[storefront.component :as component]
              [storefront.history :as history]]
       :clj  [[storefront.component-shim :as component]])
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
   [clojure.set :as set]
   [clojure.string :as string]))

(def ^:private query-param-separator "~")

(def ^:private query-params->facet-slugs
  {:grade         :hair/grade
   :family        :hair/family
   :origin        :hair/origin
   :weight        :hair/weight
   :texture       :hair/texture
   :base-material :hair/base-material
   :color         :hair/color
   :length        :hair/length
   :color.process :hair/color.process})

(def ^:private facet-slugs->query-params
  (set/map-invert query-params->facet-slugs))

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
      (merge (utils/fake-href events/control-category-option-clear)
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
  [:div.mt6.mb2 [:p.py6.max-580.mx-auto.center (:copy/description category)]])


(defn human-hair-video [video]
  (when video
    #?(:cljs
       [:div.container
        (ui/youtube-responsive (str (:url video)
                                    "?rel=0&"
                                    "modestbranding=1&"
                                    "widget_referrer=" js/window.location.href))])))

(defn product-cards-empty-state [loading?]
  [:div.col-12.my8.py4.center
   (if loading?
     (ui/large-spinner {:style {:height "4em"}})
     [:div
      [:p.h1.py4 "😞"]
      [:p.h2.dark-gray.py6 "Sorry, we couldn’t find any matches."]
      [:p.h4.dark-gray.mb10.pb10
       [:a.teal (utils/fake-href events/control-category-option-clear) "Clear all filters"]
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
           product-cards]} owner opts]
  (component/create
   [:div
    (hero-section category)
    [:div.max-960.col-12.mx-auto.px2-on-mb
     (copy-section category)
     (human-hair-video (-> category :copy/videos :learn-more))
     [:div.bg-white.sticky.z1
      ;; The -5px prevents a sliver of the background from being visible above the filters
      ;; (when sticky) on android (and sometimes desktop chrome when using the inspector)
      {:style {:top "-5px"}}
      (if open-panel
        [:div
         [:div.hide-on-tb-dt.px2.z4.fixed.overlay.overflow-auto.bg-white
          (filter-tabs category facets product-cards selections open-panel)
          (filter-panel facets represented-options selections open-panel)]
         [:div.hide-on-mb
          (filter-tabs category facets product-cards selections open-panel)
          (filter-panel facets represented-options selections open-panel)]]
        [:div
         (filter-tabs category facets product-cards selections open-panel)])]
     (render-product-cards loading-products? product-cards)]]))

(defn- product-meets-selections?
  [selections product]
  (seq (selector/query (::cached-skus product) selections)))

(defn- cache-complete-skus [all-skus product]
  (assoc product
         ::cached-skus
         (vals (select-keys all-skus (:selector/skus product)))))

(defn- cache-lowest-price [product]
  (assoc product ::cached-lowest-price
         (->> (::cached-skus product)
              (mapv :sku/price)
              sort
              first)))

(defn ^:private query [data]
  (let [category        (categories/current-category data)
        all-skus        (get-in data keypaths/v2-skus)
        category-skus   (selector/strict-query (vals all-skus)
                                               (skuers/essentials category))
        selections      (get-in data catalog.keypaths/category-selections)
        products        (->> (selector/strict-query (vals (get-in data keypaths/v2-products))
                                                    (skuers/essentials category)
                                                    selections
                                                    {:hair/color #{:query/missing}})

                             (into [] (comp
                                       (map (partial cache-complete-skus all-skus))
                                       ;; This is an optimization, do not use elsewhere (900msec -> 50msec)
                                       (filter (partial product-meets-selections? selections))
                                       (map cache-lowest-price)))

                             (sort-by ::cached-lowest-price))]
    {:category            category
     :represented-options (->> category-skus
                               (map (fn [sku]
                                      (->> (select-keys sku
                                                        (concat (:selector/essentials category)
                                                                (:selector/electives category)))
                                           (maps/map-values set))))
                               (reduce (partial merge-with set/union) {}))
     :facets              (maps/index-by :facet/slug (get-in data keypaths/v2-facets))
     :selections          selections
     :product-cards       (map (partial product-card/query data) products)
     :open-panel          (get-in data catalog.keypaths/category-panel)
     :loading-products?   (utils/requesting? data (conj request-keys/search-v2-products
                                                        (skuers/essentials category)))}))

(defn built-component [data opts]
  (component/build component (query data) opts))

(defmethod transitions/transition-state events/navigate-category
  [_ event {:keys [catalog/category-id query-params]} app-state]
  (-> app-state
      (assoc-in catalog.keypaths/category-id category-id)
      (assoc-in catalog.keypaths/category-selections
                (->> (maps/select-rename-keys query-params query-params->facet-slugs)
                     (maps/map-values #(set (string/split % query-param-separator)))))))

#?(:cljs
   (defmethod effects/perform-effects events/navigate-category
     [_ event {:keys [catalog/category-id slug]} _ app-state]
     (let [category   (categories/current-category app-state)
           success-fn #(messages/handle-message events/api-success-v2-products-for-browse
                                                (assoc % :category-id category-id))]
       (if (auth/permitted-category? app-state category)
         (do
           (storefront.api/search-v2-products (get-in app-state keypaths/api-cache)
                                              (skuers/essentials category)
                                              success-fn))
         (effects/redirect events/navigate-home)))))

(defmethod transitions/transition-state events/api-success-v2-products-for-browse
  [_ event {:keys [products skus category-id] :as response} app-state]
  app-state)

(defmethod transitions/transition-state events/control-category-panel-open
  [_ _ {:keys [selected]} app-state]
  (assoc-in app-state catalog.keypaths/category-panel selected))

(defmethod transitions/transition-state events/control-category-panel-close
  [_ _ _ app-state]
  (assoc-in app-state catalog.keypaths/category-panel nil))

#?(:cljs
   (defmethod effects/perform-effects events/control-category-option
     [_ _ _ _ app-state]
     (let [{:keys [catalog/category-id page/slug]}   (categories/current-category app-state)]
       (->> (get-in app-state catalog.keypaths/category-selections)
            (maps/map-values (fn [s] (string/join query-param-separator s)))
            (maps/map-keys facet-slugs->query-params)
            (assoc {:catalog/category-id category-id
                    :page/slug           slug}
                   :query-params)
            (history/enqueue-redirect events/navigate-category)))))

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

(defmethod transitions/transition-state events/control-category-option-clear
  [_ _ _ app-state]
  (assoc-in app-state catalog.keypaths/category-selections {}))
