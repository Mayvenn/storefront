(ns catalog.icp
  (:require [api.catalog :refer [select]]
            [adventure.components.layered :as layered]
            catalog.keypaths
            [catalog.skuers :as skuers]
            [catalog.ui.category-hero :as category-hero]
            [catalog.ui.category-filters :as category-filters]
            [catalog.ui.content-box :as content-box]
            [catalog.ui.facet-filters :as facet-filters]
            [catalog.ui.product-card-listing :as product-card-listing]
            [catalog.ui.spotlighting :as spotlighting]
            [catalog.ui.return-address-labels :as return-address-labels]
            [homepage.ui.faq :as faq]
            [spice.maps :as maps]
            [storefront.accessors.categories :as accessors.categories]
            [storefront.accessors.experiments :as experiments]
            [storefront.assets :as assets]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]))

(def ^:private contact-query
  (merge {:layer/type :shop-contact}
         layered/shop-contact-query))

(defn ^:private category-hero-query
  [category]
  ;; TODO(corey) icp heroes use #:category not #:copy for :description
  {:category-hero.title/primary (:copy/title category)
   :category-hero.body/primary  (:category/description category)})

(defn drill-category-image [image-id]
  [:picture
   [:source {:src-set (str "//ucarecdn.com/" image-id "/-/format/auto/-/quality/lightest/-/resize/124x/" " 2x,"
                           "//ucarecdn.com/" image-id "/-/format/auto/-/quality/normal/-/resize/62x/" " 1x")}]
   [:img ^:attrs {:src (str "//ucarecdn.com/" image-id "/-/format/auto/-/scale_crop/62x62/center/") :alt ""}]])

(defcomponent ^:private drill-category-list-entry-organism
  [{:drill-category/keys [id title description image-id target action-id action-label showing-3-or-more?]} _ _]
  (when id
    [:div.flex-auto.p3.content-start
     {:key       id
      :data-test id}
     [:div.flex
      (when showing-3-or-more?
        {:class "flex-column-on-tb-dt"})
      [:div.hide-on-tb-dt.mr4
       (when image-id
         (drill-category-image image-id))]
      [:div.hide-on-mb.mr4
       (when image-id
         (drill-category-image image-id))]
      [:div
       [:div.title-2.proxima.shout title]
       [:div.content-2.proxima.py1 description]
       (when action-id
         (ui/button-small-underline-primary
          (assoc (apply utils/route-to target)
                 :data-test  action-id)
          action-label))]]]))

(defcomponent ^:private drill-category-list-organism
  [{:drill-category-list/keys [values showing-3-or-more?]} _ _]
  (when (seq values)
    [:div.py8.flex.justify-center.flex-column-on-mb
     (when-not showing-3-or-more?
       {:class "max-960 mx-auto"})
     (mapv #(component/build drill-category-list-entry-organism
                             (assoc % :drill-category/showing-3-or-more? showing-3-or-more?)
                             {:key (:drill-category/id %)})
           values)]))

(defcomponent ^:private drill-category-grid-entry-organism
  [{:drill-category/keys [title svg-url target]} _ {:keys [id]}]
  (when id
    (let [link-attrs (apply utils/route-to target)]
      [:div.py3.flex.flex-column.items-center
       {:id        id
        :data-test id
        :style     {:width  "120px"
                    :height "128px"}}
       [:a.block.mt1
        link-attrs
        [:img {:src   (assets/path svg-url)
               :width 72
               :alt   title}]]
       (ui/button-small-underline-primary link-attrs title)])))

(defcomponent ^:private drill-category-grid-organism
  [{:drill-category-grid/keys [values title]} _ _]
  (when (seq values)
    [:div.py8.px4
     [:div.title-2.proxima.shout title]
     [:div.hide-on-mb.hide-on-tb
      (->> (mapv #(component/build drill-category-grid-entry-organism
                                   %
                                   (component/component-id (str (:drill-category/id %) "-desktop")))
                 values)
           (partition-all 4)
           (map-indexed (fn [i row] (component/html [:div.flex.justify-around {:key (str "i-" i)} row]))))]
     [:div.hide-on-dt.hide-on-mb ; tb
      (->> (mapv #(component/build drill-category-grid-entry-organism
                                   %
                                   (component/component-id (str (:drill-category/id %) "-tablet")))
                 values)
           (partition 3 3 [[:div {:key "placeholder" :style {:width "120px"}}]])
           (map-indexed (fn [i row] (component/html [:div.flex.justify-around {:key (str "i-" i)} row]))))]
     [:div.hide-on-dt.hide-on-tb ; mb
      (->> (mapv #(component/build drill-category-grid-entry-organism
                                   %
                                   (component/component-id (str (:drill-category/id %) "-mobile")))
                 values)
           (partition-all 2)
           (map-indexed (fn [i row] (component/html [:div.flex.justify-around {:key (str "i-" i)} row]))))]]))

