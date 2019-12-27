(ns catalog.category
  (:require
   #?@(:cljs [[goog.dom]
              [goog.events]
              [goog.events.EventType :as EventType]
              [goog.object :as object]
              [storefront.browser.scroll :as scroll]
              [storefront.api :as api]
              [storefront.effects :as effects]
              [storefront.accessors.auth :as auth]
              [storefront.history :as history]])
   [storefront.component :as component :refer [defcomponent defdynamic-component]]
   [catalog.categories :as categories]
   [storefront.assets :as assets]
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
   [catalog.ui.product-card :as product-card]))

(def query-param-separator "~")

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

(def category-query-params-ordering
  {"origin"        0
   "texture"       1
   "color"         2
   "base-material" 3
   "weight"        4
   "family"        5})

(defn sort-query-params
  [params]
  (into (sorted-map-by
         (fn [key1 key2]
           (compare (get category-query-params-ordering key1 100)
                    (get category-query-params-ordering key2 100))))
        params))

(def ^:private facet-slugs->query-params
  (set/map-invert query-params->facet-slugs))

(def allowed-query-params
  (vals facet-slugs->query-params))

(defn ^:private handle-scroll [_]
  #?(:cljs (this-as this
             (component/set-state! this :stuck? (-> (component/get-ref this "filter-tabs")
                                                    .getBoundingClientRect
                                                    (object/get "top")
                                                    (<= 0))))))

(defdynamic-component ^:private filter-tabs-component
  (constructor [this props]
               (component/create-ref! this "filter-tabs")
               (set! (.-handle-scroll this) (.bind handle-scroll this))
               {:stuck? false})
  (did-mount [this]
   #?(:cljs (goog.events/listen js/window EventType/SCROLL (.-handle-scroll this))))
  (will-unmount [this]
   #?(:cljs (goog.events/unlisten js/window EventType/SCROLL (.-handle-scroll this))))
  (render
   [this]
   (let [{:keys [stuck?]}     (component/get-state this)
         {:keys [essentials
                 electives
                 facets
                 all-product-cards
                 selections
                 open-panel]} (component/get-props this)
         product-count        (count all-product-cards)
         selections-count     (->> (apply dissoc selections essentials)
                                   (map (comp count val))
                                   (apply +))]
     (component/html
      [:div.p2.pt3.mxn2.bg-white
       (merge {:ref (component/use-ref this "filter-tabs")}
              (when (and (not open-panel) stuck?)
                {:class "border-black border-bottom top-lit"}))
       (when (seq electives)
         [:div
          [:div.flex.justify-between.items-baseline
           [:div.title-3.proxima.shout.bold
            (case selections-count
              0 "Filter by:"
              1 "1 filter applied:"
              (str selections-count " filters applied:"))]
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

(defn filter-panel [facets represented-options selections open-panel]
  [:div
   [:div.content-1.proxima.py6.pl10.pr1
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
           [:div.py1.mr4
            {:key       (str "filter-option-" slug)
             :data-test (str "filter-option-" slug)
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

(defn ^:private category-header
  [category]
  (component/html
   [:div.center.px2.py10.bg-warm-gray.max-960.mx-auto
    (when (:category/new? category)
          [:div.s-color.title-3.proxima.bold.shout "New"])
    [:div.h1.title-1.canela (:copy/title category)]
    (when-let [icon-url (-> category :images :icon :url)]
      [:div.mt4 [:img {:src   (assets/path icon-url)
                       :style {:width "54px"}}]])
    [:div.my3.mx6-on-mb.col-8-on-tb-dt.mx-auto-on-tb-dt
     (:copy/description category)
     (when-let [learn-more-event (:copy/learn-more category)]
       [:div.mt3
        (ui/button-small-underline-black {:on-click (apply utils/send-event-callback learn-more-event)}
                                         "Learn more")])]]))

(defn product-cards-empty-state [loading?]
  (component/html
   [:div.col-12.my8.py4.center
    (if loading?
      (ui/large-spinner {:style {:height "4em"}})
      [:div
       [:p.h1.py4 "😞"]
       [:p.h2.py6 "Sorry, we couldn’t find any matches."]
       [:p.h4.mb10.pb10
        [:a.p-color (utils/fake-href events/control-category-option-clear) "Clear all filters"]
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
           [:div.mx-auto.col.col-11.h5.center.pt2 copy]]
          [:div.flex.flex-column
           [:div.hide-on-mb-tb.mx1
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
   (category-header category)
   [:div.max-960.col-12.mx-auto.px2-on-mb.px2-on-tb
    [:div.bg-white.sticky.z1
      ;; The -5px prevents a sliver of the background from being visible above the filters
      ;; (when sticky) on android (and sometimes desktop chrome when using the inspector)
     {:style {:top "-5px"}}
     (let [tabs  (component/build filter-tabs-component ; TODO: clean up to query
                                  {:essentials        (:selector/essentials category)
                                   :electives         (:selector/electives category)
                                   :facets            facets
                                   :all-product-cards all-product-cards
                                   :selections        selections
                                   :open-panel        open-panel} opts)]
       (if open-panel
         [:div
          [:div.hide-on-dt.px2.z4.fixed.overlay.overflow-auto.bg-white tabs (filter-panel facets represented-options selections open-panel)]
          [:div.hide-on-mb-tb tabs (filter-panel facets represented-options selections open-panel)]]
         [:div
          [:div.hide-on-dt tabs]
          [:div.hide-on-mb-tb tabs]]))]
    [:div
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
        tabs-stuck?                true
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
     :tabs-stuck? tabs-stuck?
     :loading-products?   (utils/requesting? data (conj request-keys/get-products
                                                        (skuers/essentials category)))}))

(defn ^:export built-component
  [data opts]
  (component/build component (query data) opts))

(defmethod transitions/transition-state events/navigate-category
  [_ event {:keys [catalog/category-id query-params]} app-state]
  (let [[_ {prev-category-id :catalog/category-id}] (-> (get-in app-state keypaths/navigation-undo-stack)
                                                        first
                                                        :navigation-message)]
    (cond-> app-state
      true
      (assoc-in catalog.keypaths/category-id category-id)

      true
      (assoc-in catalog.keypaths/category-selections
                (->> (maps/select-rename-keys query-params query-params->facet-slugs)
                     (maps/map-values #(set (string/split % query-param-separator)))))

      (not= prev-category-id category-id)
      (assoc-in catalog.keypaths/category-panel nil))))

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
         (api/get-products (get-in app-state keypaths/api-cache)
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
            (maps/map-keys (comp name facet-slugs->query-params))
            sort-query-params
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
