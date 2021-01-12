(ns catalog.looks
  "Shopping by Looks: index page of 'looks' for an 'album'"
  (:require #?@(:cljs [[storefront.accessors.categories :as categories]
                       [storefront.history :as history]])
            catalog.keypaths
            clojure.string
            clojure.set
            [spice.maps :as maps]
            [storefront.accessors.contentful :as contentful]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.sites :as sites]
            [storefront.effects :as effects]
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
            [spice.selector :as selector]
            [catalog.keypaths :as catalog.keypaths]))

(def ^:private select
  (comp seq (partial selector/match-all {:selector/strict? true})))

;; Visual: Looks (new version under experiment)

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

(defcomponent no-matches-organism
  [data _ _]
  (when (seq data)
    [:div.col-12.my8.py4.center.bg-white
     (no-matches-title-molecule data)
     (no-matches-action-molecule data)]))

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
  [:div.center.py6.bg-warm-gray
   [:h1.title-1.canela.py3
    primary]
   (into [:p.col-10.col-6-on-tb-dt.mx-auto.proxima.content-2]
         (interpose [:br] secondary))])

(defn looks-card-hero-molecule
  [{:looks-card.hero/keys [image-url badge-url]}]
  [:div.relative
   {:style {:padding-right "3px"}}
   (ui/img {:class    "container-size"
            :style    {:object-position "50% 25%"
                       :object-fit      "cover"}
            :src      image-url
            :max-size 749})
   [:div.absolute.bottom-0.m1.justify-start
    {:style {:height "22px"
             :width  "22px"}}
    badge-url]])

(defn looks-card-title-molecule
  [{:looks-card.title/keys [primary]}]
  [:div.my2 primary])

(defcomponent looks-card-hair-item-molecule
  [{:looks-card.hair-item/keys [image-url]} _ {:keys [id]}]
  [:img.block
   {:key id
    :src image-url}])

(defcomponent looks-card-organism*
  [{:as data :looks-card/keys [height-px]
    target :looks-card.action/target} _ _]
  [:a.col-12.col-3-on-tb-dt.border.border-cool-gray.p2.m2.black
   (apply utils/route-to target)
   [:div.flex
    {:style {:height (str height-px "px")}}
    (looks-card-hero-molecule data)
    [:div.flex.flex-column.justify-between
     (component/elements looks-card-hair-item-molecule
                         data
                         :looks-card/hair-items)]]
   (looks-card-title-molecule data)])

(defcomponent looks-card-organism
  [data _ {:keys [id idx]}]
  (ui/screen-aware
   looks-card-organism*
   (assoc data
          :hack/above-the-fold? (zero? idx))
   {:child-handles-ref? true
    :key                id}))

(defcomponent looks-cards-organism
  [data _ _]
  (when (seq data)
    [:div.flex.flex-wrap.justify-center.justify-start-on-tb-dt.py2-on-tb-dt.px1-on-tb-dt
     (component/elements looks-card-organism
                         data
                         :looks-cards/cards)]))

(defcomponent looks-template
  [{:keys [hero filtering-summary no-matches looks-cards]} _ _]
  [:div
   (component/build looks-hero-organism hero)
   (component/build filtering-summary-organism filtering-summary)
   (component/build no-matches-organism no-matches)
   (component/build looks-cards-organism looks-cards)])

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
  [{:looks-filtering.section.filter/keys [primary target value url icon-url]} _ {:keys [id]}]
  [:div.col-12.mb2.flex
   {:on-click (apply utils/send-event-callback target)
    :key      id}
   [:div (ui/check-box {:value     value
                        :id        id
                        :data-test id})]
   (when icon-url
     [:img.block.pr2
      {:style {:width  "50px"
               :height "30px"}
       :src   icon-url}])
   [:div
    primary]])

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
  [_ _ _]
  (ui/large-spinner
   {:style {:height "4em"}}))

;; Flow Domain: Filtering Looks

(defmethod t/transition-state e/flow|looks-filtering|initialize
  [_ event args state]
  (assoc-in state catalog.keypaths/k-models-looks-filtering
            #:looks-filtering{:panel    false
                              :sections #{}
                              :filters  {}}))

(defmethod effects/perform-effects  e/flow|looks-filtering|reset
  [_ event _ _ app-state]
  #?(:cljs
     (history/enqueue-navigate e/navigate-shop-by-look
                               {:album-keyword (:album-keyword (get-in app-state keypaths/navigation-args))})))

(defmethod t/transition-state e/flow|looks-filtering|panel-toggled
  [_ _ toggled? state]
  (-> state
      (assoc-in catalog.keypaths/k-models-looks-filtering-panel toggled?)))

