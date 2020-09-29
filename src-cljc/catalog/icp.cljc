(ns catalog.icp
  (:require [adventure.components.layered :as layered]
            adventure.keypaths
            catalog.keypaths
            [catalog.skuers :as skuers]
            [catalog.ui.category-filters :as category-filters]
            [catalog.ui.category-hero :as category-hero]
            [catalog.ui.molecules :as molecules]
            [catalog.ui.product-card-listing :as product-card-listing]
            [catalog.ui.service-card-listing :as service-card-listing]
            [catalog.ui.how-it-works :as how-it-works]
            [homepage.ui.faq :as faq]
            spice.core
            [spice.maps :as maps]
            [spice.selector :as selector]
            [storefront.accessors.categories :as accessors.categories]
            [storefront.accessors.experiments :as experiments]
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

(defn drill-category-image [image-id]
  [:picture
   [:source {:src-set (str "//ucarecdn.com/" image-id "/-/format/auto/-/quality/lightest/-/resize/124x/" " 2x,"
                           "//ucarecdn.com/" image-id "/-/format/auto/-/quality/normal/-/resize/62x/" " 1x")}]
   [:img ^:attrs {:src (str "//ucarecdn.com/" image-id "/-/format/auto/-/scale_crop/62x62/center/")}]])

(defcomponent ^:private drill-category-list-entry-organism
  [{:drill-category/keys [id title description image-id target action-id action-label use-three-column-layout?]} _ _]
  (when id
    (if use-three-column-layout?
      [:div.p3.flex.flex-wrap.col-12.content-start
       {:key       id
        :data-test id
        :class     "col-4-on-tb-dt"}
       [:div.col-12-on-tb-dt.col-3
        [:div.hide-on-tb-dt.flex.justify-end.mr4.mt1
         (when image-id
           (drill-category-image image-id))]
        [:div.hide-on-mb
         (when image-id
           (drill-category-image image-id))]]
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
         (when image-id
           (drill-category-image image-id))]]
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
    (let [grid-entries (mapv #(component/build drill-category-grid-entry-organism
                                               %
                                               (component/component-id (:drill-category/id %)))
                             values)]
      [:div.py8.px4
       [:div.title-2.proxima.shout title]
       [:div.hide-on-mb.hide-on-tb ; dt
        (->> grid-entries
             (partition-all 4)
             (map-indexed (fn [i row] (component/html [:div.flex.justify-around {:key (str "i-" i)} row]))))]
       [:div.hide-on-dt.hide-on-mb ; tb
        (->> grid-entries
             (partition 3 3 [[:div {:key "placeholder" :style {:width "120px"}}]])
             (map-indexed (fn [i row] (component/html [:div.flex.justify-around {:key (str "i-" i)} row]))))]
       [:div.hide-on-dt.hide-on-tb ; mb
        (->> grid-entries
             (partition-all 2)
             (map-indexed (fn [i row] (component/html [:div.flex.justify-around {:key (str "i-" i)} row]))))]])))

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

(defcomponent content-box-organism
  [{:keys [title header summary sections]} _ _]
  [:div.py8.px4.bg-cool-gray
   [:div.max-960.mx-auto
    [:div.pb2
     [:div.proxima.title-2.bold.caps ^:inline (str title)]
     [:div.canela.title-1.pb2 ^:inline (str header)]
     [:div.canela.content-1 ^:inline (str summary)]]

    (for [{:keys [title body]} sections]
      [:div.py2 {:key title}
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
           expanding-content-box
           drill-category-grid
           drill-category-list
           product-card-listing
           service-card-listing] :as queried-data} _ _]
  [:div
   (component/build category-hero/organism category-hero)
   (vertical-squiggle-atom "-36px")
   [:div.max-960.mx-auto
    (component/build drill-category-list-organism drill-category-list)
    (component/build drill-category-grid-organism drill-category-grid)]
   [:div.mb10 purple-divider-atom
    (component/build molecules/stylist-bar queried-data {})]
   [:div.max-960.mx-auto
    (when-let [title (:title category-filters)]
      [:div.canela.title-1.center.mb2 title])
    (if (-> category-filters :tabs :tabs/elements empty?)
      [:div.stroke-s-color.center
       (svg/straight-line {:width  "1px"
                           :height "42px"})]
      (component/build category-filters/organism category-filters {}))
    (component/build service-card-listing/organism service-card-listing {})
    (component/build product-card-listing/organism product-card-listing {})]
   (when (:how-it-works queried-data)
     [:div.col-10.mx-auto.mt6
      (component/build how-it-works/organism queried-data)])
   (when content-box
     [:div green-divider-atom
      (component/build content-box-organism content-box)])
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

(defn query
  [app-state]
  (let [interstitial-category               (accessors.categories/current-category app-state)
        subcategories                       (category->subcategories (get-in app-state keypaths/categories) interstitial-category)
        selections                          (get-in app-state catalog.keypaths/category-selections)
        loaded-category-products            (selector/match-all
                                             {:selector/strict? true}
                                             (merge
                                              (skuers/electives interstitial-category)
                                              (skuers/essentials interstitial-category))
                                             (vals (get-in app-state keypaths/v2-products)))
        category-products-matching-criteria (selector/match-all {:selector/strict? true}
                                                                (merge
                                                                 (skuers/essentials interstitial-category)
                                                                 selections)
                                                                loaded-category-products)
        shop?                               (= "shop" (get-in app-state keypaths/store-slug))
        stylist-mismatch?                   (experiments/stylist-mismatch? app-state)
        servicing-stylist                   (get-in app-state adventure.keypaths/adventure-servicing-stylist)
        service-category-page?              (contains? (:catalog/department interstitial-category) "service")
        faq                                 (get-in app-state (conj keypaths/cms-faq (:contentful/faq-id interstitial-category)))]
    (cond->
        {:category-hero          (category-hero-query interstitial-category)
         :content-box            (when (and shop? (:content-block/type interstitial-category))
                                   {:title    (:content-block/title interstitial-category)
                                    :header   (:content-block/header interstitial-category)
                                    :summary  (:content-block/summary interstitial-category)
                                    :sections (:content-block/sections interstitial-category)})
         :expanding-content-box  (when (and shop? faq)
                                   (let [{:keys [question-answers]} faq]
                                     {:faq/expanded-index (get-in app-state keypaths/faq-expanded-section)
                                      :list/sections (for [{:keys [question answer]} question-answers]
                                                       {:faq/title (:text question)
                                                        :faq/content answer})}))
         :category-filters       (category-filters/query app-state
                                                         interstitial-category
                                                         loaded-category-products
                                                         category-products-matching-criteria
                                                         selections)
         :how-it-works           (when (:how-it-works/title-primary interstitial-category) interstitial-category)
         :product-card-listing   (when-not service-category-page?
                                   (product-card-listing/query app-state
                                                               interstitial-category
                                                               category-products-matching-criteria))
         :service-card-listing   (when service-category-page?
                                   (service-card-listing/query app-state
                                                               interstitial-category
                                                               category-products-matching-criteria))}

      (= :grid (:subcategories/layout interstitial-category))
      (merge {:drill-category-grid {:drill-category-grid/values (mapv category->drill-category-grid-entry subcategories)
                                    :drill-category-grid/title  (:subcategories/title interstitial-category)}})

      (= :list (:subcategories/layout interstitial-category))
      (merge (let [values (mapv category->drill-category-list-entry subcategories)]
               {:drill-category-list {:drill-category-list/values                   values
                                      :drill-category-list/use-three-column-layout? (>= (count values) 3)
                                      :drill-category-list/tablet-desktop-columns   (max 1 (min 3 (count values)))}}))

      (and service-category-page? servicing-stylist stylist-mismatch?)
      (merge {:stylist-bar/id             "category-page-stylist-bar"
              :stylist-bar/primary        (:store-nickname servicing-stylist)
              :stylist-bar/secondary      "Your Certified Mayvenn Stylist"
              :stylist-bar/rating         {:rating/id    "rating-stuff"
                                           :rating/value (spice.core/parse-double (:rating servicing-stylist))}
              :stylist-bar.thumbnail/id   "stylist-bar-thumbnail"
              :stylist-bar.thumbnail/url  (-> servicing-stylist :portrait :resizable-url)
              :stylist-bar.action/primary "change"
              :stylist-bar.action/target  [events/navigate-adventure-find-your-stylist {}]}))))

(defn page
  [app-state opts]
  (component/build template (query app-state) opts))
