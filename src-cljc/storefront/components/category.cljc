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
            [storefront.platform.messages :as messages]))

(defn facet-definition [facets facet option]
  (->> facets
       (filter (fn [{:keys [step]}] (= step facet)))
       first
       :options
       (filter (fn [{:keys [:option/slug]}] (= slug option)))
       first))

(defmulti unconstrained-facet (fn [skus facets facet] facet))
(defmethod unconstrained-facet :length [skus facets facet]
  (let [lengths  (->> skus
                      (map #(get-in % [:attributes :length]))
                      sort)
        shortest (first lengths)
        longest  (last lengths)]
    [:p.h6.dark-gray
     "in "
     (:name (facet-definition facets :length shortest))
     " - "
     (:name (facet-definition facets :length longest))]))

(defmethod unconstrained-facet :color [skus facets facet]
  (let [colors (->> skus
                    (map #(get-in % [:attributes :color]))
                    distinct)]
    (when (> (count colors) 1)
      [:p.h6.dark-gray "+ more colors available"])))

(def filter-names
  {:family   "Category"
   :style    "Texture"
   :origin   "Origin"
   :material "Material"
   :color    "Color"})

(defn segment [{:keys [title slug selected?]}]
  [:a.segmented-control-item.center.flex-auto.py1
   (merge
    (utils/fake-href events/control-category-filter-select
                     {:selected slug})
    {:key   slug}
    {:class (if selected?
              "bg-teal white"
              "dark-gray")})
   title])

(defn segment-divider [left-item right-item selected-item]
  [:div.border-left.flex-none
   (merge
    {:key (str "divider-" (:slug left-item))}
    (if (#{left-item right-item} selected-item)
      {:class "border-white"}
      {:class "my1 border-teal"}))])

(defn filter-box [items selected-item]
  (into [:div.flex.border.rounded.border-teal.teal.h6]
        (concat (->> (partition 2 1 items)
                     (map (fn [[left-item right-item]]
                            [(segment left-item)

                             (segment-divider left-item right-item selected-item)])))
                [(segment (last items))])))

(defn filter-component [{:keys [facets filtered-sku-sets]}]
  (let [selected-facet     (->> facets
                                (filter :selected?)
                                first)
        number-of-sku-sets (count filtered-sku-sets)]
    [:div.bg-white.px2
     {:class (if selected-facet
               "z4 fixed overlay"
               "sticky top-0")}
     [:div.mb4
      [:div.pb1.flex.justify-between
       [:p.h6.dark-gray "Filter By:"]
       [:p.h6.dark-gray (str number-of-sku-sets " Item" (when (not= 1 number-of-sku-sets) "s"))]]
      (filter-box facets selected-facet)]
     (when selected-facet
       [:div.px1
        [:ul.list-reset
         (for [{:keys [slug label selected?]} (:options selected-facet)]
           [:li.py1
            {:key (str "filter-option-" slug)}
            (ui/check-box {:label     label
                           :value     selected?
                           :on-change #(let [event-handler (if selected?
                                                             events/control-category-criteria-deselected
                                                             events/control-category-criteria-selected)]
                                         (messages/handle-message event-handler
                                                                  {:filter (:slug selected-facet)
                                                                   :option slug}))})])]
        [:div.clearfix.mxn3.px1.mt4
         [:div.col.col-6.px3
          (ui/teal-ghost-button
           {}
           #_(utils/fake-href events/control-pillbox-select
                              {:keypath  keypath
                               :selected id})
           "Clear all")]
         [:div.col.col-6.px3
          (ui/teal-button
           (utils/fake-href events/control-category-filters-close)
           "Done")]]])]))

(defn ^:private component [{:keys [category filters facets]} owner opts]
  (component/create
   [:div
    [:h1
     (let [{:keys [mobile-url file-name desktop-url alt]} (:hero (:images category))]
       [:picture
        [:source {:media   "(min-width: 750px)"
                  :src-set (str desktop-url "-/format/auto/" file-name " 1x")}]
        [:img.block.col-12 {:src (str mobile-url "-/format/auto/" file-name)
                            :alt alt}]])]
    [:div.max-960.col-12.mx-auto
     [:div.px2-on-mb
      [:div.py6 [:p.my6.max-580.mx-auto.center (-> category :copy :description)]]
      (filter-component filters)
      [:div.flex.flex-wrap.mxn1
       (for [{:keys [slug matching-skus name sold-out?] :as sku-set} (:filtered-sku-sets filters)]
         (let [representative-sku (apply min-key :price matching-skus)
               image (->> representative-sku :images (filter (comp #{"catalog"} :use-case)) first)]
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
                (unconstrained-facet matching-skus facets :length)
                (unconstrained-facet matching-skus facets :color)
                [:p.h6 "Starting at " (mf/as-money-without-cents (:price representative-sku))]])]]))]]]]))

(defn ^:private query [data]
  {:category (categories/current-category data)
   :filters  (get-in data keypaths/category-filters)
   :facets   (get-in data keypaths/facets)})

(defn built-component [data opts]
  (component/build component (query data) opts))