(defmethod t/transition-state e/flow|looks-filtering|section-toggled
  [_ _ [facet-key toggled?] state]
  (-> state
      (update-in catalog.keypaths/k-models-looks-filtering-sections
                 (fnil (if toggled? conj disj) #{})
                 facet-key)))

(defmethod effects/perform-effects e/flow|looks-filtering|filter-toggled
  [_ event {:keys [facet-key option-key toggled?]} _ app-state]
  #?(:cljs
     (let [existing-filters (get-in app-state catalog.keypaths/k-models-looks-filtering-filters)]
       (history/enqueue-navigate e/navigate-shop-by-look
                                 {:query-params  (->> existing-filters
                                                      (filter (fn [[_ v]] (seq v)))
                                                      (reduce merge {})
                                                      categories/category-selections->query-params)
                                  :album-keyword (:album-keyword (get-in app-state keypaths/navigation-args))}))))

(defmethod t/transition-state e/flow|looks-filtering|filter-toggled
  [_ _ {:keys [facet-key option-key toggled?]} state]
  (-> state
      (update-in (conj catalog.keypaths/k-models-looks-filtering-filters facet-key)
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
                                                                         {:facet-key  (:facet/slug option)
                                                                          :option-key (:option/slug option)
                                                                          :toggled?   false}]
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
  [facets-db
   represented-facets ;; {:facet/slug (Set :option/slug)}
   {:looks-filtering/keys [sections filters]}]

  (->> (vals (select-keys facets-db (keys represented-facets)))
       (sort-by :filter/order)
       (map
        (fn facet->section [{facet-slug    :facet/slug
                             facet-name    :facet/name
                             facet-options :facet/options}]
          (let [section-toggled?    (contains? sections facet-slug)
                represented-options (get represented-facets facet-slug)]
            (cond-> {:id                             facet-slug
                     :looks-filtering.section.title/primary  (str "Hair " facet-name)
                     :looks-filtering.section.title/target   [e/flow|looks-filtering|section-toggled
                                                              [facet-slug (not section-toggled?)]] ;; TODO: not positional?
                     :looks-filtering.section.title/id       (str "section-filter-" facet-slug)
                     :looks-filtering.section.title/rotated? section-toggled?}
              section-toggled?
              (assoc :looks-filtering.section/filters
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
                                     #:looks-filtering.section.filter
                                     {:primary option-name
                                      :target  [e/flow|looks-filtering|filter-toggled
                                                {:facet-key  facet-slug
                                                 :option-key option-slug
                                                 :toggled?   (not filter-toggled?)}]
                                      :value   filter-toggled?
                                      :url     option-name}
                                   option-swatch
                                   (assoc :looks-filtering.section.filter/icon-url
                                          (str "https://ucarecdn.com/"
                                               (ui/ucare-img-id option-swatch)
                                               "/-/format/auto/-/resize/50x/")))))))
                          vec))))))))

(defn no-matches<-
  [looks {:looks-filtering/keys [filters]}]
  (when (empty? (select filters looks))
    {:no-matches.title/primary    "😞"
     :no-matches.title/secondary  "Sorry, we couldn’t find any matches."
     :no-matches.action/primary   "Clear all filters"
     :no-matches.action/secondary " to see more looks."
     :no-matches.action/target    [e/flow|looks-filtering|reset]}))

(defn ^:private looks-card<-
  [images-db {:look/keys [title hero-imgs skus navigation-message]}]
  ;; TODO(corey) filter catalog/department #{"hair"}
  (let [height-px 240
        gap-px    3]
    {:looks-card.title/primary  title
     :looks-card.hero/image-url (:url (first hero-imgs))
     :looks-card.hero/badge-url (:platform-source (first hero-imgs))
     :looks-card.action/target  navigation-message
     :looks-card.hero/gap-px    gap-px
     :looks-card/height-px      height-px
     :looks-card/hair-items
     (->> skus
          (map (fn [{:selector/keys [image-cases]}]
                 (let [img-count      (count skus)
                       gap-count      (dec img-count)
                       img-px         (-> height-px
                                          ;; remove total gap space
                                          (- (* gap-px gap-count))
                                          ;; divided among images
                                          (/ img-count)
                                          ;; rounded up
                                          #?(:clj  identity
                                             :cljs Math/ceil))
                       [_ _ image-id] (first
                                       (filter
                                        (fn [[use _ _]]
                                          (= "cart" use))
                                        image-cases))]
                   {:looks-card.hair-item/image-url
                    (str "https://ucarecdn.com/"
                         (-> images-db
                             (get image-id)
                             :url
                             ui/ucare-img-id)
                         "/-/format/auto/-/scale_crop/"
                         img-px "x" img-px
                         "/center/")}))))}))

(defn looks-cards<-
  [images-db looks {:looks-filtering/keys [filters]}]
  {:looks-cards/cards
   (->> (select filters looks)
        (mapv (partial looks-card<-
                       images-db)))})

(def ^:private looks-hero<-
  {:looks.hero.title/primary   "Shop by Look"
   :looks.hero.title/secondary ["Get 3 or more hair items and receive a service for FREE"
                                "#MayvennMade"]})

