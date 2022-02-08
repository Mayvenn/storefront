(ns catalog.ui.facet-filters
  (:require #?@(:cljs [[storefront.accessors.categories :as categories]
                       [storefront.history :as history]
                       [storefront.hooks.stringer :as stringer]
                       [storefront.trackings :as trackings]])
            [api.catalog :refer [select]]
            [catalog.facets :as facets]
            catalog.keypaths
            [storefront.component :as c]
            storefront.keypaths
            clojure.set
            clojure.string
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [storefront.components.svg :as svg]
            [storefront.components.header :as header]
            [storefront.transitions :as t]
            [storefront.events :as e]
            [storefront.effects :as effects]
            [spice.maps :as maps]))

(defn summary-status-molecule
  [{:filtering-summary.status/keys [primary secondary]}]
  [:div.flex.justify-between
   [:div.bold.shout.content-4 primary]
   [:div.content-3 secondary]])

(c/defcomponent summary-pill-molecule
  [{:filtering-summary.pill/keys [primary primary-icon data-test target action-icon]}
   _
   {:keys [id]}]
  [:div.pb1
   {:key id}
   (ui/button-pill
    (cond-> {:class     "p1 mr1 black content-3"
             :data-test (or data-test id)}
      (not-empty target)
      (assoc :on-click
             (apply utils/send-event-callback target)))
    [:div.flex.items-center.px1
     [:span.mr1
      (when primary-icon
        ^:inline (svg/funnel {:class  "mrp3"
                              :height "9px"
                              :width  "10px"}))
      primary]
     (when action-icon
       ^:inline (svg/close-x
                 {:class  "stroke-white fill-gray"
                  :width  "13px"
                  :height "13px"}))])])

(c/defcomponent summary-organism
  [data _ _]
  (when (seq data)
    [:div.bg-white.py2.px3
     (summary-status-molecule data)
     [:div.flex.flex-wrap.py1
      (c/elements summary-pill-molecule data :filtering-summary/pills)]]))

(defn header-reset-molecule
  [{:header.reset/keys [primary id target]}]
  (c/html
   [:div (ui/button-medium-underline-black
          (merge {:data-test id}
                 (apply utils/fake-href target))
          primary)]))

(defn header-done-molecule
  [{:header.done/keys [primary id target]}]
  (c/html
   [:div (ui/button-medium-underline-primary
          (merge {:data-test id}
                 (apply utils/fake-href target))
          primary)]))

(c/defcomponent section-filter-molecule
  [{:facet-filtering.section.filter/keys [primary target value icon-url]
    :as                                  data} _ {:keys [id]}]
  [:a.col-12.mb2.flex
   {:on-click  (apply utils/send-event-callback target)
    :data-test (:facet-filtering.section.filter/id data)
    :key       id}
   [:div (ui/check-box {:value value :id (str "filter-" primary "-mobile")})]
   (when icon-url
     [:img.block.pr2
      {:style {:width  "50px"
               :height "30px"}
       :alt   (str "Color swatch of " primary "-colored hair")
       :src   icon-url}])
   [:div
    primary]])

(defn section-title-molecule
  [{:facet-filtering.section.title/keys [primary target id rotated?]}]
  [:a.block.flex.justify-between.inherit-color.items-center
   (cond-> {:data-test id}
     target
     (merge (apply utils/fake-href target)))
   [:div.shout.title-2.proxima
    primary]
   [:div.flex.items-center
    (when rotated?
      {:class "rotate-180 mrp2"})
    ^:inline (svg/dropdown-arrow
              {:class  "fill-black"
               :height "20px"
               :width  "20px"})]])

(c/defcomponent section-organism
  [data _ {:keys [id]}]
  [:div.flex.flex-column.bg-white.px5.myp1
   {:key id}
   [:div.pyj1
    (section-title-molecule data)]
   (when (:facet-filtering.section/toggled? data)
     (c/elements section-filter-molecule
                 data
                 :facet-filtering.section/filters))])

(c/defcomponent desktop-section-filter-molecule
  [{:facet-filtering.section.filter/keys [primary target value icon-url]} _ {:keys [id]}]
  [:a.col-12.mb2.flex
   {:on-click (apply utils/send-event-callback target)
    :key      id}
   [:div (ui/check-box {:value     value
                        :id        (str "filter-" primary "-desktop")
                        :data-test id})]
   (when icon-url
     [:img.block.pr2
      {:style {:width  "50px"
               :height "30px"}
       :alt   (str "Color swatch of " primary "-colored hair")
       :src   icon-url}])
   [:div primary]])

