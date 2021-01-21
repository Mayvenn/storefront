(ns catalog.category
  (:require
   #?@(:cljs [[storefront.accessors.auth :as auth]
              [storefront.api :as api]
              [storefront.browser.scroll :as scroll]
              [storefront.history :as history]
              [storefront.hooks.facebook-analytics :as facebook-analytics]
              [storefront.platform.messages :as messages]])
   adventure.keypaths
   api.current
   [catalog.icp :as icp]
   catalog.keypaths
   clojure.set
   [catalog.skuers :as skuers]
   [catalog.ui.category-hero :as category-hero]
   [catalog.ui.content-box :as content-box]
   [catalog.ui.how-it-works :as how-it-works]
   [catalog.ui.product-card-listing :as product-card-listing]
   [catalog.ui.service-card-listing :as service-card-listing]
   [homepage.ui.faq :as faq]
   spice.core
   [spice.selector :as selector]
   [storefront.accessors.categories :as accessors.categories]
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
  [{:keys [service-card-listing
           product-card-listing]} _ _]
  [:div.plj3-on-dt
   (c/build service-card-listing/organism service-card-listing)
   (c/build product-card-listing/organism product-card-listing)])

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
   (when (:how-it-works queried-data)
     [:div.col-10.col-7-on-tb-dt.mx-auto.mt6
      (c/build how-it-works/organism queried-data)])
   (when faq-section
     (c/build faq/organism faq-section))])

(defn category-hero-query
  [{:copy/keys     [title learn-more-target]
    :category/keys [description new?]
    icon-uri       :icon}]
  (cond-> {:category-hero.title/primary title
           :category-hero.body/primary  description}

    ;; TODO(corey) this key is in #:copy
    (seq learn-more-target)
    (merge {:category-hero.action/label  "Learn more"
            :category-hero.action/target learn-more-target})

    ;; TODO(corey) image handling reconciliation: svg as uri
    (seq icon-uri)
    (merge {:category-hero.icon/image-src (assets/path icon-uri)})

    new?
    (merge {:category-hero.tag/primary "New"})))

(defn page
  [app-state _]
  (let [current                             (accessors.categories/current-category app-state)
        facet-filtering-state               (merge (get-in app-state catalog.keypaths/k-models-facet-filtering)
                                                   {:facet-filtering/item-label "item"})
        loaded-category-products            (selector/match-all
                                             {:selector/strict? true}
                                             (merge
                                              (skuers/electives current)
                                              (skuers/essentials current))
                                             (vals (get-in app-state k/v2-products)))
        shop?                               (= "shop" (get-in app-state k/store-slug))
        category-products-matching-criteria (selector/match-all {:selector/strict? true}
                                                                (merge
                                                                 (skuers/essentials current)
                                                                 (:facet-filtering/filters facet-filtering-state))
                                                                loaded-category-products)
        service-category-page?              (contains? (:catalog/department current) "service")
        faq                                 (get-in app-state (conj storefront.keypaths/cms-faq (:contentful/faq-id current)))]
    (c/build template
             (merge
              (when-let [filter-title (:product-list/title current)]
                {:title filter-title})
              {:category-hero          (category-hero-query current)
               :video                  (when-let [video (get-in app-state adventure.keypaths/adventure-home-video)] video)
               :how-it-works           (when (:how-it-works/title-primary current) current)
               :content-box            (when (and shop? (:content-block/type current))
                                         {:title    (:content-block/title current)
                                          :header   (:content-block/header current)
                                          :summary  (:content-block/summary current)
                                          :sections (:content-block/sections current)})
               :service-card-listing   (when service-category-page?
                                         (service-card-listing/query app-state current category-products-matching-criteria))
               :product-card-listing   (when-not service-category-page?
                                         (product-card-listing/query app-state current category-products-matching-criteria))
               :service-category-page? service-category-page?
               :faq-section            (when (and shop? faq)
                                         (let [{:keys [question-answers]} faq]
                                           {:faq/expanded-index (get-in app-state storefront.keypaths/faq-expanded-section)
                                            :list/sections      (for [{:keys [question answer]} question-answers]
                                                                  {:faq/title   (:text question)
                                                                   :faq/content answer})}))}
              (facet-filters/filters<-
               {:facets-db             (get-in app-state storefront.keypaths/v2-facets)
                :faceted-models        loaded-category-products
                :facet-filtering-state facet-filtering-state
                :facets-to-filter-on   (:selector/electives current)
                :navigation-event      e/navigate-category
                :navigation-args       (select-keys current [:catalog/category-id :page/slug])
                :child-component-data  (if service-category-page?
                                         {:service-card-listing
                                          (service-card-listing/query app-state
                                                                      current
                                                                      category-products-matching-criteria)}
                                         {:product-card-listing
                                          (product-card-listing/query app-state
                                                                      current
                                                                      category-products-matching-criteria)})})))))

(defn ^:export built-component
  [app-state opts]
  (let [{:page/keys [icp?]} (accessors.categories/current-category app-state)]
    ((if icp? icp/page page) app-state opts)))

(def ^:private adventure-slug->video
  {"we-are-mayvenn" {:youtube-id "hWJjyy5POTE"}
   "free-install"   {:youtube-id "oR1keQ-31yc"}})

(defmethod transitions/transition-state e/navigate-category
  [_ event {:keys [catalog/category-id query-params]} app-state]
  (-> app-state
      (assoc-in catalog.keypaths/category-id category-id)

      (assoc-in catalog.keypaths/k-models-facet-filtering-filters
                (accessors.categories/query-params->selector-electives query-params))

      (assoc-in adventure.keypaths/adventure-home-video
                (adventure-slug->video (:video query-params)))))

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
       (let [store-experience (get-in app-state k/store-experience)]
         (when (and (= "mayvenn-classic"
                       store-experience)
                    (contains? (:experience/exclude category) "mayvenn-classic"))
           (effects/redirect e/navigate-home)))
       (if (auth/permitted-category? app-state category)
         (api/get-products (get-in app-state k/api-cache)
                           (skuers/essentials category)
                           success-fn)
         (effects/redirect e/navigate-home))
       (when-let [subsection-key (:subsection query-params)]
         (js/setTimeout (partial scroll/scroll-selector-to-top (str "#subsection-" subsection-key)) 0))
       (let [previous-category-id (:catalog/category-id (get-in previous-app-state k/navigation-args))
             just-arrived?        (or (not= e/navigate-category
                                            (get-in previous-app-state k/navigation-event))
                                      (not= category-id previous-category-id))]
         (when just-arrived?
           (messages/handle-message e/flow|facet-filtering|initialized))))))

(defmethod trackings/perform-track e/navigate-category
  [_ event {:keys [catalog/category-id]} app-state]
  #?(:cljs
     (when (-> category-id
               (accessors.categories/id->category (get-in app-state k/categories))
               accessors.categories/wig-category?)
       (facebook-analytics/track-event "wig_content_fired"))))
