(ns ui.promo-banner
  (:require #?@(:cljs [[goog.dom]
                       [goog.events]
                       [goog.events.EventType :as EventType]
                       [goog.style]])
            catalog.keypaths
            [mayvenn.concept.account :as account]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.promos :as promos]
            [storefront.accessors.sites :as sites]
            [storefront.component :as component :refer [defdynamic-component]]
            [storefront.components.svg :as svg]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defmulti component
  "Display a different component based on type of promotion"
  (fn [data owner opts] (:type data))
  :default :none)

(defmethod component :none
  [_ _ _]
  [:div])

(defmethod component :basic basic
  [{:keys [primary target]} _ {hide-dt? :hide-dt?}]
  [:div.inherit-color.center.pp5.bg-warm-gray.h5.bold.shout
   {:data-test (when-not hide-dt? "promo-banner")}
   (if target
     [:a.underline.inherit-color
      {:href  target}
      primary]
     [:div primary])])

(def ^:private walmart-retail-pages
  #{events/navigate-retail-walmart
    events/navigate-retail-walmart-katy
    events/navigate-retail-walmart-houston
    events/navigate-retail-walmart-grand-prairie
    events/navigate-retail-walmart-dallas
    events/navigate-retail-walmart-mansfield})

(defn ^:private promo->banner-data [promotion]
  (when promotion
    {:primary (:description promotion)
     :target  (:uri promotion)}))

(defn ^:private banner-data<-
  [data]
  (let [promotion-db    (get-in data keypaths/promotions)
        applied         (get-in data keypaths/order-promotion-codes)
        pending         (get-in data keypaths/pending-promo-code)
        omni?           (-> data account/<- :experiences :experience/omni)
        cms-omni-banner (when omni? (get-in data (conj keypaths/cms-copy-url-slug "banner-omni")))]
    (cond
      (first applied) ; on the order
      (promo->banner-data (promos/find-promotion-by-code promotion-db (first applied)))

      pending ; on a potential order
      (promo->banner-data (promos/find-promotion-by-code promotion-db pending))

      cms-omni-banner ; in the omni experience
      {:primary (:copy cms-omni-banner)
       :target  (:url cms-omni-banner)}

      (get-in data keypaths/cms-advertised-promo-text) ; CMS override (old)
      {:primary (get-in data keypaths/cms-advertised-promo-text)
       :target (get-in data keypaths/cms-advertised-promo-uri)}

      :else ; any promo with the advertise flag on in el-jefe
      (promo->banner-data (promos/default-advertised-promotion promotion-db)))))



(defn ^:private nav-allowlist-for
  "Promo code banner should only show on these nav-events

   Depending on experiments, this allowlist may be modified"
  [no-applied-promos? on-shop?]
  (cond-> (clojure.set/union #{events/navigate-home
                               events/navigate-cart
                               events/navigate-shop-by-look
                               events/navigate-shop-by-look-details
                               events/navigate-category
                               events/navigate-product-details}
                             walmart-retail-pages)
    (and (not no-applied-promos?)
         (not on-shop?))
    (disj events/navigate-cart)))

(defn query
  [data]
  (let [no-applied-promo? (orders/no-applied-promo? (get-in data keypaths/order))
        on-shop?          (= :shop (sites/determine-site data))
        nav-event         (get-in data keypaths/navigation-event)
        ;; TODO: this is a super rushed approach to promo-banner selection, we should
        ;;       reevaluate our approach to promo determination when we have more time.
        on-omni-lp?       (and (= events/navigate-landing-page nav-event)
                               (->> (get-in data keypaths/navigation-args)
                                    :landing-page-slug
                                    (= "omni")))
        show?             (or (contains? (nav-allowlist-for no-applied-promo? on-shop?) nav-event)
                              on-omni-lp?)
        hide-on-mb-tb?    (boolean (get-in data catalog.keypaths/category-panel))
        banner-data       (banner-data<- data)]
    (cond-> banner-data
      show?          (assoc :type :basic)
      hide-on-mb-tb? (assoc :hide-on-mb-tb? true))))

(defn static-organism
  [data _ opts]
  (component data opts))

(defn built-static-organism
  [app-state opts]
  (component/html
   (component (query app-state) nil opts)))

(letfn [(header-height-magic-number []
          #?(:cljs (if (< (.-width (goog.dom/getViewportSize)) 750) 75 180)))
        (handle-scroll [_]
          #?(:cljs (this-as this
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
