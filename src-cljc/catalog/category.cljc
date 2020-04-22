(ns catalog.category
  (:require
   #?@(:cljs [[storefront.api :as api]
              [storefront.accessors.auth :as auth]
              [storefront.platform.messages :as messages]
              [catalog.skuers :as skuers]
              [storefront.browser.scroll :as scroll]
              [storefront.hooks.facebook-analytics :as facebook-analytics]])
   [catalog.icp :as icp]
   catalog.keypaths
   [catalog.ui.category-filters :as category-filters]
   [catalog.ui.category-hero :as category-hero]
   [catalog.ui.how-it-works :as how-it-works]
   [catalog.ui.product-card-listing :as product-card-listing]
   [storefront.accessors.categories :as accessors.categories]
   [storefront.assets :as assets]
   [storefront.component :as c]
   [storefront.effects :as effects]
   [storefront.events :as e]
   [storefront.keypaths :as k]
   [storefront.trackings :as trackings]
   [storefront.transitions :as transitions]))

(c/defcomponent ^:private template
  [{:keys [category-hero
           how-it-works
           category-filters
           product-card-listing]} _ _]
  [:div
   (c/build category-hero/organism category-hero)
   [:div.max-960.mx-auto
    (c/build category-filters/organism category-filters {})
    (c/build product-card-listing/organism product-card-listing {})]
   [:div.col-10.mx-auto
    (c/build how-it-works/organism how-it-works)]])

(defn how-it-works-query
  [{:keys [catalog/category-id]}]
  (when (= "30" category-id)
    {:how-it-works.title/primary   "Your hair deserves a Mayvenn–Certified Stylist."
     :how-it-works.title/secondary "Here’s how it works."
     :how-it-works.step/elements
     [{:how-it-works.step.title/primary   "01"
       :how-it-works.step.title/secondary "Pick your service"
       :how-it-works.step.body/primary    (str "Lorem ipsum dolor sit amet, consectetur adipiscing elit. "
                                               "Ut sollicitudin massa sit amet efficitur sagittis.")}
      {:how-it-works.step.title/primary   "02"
       :how-it-works.step.title/secondary "Select a Mayvenn-Certified stylist"
       :how-it-works.step.body/primary    (str "We've hand-picked thousands of talented stylists around the country. "
                                               "We'll cover the cost of your salon appointment or wig customization with any qualifying purchase.") }
      {:how-it-works.step.title/primary  "03"
       :how-it-works.step.title/secondary "Schedule your appointment"
       :how-it-works.step.body/primary    (str " We’ll connect you with your stylist to set up your service. "
                                               "Then, we’ll send you a prepaid voucher to cover the cost. ")}]}))

(defn category-hero-query
  [{:copy/keys [title description learn-more]
    icon-uri   :icon
    new?       :category/new?}]
  (cond-> {:category-hero.title/primary title
           :category-hero.body/primary  description}

    ;; TODO(corey) this key is in #:copy
    (seq learn-more)
    (merge {:category-hero.action/label  "Learn more"
            :category-hero.action/target [learn-more]})

    ;; TODO(corey) image handling reconciliation: svg as uri
    (seq icon-uri)
    (merge {:category-hero.icon/image-src (assets/path icon-uri)})

    new?
    (merge {:category-hero.tag/primary "New"})))

(defn page
  [app-state opts]
  (let [current         (accessors.categories/current-category app-state)
        loaded-products (vals (get-in app-state k/v2-products))
        selections      (get-in app-state catalog.keypaths/category-selections) ]
    (c/build template
             {:category-hero        (category-hero-query current)
              :category-filters     (category-filters/query app-state
                                                            current loaded-products selections)
              :how-it-works         (how-it-works-query current)
              :product-card-listing (product-card-listing/query app-state
                                                                current loaded-products selections)}
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
           success-fn #(messages/handle-message e/api-success-v2-products-for-browse
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

