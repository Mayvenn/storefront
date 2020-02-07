(ns catalog.icp
  (:require [catalog.categories :as categories]
            catalog.keypaths
            [catalog.ui.product-list :as product-list]
            [spice.core :as spice]
            [storefront.assets :as assets]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.accessors.experiments :as experiments]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [adventure.components.layered :as layered]
            [adventure.shop-home :as shop-home]
            [spice.maps :as maps]))

(defn ^:private vertical-squiggle-atom
  [top]
  (component/html
   [:div.relative
    [:div.absolute.col-12.flex.justify-center
     {:style {:top top}}
     ^:inline (svg/vertical-squiggle {:style {:height "72px"}})]]))

(defcomponent ^:private header-organism
  [_ _ _]
  [:div])

(defcomponent ^:private footer-organism
  [_ _ _]
  [:div])

(defn category-hero-title
  [{:category-hero.title/keys [id value]}]
  (when id
    [:div.title-1.canela.pt5 value]))

(defn category-hero-description
  "A ui element"
  [{:category-hero.description/keys [id value]}]
  (when id
    [:div.content-2.proxima.py5.col-6-on-dt.mx-auto value]))

(defcomponent ^:private category-hero-organism
  "This uses ui elements to build a piece of UX"
  [data _ _]
  [:div.bg-warm-gray.center.py5.px6
   (category-hero-title data)
   (category-hero-description data)])

(defn ^:private category-hero-query
  [category wig-customization?]
  {:category-hero.title/id          "category-hero-title"
   :category-hero.title/value       (:copy/title category)
   :category-hero.description/id    "category-hero-description"
   :category-hero.description/value (if (->> category :catalog/category-id (= "13") (and wig-customization?))
                                      (str (:category/description category) " Get free customization with qualifying purchases.")
                                      (:category/description category))})

(defcomponent ^:private drill-category-list-entry-organism
  [{:drill-category/keys [id title description image-url target action-id action-label]} _ _]
  (when id
    [:div.p3.col-4-on-tb-dt.flex.flex-wrap.col-12.content-start
     {:key       id
      :data-test id}
     [:div.col-12-on-tb-dt.col-3
      [:div.hide-on-tb-dt.flex.justify-end.mr4.mt1
       (when image-url
         (ui/ucare-img {:width "62"} image-url))]
      [:div.hide-on-mb
       (when image-url
         (ui/ucare-img {:width "62"} image-url))]]
     [:div.col-12-on-tb-dt.col-9
      [:div.title-2.proxima.shout title]
      [:div.content-2.proxima.py1 description]
      (when action-id
        (ui/button-small-underline-primary
         (assoc (apply utils/route-to target)
                :data-test  action-id)
         action-label))]]))

(defcomponent ^:private drill-category-list-organism
  [{:drill-category-list/keys [values]} _ _]
  (when (seq values)
    [:div.py8.flex.flex-wrap
     (mapv #(component/build drill-category-list-entry-organism %
                             {:key (:drill-category/id %)})
           values)]))

(defcomponent ^:private drill-category-grid-entry-organism
  [{:drill-category/keys [title svg-url target]} _ {:keys [id]}]
  (when id
    [:div.py3.flex.flex-column.items-center
     {:id        id
      :data-test id
      :style     {:width "120px"}}
     [:a.block.mt1
      (apply utils/route-to target)
      [:img {:src   (assets/path svg-url)
             :width 72}]]
     (ui/button-small-underline-primary {} title)]))

(defcomponent ^:private drill-category-grid-organism
  [{:drill-category-grid/keys [values title]} _ _]
  (when (seq values)
    (let [grid-entries (mapv #(component/build drill-category-grid-entry-organism
                                               %
                                               (component/component-id (:drill-category/id %)))
                             values)]
      [:div.py8.px4
       [:div.title-2.proxima.shout title]
       [:div.hide-on-mb.hide-on-tb ; dt
        (->> grid-entries
             (partition-all 4)
             (mapv (fn [row] [:div.flex.justify-around row])))]
       [:div.hide-on-dt.hide-on-mb ; tb
        (->> grid-entries
             (partition 3 3 [[:div {:style {:width "120px"}}]])
             (mapv (fn [row] [:div.flex.justify-around row])))]
       [:div.hide-on-dt.hide-on-tb ; mb
        (->> grid-entries
             (partition-all 2)
             (mapv (fn [row] [:div.flex.justify-around row])))]])))