;; Biz Domain: Looks

(defn ^:private options|name->slug
  "Used to enrich contentful looks to be selectable"
  [facets-db]
  (->> (vals facets-db)
       (mapcat :facet/options)
       (mapv (fn [[slug option]]
               [(:option/name option) slug]))
       (into {})))

(defn ^:private looks<-
  "
  What is a look?

  A) a cart - selectable sku-items (sku x quantity)
  A.1) images derived from those sku-items
  A.2) a price derived from those sku-items
  A.3) a title derived from those sku-items
  B) image(s) of the finished look
  C) social media links
  D) promotions to apply

  Usages

  - *A* needs to be selected upon as represented as a unioned sku-items
    e.g. (select criteria (attr-value-merge sku-items))
    This is for filtering the grid-list view

  - Displaying a card for the grid-list view

  - Displaying the detailed view
  "
  [state skus-db facets-db album-keyword]
  (let [lengths-re       #",|\s+and\s+|\s?\+\s?|(?<=\")\s(?=\d)"
        name->slug       (options|name->slug facets-db)
        contentful-looks (->> (get-in state keypaths/cms-ugc-collection)
                              ;; NOTE(corey) This is hardcoded because obstensibly
                              ;; filtering should replace albums
                              :aladdin-free-install
                              :looks)]
    (->> contentful-looks
         ;; TODO(jjh) This is the shim that determines the facets from the Look (which enables the filters).
         ;; Once this page depends on SKUs being loaded in, those should be used instead.
         (mapv (fn [{:as look :keys [description color origin texture]}]
                 (let [tex-ori-col {:hair/color   #{(get name->slug color)}
                                    :hair/origin  #{(get name->slug (str origin
                                                                         " hair"))}
                                    :hair/texture #{(get name->slug texture)}}
                       skus        (->> (clojure.string/split description lengths-re)
                                        (map (fn [length]
                                               (some-> (merge
                                                        tex-ori-col
                                                        {:hair/length #{(first (re-seq #"\d+" length))}}
                                                        (cond
                                                          (not-empty (re-seq #"Closure" length))
                                                          {:hair/family        #{"closures"}
                                                           :hair/base-material #{"lace"}}
                                                          (not-empty (re-seq #"Frontal" length))
                                                          {:hair/family        #{"frontals"}
                                                           :hair/base-material #{"lace"}}
                                                          :else
                                                          {:hair/family #{"bundles"}}))
                                                       (select (vals skus-db))
                                                       first))))]
                   (merge tex-ori-col ;; TODO(corey) apply merge-with into
                          {:look/title              (or (some->> [origin texture]
                                                                 (remove nil?)
                                                                 (interpose " ")
                                                                 not-empty
                                                                 (apply str))
                                                        "Check this out!")
                           :look/hero-imgs          [{:url (:photo-url look)
                                                      :platform-source
                                                      ^:ignore-interpret-warning
                                                      (when-let [icon (svg/social-icon (:social-media-platform look))]
                                                        (icon {:class "fill-white"
                                                               :style {:opacity 0.7}}))}]
                           :look/navigation-message [e/navigate-shop-by-look-details {:album-keyword album-keyword
                                                                                      :look-id       (:content/id look)}]
                           :look/skus               skus})))))))

;; Visual Domain: Page

(defn page
  "Looks, 'Shop by Look'

  Visually: Grid, Spinning, or Filtering
  "
  [state _]
  (let [skus-db   (get-in state storefront.keypaths/v2-skus)
        images-db (get-in state storefront.keypaths/v2-images)
        facets-db (->> (get-in state storefront.keypaths/v2-facets)
                       (maps/index-by (comp keyword :facet/slug))
                       (maps/map-values (fn [facet]
                                          (update facet :facet/options
                                                  (partial maps/index-by :option/slug)))))

        selected-album-keyword (get-in state keypaths/selected-album-keyword)
        looks                  (looks<- state skus-db facets-db selected-album-keyword)
        ;; Flow models
        looks-filtering        (get-in state catalog.keypaths/k-models-looks-filtering)]
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
                                   (filtering-sections<-
                                    facets-db
                                    (->> looks ;; Note: Facets that actually exist on the looks
                                         (map #(select-keys % [:hair/origin :hair/color :hair/texture]))
                                         (apply merge-with clojure.set/union))
                                    looks-filtering)}})
      ;; Grid of Looks
      :else
      (->> {:hero              looks-hero<-
            :looks-cards       (looks-cards<- images-db
                                              looks
                                              looks-filtering)
            :no-matches        (no-matches<- looks
                                             looks-filtering)
            :filtering-summary (filtering-summary<- facets-db
                                                    looks
                                                    looks-filtering)}
           (component/build looks-template)
           (template/wrap-standard state e/navigate-shop-by-look)))))

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
