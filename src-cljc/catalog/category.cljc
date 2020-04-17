(ns catalog.category
  (:require
   #?@(:cljs [[catalog.skuers :as skuers]
              [storefront.accessors.auth :as auth]
              [storefront.api :as api]
              [storefront.browser.scroll :as scroll]
              [storefront.effects :as effects]
              [storefront.hooks.facebook-analytics :as facebook-analytics]
              [storefront.platform.messages :as messages]
              [storefront.trackings :as trackings]])
   [catalog.icp :as icp]
   [catalog.keypaths]
   [catalog.ui.product-list :as product-list]
   [storefront.accessors.categories :as accessors.categories]
   [storefront.assets :as assets]
   [storefront.component :as component :refer [defcomponent]]
   [storefront.components.ui :as ui]
   [storefront.events :as events]
   [storefront.keypaths :as keypaths]
   [storefront.platform.component-utils :as utils]
   [storefront.transitions :as transitions]
   clojure.set))

(defn ^:private category-header
  [category]
  (component/html
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
                                         "Learn more")])]]))

(defcomponent ^:private component
  [{:keys [category product-list-data]} owner opts]
  [:div
   (category-header category)
   [:div.max-960.mx-auto
    (component/build product-list/organism product-list-data)]])

(defn ^:private query
  [data]
  (let [category (accessors.categories/current-category data)]
    {:category          category
     :product-list-data (product-list/query
                         data
                         category
                         (vals (get-in data keypaths/v2-products))
                         (get-in data catalog.keypaths/category-selections))}))

(defn ^:export built-component
  [data opts]
  (let [current-category (accessors.categories/current-category data)]
    (if (:page/icp? current-category)
      (icp/page data opts)
      (component/build component (query data) opts))))

(defmethod transitions/transition-state events/navigate-category
  [_ event {:keys [catalog/category-id query-params]} app-state]
  (let [[_ {prev-category-id :catalog/category-id}] (-> (get-in app-state keypaths/navigation-undo-stack)
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

#?(:cljs
   (defmethod effects/perform-effects events/navigate-category
     [_ event {:keys [catalog/category-id slug query-params]} _ app-state]
     (let [category   (accessors.categories/current-category app-state)
           success-fn #(messages/handle-message events/api-success-v2-products-for-browse
                                                (assoc % :category-id category-id))]
       ;; Some pages may disable scrolling on the body, e.g.: product detail page
       ;; and it must be re-enabled for this page
       (scroll/enable-body-scrolling)
       (let [store-experience (get-in app-state keypaths/store-experience)]
         (when (and (= "mayvenn-classic"
                       store-experience)
                    (contains? (:experience/exclude category)
                               "mayvenn-classic"))
           (effects/redirect events/navigate-home)))
       (if (auth/permitted-category? app-state category)
         (api/get-products (get-in app-state keypaths/api-cache)
                           (skuers/essentials category)
                           success-fn)
         (effects/redirect events/navigate-home))
       (when-let [subsection-key (:subsection query-params)]
         (js/setTimeout (partial scroll/scroll-selector-to-top (str "#subsection-" subsection-key)) 0)))))

#?(:cljs
   (defmethod trackings/perform-track events/navigate-category
     [_ event {:keys [catalog/category-id]} app-state]
     (when (-> category-id
               (accessors.categories/id->category (get-in app-state keypaths/categories))
               accessors.categories/wig-category?)
       (facebook-analytics/track-event "wig_content_fired"))))