(c/defcomponent desktop-section-title-molecule
  [{:facet-filtering.section.title/keys [primary target id]} _ _]
  [:a.block.flex.justify-between.inherit-color.items-center
   (cond-> {:data-test (str id "-desktop")}
     target
     (merge (apply utils/fake-href target)))
   [:div.shout.title-2.proxima primary]])

(c/defcomponent desktop-section-organism
  [data _ {:keys [id]}]
  [:div.flex.flex-column.bg-white.px5.myp1
   {:key id}
   [:div.pyj1 (c/build desktop-section-title-molecule data)]
   (c/elements desktop-section-filter-molecule data :facet-filtering.section/filters)])

(c/defcomponent desktop-sections-organism
  [{:keys [sections]} _ _]
  [:div {:style {:min-width "250px"}}
   [:div.mynp1.bg-refresh-gray
    (c/elements desktop-section-organism sections :facet-filtering/sections)]])

(c/defcomponent header-organism
  [data _ _]
  (header/nav-header
   {:class "border-bottom border-gray"}
   (header-reset-molecule data)
   (c/html
    [:div.center.proxima.content-1 "Filters"])
   (header-done-molecule data)))

(c/defcomponent panel-template
  [{:keys [header sections]} _ _]
  [:div.fixed.overlay.col-12.bg-white.z6.overflow-scroll
   {:key "panel-template"}
   (c/build header-organism header)
   [:div.mynp1.bg-refresh-gray
    (c/elements section-organism
                sections
                :facet-filtering/sections)]])

(c/defcomponent desktop-header-organism
  [{:as                            data
    :filtering-summary.status/keys [primary secondary]} _ _]
  [:div.col-12.flex.justify-between.pr2.pt6
   [:div.title-3.proxima.shout.pl5.flex.items-center.justify-between {:style {:width "250px"}}
    primary
    (header-reset-molecule data)]
   secondary])

