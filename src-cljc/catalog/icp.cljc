(ns catalog.icp
  (:require [adventure.components.layered :as layered]
            catalog.keypaths
            [catalog.skuers :as skuers]
            [catalog.ui.category-filters :as category-filters]
            [catalog.ui.category-hero :as category-hero]
            [catalog.ui.product-card-listing :as product-card-listing]
            [spice.maps :as maps]
            [spice.selector :as selector]
            [storefront.accessors.categories :as accessors.categories]
            [storefront.assets :as assets]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.config :as config]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]))

(def ^:private contact-query
  {:layer/type         :shop-contact
   :title/value        "Contact Us"
   :sub-subtitle/value "We're here to help"
   :subtitle/value     "Have Questions?"
   :contact-us-blocks  [{:url   (ui/sms-url "346-49")
                         :svg   (svg/icon-sms {:height 51
                                               :width  56})
                         :title "Live Chat"
                         :copy  "Text: 346-49"}
                        {:url   (ui/phone-url "1 (855) 287-6868")
                         :svg   (svg/icon-call {:class  "bg-white fill-black stroke-black circle"
                                                :height 57
                                                :width  57})
                         :title "Call Us"
                         :copy  "1 (855) 287-6868"}
                        {:url   (ui/email-url "help@mayvenn.com")
                         :svg   (svg/icon-email {:height 39
                                                 :width  56})
                         :title "Email Us"
                         :copy  "help@mayvenn.com"}]})

(defn ^:private vertical-squiggle-atom
  [top]
  (component/html
   [:div.relative
    [:div.absolute.col-12.flex.justify-center
     {:style {:top top}}
     ^:inline (svg/vertical-squiggle {:style {:height "72px"}})]]))

(defn ^:private category-hero-query
  [category]
  ;; TODO(corey) icp heroes use #:category not #:copy for :description
  (cond-> {:category-hero.title/primary (:copy/title category)
           :category-hero.body/primary  (:category/description category)}

    ;; TODO(corey) why can't this be data?
    (= "13" (:catalog/category-id category))
    (update :category-hero.body/primary
            str " Get free customization with qualifying purchases.")))

(defcomponent ^:private drill-category-list-entry-organism
  [{:drill-category/keys [id title description image-url target action-id action-label use-three-column-layout?]} _ _]
  (when id
    (if use-three-column-layout?
      [:div.p3.flex.flex-wrap.col-12.content-start
       {:key       id
        :data-test id
        :class     "col-4-on-tb-dt"}
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
           action-label))]]
      [:div.p3.flex.flex-wrap.col-12.content-start
       {:key       id
        :data-test id
        :class     "col-6-on-tb-dt"}
       [:div.col-3
        [:div.flex.justify-end.mr4.mt1
         (when image-url
           (ui/ucare-img {:width "62"} image-url))]]
       [:div.col-9
        [:div.title-2.proxima.shout title]
        [:div.content-2.proxima.py1 description]
        (when action-id
          (ui/button-small-underline-primary
           (assoc (apply utils/route-to target)
                  :data-test  action-id)
           action-label))]])))

(defcomponent ^:private drill-category-list-organism
  [{:drill-category-list/keys [values use-three-column-layout?]} _ _]
  (when (seq values)
    [:div.py8.flex.flex-wrap.justify-center
     (mapv #(component/build drill-category-list-entry-organism
                             (assoc % :drill-category/use-three-column-layout? use-three-column-layout?)
                             {:key (:drill-category/id %)})
           values)]))

(defcomponent ^:private drill-category-grid-entry-organism
  [{:drill-category/keys [title svg-url target]} _ {:keys [id]}]
  (when id
    (let [link-attrs (apply utils/route-to target)]
      [:div.py3.flex.flex-column.items-center
       {:id        id
        :data-test id
        :style     {:width "120px"}}
       [:a.block.mt1
        link-attrs
        [:img {:src   (assets/path svg-url)
               :width 72}]]
       (ui/button-small-underline-primary link-attrs title)])))

