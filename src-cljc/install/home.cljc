(ns install.home
  (:require #?@(:cljs [[om.core :as om]
                       [goog.events]
                       [goog.dom]
                       [goog.events.EventType :as EventType]])
            [storefront.events :as events]
            [storefront.assets :as assets]
            [storefront.platform.component-utils :as utils]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
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
  [:div.center.p2 [:div.bold.teal.letter-spacing-1 header]
   [:div.h6.line-height-1 content]])

(defn ^:private as-seen-in-logos [& logo-urls]
  (for [url logo-urls]
    [:img.mx2.my2 {:src url}]))

(defn ucare-img [{:as img-attrs :keys [width class]} image-id]
  (let [retina-url  (cond-> (str "//ucarecdn.com/" image-id "/-/format/auto/-/quality/lightest/")
                      width (str "-/resize/" width "x"))
        default-url (cond-> (str "//ucarecdn.com/" image-id "/-/format/auto/")
                      width (str "-/resize/" width "x"))]
    [:picture
     [:source {:src-set (str retina-url " 2x,"
                             default-url " 1x")}]
     [:img (assoc img-attrs :src default-url)]]))


(defn img-with-circle [diameter img-id content]
  (let [radius (quot diameter 2)]
    [:div
     {:style {:padding-bottom (str radius "px")}}
     [:div.relative
      [:div
       (ucare-img {:class "col-12 col-4-on-tb-dt"} img-id)]
      [:div.bg-teal.border.border-white.border-width-3.circle.absolute.right-0.left-0.mx-auto.flex.items-center.justify-center
       {:style {:height (str diameter "px")
                :width (str diameter "px")
                :bottom (str "-" (- radius 4) "px")}}
       content]]]))

(defn ^:private easy-step [number title copy img-id]
  [:div.py4
   [:div.px6 (img-with-circle 60 img-id [:div.h1.bold.white number])]
   [:div.h3 title]
   [:div.dark-gray.h6 copy]])

(defn ^:private happy-customer [img-id testimony customer-name]
  [:div.p4.flex.items-start.justify-center
   [:div.col-5
    (ucare-img {:class "col-12"} img-id)]
   [:div.px2.flex.flex-column
    [:div.line-height-3.h6 \“ testimony \”]
    [:div.h6.bold "- "  customer-name]]])

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

    [:div.border.border-teal.border-width-2.m3.center.p3
     [:div
      [:div.py2
       [:div.teal.letter-spacing-6.medium.h6 "3 EASY STEPS"]
       [:div.h2 "Get a FREE install"]]
      [:div.h6 "Purchase 3 bundles of Mayvenn hair and your install by a Mayvenn Certified Stylist is FREE!"]]
     (easy-step 1
                "Buy 3 bundles or more"
                "Closures and fronts count, too! Our hair is 100% human, backed by a 30 day guarantee and starts at $30 per bundle."
                "fdcc8acc-443c-4b2f-b510-0d940297f997")
     (easy-step 2
                "A Fayetteville, NC exclusive offer"
                "Your Mayvenn order must be shipped to a qualified address in Fayetteville, NC."
                "6263c536-f548-45dc-ba89-ca68ad7c44c8")
     (easy-step 3
                "Book your FREE install"
                "After completing your purchase, Mayvell will contact you to arrange your FREE install appointment with a Mayvenn Certified Stylists."
                "52dcdffb-cc44-4f80-88c8-325de7c3fa62")]

    [:div.bg-transparent-teal.py6
     [:div.pt4.teal.letter-spacing-6.bold.center.h6 "HAPPY CUSTOMERS"]
     [:div.px3
      (happy-customer "dd6b26ed-1f15-437c-ac2c-e289d3a854fe"
                      "I freaking love Mayvenn hair, like they are BOMB.COM GIRL! Yass!"
                      "J Luxe")
      (happy-customer "014856c8-7fa5-40b7-a707-48361c37f04f"
                      "I'm 100% satisfied, that's all I can say, I'm 100% satisfied"
                      "Cara Scott")
      (happy-customer "b7258cae-1aac-4755-9f61-90e9908ff7a7"
                      "Ugh God you guys, like you don't understand, I love this hair."
                      "Tiona Chantel")]]]))

(defn ^:private query
  [data]
  {:header {:text-or-call-number "1-310-733-0284"}})

(defn built-component
  [data opts]
  (component/build component (query data) opts))
