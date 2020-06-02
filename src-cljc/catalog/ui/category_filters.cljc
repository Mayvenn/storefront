(ns catalog.ui.category-filters
  (:require
   #?@(:cljs [goog.events
              [goog.events.EventType :as EventType]
              [storefront.effects :as effects]
              [storefront.history :as history]
              [storefront.accessors.categories :as accessors.categories]
              [goog.object :as object]])
   [catalog.categories :as categories]
   catalog.keypaths
   clojure.set
   [clojure.string :as string]
   [spice.maps :as maps]
   [storefront.component :as c]
   [storefront.components.ui :as ui]
   [storefront.events :as events]
   [storefront.keypaths :as keypaths]
   [storefront.platform.component-utils :as utils]
   [storefront.transitions :as transitions]
   [spice.core :as spice]
   [storefront.platform.messages :as messages]))

(defn- hacky-fix-of-bad-slugs-on-facets [slug]
  (string/replace (str slug) #"#" ""))

(c/defcomponent ^:private filter-tab
  [{:filter-tab/keys [any-selected? selected? target id label]} _ _]
  [:a.flex-auto.x-group-item.border.border-black.py1.whisper.black
   (assoc (apply utils/fake-href target)
          :data-test id
          :key       id
          :style     (when selected? {:background "linear-gradient(to bottom, #4427c1 4px, #ffffff 4px)"})
          :class     (when any-selected? "bg-cool-gray"))
   ;; This extra div is for pixel-pushing
   [:div.pyp2 label]])

(c/defdynamic-component ^:private filter-tabs
  (constructor [this props]
               (c/create-ref! this "filter-tabs")
               #?(:cljs
                  (set! (.-handle-scroll this)
                        (fn [e] (c/set-state! this :stuck? (-> (c/get-ref this "filter-tabs")
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
   (let [{:keys [stuck?]} (c/get-state this)
         {:tabs/keys [open-panel
                      open?
                      primary
                      secondary
                      elements]} (c/get-props this)]
     (c/html
      (if (empty? elements)
        [:div]
        [:div.p2.pt3.mxn3.bg-white
         (merge {:ref (c/use-ref this "filter-tabs")}
                (when (and (not open?) stuck?)
                  {:class "border-black border-bottom top-lit"}))
         [:div
          [:div.flex.justify-between.items-baseline
           [:div.title-3.proxima.shout.bold
            primary]
           [:p.content-3 secondary]]
          (into [:div.content-2.flex.center]
                (for [e elements]
                  (c/build filter-tab e)))]])))))

(c/defcomponent ^:private filter-panel
  [{:keys [options]} _ _]
  [:div
   [:div.content-1.proxima.py6.pl10.pr1
    (for [[i options] (->> options
                           (partition-all 4)
                           (map-indexed vector))]
      [:div.flex-on-dt.justify-between.items-center
       {:key (str "filter-panel-" i)}
       (for [{:filter-option/keys
             [selected?
              disabled?
              id
              target
              label]} options]
         [:div.py1.mr4.col-3-on-dt
          {:key       id
           :data-test id
           :disabled  disabled?}
          (ui/check-box {:label     label
                         :value     selected?
                         :disabled  disabled?
                         :on-change #(apply messages/handle-message target)})])])]
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

(c/defcomponent organism
  [{:keys [title open-panel tabs filter-panel-data]} _ _]
  [:div.px3.bg-white.sticky.z1
   ;; The -5px prevents a sliver of the background from being visible above the filters
   ;; (when sticky) on android (and sometimes desktop chrome when using the inspector)
   {:style {:top "-5px"}}
   (let [tabs (c/build filter-tabs tabs nil)]
     (if open-panel
       (let [panel (c/build filter-panel filter-panel-data)]
         [:div
          [:div.hide-on-dt.px2.z4.fixed.overlay.overflow-auto.bg-white
           tabs panel]
          [:div.hide-on-mb-tb
           tabs panel]])
       [:div
        [:div.hide-on-dt tabs]
        [:div.hide-on-mb-tb tabs]]))])

(defn filter-option-query
  [{facet-slug :facet/slug}
   selections
   represented-options
   {facet-option-slug :option/slug
    facet-option-name :option/name}]
  (let [selected?    (contains? selections facet-option-slug)
        represented? (contains? represented-options facet-option-slug)]
    {:filter-option/selected? selected?
     :filter-option/disabled? (not represented?)
     :filter-option/id        (str "filter-option-" (hacky-fix-of-bad-slugs-on-facets facet-option-slug))
     :filter-option/target    [(if selected?
                                 events/control-category-option-unselect
                                 events/control-category-option-select)
                               {:facet  facet-slug
                                :option facet-option-slug}]
     :filter-option/label     [:span
                               (when (categories/new-facet? [facet-slug facet-option-slug])
                                 [:span.mr1.p-color "NEW"])
                               facet-option-name]}))

(defn filter-options-query
  [selections represented-options facet]
  (->> facet
       :facet/options
       (sort-by :option/order)
       (mapv (partial filter-option-query facet selections represented-options))))

(defn tab-query
  [open-panel
   {facet-slug :facet/slug
    facet-name :facet/name}]
  (let [selected? (= open-panel facet-slug)]
    {:filter-tab/id            (str "filter-" (name facet-slug))
     :filter-tab/label         facet-name
     :filter-tab/target        (if selected?
                                 [events/control-category-panel-close]
                                 [events/control-category-panel-open {:selected facet-slug}])
     :filter-tab/any-selected? open-panel
     :filter-tab/selected?     selected?}))

(defn query
  [app-state category category-products category-products-matching-filter-selections selections]
  (let [indexed-facets   (-> (maps/index-by :facet/slug (get-in app-state keypaths/v2-facets))
                             (select-keys (:selector/electives category)))
        open-panel       (get-in app-state catalog.keypaths/category-panel)
        product-count    (count category-products-matching-filter-selections)
        selections-count (->> (apply dissoc selections (:selector/essentials category))
                              (map (comp count val))
                              (apply +))]
    (merge
     (when-let [filter-title (:product-list/title category)]
       {:title filter-title})
     {:open-panel        open-panel
      :tabs              {:tabs/elements  (mapv (partial tab-query open-panel) (vals indexed-facets))
                          :tabs/open?     (boolean open-panel)
                          :tabs/primary   (case selections-count
                                            0 "Filter by"
                                            1 "1 filter applied"
                                            (str selections-count " filters applied"))
                          :tabs/secondary (str product-count " item" (when (not= 1 product-count) "s"))}
      :filter-panel-data {:options (when open-panel
                                     (let [represented-options
                                           (->> category-products
                                                (map (fn [product] (->> (get product open-panel) set)))
                                                (reduce clojure.set/union #{}))]
                                       (filter-options-query
                                        (open-panel selections)
                                        represented-options
                                        (open-panel indexed-facets))))}
      :selections        selections})))

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
