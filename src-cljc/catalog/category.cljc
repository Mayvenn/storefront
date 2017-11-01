(ns catalog.category
  (:require
   #?(:cljs [storefront.component :as component]
      :clj  [storefront.component-shim :as component])
   [catalog.category-filters :as category-filters]
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
   [storefront.request-keys :as request-keys]))

(defn filter-tabs [category-criteria {:keys [facets filtered-sku-sets criteria]}]
  (let [sku-set-count        (count filtered-sku-sets)
        applied-filter-count (->> (apply dissoc criteria (keys category-criteria))
                                  (map (comp count val))
                                  (apply +))]
    [:div.py4
     (when (seq facets)
       [:div
        [:div.pb1.flex.justify-between
         [:p.h6.dark-gray (case applied-filter-count
                            0 "Filter by:"
                            1 "1 filter applied:"
                            (str applied-filter-count " filters applied:"))]
         [:p.h6.dark-gray (str sku-set-count " Item" (when (not= 1 sku-set-count) "s"))]]
        (into [:div.border.h6.border-teal.rounded.flex.center]
              (map-indexed
               (fn [idx {:keys [slug title selected?]}]
                 [:a.flex-auto.x-group-item.rounded-item
                  (assoc
                   (if selected?
                     (utils/fake-href events/control-category-filters-close)
                     (utils/fake-href events/control-category-filter-select {:selected slug}))
                   :key slug
                   :class (if selected? "bg-teal white" "dark-gray"))
                  [:div.border-teal.my1
                   {:class (when-not (zero? idx) "border-left")}
                   title]])
               facets))])]))

(defn filter-panel [selected-facet]
  [:div.px1
   (for [options (partition-all 4 (:options selected-facet))]
     [:div.flex-on-tb-dt.justify-around
      (for [{:keys [slug label represented? selected?]} options]
        [:div.py1.mr4
         {:key (str "filter-option-" slug)}
         (ui/check-box {:label     [:span
                                    (when (categories/new-facet? [(:slug selected-facet) slug]) [:span.mr1.teal "NEW"])
                                    label]
                        :value     selected?
                        :disabled  (not represented?)
                        :on-change #(let [event-handler (if selected?
                                                          events/control-category-criterion-deselected
                                                          events/control-category-criterion-selected)]
                                      (messages/handle-message event-handler
                                                               {:filter (:slug selected-facet)
                                                                :option slug}))})])])
   [:div.clearfix.mxn3.px1.py4.hide-on-tb-dt
    [:div.col.col-6.px3
     (ui/teal-ghost-button
      (utils/fake-href events/control-category-criteria-cleared)
      "Clear all")]
    [:div.col.col-6.px3
     (ui/teal-button
      (utils/fake-href events/control-category-filters-close)
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
       [:a.teal (utils/fake-href events/control-category-criteria-cleared) "Clear all filters"]
       " to see more hair."]])])

(defn product-cards [loading? sku-sets facets affirm? dyed-hair?]
  [:div.flex.flex-wrap.mxn1
   (if (empty? sku-sets)
     (product-cards-empty-state loading?)
     (for [product sku-sets
           :let [product-experiment-set (set (:experiment/dyed-hair product))]
           :when (contains? product-experiment-set
                            (if dyed-hair? "experiment" "control"))]
       (product-card/component product facets affirm?)))])

(defn ^:private component [{:keys [category filters facets loading-products? affirm? dyed-hair?]} owner opts]
  (let [category-criteria (:criteria category)]
    (component/create
     [:div
      (hero-section category)
      [:div.max-960.col-12.mx-auto.px2-on-mb
       (copy-section category)
       [:div.bg-white.sticky
        ;; The -5px prevents a sliver of the background from being visible above the filters
        ;; (when sticky) on android (and sometimes desktop chrome when using the inspector)
        {:style {:top "-5px"}}
        (if-let [selected-facet (->> filters
                                     :facets
                                     (filter :selected?)
                                     first)]
          [:div
           [:div.hide-on-tb-dt.px2.z4.fixed.overlay.overflow-auto.bg-white
            (filter-tabs category-criteria filters)
            (filter-panel selected-facet)]
           [:div.hide-on-mb
            (filter-tabs category-criteria filters)
            (filter-panel selected-facet)]]
          [:div
           (filter-tabs category-criteria filters)])]
       (product-cards loading-products? (:filtered-sku-sets filters) facets affirm? dyed-hair?)]])))

(defn ^:private query [data]
  (let [category (categories/current-category data)]
    {:category          category
     :filters           (get-in data keypaths/category-filters-for-browse)
     :facets            (get-in data keypaths/facets)
     :affirm?           (experiments/affirm? data)
     :dyed-hair?        (experiments/dyed-hair? data)
     :loading-products? (utils/requesting? data (conj request-keys/search-sku-sets
                                                      (:criteria category)))}))

(defn built-component [data opts]
  (component/build component (query data) opts))

(defmethod transitions/transition-state events/navigate-category
  [_ event {:keys [catalog/category-id]} app-state]
  (assoc-in app-state keypaths/current-category-id category-id))

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
                                           (:criteria category)
                                           success-fn))
         (effects/redirect events/navigate-home)))))

(defmethod transitions/transition-state events/api-success-sku-sets-for-browse
  [_ event {:keys [sku-sets] :as response} app-state]
  (-> app-state
      (assoc-in keypaths/category-filters-for-browse
                (categories/make-category-filters app-state response))))

(defmethod transitions/transition-state events/control-category-filter-select
  [_ _ {:keys [selected]} app-state]
  (update-in app-state
             keypaths/category-filters-for-browse
             category-filters/open selected))

(defmethod transitions/transition-state events/control-category-filters-close
  [_ _ _ app-state]
  (update-in app-state
             keypaths/category-filters-for-browse
             category-filters/close))

(defmethod transitions/transition-state events/control-category-criterion-selected
  [_ _ {:keys [filter option]} app-state]
  (update-in app-state
             keypaths/category-filters-for-browse
             category-filters/select-criterion filter option))

(defmethod transitions/transition-state events/control-category-criterion-deselected
  [_ _ {:keys [filter option]} app-state]
  (update-in app-state
             keypaths/category-filters-for-browse
             category-filters/deselect-criterion filter option))

(defmethod transitions/transition-state events/control-category-criteria-cleared
  [_ _ _ app-state]
  (update-in app-state
             keypaths/category-filters-for-browse
             category-filters/clear-criteria))
