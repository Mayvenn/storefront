(ns ui.promo-banner
  (:require #?@(:cljs [[goog.dom]
                       [goog.events]
                       [goog.events.EventType :as EventType]
                       [goog.style]
                       [om.core :as om]])
            [storefront.component :as component]
            storefront.ui.promo-banner))

#_(defn organism
  [_ _ _]
  (component/create
   [:div]))

(defn static-organism
  [data owner opts]
  (storefront.ui.promo-banner/component data opts))

(defn sticky-organism
  [data owner opts]
  #?(:clj (component/create [:div])
     :cljs
     (letfn [(handle-scroll [e] (om/set-state! owner :show? (< 75 (.-y (goog.dom/getDocumentScroll)))))
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
              (om/build storefront.ui.promo-banner/component data opts)]]))))))

(defn built-organism
  [{:promo-banner/keys [static? sticky?] :as data} opts]
  [:div
   (cond sticky?  (component/build sticky-organism data opts)
         static?  (component/build static-organism data opts)
         :default nil)])