(defn summary<-
  "Takes (Biz)
   - Defined Facets
   - Count of items
   - User's desired Filters

   Produces (Viz)
   - Filtering Summary
   - Status (with Filtered Count)
   - Pills"
  [facets-db
   item-count
   navigation-event
   navigation-args

   {:facet-filtering/keys [filters item-label]}]
  (let [filtering-options (->> filters
                               (mapcat (fn selections->options
                                         [[facet-slug option-slugs]]
                                         (let [facet-options (-> facets-db
                                                                 (get facet-slug)
                                                                 :facet/options)]
                                           (->> option-slugs
                                                (map #(get facet-options %))
                                                (map #(assoc % :facet/slug facet-slug))))))
                               concat)
        pills             (concat
                           [{:filtering-summary.pill/primary-icon :funnel
                             :filtering-summary.pill/data-test    "filter-open-main"
                             :filtering-summary.pill/primary      (if (empty? filtering-options)
                                                                    "Filters"
                                                                    (str "- " (count filtering-options)))
                             :filtering-summary.pill/target       [e/flow|facet-filtering|panel-toggled {:toggled? true}]}]
                           (mapv (fn options->pills
                                   [option]
                                   {:filtering-summary.pill/primary     (:option/name option)
                                    :filtering-summary.pill/target      [e/flow|facet-filtering|filter-toggled
                                                                         {:navigation-args  navigation-args
                                                                          :navigation-event navigation-event
                                                                          :facet-key        (:facet/slug option)
                                                                          :option-key       (:option/slug option)
                                                                          :toggled?         false}]
                                    :filtering-summary.pill/action-icon :close-x})
                                 filtering-options))]
    {:filtering-summary.status/primary   "Filter By:"
     :filtering-summary.status/secondary (ui/pluralize-with-amount item-count item-label)
     :filtering-summary/pills            pills}))

(defn header<- [navigation-event navigation-args]
  {:header.reset/primary "RESET"
   :header.reset/target  [e/flow|facet-filtering|reset
                          {:navigation-event navigation-event
                           :navigation-args  navigation-args}]
   :header.done/primary  "DONE"
   :header.done/id       "filters-done"
   :header.done/target   [e/flow|facet-filtering|panel-toggled false]})

(defn sections<-
  "Takes (Biz)
   - Defined Facets
   - User's desired Filters

   Produces (Viz)
   - Sections
   - Filters"
  [facets-db
   represented-facets ;; {:facet/slug (Set :option/slug)}
   navigation-event
   navigation-args
   {:facet-filtering/keys [sections
                           filters]}]
  {:facet-filtering/sections
   (->> (vals (select-keys facets-db (keys represented-facets)))
        (sort-by :filter/order)
        (map
         (fn facet->section [{facet-slug    :facet/slug
                              facet-name    :facet/name
                              facet-options :facet/options}]
           (let [section-toggled?    (contains? sections facet-slug)
                 represented-options (get represented-facets facet-slug)]
             {:id                             (some->> facet-slug
                                                      name
                                                      (str "filter-"))
              :facet-filtering.section/toggled? section-toggled?
              :facet-filtering.section.title/primary (cond->> facet-name
                                                       (contains? #{:hair/origin :hair/color :hair/texture} facet-slug)
                                                       (str "Hair "))
              :facet-filtering.section.title/target   [e/flow|facet-filtering|section-toggled {:facet-key facet-slug :toggled? (not section-toggled?)}]
              :facet-filtering.section.title/id       (some->> facet-slug name (str "filter-"))
              :facet-filtering.section.title/rotated? section-toggled?
              :facet-filtering.section/filters
              (->> (vals facet-options)
                   (sort-by :filter/order)
                   (keep
                    (fn option->filter [{option-slug   :option/slug
                                         option-name   :option/name
                                         option-swatch :option/rectangle-swatch}]
                      (when (contains? represented-options option-slug)
                        (let [filter-toggled? (contains?
                                               (get filters facet-slug)
                                               option-slug)]
                          (cond->
                              #:facet-filtering.section.filter
                              {:primary option-name
                               :id      (str "filter-option-" (facets/hacky-fix-of-bad-slugs-on-facets option-slug))
                               :target  [e/flow|facet-filtering|filter-toggled
                                         {:facet-key  facet-slug
                                          :navigation-event navigation-event
                                          :navigation-args navigation-args
                                          :option-key option-slug
                                          :toggled?   (not filter-toggled?)}]
                               :value   filter-toggled?
                               :url     option-name}
                            option-swatch
                            (assoc :facet-filtering.section.filter/icon-url
                                   (str "https://ucarecdn.com/"
                                        (ui/ucare-img-id option-swatch)
                                        "/-/format/auto/-/resize/50x/")))))))
                   vec)}))))})

(defn no-matches<-
  [item-label navigation-event navigation-args]
  {:no-matches.title/primary    "😞"
   :no-matches.title/secondary  "Sorry, we couldn’t find any matches."
   :no-matches.action/primary   "Clear all filters"
   :no-matches.action/secondary (clojure.string/lower-case (str " to see more " item-label "s."))
   :no-matches.action/target    [e/flow|facet-filtering|reset
                                 {:navigation-event navigation-event
                                  :navigation-args  navigation-args}]})

(defn no-matches-title-molecule
  [{:no-matches.title/keys [primary secondary]}]
  [:div
   [:p.h1.py4 primary]
   [:p.h2.py6 secondary]])

(defn no-matches-action-molecule
  [{:no-matches.action/keys [primary secondary target]}]
  [:p.h4.mb10.pb10
   [:a.p-color (apply utils/fake-href target) primary]
   secondary])

(c/defcomponent no-matches-organism
  [data _ _]
  (when (seq data)
    [:div.center.bg-white.flex.flex-column.items-center.justify-center
     (no-matches-title-molecule data)
     (no-matches-action-molecule data)]))

(c/defcomponent organism ;; This is main component
  [{:filtering/keys
    [summary
     header
     sections
     child-component-data
     no-matches
     facet-filtering-state]} _ {:keys [child-component]}]
  (if header
    (list
     (when (:facet-filtering/panel facet-filtering-state)
       (c/build panel-template
                {:header header
                 :sections sections}))
     [:div
      {:key "filtering-component"}
      [:div.hide-on-dt
       (c/build summary-organism summary)]
      [:div.hide-on-mb-tb
       (c/build desktop-header-organism (merge summary header))]
      [:div.flex.justify-between
       [:div.hide-on-mb-tb
        (c/build desktop-sections-organism {:sections sections})]
       [:div.col-12
        (c/build child-component child-component-data)
        (c/build no-matches-organism no-matches)]]])
    [:div.mx-auto.max-960
     (c/build child-component child-component-data)]))

(defn filters<-
  [{:keys
    [facets-db
     faceted-models
     facet-filtering-state
     facets-to-filter-on
     ;; For the page the filters are rendered on
     navigation-event
     navigation-args
     child-component-data]}]
  (let [indexed-facets (->> facets-db
                            (maps/index-by (comp keyword :facet/slug))
                            (maps/map-values (fn [facet]
                                               (update facet :facet/options
                                                       (partial maps/index-by :option/slug)))))
        item-count     (->> faceted-models
                            (select (:facet-filtering/filters facet-filtering-state))
                            count)]
    (cond-> {:filtering/child-component-data  child-component-data}

      (seq facets-to-filter-on)
      (merge
       {:filtering/summary               (summary<- indexed-facets
                                                    item-count
                                                    navigation-event
                                                    navigation-args
                                                    facet-filtering-state)
        :filtering/header                (header<- navigation-event navigation-args)
        :filtering/sections              (sections<- indexed-facets
                                                     (->> faceted-models
                                                          (mapv #(select-keys % facets-to-filter-on))
                                                          (apply merge-with clojure.set/union))
                                                     navigation-event
                                                     navigation-args
                                                     facet-filtering-state)
        :filtering/facet-filtering-state facet-filtering-state})

      (zero? item-count)
      (merge {:filtering/no-matches (no-matches<-
                                     (:facet-filtering/item-label facet-filtering-state)
                                     navigation-event navigation-args)}))))


;; Flow Domain: Filtering Looks

(defmethod t/transition-state e/flow|facet-filtering|initialized
  [_ event args state]
  (update-in state catalog.keypaths/k-models-facet-filtering
             merge #:facet-filtering{:panel    false
                                     :sections #{}}))

#?(:cljs
   (defmethod trackings/perform-track e/flow|facet-filtering|reset
     [_ event _args app-state]
     (stringer/track-event "reset_filters_clicked" {})))

(defmethod effects/perform-effects e/flow|facet-filtering|reset
  [_ event {:keys [navigation-event navigation-args]} _ app-state]
  #?(:cljs (history/enqueue-redirect navigation-event navigation-args)))

(defmethod t/transition-state e/flow|facet-filtering|panel-toggled
  [_ _ toggled? state]
  (assoc-in state catalog.keypaths/k-models-facet-filtering-panel toggled?))

#?(:cljs
   (defmethod trackings/perform-track e/flow|facet-filtering|section-toggled
     [_ event {:keys [facet-key toggled?]} app-state]
     (when toggled?
       (stringer/track-event "category_page_filter-select"
                                        {:filter_name (pr-str facet-key)}))))

(defmethod t/transition-state e/flow|facet-filtering|section-toggled
  [_ _ {:keys [facet-key toggled?]} state]
  (-> state
      (update-in catalog.keypaths/k-models-facet-filtering-sections
                 (fnil (if toggled? conj disj) #{})
                 facet-key)))

#?(:cljs
   (defmethod trackings/perform-track e/flow|facet-filtering|filter-toggled
     [_ event {:keys [facet-key option-key toggled?]} app-state]
     (when toggled?
       (stringer/track-event "category_page_option-select"
                             {:filter_name     (pr-str facet-key)
                              :selected_option option-key}))))

(defmethod effects/perform-effects e/flow|facet-filtering|filter-toggled
  [_ event args _ app-state]
  #?(:cljs
     (let [existing-filters (get-in app-state catalog.keypaths/k-models-facet-filtering-filters)
           {:keys [navigation-event navigation-args]} args]
       (history/enqueue-redirect navigation-event
                                 (merge
                                  navigation-args
                                  {:query-params (->> existing-filters
                                                      (filter (fn [[_ v]] (seq v)))
                                                      (reduce merge {})
                                                      categories/category-selections->query-params)})))))

(defmethod t/transition-state e/flow|facet-filtering|filter-toggled
  [_ _ {:keys [facet-key option-key toggled?]} state]
  (-> state
      (update-in (conj catalog.keypaths/k-models-facet-filtering-filters facet-key)
                 (fnil (if toggled? conj disj) #{})
                 option-key)
      (update-in catalog.keypaths/k-models-facet-filtering-filters
                 (fn [filters]
                   (cond-> filters
                     (empty? (get filters facet-key))
                     (dissoc facet-key))))))
