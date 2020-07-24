(ns catalog.category
  (:require
   #?@(:cljs [[storefront.api :as api]
              [storefront.accessors.auth :as auth]
              [storefront.platform.messages :as messages]
              [storefront.browser.scroll :as scroll]
              [storefront.hooks.facebook-analytics :as facebook-analytics]])
   adventure.keypaths
   [catalog.icp :as icp]
   [catalog.skuers :as skuers]
   catalog.keypaths
   [catalog.ui.category-filters :as category-filters]
   [catalog.ui.category-hero :as category-hero]
   [catalog.ui.molecules :as molecules]
   [catalog.ui.how-it-works :as how-it-works]
   [catalog.ui.product-card-listing :as product-card-listing]
   [catalog.ui.service-card-listing :as service-card-listing]
   [storefront.accessors.categories :as accessors.categories]
   [storefront.assets :as assets]
   [storefront.component :as c]
   [storefront.effects :as effects]
   [storefront.events :as e]
   [storefront.keypaths :as k]
   [storefront.trackings :as trackings]
   [storefront.transitions :as transitions]
   [spice.selector :as selector]
   spice.core
   [storefront.events :as events]))

(c/defcomponent ^:private template
  [{:keys [category-hero
           how-it-works
           category-filters
           card-listing
           service-category-page?] :as queried-data} _ _]
  [:div
   (c/build category-hero/organism category-hero)
   (c/build molecules/stylist-bar queried-data {})
   [:div.max-960.mx-auto
    [:div.pt4]
    (when-let [title (:title category-filters)]
      [:div.canela.title-1.center.mt3.py4 title])
    (c/build category-filters/organism category-filters {})
    (if service-category-page?
      (c/build service-card-listing/organism card-listing {})
      (c/build product-card-listing/organism card-listing {}))]
   [:div.col-10.mx-auto.mt6
    (c/build how-it-works/organism how-it-works)]])

(defn category-hero-query
  [{:copy/keys [title description learn-more-target]
    icon-uri   :icon
    new?       :category/new?}]
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
  [app-state opts]
  (let [current                             (accessors.categories/current-category app-state)
        selections                          (get-in app-state catalog.keypaths/category-selections)
        loaded-category-products            (selector/match-all
                                             {:selector/strict? true}
                                             (merge
                                              (skuers/electives current)
                                              (skuers/essentials current))
                                             (vals (get-in app-state k/v2-products)))
        category-products-matching-criteria (selector/match-all {:selector/strict? true}
                                                                (merge
                                                                 (skuers/essentials current)
                                                                 selections)
                                                                loaded-category-products)
        service-category-page?              (contains? (:catalog/department current) "service")
        card-listing-query                  (if service-category-page?
                                              service-card-listing/query
                                              product-card-listing/query)
        servicing-stylist                   (get-in app-state adventure.keypaths/adventure-servicing-stylist)]
    (c/build template
             (merge {:category-hero          (category-hero-query current)
                     :category-filters       (category-filters/query app-state
                                                                     current
                                                                     loaded-category-products
                                                                     category-products-matching-criteria
                                                                     selections)
                     :how-it-works           current
                     :card-listing           (card-listing-query app-state current category-products-matching-criteria)
                     :service-category-page? service-category-page?}
                    (when (and service-category-page? servicing-stylist)
                      {:stylist-bar/id             "category-page-stylist-bar"
                       :stylist-bar/primary        (:store-nickname servicing-stylist)
                       :stylist-bar/secondary      "Your Certified Mayvenn Stylist"
                       :stylist-bar/rating         {:rating/id    "rating-stuff"
                                                    :rating/value (spice.core/parse-double (:rating servicing-stylist))}
                       :stylist-bar.thumbnail/id   "stylist-bar-thumbnail"
                       :stylist-bar.thumbnail/url  (-> servicing-stylist :portrait :resizable-url)
                       :stylist-bar.action/primary "change"
                       :stylist-bar.action/target  [events/navigate-adventure-find-your-stylist {}]}))
             opts)))

(defn ^:export built-component
  [app-state opts]
  (let [{:page/keys [icp?]} (accessors.categories/current-category app-state)]
    ((if icp? icp/page page) app-state opts)))

(defmethod transitions/transition-state e/navigate-category
  [_ event {:keys [catalog/category-id query-params]} app-state]
  (let [[_ {prev-category-id :catalog/category-id}] (-> (get-in app-state k/navigation-undo-stack)
                                                        first
                                                        :navigation-message)]
    (cond-> app-state
      true
      (assoc-in catalog.keypaths/category-id category-id)

      true
      (assoc-in catalog.keypaths/category-selections
                (accessors.categories/query-params->selector-electives query-params))

      (not= prev-category-id category-id)
      (assoc-in catalog.keypaths/category-panel nil))))

(defmethod effects/perform-effects e/navigate-category
  [_ event {:keys [catalog/category-id slug query-params]} _ app-state]
  #?(:cljs
     (let [category   (accessors.categories/current-category app-state)
           success-fn #(messages/handle-message e/api-success-v3-products-for-browse
                                                (assoc % :category-id category-id))]
       ;; Some pages may disable scrolling on the body, e.g.: product detail page
       ;; and it must be re-enabled for this page
       (scroll/enable-body-scrolling)
       (let [store-experience (get-in app-state k/store-experience)]
         (when (and (= "mayvenn-classic"
                       store-experience)
                    (contains? (:experience/exclude category)
                               "mayvenn-classic"))
           (effects/redirect e/navigate-home)))
       (if (auth/permitted-category? app-state category)
         (api/get-products (get-in app-state k/api-cache)
                           (skuers/essentials category)
                           success-fn)
         (effects/redirect e/navigate-home))
       (when-let [subsection-key (:subsection query-params)]
         (js/setTimeout (partial scroll/scroll-selector-to-top (str "#subsection-" subsection-key)) 0)))))

(defmethod trackings/perform-track e/navigate-category
  [_ event {:keys [catalog/category-id]} app-state]
  #?(:cljs
     (when (-> category-id
               (accessors.categories/id->category (get-in app-state k/categories))
               accessors.categories/wig-category?)
       (facebook-analytics/track-event "wig_content_fired"))))
