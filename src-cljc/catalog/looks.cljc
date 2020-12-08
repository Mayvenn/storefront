(ns catalog.looks
  "Shopping by Looks: index page of 'looks' for an 'album'"
  (:require [spice.maps :as maps]
            [storefront.accessors.contentful :as contentful]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.sites :as sites]
            [storefront.events :as e]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.svg :as svg]
            [storefront.components.ugc :as component-ugc]
            [storefront.components.ui :as ui]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.ugc :as ugc]))

(defn filters-status-molecule
  [{:filters.status/keys [primary secondary]}]
  [:div.flex.justify-between
   [:div.bold.shout.content-4 primary]
   [:div.content-3 secondary]])

(defcomponent filters-pills-pill-molecule
  [{:filters.pills.pill/keys [primary id target icon]} _ {:keys [id]}]
  [:div {:key id}
   (ui/button-pill
    (cond-> {:class     "p1 mr1 black content-3"
             :key       "filters-key"
             :data-test "button-show-shop-by-look-filters"}
      (not-empty target)
      (assoc :on-click (apply utils/send-event-callback target)))
    [:div.flex.items-center.px1
     primary
     (when (and target icon)
       [:a.flex.items-center.pl1
        ^:attrs (merge {:data-test id}
                       (apply utils/fake-href target))
        icon])])])

(defcomponent filters-organism
  [data _ _]
  (when (seq data)
    [:div.bg-white.py2.px3
     (filters-status-molecule data)
     [:div.flex.flex-wrap.py1
      (component/elements filters-pills-pill-molecule data
                          :filters/pills)]]))

(defcomponent template
  [{:keys [looks copy filters spinning?]} _ _]
  (if spinning?
    (ui/large-spinner {:style {:height "4em"}})
    [:div.bg-warm-gray
     [:div.center.py6
      [:h1.title-1.canela.py3 (:title copy)]
      [:p.col-10.col-6-on-tb-dt.mx-auto.proxima.content-2 (:description copy)]
      [:p.col-10.col-6-on-tb-dt.mx-auto.proxima.content-2 (:secondary-description copy)]]
     (component/build filters-organism filters)
     [:div.flex.flex-wrap.mbn2.justify-center.justify-start-on-tb-dt.bg-cool-gray.py2-on-tb-dt.px1-on-tb-dt
      (map-indexed
       (fn [idx look]
         (ui/screen-aware component-ugc/social-image-card-component
                          (assoc look :hack/above-the-fold? (zero? idx))
                          {:opts               {:copy copy}
                           :child-handles-ref? true
                           :key                (str (:id look))}))
       looks)]]))

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

(def default-copy
  {:title       "Shop by Look"
   :description "Get 3 or more hair items and receive a service for FREE"
   :secondary-description "#MayvennMade"
   :button-copy "Shop Look"
   :short-name  "look"
   :seo-title   "Shop by Look | Mayvenn"
   :og-title    "Shop by Look - Find and Buy your favorite Mayvenn bundles!"})

(defn sbl-query
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
     :filters   (let [filters ["straight" "black"]
                      pills   (concat
                               [{:filters.pills.pill/primary
                                 ^:inline
                                 [:span.mr1
                                  (svg/funnel {:class  "mrp3"
                                               :height "9px"
                                               :width  "10px"})
                                  (if (empty? filters)
                                    "Filters"
                                    (str "- " (count filters)))]
                                 :filters.pills.pill/target [e/navigate-home]}]
                               (mapv (fn [filter]
                                       {:filters.pills.pill/primary filter
                                        :filters.pills.pill/target  [e/navigate-home]
                                        :filters.pills.pill/icon
                                        ^:inline
                                        (svg/close-x
                                         {:class  "stroke-white fill-gray"
                                          :width  "13px"
                                          :height "13px"})})
                                     filters))]
                  {:filters.status/primary   "Filter By:"
                   :filters.status/secondary "88888 Looks"
                   :filters/pills            pills})
     :spinning? (empty? looks)}))

(defn ^:export built-component [data opts]
  (let [album-kw (ugc/determine-look-album data (get-in data keypaths/selected-album-keyword))]
    (if (and (experiments/sbl-update? data)        ;; featured
             (= :shop (sites/determine-site data)) ;; dtc, shop
             (= :aladdin-free-install album-kw))   ;; main look page
      (component/build template
                       (merge (sbl-query data)
                              {}))
      (component/build original-component (query data) opts))))
