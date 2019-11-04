(ns catalog.category
  (:require
   #?@(:cljs [[storefront.browser.scroll :as scroll]
              [storefront.api :as api]
              [storefront.effects :as effects]
              [storefront.accessors.auth :as auth]
              [storefront.history :as history]])
   [storefront.component :as component :refer [defcomponent]]
   [catalog.categories :as categories]
   [storefront.components.ui :as ui]
   [storefront.events :as events]
   [storefront.transitions :as transitions]
   [storefront.keypaths :as keypaths]
   [storefront.platform.component-utils :as utils]
   [storefront.platform.messages :as messages]
   [storefront.request-keys :as request-keys]
   [catalog.keypaths]
   [catalog.skuers :as skuers]
   [spice.maps :as maps]
   [spice.selector :as selector]
   [clojure.set :as set]
   [clojure.string :as string]
   [catalog.ui.product-card :as product-card]

   [storefront.component :as component :refer [defcomponent]]
   [storefront.component :as component :refer [defcomponent]]))

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
   (for [[i options] (->> facets
                          open-panel
                          :facet/options
                          (sort-by :option/order)
                          (partition-all 4)
                          (map-indexed vector))]
     [:div.flex-on-dt.justify-around
      {:key (str "filter-panel-" i)}
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

(defn hero-section
  [category]
  (component/html
   [:h1 {:style {:min-height "180px"}} ; To aid scroll-to estimation
    (let [{:keys [mobile-url file-name desktop-url alt]} (-> category :images :hero)]
      (when (and mobile-url desktop-url)
        [:picture
         [:source {:media   "(min-width: 750px)"
                   :src-set (str desktop-url "-/format/auto/" file-name " 1x")}]
         [:img.block.col-12 {:src   (str mobile-url "-/format/auto/" file-name)
                             :alt   alt}]]))]))

(defn copy-section
  [category]
  (component/html
   [:div.center.mx2.pt6
    (when (:category/show-title? category)
      [:div
       (when (:category/new? category)
         [:div.purple.h7.medium.mbn1 "NEW!"])
       [:div.h1 (:copy/title category)]])
    [:div.h5.dark-gray.light.my2.mx6-on-mb.col-8-on-tb-dt.mx-auto-on-tb-dt
     (:copy/description category)
     (when-let [learn-more-event (:copy/learn-more category)]
       [:a.teal.h6.medium
        {:on-click (apply utils/send-event-callback learn-more-event)}
        "learn" ui/nbsp "more"])]]))

(defn product-cards-empty-state [loading?]
  (component/html
   [:div.col-12.my8.py4.center
    (if loading?
      (ui/large-spinner {:style {:height "4em"}})
      [:div
       [:p.h1.py4 "😞"]
       [:p.h2.dark-gray.py6 "Sorry, we couldn’t find any matches."]
       [:p.h4.dark-gray.mb10.pb10
        [:a.teal (utils/fake-href events/control-category-option-clear) "Clear all filters"]
        " to see more hair."]])]))

(defn ^:private subsection-component
  [{:keys         [product-cards image/mob-url image/dsk-url copy subsection-key]
    primary-title :title/primary
    title-side    :title/side}]
  (component/html
   (let [subsection-id (str "subsection-" subsection-key)]
     [:div
      {:key subsection-id
       :id  subsection-id}
      (when (and mob-url dsk-url copy)
        (if (= "bottom" title-side)
          [:div.pb6.flex.flex-column
           [:div.hide-on-mb-tb.mx1
            (ui/defer-ucare-img {:class "col col-12 container-height"} dsk-url)]
           [:div.mxn2.hide-on-dt
            (ui/defer-ucare-img {:class "col col-12"} mob-url)]
           [:div.mx-auto.col.col-11.h5.dark-gray.center.pt2 copy]]
          [:div.flex.flex-column
           [:div.hide-on-mb-tb.mx1 ;; dt
            (ui/aspect-ratio
             950 223.7
             [:div.col.col-12.relative
              {:style {:min-height "223.7px"}}
              [:div.absolute.container-size
               [:div.container-height.col-6.flex.items-center
                {:class (str (case title-side
                               "right" "justify-start"
                               "left"  "justify-end"
                               "justify-start")
                             " "
                             title-side)}
                [:div.p3.col-10
                 [:div.h2.mb1.bold primary-title]
                 [:div.h5 copy]]]]
              (ui/defer-ucare-img {:class "col-12 container-height"} dsk-url)])]
           [:div.mxn2.hide-on-dt.pt3.px3.pb1 ;; mb, tb
            (ui/aspect-ratio
             345 200
             [:div.col.col-12.relative
              {:style {:min-height "200px"}}
              [:div.absolute.container-size
               [:div.container-height.col-6.flex.items-center.justify-center
                {:class title-side}
                [:div.p3
                 [:div.h3.mb1.bold primary-title]
                 [:div.h7 copy]]]]
              (ui/defer-ucare-img {:class "col-12 container-height"} mob-url)])]]))
      [:div.flex.flex-wrap
       (map product-card/organism product-cards)]])))

(defcomponent ^:private component
  [{:keys [category
           facets
           loading-products?
           selections
           represented-options
           open-panel
           all-product-cards
           subsections]} owner opts]
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
        [:div.hide-on-dt
         (filter-tabs category facets all-product-cards selections open-panel)]
        [:div.hide-on-mb-tb.pb6
         (filter-tabs category facets all-product-cards selections open-panel)]])]
    [:div.flex.flex-wrap
     (if (empty? all-product-cards)
       (product-cards-empty-state loading-products?)
       (map subsection-component subsections))]]])

