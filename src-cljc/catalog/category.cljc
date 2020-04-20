(ns catalog.category
  (:require
   #?@(:cljs [[storefront.api :as api]
              [storefront.browser.scroll :as scroll]
              [storefront.hooks.facebook-analytics :as facebook-analytics]])
   [catalog.icp :as icp]
   catalog.keypaths
   [catalog.skuers :as skuers]
   [catalog.ui.product-list :as product-list]
   [storefront.accessors.auth :as auth]
   [storefront.accessors.categories :as accessors.categories]
   [storefront.assets :as assets]
   [storefront.component :as c]
   [storefront.components.ui :as ui]
   [storefront.effects :as effects]
   [storefront.events :as e]
   [storefront.keypaths :as k]
   [storefront.platform.component-utils :as utils]
   [storefront.platform.messages :as messages]
   [storefront.trackings :as trackings]
   [storefront.transitions :as transitions]))

(c/defcomponent ^:private category-header-organism
  [category _ _]
  [:div.center.px2.py10.bg-warm-gray.max-960.mx-auto
   (when (:category/new? category)
     [:div.s-color.title-3.proxima.bold.shout "New"])
   [:div.h1.title-1.canela (:copy/title category)]
   (when-let [icon-url (:icon category)]
     [:div.mt4 [:img {:src   (assets/path icon-url)
                      :style {:width "54px"}}]])
   [:div.my3.mx6-on-mb.col-8-on-tb-dt.mx-auto-on-tb-dt
    (:copy/description category)
    (when-let [learn-more-event (:copy/learn-more category)]
      [:div.mt3
       (ui/button-small-underline-black {:on-click (apply utils/send-event-callback learn-more-event)}
                                        "Learn more")])]])

(c/defcomponent ^:private template
  [{:keys [category-header
           product-list]} _ _]
  [:div
   (c/build category-header-organism category-header)
   [:div.max-960.mx-auto
    (c/build product-list/organism product-list)]])

(defn page
  [app-state opts]
  (let [category        (accessors.categories/current-category app-state)
        loaded-products (vals (get-in app-state k/v2-products))
        selections      (get-in app-state catalog.keypaths/category-selections) ]
    (c/build template
             {:category-header category
              :product-list    (product-list/query app-state
                                                   category loaded-products selections)}
             opts)))

(defn ^:export built-component
  [app-state opts]
  (let [{:icp/keys [icp?]} (accessors.categories/current-category app-state)]
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

