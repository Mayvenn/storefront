(ns catalog.ui.product-list
  (:require
   #?@(:cljs [goog.events
              [goog.events.EventType :as EventType]
              [storefront.effects :as effects]
              [storefront.history :as history]
              [storefront.accessors.categories :as accessors.categories]
              [goog.object :as object]])
   [catalog.categories :as categories]
   [catalog.keypaths]
   [catalog.skuers :as skuers]
   [catalog.ui.product-card :as product-card]
   [clojure.string :as string]
   [spice.maps :as maps]
   [spice.selector :as selector]
   [storefront.component :as component :refer [defcomponent defdynamic-component]]
   [storefront.components.ui :as ui]
   [storefront.events :as events]
   [storefront.keypaths :as keypaths]
   [storefront.platform.component-utils :as utils]
   [storefront.platform.messages :as messages]
   [storefront.request-keys :as request-keys]
   [storefront.transitions :as transitions]
   clojure.set))

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

(defn- hacky-fix-of-bad-slugs-on-facets [slug]
  (string/replace (str slug) #"#" ""))

(defdynamic-component ^:private filter-tabs
  (constructor [this props]
               (component/create-ref! this "filter-tabs")
               #?(:cljs
                  (set! (.-handle-scroll this)
                        (fn [e] (component/set-state! this :stuck? (-> (component/get-ref this "filter-tabs")
                                                                       .getBoundingClientRect
                                                                       (object/get "top")
                                                                       (<= 0))))))
               {:stuck? false})
  (did-mount [this]
   #?(:cljs (goog.events/listen js/window EventType/SCROLL (.-handle-scroll this))))
  (will-unmount [this]
   #?(:cljs (goog.events/unlisten js/window EventType/SCROLL (.-handle-scroll this))))
  (render
   [this]
   (let [{:keys [stuck?]}     (component/get-state this)
         {:keys [open-panel
                 electives
                 selections-count
                 product-count
                 facets]} (component/get-props this)]
     (component/html
      [:div.p2.pt3.mxn3.bg-white
       (merge {:ref (component/use-ref this "filter-tabs")}
              (when (and (not open-panel) stuck?)
                {:class "border-black border-bottom top-lit"}))
       (when (seq electives)
         [:div
          [:div.flex.justify-between.items-baseline
           [:div.title-3.proxima.shout.bold
            (case selections-count
              0 "Filter by"
              1 "1 filter applied"
              (str selections-count " filters applied"))]
           [:p.content-3 (str product-count " item" (when (not= 1 product-count) "s"))]]
          (into [:div.content-2.flex.center]
                (map
                 (fn [elective]
                   (let [facet     (elective facets)
                         selected? (= open-panel elective)
                         title     (:facet/name facet)]
                     [:a.flex-auto.x-group-item.border.border-black.py1.whisper.black
                      (merge
                       (if selected?
                         (utils/fake-href events/control-category-panel-close)
                         (utils/fake-href events/control-category-panel-open {:selected elective}))
                       {:data-test (str "filter-" (name elective))
                        :key       elective
                        :style     (when selected? {:background "linear-gradient(to bottom, #4427c1 4px, #ffffff 4px)"})
                        :class     (when open-panel "bg-cool-gray")})
                      ;; This extra div is for pixel-pushing
                      [:div.pyp2 title]]))
                 electives))])]))))

(defn ^:private filter-panel
  [facets represented-options selections open-panel]
  [:div
   [:div.content-1.proxima.py6.pl10.pr1
    (for [[i options] (->> facets
                           open-panel
                           :facet/options
                           (sort-by :option/order)
                           (partition-all 4)
                           (map-indexed vector))]
      [:div.flex-on-dt.justify-between.items-center
       {:key (str "filter-panel-" i)}
       (for [option options]
         (let [selected?    (contains? (open-panel selections)
                                       (:option/slug option))
               slug         (:option/slug option)
               represented? (contains? (open-panel represented-options) slug)]
           [:div.py1.mr4.col-3-on-dt
            {:key       (str "filter-option-" slug)
             :data-test (str "filter-option-" (hacky-fix-of-bad-slugs-on-facets slug))
             :disabled  (not represented?)}
           (ui/check-box {:label     [:span
                                      (when (categories/new-facet? [open-panel slug])
                                        [:span.mr1.p-color "NEW"])
                                      (:option/name option)]
                          :value     selected?
                          :disabled  (not represented?)
                          :on-change #(let [event-handler (if selected?
                                                            events/control-category-option-unselect
                                                            events/control-category-option-select)]
                                        (messages/handle-message event-handler
                                                                 {:facet  open-panel
                                                                  :option slug}))})]))])]
   [:div.clearfix.mxn3.mb2.hide-on-dt.flex.justify-around.items-center
    [:div.col-6.center.px5
     (ui/button-medium-underline-primary
      (merge (utils/fake-href events/control-category-option-clear)
             {:data-test "filters-clear-all"})
      "reset")]
    [:div.col-6.px5
     (ui/button-medium-primary
      (merge (utils/fake-href events/control-category-panel-close)
             {:data-test "filters-done"})
      "Done")]]])

