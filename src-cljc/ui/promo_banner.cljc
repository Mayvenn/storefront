(ns ui.promo-banner
  (:require #?@(:cljs [[goog.dom]
                       [goog.events]
                       [goog.events.EventType :as EventType]
                       [goog.style]
                       [om.core :as om]])
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.promos :as promos]
            [storefront.component :as component]
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
  (component/create [:div]))

(defmethod component :adventure-freeinstall/applied
  [_ _ _]
  (component/create
   [:a.white.center.p2.bg-teal.mbnp5.h6.bold.flex.items-center.justify-center
    {:on-click  (utils/send-event-callback events/popup-show-adventure-free-install)
     :data-test "adventure-promo-banner"}
    (svg/celebration-horn {:height "1.6em"
                           :width  "1.6em"
                           :class  "mr1 fill-white stroke-white"})
    [:div.pointer "CONGRATS — Your next install is FREE! "
     [:span.underline "More info"]]]))

(defmethod component :v2-freeinstall/eligible
  [_ _ _]
  (component/create
   [:a {:on-click  (utils/send-event-callback events/popup-show-v2-homepage)
        :data-test "v2-free-install-promo-banner"}
    [:div.white.center.pp5.bg-teal.h5.bold.pointer
     "Mayvenn will pay for your install! " [:span.underline "Learn more"]]]))

(defmethod component :v2-freeinstall/applied
  [_ _ _]
  (component/create
   [:a.white.center.p2.bg-teal.mbnp5.h6.bold.flex.items-center.justify-center
    {:on-click  (utils/send-event-callback events/popup-show-v2-homepage)
     :data-test "v2-free-install-promo-banner"}
    (svg/celebration-horn {:height "1.6em"
                           :width  "1.6em"
                           :class  "mr1 fill-white stroke-white"})
    [:div.pointer "CONGRATS — Your next install is FREE! "
     [:span.underline "More info"]]]))

(defmethod component :shop/freeinstall
  [_ _ _]
  (component/create
   [:a.block.white.p2.bg-lavender.flex.justify-center
    {:on-click  (utils/send-event-callback events/popup-show-adventure-free-install)
     :data-test "shop-freeinstall-promo-banner"}
    (svg/info {:height "14px"
               :width  "14px"
               :class  "mr1 mt1"})
    [:div.pointer.h6.medium "Buy 3 items and receive your free "
     [:span.bold.underline
      "Mayvenn" ui/nbsp "Install"]]]))

(defmethod component :basic
  [{:keys [promo]} _ _]
  (component/create
   [:div.white.center.pp5.bg-teal.h5.bold
    {:data-test "promo-banner"}
    (:description promo)]))


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
  [no-promotions? consolidated-cart? promo-type]
  (cond-> #{events/navigate-home
            events/navigate-cart
            events/navigate-shop-by-look
            events/navigate-shop-by-look-details
            events/navigate-category
            events/navigate-product-details}

    ;; Incentivize checkout by reminding them they are saving
    (#{:v2-freeinstall/applied
       :adventure-freeinstall/applied} promo-type)
    (conj events/navigate-checkout-returning-or-guest
          events/navigate-checkout-address
          events/navigate-checkout-payment
          events/navigate-checkout-confirmation)

    (and (not no-promotions?) (not consolidated-cart?))
    (disj events/navigate-cart)))

(defn ^:private promo-type*
  "Determine what type of promotion behavior we are under
   experiment for"
  [data]
  (let [shop? (= "shop" (get-in data keypaths/store-slug))]
    (cond
      (and shop?
           (experiments/consolidated-cart? data))
      :shop/freeinstall

      ;; GROT: freeinstall-applied? when adventure orders using freeinstall promo code are no longer relevant
      (and
       (or (orders/freeinstall-applied? (get-in data keypaths/order))
           (orders/freeinstall-included? (get-in data keypaths/order)))
       (= "freeinstall" (get-in data keypaths/store-slug)))
      :adventure-freeinstall/applied

      (and
       (orders/freeinstall-applied? (get-in data keypaths/order))
       (experiments/aladdin-experience? data))
      :v2-freeinstall/applied

      (experiments/aladdin-experience? data)
      :v2-freeinstall/eligible

      :else :basic)))

(defn query
  [data]
  (let [no-applied-promo?  (orders/no-applied-promo? (get-in data keypaths/order))
        consolidated-cart? (experiments/consolidated-cart? data)
        nav-event          (get-in data keypaths/navigation-event)
        promo-type         (promo-type* data)]
    (cond-> {:promo (promotion-to-advertise data)}
      (contains? (nav-whitelist-for no-applied-promo? consolidated-cart? promo-type) nav-event)
      (assoc :promo/type promo-type))))

(defn static-organism
  [data owner opts]
  (component data opts))

(defn sticky-organism
  [data owner opts]
  #?(:clj (component/create [:div])
     :cljs
     (letfn [(header-height-magic-number [] (if (< (.-width (goog.dom/getViewportSize)) 750) 75 180))
             (handle-scroll [e] (om/set-state! owner :show? (< (header-height-magic-number) (.-y (goog.dom/getDocumentScroll)))))
             (set-height [] (om/set-state! owner :banner-height (some-> owner
                                                                        (om/get-node "banner")
                                                                        goog.style/getSize
                                                                        .-height)))]
       (reify
         om/IInitState
         (init-state [this]
           {:show? false})
         om/IDidMount
         (did-mount [this]
           (om/set-state! owner :description-length (count (:description (:promo data))))
           (set-height)
           (goog.events/listen js/window EventType/SCROLL handle-scroll))
         om/IWillUnmount
         (will-unmount [this]
           (goog.events/unlisten js/window EventType/SCROLL handle-scroll))
         om/IWillReceiveProps
         (will-receive-props [this next-props]
           (set-height))
         om/IRenderState
         (render-state [this {:keys [show? banner-height]}]
           (component/html
            [:div.fixed.z4.top-0.left-0.right-0
             (if show?
               {:style {:margin-top "0"}
                :class "transition-2"}
               {:class "hide"
                :style {:margin-top (str "-" banner-height "px")}})
             [:div {:ref "banner"}
              (om/build component data opts)]]))))))

(defn built-sticky-organism
  [app-state opts]
  [:div
   (component/build sticky-organism (query app-state) opts)])

(defn built-static-organism
  [app-state opts]
  [:div
   (component/build component (query app-state) opts)])
