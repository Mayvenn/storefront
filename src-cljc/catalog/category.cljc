(ns catalog.category
  (:require
   #?@(:cljs [[storefront.accessors.auth :as auth]
              [storefront.api :as api]
              [storefront.browser.scroll :as scroll]
              [storefront.history :as history]
              [storefront.hooks.facebook-analytics :as facebook-analytics]
              [storefront.platform.messages :as messages]])
   [api.catalog :refer [select]]
   adventure.keypaths
   api.current
   catalog.facets
   [catalog.icp :as icp]
   catalog.keypaths
   [catalog.skuers :as skuers]
   [catalog.ui.category-hero :as category-hero]
   [catalog.ui.content-box :as content-box]
   [catalog.ui.product-card-listing :as product-card-listing]
   [homepage.ui.faq :as faq]
   [storefront.accessors.categories :as accessors.categories]
   [storefront.accessors.experiments :as experiments]
   [storefront.assets :as assets]
   [storefront.component :as c]
   [storefront.components.video :as video]
   [storefront.effects :as effects]
   [storefront.events :as e]
   [storefront.keypaths :as k]
   [storefront.platform.component-utils :as utils]
   [storefront.trackings :as trackings]
   [storefront.transitions :as transitions]
   [catalog.ui.facet-filters :as facet-filters]))

(def ^:private green-divider-atom
  (c/html
   [:div
    {:style {:background-image  "url('//ucarecdn.com/7e91271e-874c-4303-bc8a-00c8babb0d77/-/resize/x24/')"
             :background-position "center center"
             :background-repeat   "repeat-x"
             :height              "24px"}}]))

(c/defcomponent ^:private product-list
  [{:keys [product-card-listing]} _ _]
  (c/build product-card-listing/organism product-card-listing))

(c/defcomponent ^:private template
  [{:keys [category-hero
           content-box
           faq-section
           video] :as queried-data} _ _]
  [:div
   (c/build category-hero/organism category-hero)
   (when video
     (c/build video/component
                      video
                      ;; NOTE(jeff): we use an invalid video slug to preserve back behavior. There probably should be
                      ;;             an investigation to why history is replaced when doing A -> B -> A navigation
                      ;;             (B is removed from history).
                      {:opts
                       {:close-attrs
                        (utils/route-to e/navigate-category
                                        {:query-params        {:video "0"}
                                         :page/slug           "mayvenn-install"
                                         :catalog/category-id 23})}}))

   [:div.py5
    (c/build facet-filters/organism queried-data {:opts {:child-component product-list}})]
   (when content-box
     [:div green-divider-atom
      (c/build content-box/organism content-box)])
   (when faq-section
     (c/build faq/organism faq-section))])

(defn category-hero-query
  [{:copy/keys     [title learn-more-target banner-img-dsk-id banner-img-mob-id banner-alt]
    :category/keys [description new?]
    icon-uri       :icon}]
  (cond-> {:category-hero.title/primary title
           :category-hero.body/primary  description}

    ;; TODO(corey) this key is in #:copy
    (seq learn-more-target)
    (merge {:category-hero.action/label  "Learn more"
            :category-hero.action/aria   "Learn more about complimentary Mayvenn Services."
            :category-hero.action/target learn-more-target})

    (seq banner-img-dsk-id)
    (merge {:category-hero.banner/img-dsk-id banner-img-dsk-id
            :category-hero.banner/img-mob-id banner-img-mob-id
            :category-hero.banner/img-alt    banner-alt})

    ;; TODO(corey) image handling reconciliation: svg as uri
    (seq icon-uri)
    (merge {:category-hero.icon/image-src (assets/path icon-uri)})

    new?
    (merge {:category-hero.tag/primary "New"})))

