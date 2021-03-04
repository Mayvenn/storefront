(ns ui.promo-banner
  (:require #?@(:cljs [[goog.dom]
                       [goog.events]
                       [goog.events.EventType :as EventType]
                       [goog.style]
                       ["react" :as react]])
            catalog.keypaths
            [storefront.accessors.orders :as orders]
            [storefront.accessors.promos :as promos]
            [storefront.accessors.sites :as sites]
            [storefront.component :as component :refer [defdynamic-component]]
            [storefront.components.svg :as svg]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defmulti component
  "Display a different component based on type of promotion"
  (fn [data owner opts] (:promo/type data))
  :default :none)

(defmethod component :none
  [_ _ _]
  [:div])

(defmethod component :basic basic
  [{:keys [promo]} _ {hide-dt? :hide-dt?}]
  [:div.inherit-color.center.pp5.bg-warm-gray.h5.bold
   {:data-test (when-not hide-dt? "promo-banner")}
   (:description promo)])

(defn ^:private promotion-to-advertise
  [data]
  (let [promotion-db (get-in data keypaths/promotions)
        applied      (get-in data keypaths/order-promotion-codes)
        pending      (get-in data keypaths/pending-promo-code)]
    (or (promos/find-promotion-by-code promotion-db (first applied)) ;; on the order
        (promos/find-promotion-by-code promotion-db pending) ;; on a potential order
        (if-let [default-advertised-promo-text (get-in data keypaths/cms-advertised-promo-text)]
          ;; NOTE(jeff, justin): ideally contentful should provide the entire
          ;; promo object, but it's so much easier to pretend we have a
          ;; promotion object here.
          {:id -1
           :code nil
           :description default-advertised-promo-text
           :advertised true}
          (promos/default-advertised-promotion promotion-db)))))

(defn ^:private nav-whitelist-for
  "Promo code banner should only show on these nav-events

   Depending on experiments, this whitelist may be modified"
  [no-applied-promos? on-shop?]
  (cond-> #{events/navigate-about-mayvenn-install
            events/navigate-cart
            events/navigate-shop-by-look
            events/navigate-shop-by-look-details
            events/navigate-category
            events/navigate-product-details}
    (and (not no-applied-promos?)
         (not on-shop?))
    (disj events/navigate-cart)))

(defn query
  [data]
  (let [no-applied-promo? (orders/no-applied-promo? (get-in data keypaths/order))
        on-shop?          (= :shop (sites/determine-site data))
        nav-event         (get-in data keypaths/navigation-event)
        show?             (contains? (nav-whitelist-for no-applied-promo? on-shop?) nav-event)
        hide-on-mb-tb?    (boolean (get-in data catalog.keypaths/category-panel))]
    (cond-> {:promo (promotion-to-advertise data)}
      show?          (assoc :promo/type :basic)
      hide-on-mb-tb? (assoc :hide-on-mb-tb? true))))

(defn static-organism
  [data owner opts]
  (component data opts))

(letfn [(header-height-magic-number []
          #?(:cljs
             (if (< (.-width (goog.dom/getViewportSize)) 750) 75 180)))
        (handle-scroll [_]
          #?(:cljs
             (this-as this
                      (component/set-state! this :show? (< (header-height-magic-number) (.-y (goog.dom/getDocumentScroll)))))))]
  (defdynamic-component sticky-organism
    (constructor [this props]
                 (component/create-ref! this "banner")
                 (set! (.-handle-scroll this) (.bind handle-scroll this))
                 {:show?              false
                  :description-length (-> this component/get-props :promo :description count)})
    (did-mount [this]
               #?(:cljs
                  (do
                    (component/set-state! this :banner-height (some-> (component/get-ref this "banner")
                                                                      goog.style/getSize
                                                                      .-height))
                    (goog.events/listen js/window EventType/SCROLL (.-handle-scroll this)))))
    (will-unmount [this]
                  #?(:cljs
                     (goog.events/unlisten js/window EventType/SCROLL (.-handle-scroll this))))
    (render [this]
            (component/html
             (let [{:keys [show? banner-height]} (component/get-state this)
                   data                          (component/get-props this)
                   opts                          (component/get-opts this)]
               [:div.fixed.z4.top-0.left-0.right-0
                (if show?
                  {:style {:margin-top "0"}
                   :class "transition-2"}
                  {:class "hide"
                   :style {:margin-top (str "-" banner-height "px")}})
                [:div {:ref (component/use-ref this "banner")}
                 (component data nil opts)]])))))

(defn built-sticky-organism
  [app-state opts]
  [:div
   (component/build sticky-organism (query app-state) opts)])

(defn built-static-organism
  [app-state opts]
  (component/html
   (component (query app-state) nil opts)))

;; page-top-most sticky promo bar
(defn built-static-sticky-organism
  [app-state opts]
  (let [{:as data :keys [hide-on-mb-tb?]} (query app-state)]
    (component/html
     [:div {:class (when hide-on-mb-tb? "hide-on-mb-tb")}
      [:div.invisible {:key "promo-filler"} ;; provides height spacing for layout
       (component data nil (assoc opts :hide-dt? true))] ; Hide redundant data-test for Heat
      [:div.fixed.z5.top-0.left-0.right-0 {:key "promo"}
       (component data nil opts)]])))
