(ns catalog.category
  (:require
   #?@(:cljs [[storefront.browser.scroll :as scroll]
              [storefront.api :as api]
              [storefront.history :as history]])
   [storefront.component :as component]
   [catalog.categories :as categories]
   [catalog.product-card :as product-card]
   [storefront.components.ui :as ui]
   [storefront.events :as events]
   [storefront.transitions :as transitions]
   [storefront.effects :as effects]
   [storefront.keypaths :as keypaths]
   [storefront.platform.component-utils :as utils]
   [storefront.platform.messages :as messages]
   [storefront.accessors.auth :as auth]
   [storefront.request-keys :as request-keys]
   [catalog.keypaths]
   [catalog.skuers :as skuers]
   [spice.maps :as maps]
   [spice.selector :as selector]
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
                   all-product-cards
                   selections
                   open-panel]
  (let [product-count    (count all-product-cards)
        selections-count (->> (apply dissoc selections essentials)
                              (map (comp count val))
                              (apply +))]
    [:div.py4.mxn2.px2.bg-white
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
     [:div.flex-on-dt.justify-around
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
   [:div.clearfix.mxn3.px1.py4.hide-on-dt
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

(defn render-subsection [loading? {:keys [product-cards image/mob-url image/dsk-url copy]}]
  [:div
   (when (and mob-url dsk-url copy)
     [:div.pb6.flex.flex-column
      [:div.hide-on-mb-tb.mx1
       [:img.col.col-12 {:src dsk-url}]]
      [:div.mxn2.hide-on-dt
       [:img.col.col-12 {:src mob-url}]]
      [:div.mx-auto.col.col-11.h5.dark-gray.center.pt2 copy]])
   [:div.flex.flex-wrap
    (map product-card/component product-cards)]])

(defn ^:private component
  [{:keys [category
           facets
           loading-products?
           selections
           represented-options
           open-panel
           all-product-cards
           subsections]} owner opts]
  (component/create
   [:div
    (hero-section category)
    [:div.max-960.col-12.mx-auto.px2-on-mb.px2-on-tb
     (copy-section category)
     [:div.bg-white.sticky.z1
      ;; The -5px prevents a sliver of the background from being visible above the filters
      ;; (when sticky) on android (and sometimes desktop chrome when using the inspector)
      {:style {:top "-5px"}}
      (if open-panel
        [:div
         [:div.hide-on-dt.px2.z4.fixed.overlay.overflow-auto.bg-white
          (filter-tabs category facets all-product-cards selections open-panel)
          (filter-panel facets represented-options selections open-panel)]
         [:div.hide-on-mb-tb
          (filter-tabs category facets all-product-cards selections open-panel)
          (filter-panel facets represented-options selections open-panel)]]
        [:div
         (filter-tabs category facets all-product-cards selections open-panel)])]
     [:div.flex.flex-wrap
      (if (empty? all-product-cards)
        (product-cards-empty-state loading-products?)
        (map (partial render-subsection loading-products?) subsections))]]]))

(defn ^:private query
  [data]
  (let [category      (categories/current-category data)
        selections    (get-in data catalog.keypaths/category-selections )
        products-matching-category (selector/match-all {:selector/strict? true}
                                                       (skuers/essentials category)
                                                       (vals (get-in data keypaths/v2-products)))
        products-matching-criteria (selector/match-all {:selector/strict? true}
                                                       (merge
                                                        (skuers/essentials category)
                                                        selections)
                                                       products-matching-category)
        subsections                    (->> products-matching-criteria
                                            (group-by (or (categories/category-id->subsection-fn (:catalog/category-id category))
                                                          (constantly :no-subsections)))
                                            (spice.maps/map-values (partial map (partial product-card/query data)))
                                            (spice.maps/map-values (partial sort-by (comp :sku/price :cheapest-sku)))
                                            (map (fn [[k cards]]
                                                   (-> category
                                                       :subsections
                                                       (get k)
                                                       (assoc :product-cards cards))))
                                            (sort-by :order))
        product-cards                  (mapcat :product-cards subsections)]
    {:category            category
     :represented-options (->> products-matching-category
                               (map (fn [skuer]
                                      (->> (select-keys skuer
                                                        (concat (:selector/essentials category)
                                                                (:selector/electives category)))
                                           (maps/map-values set))))
                               (reduce (partial merge-with set/union) {}))
     :facets              (maps/index-by :facet/slug (get-in data keypaths/v2-facets))
     :selections          selections
     :all-product-cards   product-cards
     :subsections         subsections
     :open-panel          (get-in data catalog.keypaths/category-panel)
     :loading-products?   (utils/requesting? data (conj request-keys/search-v2-products
                                                        (skuers/essentials category)))}))

(defn ^:export built-component
  [data opts]
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
       ;; Some pages may disable scrolling on the body, e.g.: product detail page
       ;; and it must be re-enabled for this page
       (scroll/enable-body-scrolling)
       (if (auth/permitted-category? app-state category)
         (api/search-v2-products (get-in app-state keypaths/api-cache)
                                 (skuers/essentials category)
                                 success-fn)
         (effects/redirect events/navigate-home)))))

;; TODO: why?
(defmethod transitions/transition-state events/api-success-v2-products-for-browse
  [_ event {:keys [products skus category-id] :as response} app-state]
  app-state)

(defmethod transitions/transition-state events/control-category-panel-open
  [_ _ {:keys [selected]} app-state]
  (-> app-state
      (assoc-in keypaths/hide-header? selected)
      (assoc-in catalog.keypaths/category-panel selected)))

(defmethod transitions/transition-state events/control-category-panel-close
  [_ _ _ app-state]
  (-> app-state
      (assoc-in keypaths/hide-header? nil)
      (assoc-in catalog.keypaths/category-panel nil)))

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