(defn page
  [app-state _]
  (let [category                            (accessors.categories/current-category app-state) 
        experiment-color-shorthand?         (experiments/color-shorthand? app-state)
        facet-filtering-state               (merge (get-in app-state catalog.keypaths/k-models-facet-filtering)
                                                   {:facet-filtering/item-label "item"})
        facet-filtering-longhand-state      (update facet-filtering-state
                                                    :facet-filtering/filters
                                                    catalog.facets/expand-shorthand-colors)
        loaded-category-products            (->> (get-in app-state k/v2-products)
                                                 vals
                                                 (select (merge
                                                          (skuers/electives< category)
                                                          (skuers/essentials< category))))
        selections                          (:facet-filtering/filters (if experiment-color-shorthand?
                                                                        facet-filtering-longhand-state
                                                                        facet-filtering-state))
        category-skus                       (->> (get-in app-state k/v2-skus)
                                                 vals
                                                 (select (merge
                                                          (skuers/electives< category)
                                                          (skuers/essentials< category))))
        category-skus-matching-criteria     (select selections category-skus)
        category-products-matching-criteria (select selections loaded-category-products) 
        dimensions                          (:selector/dimensions category)
        category-skuers                     (skuers/skus->skuers dimensions
                                                                 category-skus
                                                                 loaded-category-products)
        card-skuers                         (skuers/skus->skuers dimensions
                                                                 category-skus-matching-criteria
                                                                 category-products-matching-criteria)

        shop?                               (or (= "shop" (get-in app-state k/store-slug))
                                                (= "retail-location" (get-in app-state k/store-experience)))
        faq                                 (get-in app-state (conj storefront.keypaths/cms-faq (:contentful/faq-id category)))
        splay?                              (-> dimensions seq boolean)]
    (c/build template
             (merge
              (when-let [filter-title (:product-list/title category)]
                {:title filter-title})
              {:category-hero (category-hero-query category)
               :video         (when-let [video (get-in app-state adventure.keypaths/adventure-home-video)] video)
               :content-box   (when (and shop? (:content-block/type category))
                                (let [{:content-block/keys [title header summary sections]} category]
                                  {:title    title
                                   :header   header
                                   :summary  summary
                                   :sections sections}))
               :faq-section   (when (and shop? faq)
                                (let [{:keys [question-answers]} faq]
                                  {:faq/expanded-index (get-in app-state storefront.keypaths/faq-expanded-section)
                                   :list/sections      (for [{:keys [question answer]} question-answers]
                                                         {:faq/title   (:text question)
                                                          :faq/content answer})}))}
              (facet-filters/filters<-
               {:facets-db                      (get-in app-state storefront.keypaths/v2-facets)
                :faceted-models                 (if splay? category-skuers loaded-category-products)
                :facet-filtering-state          facet-filtering-state
                :experiment-color-shorthand?    experiment-color-shorthand?
                :facet-filtering-longhand-state (if experiment-color-shorthand? 
                                                  facet-filtering-longhand-state
                                                  facet-filtering-state)
                :facets-to-filter-on            (:selector/electives category)
                :navigation-event               e/navigate-category
                :navigation-args                (select-keys category [:catalog/category-id :page/slug])
                :child-component-data           {:product-card-listing (product-card-listing/query app-state
                                                                                                   category
                                                                                                   (if splay? card-skuers category-products-matching-criteria))}})))))

(defn ^:export built-component
  [app-state opts]
  (let [{:page/keys [icp?]} (accessors.categories/current-category app-state)]
    ((if icp? icp/page page) app-state opts)))

(def ^:private adventure-slug->video
  {"we-are-mayvenn" {:youtube-id "hWJjyy5POTE"}
   "free-install"   {:youtube-id "oR1keQ-31yc"}})

(defmethod transitions/transition-state e/navigate-category
  [_ event {:keys [catalog/category-id query-params]} app-state]
  (let [[_ {prev-category-id :catalog/category-id}] (-> (get-in app-state k/navigation-undo-stack)
                                                        first
                                                        :navigation-message)]
    (cond-> app-state
      :always
      (->
       (assoc-in catalog.keypaths/category-id category-id)

       (assoc-in catalog.keypaths/category-selections
                 (accessors.categories/query-params->selector-electives query-params))

       (assoc-in catalog.keypaths/k-models-facet-filtering-filters
                 (accessors.categories/query-params->selector-electives query-params))

       (assoc-in adventure.keypaths/adventure-home-video
                 (adventure-slug->video (:video query-params))))

      (not= prev-category-id category-id)
      (assoc-in catalog.keypaths/category-panel nil))))

(defmethod effects/perform-effects e/navigate-category
  [_ event {:keys [catalog/category-id slug query-params]} previous-app-state app-state]
  #?(:cljs
     (let [category   (accessors.categories/current-category app-state)
           success-fn #(messages/handle-message e/api-success-v3-products-for-browse
                                                (assoc % :category-id category-id))]
       ;; Some pages may disable scrolling on the body, e.g.: product detail page
       ;; and it must be re-enabled for this page
       (scroll/enable-body-scrolling)
       (when-let [contentful-faq-id (:contentful/faq-id category)]
         (effects/fetch-cms-keypath app-state [:faq contentful-faq-id]))
       (when (-> category :experience/exclude (contains? (get-in app-state k/store-experience)))
         (effects/redirect e/navigate-home))
       (if (auth/permitted-category? app-state category)
         (api/get-products (get-in app-state k/api-cache)
                           (skuers/essentials< category)
                           success-fn)
         (effects/redirect e/navigate-home))
       (when-let [subsection-key (:subsection query-params)]
         (js/setTimeout (partial scroll/scroll-selector-to-top (str "#subsection-" subsection-key)) 0))
       (let [just-arrived? (not= e/navigate-category
                                 (get-in previous-app-state k/navigation-event))]
         (when just-arrived?
           (messages/handle-message e/flow|facet-filtering|initialized))))))

(defmethod trackings/perform-track e/navigate-category
  [_ event {:keys [catalog/category-id]} app-state]
  #?(:cljs
     (when (-> category-id
               (accessors.categories/id->category (get-in app-state k/categories))
               accessors.categories/wig-category?)
       (facebook-analytics/track-event "wig_content_fired"))))
