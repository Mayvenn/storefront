(ns install.home
  (:require #?@(:cljs [[om.core :as om]
                       [goog.events]
                       [goog.dom]
                       [goog.events.EventType :as EventType]])
            [storefront.events :as events]
            [storefront.assets :as assets]
            [storefront.platform.component-utils :as utils]
            [storefront.components.ui :as ui]
            [storefront.component :as component]))

(defn follow-header [{:keys [text-or-call-number]} owner opts]
  #?(:cljs
     (letfn [(handle-scroll [e] (om/set-state! owner :show? (< 750 (.-y (goog.dom/getDocumentScroll)))))]
       (reify
         om/IInitState
         (init-state [this]
           {:show? false})
         om/IDidMount
         (did-mount [this]
           (goog.events/listen js/window EventType/SCROLL handle-scroll))
         om/IWillUnmount
         (will-unmount [this]
           (goog.events/unlisten js/window EventType/SCROLL handle-scroll))
         om/IWillReceiveProps
         (will-receive-props [this next-props])
         om/IRenderState
         (render-state [this {:keys [show?]}]
           (component/html
            [:div.fixed.top-0.left-0.right-0.z4.bg-white
             (if show?
               {:style {:margin-top "0"}
                :class "transition-2"}
               {:style {:margin-top "-100px"}})
             [:div.border-bottom.border-gray.mx-auto
              {:style {:max-width "1440px"}}
              [:div.container.flex.items-center.justify-between.px3.py2
               [:div
                [:img {:src (assets/path "/images/header_logo.svg")
                       :style {:height "40px"}}]
                [:div.h6 "Questions? Text or call: "
                 (ui/link :link/phone :a.inherit-color {} text-or-call-number)]]
               [:div.col.col-4.h5
                (ui/teal-button (assoc (utils/route-to events/navigate-home)
                                       :data-test "shop"
                                       :height-class "py2")
                                "Shop")]]]]))))
     :clj [:span]))

(defn ^:private component
  [queried-data owner opts]
  (component/create
   [:div
    #_(component/build header/component (:header queried-data) nil)
    (component/build follow-header (:header queried-data) nil)
]))

(defn ^:private query
  [data]
  {:header {:text-or-call-number "1-310-733-0284"}})

(defn built-component
  [data opts]
  (component/build component (query data) opts))