(defn ^:private category->drill-category-list-entry
  [category]
  {:drill-category/id           (:page/slug category)
   :drill-category/title        (:copy/title category)
   :drill-category/description  (:copy/description category)
   :drill-category/image-url    (:subcategory/image-uri category)
   :drill-category/target       (if-let [product-id (:direct-to-details/id category)]
                                  [events/navigate-product-details (merge
                                                                    {:catalog/product-id product-id
                                                                     :page/slug          (:direct-to-details/slug category)}
                                                                    (when-let [sku-id (:direct-to-details/sku-id category)]
                                                                      {:query-params {:SKU sku-id}}))]
                                  [events/navigate-category category])
   :drill-category/action-id    (str "drill-category-action-" (:page/slug category))
   :drill-category/action-label (str "Shop " (:copy/title category))})

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
  [:div
   {:style {:background-image    "url('//ucarecdn.com/73db5b08-860e-4e6c-b052-31ed6d951f00/-/resize/x24/')"
            :background-position "center center"
            :background-repeat   "repeat-x"
            :height              "24px"}}])

(def ^:private green-divider-atom
  [:div
   {:style {:background-image  "url('//ucarecdn.com/7e91271e-874c-4303-bc8a-00c8babb0d77/-/resize/x24/')"
            :background-position "center center"
            :background-repeat   "repeat-x"
            :height              "24px"}}])

(defcomponent content-box-organism
  [{:keys [title header summary sections]} _ _]
  [:div.py8.px4.bg-cool-gray
   [:div.max-960.mx-auto
    [:div.pb2
     [:div.proxima.title-2.bold.caps ^:inline (str title)]
     [:div.canela.title-1.pb2 ^:inline (str header)]
     [:div.canela.content-1 ^:inline (str summary)]]

    (for [{:keys [title body]} sections]
      [:div.py2 {:keys title}
       [:div.proxima.title-2.bold.caps.pb1 ^:inline (str title)]
       [:div.canela.content-2 ^:inline (str body)]])

    [:div.py2
     [:div.proxima.title-2.bold.caps.pb1 "Still Have Questions?"]
     [:div.canela.content-2
      [:div "Customer Service can help!"]
      [:div "Call " [:a.inherit-color {:href (ui/phone-url "18885627952")} "(888) 562-7952 "]]
      [:div "Monday through Friday from 8am-5pm PST."]]]]])

(defcomponent ^:private template
  "This lays out different ux pieces to form a cohesive ux experience"
  [{:keys [header footer category-hero content-box drill-category-list drill-category-grid product-list]} _ _]
  [:div
   (component/build header-organism header)
   [:div
    (component/build category-hero-organism category-hero)
    (vertical-squiggle-atom "-36px")
    [:div.max-960.mx-auto
     (component/build drill-category-list-organism drill-category-list)
     (component/build drill-category-grid-organism drill-category-grid)]
    purple-divider-atom
    [:div.max-960.mx-auto
     (component/build product-list/organism product-list)]
    (when content-box green-divider-atom)
    (when content-box (component/build content-box-organism content-box))
    (component/build layered/shop-contact shop-home/shop-contact-query)]
   (component/build footer-organism footer)])

(defn category->subcategories
  "Returns a hydrated sequence of subcategories for the given category"
  [all-categories {subcategory-ids :subcategories/ids}]
  (let [indexed-categories (maps/index-by :catalog/category-id all-categories)
        category-order     (zipmap subcategory-ids (range))]
    (->> (select-keys indexed-categories subcategory-ids)
         vals
         (sort-by (comp category-order :catalog/category-id)))) )

(defn query
  [app-state]
  (let [category      (catalog.categories/current-category app-state)
        subcategories (category->subcategories (get-in app-state keypaths/categories) category)
        selections    (get-in app-state catalog.keypaths/category-selections)
        products      (vals (get-in app-state keypaths/v2-products))]
    (cond->
        {:header              {}
         :footer              {}
         :category-hero       (category-hero-query category (experiments/wig-customization? app-state))
         :content-box         (when (:content-block/type category)
                                {:title    (:content-block/title category)
                                 :header   (:content-block/header category)
                                 :summary  (:content-block/summary category)
                                 :sections (:content-block/sections category)})
         :product-list        (product-list/query app-state category products selections)}

      (= :grid (:subcategories/layout category))
      (merge {:drill-category-grid {:drill-category-grid/values (mapv category->drill-category-grid-entry subcategories)
                                    :drill-category-grid/title  (:subcategories/title category)}})

      (= :list (:subcategories/layout category))
      (merge {:drill-category-list {:drill-category-list/values (mapv category->drill-category-list-entry subcategories)}}))))

(defn page
  [app-state opts]
  (component/build template (query app-state) opts))