(defn subsections-query
  [{:keys [catalog/category-id subsections]}
   products-matching-criteria
   data]
  (->> products-matching-criteria
       (group-by (or (categories/category-id->subsection-fn category-id)
                     (constantly :no-subsections)))
       (sequence
        (comp
         (map (fn [[subsection-key products]] (assoc (get subsections subsection-key)
                                                     :products products
                                                     :subsection-key subsection-key)))
         (map #(update % :products (partial map (partial product-card/query data))))
         (map #(set/rename-keys % {:products :product-cards}))
         (map #(update % :product-cards (partial sort-by :sort/value)))))
       (sort-by :order)))

(defn ^:private query
  [data]
  (let [category                   (categories/current-category data)
        selections                 (get-in data catalog.keypaths/category-selections)
        products-matching-category (selector/match-all {:selector/strict? true}
                                                       (skuers/essentials category)
                                                       (vals (get-in data keypaths/v2-products)))
        products-matching-criteria (selector/match-all {:selector/strict? true}
                                                       (merge
                                                        (skuers/essentials category)
                                                        selections)
                                                       products-matching-category)
        subsections                (subsections-query category
                                                      products-matching-criteria
                                                      data)
        product-cards (mapcat :product-cards subsections)]
    {:category            category
     :represented-options (->> products-matching-category
                               (map (fn [product]
                                      (->> (select-keys product
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
     [_ event {:keys [catalog/category-id slug query-params]} _ app-state]
     (let [category   (categories/current-category app-state)
           success-fn #(messages/handle-message events/api-success-v2-products-for-browse
                                                (assoc % :category-id category-id))]
       ;; Some pages may disable scrolling on the body, e.g.: product detail page
       ;; and it must be re-enabled for this page
       (scroll/enable-body-scrolling)
       (let [store-experience (get-in app-state keypaths/store-experience)]
         (when (and (= "mayvenn-classic"
                       store-experience)
                    (contains? (:experience/exclude category)
                               "mayvenn-classic"))
           (effects/redirect events/navigate-home)))
       (if (auth/permitted-category? app-state category)
         (api/search-v2-products (get-in app-state keypaths/api-cache)
                                 (skuers/essentials category)
                                 success-fn)
         (effects/redirect events/navigate-home))
       (when-let [subsection-key (:subsection query-params)]
         (js/setTimeout (partial scroll/scroll-selector-to-top (str "#subsection-" subsection-key)) 0)))))

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
     (let [{:keys [catalog/category-id page/slug]} (categories/current-category app-state)]
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