(defn ^:private category->drill-category-list-entry
  [category]
  {:drill-category/id           (:page/slug category)
   :drill-category/title        (:copy/title category)
   :drill-category/description  (or
                                 (:subcategory/description category)
                                 (:category/description category))
   :drill-category/image-id     (:subcategory/image-id category)
   :drill-category/target       (if-let [product-id (:direct-to-details/id category)]
                                  [events/navigate-product-details (merge
                                                                    {:catalog/product-id product-id
                                                                     :page/slug          (:direct-to-details/slug category)}
                                                                    (when-let [sku-id (:direct-to-details/sku-id category)]
                                                                      {:query-params {:SKU sku-id}}))]
                                  [events/navigate-category category])
   :drill-category/action-id    (str "drill-category-action-" (:page/slug category))
   :drill-category/action-label (or (:copy/icp-action-label category) (str "Shop " (:copy/title category)))})

(defn ^:private category->drill-category-grid-entry
  [category]
  {:drill-category/id      (str "drill-category-action-" (:page/slug category))
   :drill-category/title   (:subcategory/title category)
   :drill-category/svg-url (:icon category)
   :drill-category/target  (if-let [product-id (:direct-to-details/id category)]
                             [events/navigate-product-details (merge
                                                               {:catalog/product-id product-id
                                                                :page/slug          (:direct-to-details/slug category)}
                                                               (when-let [sku-id (:direct-to-details/sku-id category)]
                                                                 {:query-params {:SKU sku-id}}))]
                             [events/navigate-category category])})

(def ^:private purple-divider-atom
  (component/html
   [:div
    {:style {:background-image    "url('//ucarecdn.com/73db5b08-860e-4e6c-b052-31ed6d951f00/-/resize/x24/')"
             :background-position "center center"
             :background-repeat   "repeat-x"
             :height              "24px"}}]))

(def ^:private green-divider-atom
  (component/html
   [:div
    {:style {:background-image  "url('//ucarecdn.com/7e91271e-874c-4303-bc8a-00c8babb0d77/-/resize/x24/')"
             :background-position "center center"
             :background-repeat   "repeat-x"
             :height              "24px"}}]))


(defcomponent ^:private product-list
  [{:keys [product-card-listing]} _ _]
  [:div.plj3-on-dt
   (component/build product-card-listing/organism product-card-listing)])

(defcomponent ^:private template
  "This lays out different ux pieces to form a cohesive ux experience"
  [{:keys [category-hero
           title
           content-box
           expanding-content-box
           drill-category-grid
           drill-category-list] :as queried-data} _ _]
  [:div
   (component/build category-hero/organism category-hero)
   (when-let [data (:spotlighting queried-data)]
     (component/build spotlighting/organism data {}))
   (when-let [data (:return-address-labels queried-data)]
     (component/build return-address-labels/organism data {}))
   [:div.mx-auto
    (component/build drill-category-list-organism drill-category-list)
    (component/build drill-category-grid-organism drill-category-grid)]

   [:div.mb10 purple-divider-atom]
   [:div.py5
    (when title [:div.canela.title-1.center.mb2 title])
    (component/build facet-filters/organism queried-data {:opts {:child-component product-list}})]

   (when content-box
     [:div green-divider-atom
      (component/build content-box/organism content-box)])
   (when expanding-content-box
     [:div green-divider-atom
      (component/build faq/organism expanding-content-box)])
   (component/build layered/shop-contact contact-query)])

(defn category->subcategories
  "Returns a hydrated sequence of subcategories for the given category"
  [all-categories {subcategory-ids :subcategories/ids}]
  (let [indexed-categories (maps/index-by :catalog/category-id all-categories)
        category-order     (zipmap subcategory-ids (range))]
    (->> (select-keys indexed-categories subcategory-ids)
         vals
         (sort-by (comp category-order :catalog/category-id)))) )