(defcomponent ^:private product-cards-empty-state
  [_ _ _]
  [:div.col-12.my8.py4.center
   [:p.h1.py4 "😞"]
   [:p.h2.py6 "Sorry, we couldn’t find any matches."]
   [:p.h4.mb10.pb10
    [:a.p-color (utils/fake-href events/control-category-option-clear) "Clear all filters"]
    " to see more hair."]])

(defcomponent ^:private product-list-subsection-component
  [{:keys [product-cards subsection-key] primary-title :title/primary} _ {:keys [id]}]
  [:div
   {:id id :data-test id}
   (when primary-title
     [:div.canela.title-2.center.mt8.mb2 primary-title])
   [:div.flex.flex-wrap
    (map product-card/organism product-cards)]])

(defcomponent organism
  [{:keys [title subsections all-product-cards loading-products? filter-tabs-data]} _ _]
  [:div.px2.py4
   [:div.canela.title-1.center.mt3.py4 title]
   [:div.px1.bg-white.sticky.z1
    ;; The -5px prevents a sliver of the background from being visible above the filters
    ;; (when sticky) on android (and sometimes desktop chrome when using the inspector)
    {:style {:top "-5px"}}
    (let [tabs                                                       (component/build filter-tabs filter-tabs-data nil)
          {:keys [open-panel facets selections represented-options]} filter-tabs-data]
      (if open-panel
        [:div
         [:div.hide-on-dt.px2.z4.fixed.overlay.overflow-auto.bg-white
          tabs (filter-panel facets represented-options selections open-panel)]
         [:div.hide-on-mb-tb
          tabs (filter-panel facets represented-options selections open-panel)]]
        [:div
         [:div.hide-on-dt tabs]
         [:div.hide-on-mb-tb tabs]]))]

   (cond
     loading-products?          [:div.col-12.my8.py4.center (ui/large-spinner {:style {:height "4em"}})]

     (empty? all-product-cards) (component/build product-cards-empty-state {} {})

     :else                      (mapv (fn build [{:as subsection :keys [subsection-key]}]
                                        (component/build product-list-subsection-component
                                                         subsection
                                                         (component/component-id (str "subsection-" subsection-key))))
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
                                    app-state)
        product-cards              (mapcat :product-cards subsections)
        open-panel                 (get-in app-state catalog.keypaths/category-panel)]
    {:title             (:product-list/title category)
     :subsections       subsections
     :all-product-cards (mapcat :product-cards subsections)
     :loading-products? (utils/requesting? app-state (conj request-keys/get-products
                                                           (skuers/essentials category)))
     :filter-tabs-data  {:open-panel          open-panel
                         :selections          selections
                         :electives           (:selector/electives category)
                         :product-count       (count product-cards)
                         :represented-options (->> products-matching-category
                                                   (map (fn [product]
                                                          (->> (select-keys product
                                                                            (concat (:selector/essentials category)
                                                                                    (:selector/electives category)))
                                                               (maps/map-values set))))
                                                   (reduce (partial merge-with clojure.set/union) {}))
                         :selections-count    (->> (apply dissoc selections (:selector/essentials category))
                                                   (map (comp count val))
                                                   (apply +))
                         :facets              facets}}))

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
     (let [{:keys [catalog/category-id page/slug]} (accessors.categories/current-category app-state)]
       (->> (get-in app-state catalog.keypaths/category-selections)
            accessors.categories/category-selections->query-params
            (assoc {:catalog/category-id category-id
                    :page/slug           slug}
                   :query-params)
            (history/enqueue-redirect events/navigate-category)))))

(defmethod transitions/transition-state events/control-category-option-select
  [_ _ {:keys [facet option]} app-state]
  (update-in app-state (conj catalog.keypaths/category-selections facet) clojure.set/union #{option}))

(defmethod transitions/transition-state events/control-category-option-unselect
  [_ _ {:keys [facet option]} app-state]
  (let [facet-path       (conj catalog.keypaths/category-selections facet)
        facet-selections (clojure.set/difference (get-in app-state facet-path)
                                                 #{option})]
    (if (empty? facet-selections)
      (update-in app-state catalog.keypaths/category-selections dissoc facet)
      (assoc-in app-state facet-path facet-selections))))

(defmethod transitions/transition-state events/control-category-option-clear
  [_ _ _ app-state]
  (assoc-in app-state catalog.keypaths/category-selections {}))
