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
            [storefront.ugc :as ugc]
            [spice.selector :as selector]))

(def ^:private select
  (comp seq (partial selector/match-all {:selector/strict? true})))

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

(defcomponent looks-hero-organism
  [{:looks.hero.title/keys [primary secondary]} _ _]
  [:div.center.py6
   [:h1.title-1.canela.py3
    primary]
   [:p.col-10.col-6-on-tb-dt.mx-auto.proxima.content-2
    (interpose [:br] secondary)]])

(defcomponent looks-template
  [{:keys [looks hero filtering-summary]} _ _]
  [:div.bg-warm-gray
   (component/build looks-hero-organism hero)
   (component/build filtering-summary-organism filtering-summary)
   [:div.flex.flex-wrap.mbn2.justify-center.justify-start-on-tb-dt.bg-cool-gray.py2-on-tb-dt.px1-on-tb-dt
    (map-indexed
     (fn [idx look]
       (ui/screen-aware component-ugc/social-image-card-component
                        (assoc look :hack/above-the-fold? (zero? idx))
                        {:child-handles-ref? true
                         :key                (str (:id look))}))
     looks)]])

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
  (header/mobile-nav-header
   {:class "border-bottom border-gray"}
   (looks-filtering-header-reset-molecule data)
   (component/html
    [:div.center.proxima.content-1 "Filters"])
   (looks-filtering-header-done-molecule data)))

(defn looks-filtering-section-title-molecule
  [{:looks-filtering.section.title/keys [primary target id rotated?]}]
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

(defcomponent looks-filtering-section-filter-molecule
  [{:looks-filtering.section.filter/keys [primary target value url]} _ {:keys [id]}]
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
   (component/elements looks-filtering-section-filter-molecule
                       data
                       :looks-filtering.section/filters)])

(defcomponent looks-filtering-panel-template
  [{:keys [header sections]} _ _]
  [:div.col-12.bg-white {:style {:min-height "100vh"}}
   (component/build looks-filtering-header-organism
                    header)
   [:div.mynp1.bg-refresh-gray
    (component/elements looks-filtering-section-organism
                        sections
                        :looks-filtering/sections)]])

;; Visual: Spinning

(defcomponent spinning-template
  [{:keys [header sections]} _ _]
  (ui/large-spinner
   {:style {:height "4em"}}))

;; Flow Domain: Filtering Looks

(def k-models-looks-filtering (conj keypaths/models-root :looks-filtering))

(defn ^:private looks-filtering<-
  [state]
  (let [initial-state #:looks-filtering{:panel    false
                                        :sections #{}
                                        :filters  {}}]
    (-> initial-state
        (merge (get-in state k-models-looks-filtering))
        (update :looks-filtering/filters
                ;; Remove empty vals
                #(->> %
                      (remove (comp empty? last))
                      (into {}))))))

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

(defn ^:private filtering-summary<-
  "Takes (Biz)
   - Defined Facets
   - Looks
   - User's desired Filters

   Produces (Viz)
   - Filtering Summary
     - Status (with Filtered Count)
     - Pills"
  [facets-db looks {:looks-filtering/keys [filters]}]
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
     :filtering-summary.status/secondary (str
                                          (count (select filters looks))
                                          " Looks")
     :filtering-summary/pills            pills}))

(defn filtering-sections<-
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
                     :looks-filtering.section.title/primary  (str "Hair " facet-name)
                     :looks-filtering.section.title/target   [e/flow|looks-filtering|section-toggled
                                                      [facet-slug (not section-toggled?)]]
                     :looks-filtering.section.title/id       (str "section-filter-" facet-slug)
                     :looks-filtering.section.title/rotated? section-toggled?}
              section-toggled?
              (assoc :looks-filtering.section/filters
                     (->> (vals facet-options)
                          (sort-by :filter/order)
                          (mapv
                           (fn option->filter [{option-slug :option/slug
                                                option-name :option/name}]
                             (let [filter-toggled? (contains?
                                                    (get filters facet-slug)
                                                    option-slug)]
                               {:looks-filtering.section.filter/primary option-name
                                :looks-filtering.section.filter/target  [e/flow|looks-filtering|filter-toggled
                                                                         [facet-slug
                                                                          option-slug
                                                                          (not filter-toggled?)]]
                                :looks-filtering.section.filter/value   filter-toggled?
                                :looks-filtering.section.filter/url     option-name})))))))))))

(defn looks-cards<-
  [state facets-db looks {:looks-filtering/keys [filters]}]
  (->> (select filters looks)
       (mapv (partial contentful/look->social-card
                      (get-in state keypaths/selected-album-keyword)
                      (:facet/options (:hair/color facets-db))))
       (mapv #(assoc %
                     ;; TODO(corey) tidy this up when the cards are worked on
                     :button-copy           "Shop Look"
                     :short-name            "look"))))

(def ^:private looks-hero<-
  {:looks.hero.title/primary   "Shop by Look"
   :looks.hero.title/secondary ["Get 3 or more hair items and receive a service for FREE"
                                "#MayvennMade"]})

;; Biz Domain: Looks

(defn ^:private options|name->slug
  "Used to enrich contenful looks to be selectable"
  [facets-db]
  (->> (vals facets-db)
       (mapcat :facet/options)
       (mapv (fn [[slug option]]
               [(:option/name option) slug]))
       (into {})))

(defn ^:private looks<-
  [state facets-db]
  (let [name->slug (options|name->slug facets-db)]
    (->> (get-in state keypaths/cms-ugc-collection)
         ;; NOTE(corey) This is hardcoded because obstensibly
         ;; filtering should replace albums
         :aladdin-free-install
         :looks
         ;; Enrich contentful looks to be selectable
         (mapv (fn [{:as look :keys [color origin texture]}]
                 (assoc look
                        :hair/color #{(get name->slug color)}
                        :hair/origin #{(get name->slug (str origin
                                                            " hair"))}
                        :hair/texture #{(get name->slug texture)}))))))

;; Visual Domain: Page

(defn page
  "Looks, 'Shop by Look'

  Visually: Grid, Spinning, or Filtering
  "
  [state _]
  (let [facets-db (->> (get-in state storefront.keypaths/v2-facets)
                       (maps/index-by (comp keyword :facet/slug))
                       (maps/map-values (fn [facet]
                                          (update facet :facet/options
                                                  (partial maps/index-by :option/slug)))))

        looks           (looks<- state facets-db)
        ;; Flow models
        looks-filtering (looks-filtering<- state)]
    (cond
      ;; Spinning
      (empty? looks)
      (->> (component/build spinning-template)
           (template/wrap-standard state
                                   e/navigate-shop-by-look))
      ;; Looks Filtering Panel
      (:looks-filtering/panel looks-filtering)
      (component/build looks-filtering-panel-template
                       {:header   {:header.reset/primary "RESET"
                                   :header.reset/target  [e/flow|looks-filtering|reset]
                                   :header.done/primary  "DONE"
                                   :header.done/target   [e/flow|looks-filtering|panel-toggled false]}
                        :sections {:looks-filtering/sections
                                   (filtering-sections<- facets-db looks-filtering)}})
      ;; Grid of Looks
      :else
      (->> {:hero              looks-hero<-
            :looks             (looks-cards<- state
                                              facets-db
                                              looks
                                              looks-filtering)
            :filtering-summary (filtering-summary<- facets-db
                                                    looks
                                                    looks-filtering)}
           (component/build looks-template)
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
