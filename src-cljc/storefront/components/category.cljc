(ns storefront.components.category
  (:require [storefront.platform.component-utils :as utils]
            [storefront.components.money-formatters :refer [as-money-without-cents as-money]]
            [storefront.accessors.promos :as promos]
            [storefront.accessors.named-searches :as named-searches]
            [storefront.accessors.categories :as categories]
            [storefront.accessors.products :as products]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.bundle-builder :as bundle-builder]
            [storefront.platform.reviews :as reviews]
            [storefront.platform.ugc :as ugc]
            [storefront.components.ui :as ui]
            [clojure.string :as string]
            [clojure.set :as set]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.assets :as assets]
            [storefront.request-keys :as request-keys]
            [storefront.platform.carousel :as carousel]
            [storefront.components.money-formatters :as mf]
            [storefront.platform.messages :as messages]
            [clojure.string :as str]))

(defn slug->facet [facet facets]
  (->> facets
       (filter (fn [{:keys [:facet/slug]}] (= slug facet)))
       first))

(defn slug->option [option options]
  (->> options
       (filter (fn [{:keys [:option/slug]}] (= slug option)))
       first))

(defmulti unconstrained-facet (fn [skus facets facet] facet))
(defmethod unconstrained-facet :hair/length [skus facets facet]
  (let [lengths  (->> skus
                      (map #(get-in % [:attributes :hair/length]))
                      sort)
        shortest (first lengths)
        longest  (last lengths)]
    [:p.h6.dark-gray
     "in "
     (->> facets
          (slug->facet :hair/length)
          :facet/options
          (slug->option shortest)
          :option/name)
     " - "
     (->> facets
          (slug->facet :hair/length)
          :facet/options
          (slug->option longest)
          :option/name)]))

(defmethod unconstrained-facet :hair/color [skus facets facet]
  (let [colors (->> skus
                    (map #(get-in % [:attributes :hair/color]))
                    distinct)]
    (when (> (count colors) 1)
      [:p.h6.dark-gray "+ more colors available"])))

(defn filter-tabs [{:keys [facets filtered-sku-sets criteria]}]
  (let [sku-set-count        (count filtered-sku-sets)
        applied-filter-count (->> criteria
                                  (map (comp count val))
                                  (apply +))]
    [:div.py4
     (when (seq facets)
       [:div
        [:div.pb1.flex.justify-between
         [:p.h6.dark-gray (case applied-filter-count
                            0 "Filter By:"
                            1 "1 filter applied:"
                            (str applied-filter-count " filters applied:"))]
         [:p.h6.dark-gray (str sku-set-count " Item" (when (not= 1 sku-set-count) "s"))]]
        (into [:div.border.h6.border-teal.rounded.flex.center]
              (map-indexed
               (fn [idx {:keys [slug title selected?]}]
                 [:a.flex-auto.x-group-item.rounded-item
                  (assoc
                   (if selected?
                     (utils/fake-href events/control-category-filters-close)
                     (utils/fake-href events/control-category-filter-select {:selected slug}))
                   :key slug
                   :class (if selected? "bg-teal white" "dark-gray"))
                  [:div.border-teal.my1
                   {:class (when-not (zero? idx) "border-left")}
                   title]])
               facets))])]))

(defn filter-panel [selected-facet]
  [:div.px1
   (for [options (partition-all 4 (:options selected-facet))]
     [:div.flex-on-tb-dt.justify-around
      (for [{:keys [slug label represented? selected?]} options]
        [:div.py1.mr4
         {:key (str "filter-option-" slug)}
         (ui/check-box {:label     [:span
                                    (when (categories/new-facet? [(:slug selected-facet) slug]) [:span.mr1.teal "NEW"])
                                    label]
                        :value     selected?
                        :disabled  (not represented?)
                        :on-change #(let [event-handler (if selected?
                                                          events/control-category-criterion-deselected
                                                          events/control-category-criterion-selected)]
                                      (messages/handle-message event-handler
                                                               {:filter (:slug selected-facet)
                                                                :option slug}))})])])
   [:div.clearfix.mxn3.px1.py4.hide-on-tb-dt
    [:div.col.col-6.px3
     (ui/teal-ghost-button
      (utils/fake-href events/control-category-criteria-cleared)
      "Clear all")]
    [:div.col.col-6.px3
     (ui/teal-button
      (utils/fake-href events/control-category-filters-close)
      "Done")]]])

(defn hero-section [category]
  [:h1
   (let [{:keys [mobile-url file-name desktop-url alt]} (:hero (:images category))]
     [:picture
      [:source {:media   "(min-width: 750px)"
                :src-set (str desktop-url "-/format/auto/" file-name " 1x")}]
      [:img.block.col-12 {:src (str mobile-url "-/format/auto/" file-name)
                          :alt alt}]])])

(defn copy-section [category]
  [:div.mt6.mb2 [:p.py6.max-580.mx-auto.center (-> category :copy :description)]])

(defn product-cards [sku-sets facets]
  [:div.flex.flex-wrap.mxn1
   (if (empty? sku-sets)
     [:div.col-12.my8.py4.center
      [:p.h1.py4 "😞"]
      [:p.h2.dark-gray.py6 "Sorry, we couldn’t find any matches."]
      [:p.h4.dark-gray.mb10.pb10
       [:a.teal (utils/fake-href events/control-category-criteria-cleared) "Clear all filters"]
       " to see more hair."]]
     (for [{:keys [slug matching-skus representative-sku name sold-out?] :as sku-set} sku-sets]
       (let [image (->> representative-sku :images (filter (comp #{"catalog"} :use-case)) first)]
         [:div.col.col-6.col-4-on-tb-dt.px1 {:key slug}
          [:div.mb10.center
           ;; TODO: when adding aspect ratio, also use srcset/sizes to scale these images.
           [:img.block.col-12 {:src (str (:url image)
                                         (:filename image))
                               :alt (:alt image)}]
           [:h2.h4.mt3.mb1 name]
           (if sold-out?
             [:p.h6.dark-gray "Out of stock"]
             [:div
              ;; This is pretty specific to hair. Might be better to have a
              ;; sku-set know its "constrained" and "unconstrained" facets.
              (unconstrained-facet matching-skus facets :hair/length)
              (unconstrained-facet matching-skus facets :hair/color)
              [:p.h6 "Starting at " (mf/as-money-without-cents (:price representative-sku))]])]])))])

(defn ^:private component [{:keys [category filters facets]} owner opts]
  (component/create
   [:div
    (hero-section category)
    [:div.max-960.col-12.mx-auto.px2-on-mb
     (copy-section category)
     [:div.bg-white.sticky.pt1
      {:style {:top "-5px"}}
      (if-let [selected-facet (->> filters
                                   :facets
                                   (filter :selected?)
                                   first)]
        [:div
         [:div.hide-on-tb-dt.px2.z4.fixed.overlay.overflow-auto.bg-white
          (filter-tabs filters)
          (filter-panel selected-facet)]
         [:div.hide-on-mb
          (filter-tabs filters)
          (filter-panel selected-facet)]]
        [:div
         (filter-tabs filters)])]
     (product-cards (:filtered-sku-sets filters) facets)]]))

(defn ^:private query [data]
  {:category (categories/current-category data)
   :filters  (get-in data keypaths/category-filters)
   :facets   (get-in data keypaths/facets)})

(defn built-component [data opts]
  (component/build component (query data) opts))