(defcomponent ^:private drill-category-grid-organism
  [{:drill-category-grid/keys [values title]
    :browser/keys [desktop? tablet? mobile?]} _ _]
  (when (seq values)
    (let [grid-entries (mapv #(component/build drill-category-grid-entry-organism
                                               %
                                               (component/component-id (:drill-category/id %)))
                             values)]
      [:div.py8.px4
       [:div.title-2.proxima.shout "YOYOYO" title]
       (when (or desktop? (nil? desktop?))
         [:div.hide-on-mb.hide-on-tb ; dt
          (->> grid-entries
               (partition-all 4)
               (mapv (fn [row] (component/html [:div.flex.justify-around row]))))])
       (when (or tablet? (nil? tablet?))
         [:div.hide-on-dt.hide-on-mb ; tb
          (->> grid-entries
               (partition 3 3 [[:div {:style {:width "120px"}}]])
               (mapv (fn [row] (component/html [:div.flex.justify-around row]))))])
       (when (or mobile? (nil? mobile?))
         [:div.hide-on-dt.hide-on-tb ; mb
          (->> grid-entries
               (partition-all 2)
               (mapv (fn [row] (component/html [:div.flex.justify-around row]))))])])))

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
      [:div "Call " [:a.inherit-color {:href (ui/phone-url config/support-phone-number)} config/support-phone-number " "]]
      [:div "Monday through Friday from 8am-5pm PST."]]]]])

(defcomponent ^:private template
  "This lays out different ux pieces to form a cohesive ux experience"
  [{:keys [category-filters
           category-hero
           content-box
           drill-category-grid
           drill-category-list
           footer
           header
           product-card-listing]} _ _]
  [:div
   (component/build category-hero/organism category-hero)
   (vertical-squiggle-atom "-36px")
   [:div.max-960.mx-auto
    (component/build drill-category-list-organism drill-category-list)
    (ui/width-aware drill-category-grid-organism drill-category-grid)]
   purple-divider-atom
   [:div.max-960.mx-auto
    [:div.pt4]
    (when-let [title (:title category-filters)]
      [:div.canela.title-1.center.mt3.py4 title])
    (component/build category-filters/organism category-filters {})
    (component/build product-card-listing/organism product-card-listing {})]
   (when content-box ^:inline green-divider-atom)
   (when content-box (component/build content-box-organism content-box))
   (component/build layered/shop-contact contact-query)])

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
  (let [current                  (accessors.categories/current-category app-state)
        subcategories            (category->subcategories (get-in app-state keypaths/categories) current)
        selections               (get-in app-state catalog.keypaths/category-selections)
        loaded-category-products (selector/match-all
                                  {:selector/strict? true}
                                  (merge
                                   (skuers/electives current)
                                   (skuers/essentials current))
                                  (vals (get-in app-state keypaths/v2-products)))
        category-products-matching-criteria
        (selector/match-all {:selector/strict? true}
                            (merge
                             (skuers/essentials current)
                             selections)
                            loaded-category-products)]
    (cond->
        {:header               {}
         :footer               {}
         :category-hero        (category-hero-query current)
         :content-box          (when (:content-block/type current)
                                 {:title    (:content-block/title current)
                                  :header   (:content-block/header current)
                                  :summary  (:content-block/summary current)
                                  :sections (:content-block/sections current)})
         :category-filters     (category-filters/query app-state
                                                       current
                                                       loaded-category-products
                                                       category-products-matching-criteria
                                                       selections)
         :product-card-listing (product-card-listing/query app-state
                                                           current
                                                           category-products-matching-criteria)}

      (= :grid (:subcategories/layout current))
      (merge {:drill-category-grid {:drill-category-grid/values (mapv category->drill-category-grid-entry subcategories)
                                    :drill-category-grid/title  (:subcategories/title current)}})

      (= :list (:subcategories/layout current))
      (merge (let [values (mapv category->drill-category-list-entry subcategories)]
               {:drill-category-list {:drill-category-list/values                   values
                                      :drill-category-list/use-three-column-layout? (>= (count values) 3)
                                      :drill-category-list/tablet-desktop-columns   (max 1 (min 3 (count values)))}})))))

(defn page
  [app-state opts]
  (component/build template (query app-state) opts))
