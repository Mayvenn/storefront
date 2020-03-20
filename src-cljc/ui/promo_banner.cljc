(ns ui.promo-banner
  (:require #?@(:cljs [[goog.dom]
                       [goog.events]
                       [goog.events.EventType :as EventType]
                       [goog.style]
                       ["react" :as react]])
            catalog.keypaths
            [catalog.products :as products]
            [storefront.accessors.categories :as accessors.categories]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.products :as accessors.products]
            [storefront.accessors.promos :as promos]
            [storefront.component :as component :refer [defcomponent defdynamic-component]]
            [storefront.components.svg :as svg]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.components.ui :as ui]))

(defmulti component
  "Display a different component based on type of promotion"
  (fn [data owner opts] (:promo/type data))
  :default :none)

(defmethod component :none
  [_ _ _]
  [:div])

(defmethod component :v2-freeinstall/eligible v2-freeinstall-eligible
  [_ _ {hide-dt? :hide-dt?}]
  [:a {:on-click  (utils/send-event-callback events/popup-show-v2-homepage)
       :data-test (when-not hide-dt? "v2-free-install-promo-banner")}
   [:div.white.center.pp5.bg-p-color.h5.bold.pointer
    "Mayvenn will pay for your install! " [:span.underline "Learn more"]]])

(defmethod component :v2-freeinstall/applied v2-freeinstall-applied
  [_ _ {hide-dt? :hide-dt?}]
  [:a.white.center.p2.bg-p-color.mbnp5.h6.bold.flex.items-center.justify-center
   {:on-click  (utils/send-event-callback events/popup-show-v2-homepage)
    :data-test (when-not hide-dt? "v2-free-install-promo-banner")}
   (svg/celebration-horn {:height "1.6em"
                          :width  "1.6em"
                          :class  "mr1 fill-white stroke-white"})
   [:div.pointer "CONGRATS — Your next install is FREE! "
    [:span.underline "More info"]]])

(defmethod component :shop/covid19 covid19
  [_ _ {hide-dt? :hide-dt?}]
  [:a.block.white.p2.bg-s-color.flex.items-top
   {:href      "https://looks.mayvenn.com/covid19"
    :target    "_blank"
    :data-test (when-not hide-dt? "shop-covid19-promo-banner")}
   [:div.mtp2.mr1 (svg/info {:height "16px"
                             :width  "16px"
                             :class  "mr1"})]
   [:div.pointer.h6 "Mayvenn is still shipping and booking appointments! Click to learn about the precautions we are taking for COVID-19"]])

(defmethod component :shop/freeinstall shop-freeinstall
  [_ _ {hide-dt? :hide-dt?}]
  [:a.block.white.p2.bg-p-color.flex.justify-center.items-center
   {:on-click  (utils/send-event-callback events/popup-show-consolidated-cart-free-install)
    :data-test (when-not hide-dt? "shop-freeinstall-promo-banner")}
   (svg/info {:height "14px"
              :width  "14px"
              :class  "mr1"})
   [:div.pointer.h6 "Buy 3 items and receive your free "
    [:span.underline
     "Mayvenn" ui/nbsp "Install"]]])

(defmethod component :shop/wigs shop-wigs
  [_ _ {hide-dt? :hide-dt?}]
  [:a.block.white.p2.bg-p-color.flex.justify-center.items-center
   {:on-click  (utils/send-event-callback events/popup-show-wigs-customization)
    :data-test (when-not hide-dt? "wig-customization-promo-banner")}
   (svg/info {:height "14px"
              :width  "14px"
              :class  "mr1"})
   [:div.pointer.h6 "Buy a wig & get it customized for free. "
    [:span.underline
     "Learn" ui/nbsp "more"]]])

(defmethod component :basic basic
  [{:keys [promo]} _ {hide-dt? :hide-dt?}]
  [:div.white.center.pp5.bg-p-color.h5.bold
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
  (cond-> #{events/navigate-home
            events/navigate-cart
            events/navigate-shop-by-look
            events/navigate-shop-by-look-details
            events/navigate-category
            events/navigate-product-details}

    ;; TODO SPEC classic or aladdin. Needs product verification
    (and (not no-applied-promos?)
         (not on-shop?))
    (disj events/navigate-cart)))

(defn ^:private promo-type*
  "Determine what type of promotion behavior we are under
   experiment for"
  [data]
  (let [shop?                (= "shop" (get-in data keypaths/store-slug))
        aladdin?             (experiments/aladdin-experience? data)
        affiliate?           (= "influencer" (get-in data keypaths/store-experience))
        [navigation-event _] (get-in data keypaths/navigation-message)
        wigs?                (or (and (= events/navigate-product-details navigation-event)
                                      (accessors.products/wig-product? (products/current-product data)))
                                 (and (= events/navigate-category navigation-event)
                                      (accessors.categories/wig-category? (accessors.categories/current-category data))))]
    (cond
      shop?
      :shop/covid19

      ;; Covid-19: Wigs never triggers on shop (top condition overrides)
      (and (or shop? aladdin? affiliate?)
           wigs?)
      :shop/wigs

      ;; Covid-19: Freeinstall never triggers (top condition overrides)
      shop?
      :shop/freeinstall

      (and aladdin? (orders/service-line-item-promotion-applied? (get-in data keypaths/order)))
      :v2-freeinstall/applied

      aladdin?
      :v2-freeinstall/eligible

      :else :basic)))

(defn query
  [data]
  (let [no-applied-promo? (orders/no-applied-promo? (get-in data keypaths/order))
        on-shop?          (= "shop" (get-in data keypaths/store-slug))
        nav-event         (get-in data keypaths/navigation-event)
        promo-type        (promo-type* data)
        show?             (contains? (nav-whitelist-for no-applied-promo? on-shop?) nav-event)
        hide-on-mb-tb?    (boolean (get-in data catalog.keypaths/category-panel))]
    (cond-> {:promo (promotion-to-advertise data)}
      show?          (assoc :promo/type promo-type)
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