(defn page
  [state _]
  (let [current-category      (accessors.categories/current-category state)
        interstitial-category (accessors.categories/current-category state)

        facet-filtering-state               (assoc (get-in state catalog.keypaths/k-models-facet-filtering)
                                                   :facet-filtering/item-label "item")
        selections                          (:facet-filtering/filters facet-filtering-state)
        loaded-category-products            (->> (get-in state keypaths/v2-products)
                                                 vals
                                                 (select (merge
                                                          (skuers/electives interstitial-category)
                                                          (skuers/essentials interstitial-category))))
        categories                          (get-in state keypaths/categories)
        subcategories                       (category->subcategories categories interstitial-category)
        spotlight-subcategories             (map #(accessors.categories/id->category % categories)
                                                 (:spotlighting/category-ids interstitial-category))
        category-products-matching-criteria (->> loaded-category-products
                                                 (select (merge
                                                          (skuers/essentials interstitial-category)
                                                          selections)))
        shop?                               (or (= "shop" (get-in state keypaths/store-slug))
                                                (= "retail-location" (get-in state keypaths/store-experience)))
        faq                                 (get-in state (conj keypaths/cms-faq (:contentful/faq-id interstitial-category)))]
    (component/build template
                     (merge
                      (when-let [filter-title (:product-list/title interstitial-category)]
                        {:title filter-title})
                      {:category-hero         (category-hero-query interstitial-category)
                       :content-box           (when (and shop? (:content-block/type interstitial-category))
                                                {:title    (:content-block/title interstitial-category)
                                                 :header   (:content-block/header interstitial-category)
                                                 :summary  (:content-block/summary interstitial-category)
                                                 :sections (:content-block/sections interstitial-category)})
                       :expanding-content-box (when (and shop? faq)
                                                (let [{:keys [question-answers]} faq]
                                                  {:faq/expanded-index (get-in state keypaths/faq-expanded-section)
                                                   :list/sections      (for [{:keys [question answer]} question-answers]
                                                                         {:faq/title   (:text question)
                                                                          :faq/content answer})}))
                       :spotlighting          (when (= "13" (:catalog/category-id current-category))
                                                {:title      (:spotlighting/title interstitial-category)
                                                 :spotlights (map (fn [{:keys [:subcategory/image-id
                                                                               :subcategory/title
                                                                               :catalog/category-id
                                                                               :page/slug]}]
                                                                    {:image-src image-id
                                                                     :title     title
                                                                     :alt       (str "Woman wearing a " (clojure.string/join (butlast title)))
                                                                     :nav-event [events/navigate-category
                                                                                 {:page/slug           slug
                                                                                  :catalog/category-id category-id}]})
                                                                  spotlight-subcategories)})
                       :product-card-listing  (product-card-listing/query state
                                                                          interstitial-category
                                                                          category-products-matching-criteria)}

                      (facet-filters/filters<-
                       {:facets-db             (get-in state storefront.keypaths/v2-facets)
                        :faceted-models        loaded-category-products
                        :facet-filtering-state facet-filtering-state
                        :facets-to-filter-on   (:selector/electives interstitial-category)
                        :navigation-event      events/navigate-category
                        :navigation-args       (select-keys interstitial-category [:catalog/category-id :page/slug])
                        :child-component-data  {:product-card-listing
                                                (product-card-listing/query state
                                                                            interstitial-category
                                                                            category-products-matching-criteria)}})

                      (case (:subcategories/layout interstitial-category)
                        :grid
                        {:drill-category-grid {:drill-category-grid/values (mapv category->drill-category-grid-entry subcategories)
                                               :drill-category-grid/title  (:subcategories/title interstitial-category)}}
                        :list
                        (let [values (mapv category->drill-category-list-entry subcategories)]
                          {:drill-category-list {:drill-category-list/values                 values
                                                 :drill-category-list/showing-3-or-more?     (>= (count values) 3)
                                                 :drill-category-list/tablet-desktop-columns (max 1 (min 3 (count values)))}})
                        :return-address-labels
                        {:return-address-labels {:title  "Shop by Category"
                                                 :labels (map (fn [{:keys [:subcategory/image-id
                                                                           :subcategory/title
                                                                           :catalog/category-id
                                                                           :page/slug]
                                                                    :as   subcategory}]
                                                                {:image-src image-id
                                                                 :title     (or title (:copy/title subcategory))
                                                                 :alt       (str "Woman wearing a " (clojure.string/join (butlast (or title (:copy/title subcategory)))))
                                                                 :nav-event [events/navigate-category
                                                                             {:page/slug           slug
                                                                              :catalog/category-id category-id}]})
                                                              subcategories)}}
                        nil)))))
