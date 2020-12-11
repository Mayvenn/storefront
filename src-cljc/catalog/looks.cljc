(ns catalog.looks
  "Shopping by Looks: index page of 'looks' for an 'album'"
  (:require [spice.maps :as maps]
            [storefront.accessors.contentful :as contentful]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.sites :as sites]
            [storefront.events :as e]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.header :as header]
            [storefront.components.svg :as svg]
            [storefront.components.ugc :as component-ugc]
            [storefront.components.template :as template]
            [storefront.components.ui :as ui]
            [storefront.transitions :as t]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.ugc :as ugc]))

;; Visual: Looks (new version under experiment)

(defn filtering-summary-status-molecule
  [{:filtering-summary.status/keys [primary secondary]}]
  [:div.flex.justify-between
   [:div.bold.shout.content-4 primary]
   [:div.content-3 secondary]])

(defcomponent filtering-summary-pill-molecule
  [{:filtering-summary.pill/keys [primary primary-icon id target action-icon]}
   _
   {:keys [id]}]
  [:div.pb1
   {:key id}
   (ui/button-pill
    (cond-> {:class     "p1 mr1 black content-3"
             :data-test id}
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

(defcomponent filtering-summary-organism
  [data _ _]
  (when (seq data)
    [:div.bg-white.py2.px3
     (filtering-summary-status-molecule data)
     [:div.flex.flex-wrap.py1
      (component/elements filtering-summary-pill-molecule data
                          :filtering-summary/pills)]]))

(defcomponent looks-template
  [{:keys [looks copy filtering-summary spinning?]} _ _]
  (if spinning?
    (ui/large-spinner {:style {:height "4em"}})
    [:div.bg-warm-gray
     [:div.center.py6
      [:h1.title-1.canela.py3 (:title copy)]
      [:p.col-10.col-6-on-tb-dt.mx-auto.proxima.content-2 (:description copy)]
      [:p.col-10.col-6-on-tb-dt.mx-auto.proxima.content-2 (:secondary-description copy)]]
     (component/build filtering-summary-organism filtering-summary)
     [:div.flex.flex-wrap.mbn2.justify-center.justify-start-on-tb-dt.bg-cool-gray.py2-on-tb-dt.px1-on-tb-dt
      (map-indexed
       (fn [idx look]
         (ui/screen-aware component-ugc/social-image-card-component
                          (assoc look :hack/above-the-fold? (zero? idx))
                          {:opts               {:copy copy}
                           :child-handles-ref? true
                           :key                (str (:id look))}))
       looks)]]))

(defn looks-filtering-header-reset-molecule
  [{:header.reset/keys [primary id target]}]
  (component/html
   [:div (ui/button-medium-underline-black
          (merge {:data-test id}
                 (apply utils/fake-href target))
          primary)]))

(defn looks-filtering-header-done-molecule
  [{:header.done/keys [primary id target]}]
  (component/html
   [:div (ui/button-medium-underline-primary
          (merge {:data-test id}
                 (apply utils/fake-href target))
          primary)]))

(defcomponent looks-filtering-header-organism
  [data _ _]
  (header/mobile-nav-header ;; TODO(corey) reconc with being wrapped in top-level
   {:class "border-bottom border-gray"}
   (looks-filtering-header-reset-molecule data)
   (component/html
    [:div.center.proxima.content-1 "Filters"])
   (looks-filtering-header-done-molecule data)))

(defn looks-filtering-section-title-molecule
  [{:filters.section.title/keys [primary target id rotated?]}]
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

(defcomponent looks-filtering-section-option-molecule
  [{:filters.section.filter/keys [primary target value url]} _ {:keys [id]}]
  [:div.col-12.mb2.flex
   {:on-click (apply utils/send-event-callback target)
    :key      id}
   [:div (ui/check-box {:value     value
                        :id        id
                        :data-test id})]
   [:div primary]])

(defcomponent looks-filtering-section-organism
  [data _ {:keys [id]}]
  [:div.flex.flex-column.bg-white.px5.myp1
   {:key id}
   [:div.pyj1
    (looks-filtering-section-title-molecule data)]
   (component/elements looks-filtering-section-option-molecule
                       data
                       :filters.section/filters)])

(defcomponent looks-filtering-panel-template
  [{:keys [header sections]} _ _]
  [:div.col-12.bg-white {:style {:min-height "100vh"}}
   (component/build looks-filtering-header-organism
                    header)
   [:div.mynp1.bg-refresh-gray
    (component/elements looks-filtering-section-organism
                        sections
                        :sections)]])

;; Flow Domain: Filtering Looks

(def k-models-looks-filtering (conj keypaths/models-root :looks-filtering))

(defn ^:private looks-filtering<-
  [state]
  (let [initial-state #:looks-filtering{:panel    false
                                        :sections #{}
                                        :filters  {}}]
    (merge initial-state
           (get-in state k-models-looks-filtering))))

(defmethod t/transition-state e/flow|looks-filtering|reset
  [_ _ _ state]
  (-> state
      (assoc-in (conj k-models-looks-filtering
                      :looks-filtering/filters)
                {})))

(defmethod t/transition-state e/flow|looks-filtering|panel-toggled
  [_ _ toggled? state]
  (-> state
      (assoc-in (conj k-models-looks-filtering
                      :looks-filtering/panel)
                toggled?)))

(defmethod t/transition-state e/flow|looks-filtering|section-toggled
  [_ _ [facet-key toggled?] state]
  (-> state
      (update-in (conj k-models-looks-filtering
                       :looks-filtering/sections)
                 (fnil (if toggled? conj disj) #{})
                 facet-key)))

(defmethod t/transition-state e/flow|looks-filtering|filter-toggled
  [_ _ [facet-key option-key toggled?] state]
  (-> state
      (update-in (conj k-models-looks-filtering
                       :looks-filtering/filters
                       facet-key)
                 (fnil (if toggled? conj disj) #{})
                 option-key)))

;; Biz domains -> Viz domains

(def default-copy
  {:title                 "Shop by Look"
   :description           "Get 3 or more hair items and receive a service for FREE"
   :secondary-description "#MayvennMade"
   :button-copy           "Shop Look"
   :short-name            "look"
   :seo-title             "Shop by Look | Mayvenn"
   :og-title              "Shop by Look - Find and Buy your favorite Mayvenn bundles!"})

(defn ^:private filtering-summary<-
  "Takes (Biz)
   - Defined Facets
   - User's desired Filters

   Produces (Viz)
   - Filtering Summary
     - Status
     - Pills"
  [facets-db {:looks-filtering/keys [filters]}]
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
                             :filtering-summary.pill/primary      (if (empty? filtering-options)
                                                                    "Filters"
                                                                    (str "- " (count filtering-options)))
                             :filtering-summary.pill/target       [e/flow|looks-filtering|panel-toggled
                                                                   true]}]
                           (mapv (fn options->pills
                                   [option]
                                   {:filtering-summary.pill/primary     (:option/name option)
                                    :filtering-summary.pill/target      [e/flow|looks-filtering|filter-toggled
                                                                         [(:facet/slug option)
                                                                          (:option/slug option)
                                                                          false]]
                                    :filtering-summary.pill/action-icon :close-x})
                                 filtering-options))]
    {:filtering-summary.status/primary   "Filter By:"
     :filtering-summary.status/secondary "All Looks"
     :filtering-summary/pills            pills}))


(defn sections<-
  "Takes (Biz)
   - Defined Facets
   - User's desired Filters

   Produces (Viz)
   - Sections
     - Filters"
  [facets-db {:looks-filtering/keys [sections filters]}]
  (->> (vals facets-db)
       (sort-by :filter/order)
       ;; NOTE sections to filter by
       (filter (comp #{:hair/origin :hair/texture :hair/color}
                     :facet/slug))
       (map
        (fn facet->section [{facet-slug    :facet/slug
                             facet-name    :facet/name
                             facet-options :facet/options}]
          (let [section-toggled? (contains? sections facet-slug)]
            (cond-> {:id                             facet-slug
                     :filters.section.title/primary  (str "Hair " facet-name)
                     :filters.section.title/target   [e/flow|looks-filtering|section-toggled
                                                      [facet-slug (not section-toggled?)]]
                     :filters.section.title/id       (str "section-filter-" facet-slug)
                     :filters.section.title/rotated? section-toggled?}
              section-toggled?
              (assoc :filters.section/filters
                     (->> (vals facet-options)
                          (sort-by :filter/order)
                          (mapv
                           (fn option->filter [{option-slug :option/slug
                                                  option-name :option/name}]
                             {:filters.section.filter/primary option-name
                              :filters.section.filter/target  [e/flow|looks-filtering|filter-toggled
                                                               [facet-slug option-slug true]]
                              :filters.section.filter/value   (contains?
                                                               (get filters facet-slug)
                                                               option-slug)
                              :filters.section.filter/url     option-name}))))))))))

(defn looks-template-query
  [data]
  (let [selected-album-kw (get-in data keypaths/selected-album-keyword)
        actual-album-kw   (ugc/determine-look-album data selected-album-kw)
        looks             (-> data (get-in keypaths/cms-ugc-collection) actual-album-kw :looks)
        color-details     (->> (get-in data keypaths/v2-facets)
                               (filter #(= :hair/color (:facet/slug %)))
                               first
                               :facet/options
                               (maps/index-by :option/slug))]
    {:looks     (mapv (partial contentful/look->social-card
                               selected-album-kw
                               color-details)
                      looks)
     :copy      default-copy
     :spinning? (empty? looks)}))

(defn page
  [state _]
  (let [facets-db       (->> storefront.keypaths/v2-facets
                          (get-in state)
                          (maps/index-by (comp keyword :facet/slug))
                          (maps/map-values (fn [facet]
                                             (update facet :facet/options
                                                     (partial maps/index-by :option/slug)))))
        looks-filtering (looks-filtering<- state)]
    (if (:looks-filtering/panel looks-filtering)
      (component/build looks-filtering-panel-template
                       {:header   {:header.reset/primary "RESET"
                                   :header.reset/target  [e/flow|looks-filtering|reset]
                                   :header.done/primary  "DONE"
                                   :header.done/target   [e/flow|looks-filtering|panel-toggled false]}
                        :sections {:sections (sections<- facets-db
                                                         looks-filtering)}})
      (->> (component/build looks-template
                            (merge (looks-template-query state)
                                   {:filtering-summary (filtering-summary<- facets-db
                                                                            looks-filtering)}))
           (template/wrap-standard state
                                   e/navigate-shop-by-look)))))

;; -- Original Views

(defcomponent original-component [{:keys [looks copy spinning?]} owner opts]
  (if spinning?
    (ui/large-spinner {:style {:height "4em"}})
    [:div.bg-warm-gray
     [:div.center.py6
      [:h1.title-1.canela.py3 (:title copy)]
      [:p.col-10.col-6-on-tb-dt.mx-auto.proxima.content-2 (:description copy)]]
     [:div.flex.flex-wrap.mbn2.justify-center.justify-start-on-tb-dt.bg-cool-gray.py2-on-tb-dt.px1-on-tb-dt
      (map-indexed
       (fn [idx look]
         (ui/screen-aware component-ugc/social-image-card-component
                          (assoc look :hack/above-the-fold? (zero? idx))
                          {:opts               {:copy copy}
                           :child-handles-ref? true
                           :key                (str (:id look))}))
       looks)]]))

(defn query [data]
  (let [selected-album-kw (get-in data keypaths/selected-album-keyword)
        actual-album-kw   (ugc/determine-look-album data selected-album-kw)
        looks             (-> data (get-in keypaths/cms-ugc-collection) actual-album-kw :looks)
        color-details     (->> (get-in data keypaths/v2-facets)
                               (filter #(= :hair/color (:facet/slug %)))
                               first
                               :facet/options
                               (maps/index-by :option/slug))]
    {:looks     (mapv (partial contentful/look->social-card
                               selected-album-kw
                               color-details)
                      looks)
     :copy      (actual-album-kw ugc/album-copy)
     :spinning? (empty? looks)}))

(defn ^:export built-component [data opts]
  (let [album-kw (ugc/determine-look-album data (get-in data keypaths/selected-album-keyword))]
    (if (and (experiments/sbl-update? data)        ;; featured
             (= :shop (sites/determine-site data)) ;; dtc, shop
             (= :aladdin-free-install album-kw))   ;; main look page
      (page data opts)
      (->> (component/build original-component
                            (query data)
                            opts)
           (template/wrap-standard data
                                   e/navigate-shop-by-look)))))
