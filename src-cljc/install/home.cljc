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

(defn header [text-or-call-number]
  [:div.container.flex.items-center.justify-between.px3.py2
   [:div
    [:img {:src (assets/path "/images/header_logo.svg")
           :style {:height "40px"}}]
    [:div.h7 "Questions? Text or call: "
     (ui/link :link/phone :a.inherit-color {} text-or-call-number)]]
   [:div.col.col-4.h5
    (ui/teal-button (assoc (utils/route-to events/navigate-home)
                           :data-test "shop"
                           :height-class "py2")
                    "Shop")]])

(defn relative-header [{:keys [text-or-call-number]} owner opts]
   (component/create (header text-or-call-number)))

(defn fixed-header [{:keys [text-or-call-number]} owner opts]
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
             (header text-or-call-number)]))))
     :clj [:span]))

(defn ^:private stat-block [header content]
  [:div.center.p2 [:div.bold.teal header]
   [:div.h6.line-height-1 content]])

(defn ^:private as-seen-in-logos [& logo-urls]
  (for [url logo-urls]
    [:img.mx2.my2 {:src url}]))

(defn ^:private component
  [queried-data owner opts]
  (component/create
   [:div
    (component/build relative-header (:header queried-data) nil)
    (component/build fixed-header (:header queried-data) nil)

    [:div.bg-cover.bg-top.bg-free-install-landing.col-12.p4
     [:div.teal.h1.shadow.bold.pt2 "FREE INSTALL"]
     [:div.medium.letter-spacing-1.col-7.h3.white.shadow "Get your Mayvenn hair installed for FREE by some of the best stylists in Fayetteville, NC"]]

    [:div.flex.items-center.justify-center.p1.pt2.pb3
     (stat-block "100,000+" "Mayvenn Stylists Nationwide")
     (stat-block "200,000+" "Happy Mayvenn Customers")
     (stat-block "100%" "Guaranteed Human Hair")]


    [:div.col-12.bg-gray.py2
     [:div.dark-gray.col-12.center.h7.medium.letter-spacing-4.p1 "AS SEEN IN"]
     (into [:div.flex.flex-wrap.justify-around.items-center]
           (as-seen-in-logos
            "//ucarecdn.com/a2e763ea-1837-43fd-8531-440d18360e1e/-/format/auto/-/resize/160x/pressmadamenoirelogo3x.png"
            "//ucarecdn.com/74f56834-b879-415a-9e55-87a059767297/-/format/auto/-/resize/75x/pressessence3x.png"
            "//ucarecdn.com/b1a3d9c1-80a0-4549-9603-36fb65b5bebb/-/format/auto/-/resize/56x/pressebonylogo3x.png"
            "//ucarecdn.com/4f8c1a9d-ab71-4881-97df-b4a724354faa/-/format/auto/-/resize/45x/pressvoiceofhairlogo3x.png"
            "//ucarecdn.com/3428dfc2-bc0a-40f2-9bdd-c79df6abd63f/-/format/auto/-/resize/150x/presshellobeautiful3x.png"))]

    [:div "3 EASY STEPS"]

    [:div "HAPPY CUSTOMERS"]]))

(defn ^:private query
  [data]
  {:header {:text-or-call-number "1-310-733-0284"}})

(defn built-component
  [data opts]
  (component/build component (query data) opts))
