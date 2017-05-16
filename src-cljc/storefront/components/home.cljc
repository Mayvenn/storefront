(ns storefront.components.home
  (:require [storefront.platform.component-utils :as utils]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.platform.date :as date]
            [storefront.platform.images :as images]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.named-searches :as named-searches]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.assets :as assets]
            [storefront.platform.messages :refer [handle-message]]))

(defn homepage-images
  "Adds image effect where images are always full width. On mobile, the image
  height grows as the screen grows. On desktop, the image height is fixed. The
  image is centered and the edges are clipped at narrower screens, then are
  gradually exposed on wider screens. All meaningful content has to fit within
  the center 640px of the desktop image.

  mobile-asset can be any height but must be 640px wide
  desktop-asset should be 408px high and at least 1024px wide"
  [mobile-asset desktop-asset alt-text]
  [:span.block
   [:span.block.hide-on-mb.bg-center.bg-no-repeat
    {:style {:height "408px"
             :background-image (assets/css-url desktop-asset)}
     :title alt-text}]
   [:img.hide-on-tb-dt.col-12 {:src mobile-asset
                               :alt alt-text}]])

(defn hero-image
  "Changes height for image to be full width."
  [mobile-image desktop-image]
  [:div
   [:div.hide-on-mb (ui/lqip 96 40 (assoc desktop-image :alt "Get 15% Off Hair Extensions Mayvenn"))]
   [:div.hide-on-tb-dt (ui/lqip 100 64 (assoc mobile-image :alt "Get 15% Off Hair Extensions Mayvenn"))]])

(defn popular-grid [featured-searches]
  (let [grid-block   (fn [key content]
                       [:div.col.col-6.col-4-on-tb-dt.border.border-white {:key key}
                        (ui/aspect-ratio 4 3 content)])]
    [:div.container.center.mb4.pb4
     [:div.flex.flex-column.my4
      [:h1.h4.order-2.px2 "100% Virgin Human Hair, always fast and free shipping"]
      [:h2.h1.order-1 "Shop our styles"]]
     [:div
      (for [{:keys [representative-images name slug]} featured-searches]
        (let [{:keys [model-grid]} representative-images]
          (grid-block slug
                      [:a.absolute.overlay.overflow-hidden
                       (merge {:data-test (str "named-search-" slug)}
                              (utils/route-to events/navigate-category {:named-search-slug slug}))
                       (ui/lqip 48 36 model-grid)
                       [:h3.h2.white.absolute.col-12.titleize
                        {:style {:text-shadow "black 0px 0px 25px, black 0px 0px 25px"
                                 :top         "50%"}}
                        name]])))
      (grid-block "spare-block"
                  [:a.bg-light-teal.white.absolute.overlay
                   (assoc (utils/route-to events/navigate-shop-by-look)
                          :data-test "nav-shop-look")
                   [:div.flex.container-size.justify-center.items-center
                    [:h3.hide-on-tb-dt
                     [:div "Need inspiration?"]
                     [:div "Try shop by look."]]
                    [:h3.h2.hide-on-mb
                     [:div "Need inspiration?"]
                     [:div "Try shop by look."]]]])]]))

(defn banner [store-slug]
  [:h1.h2
   [:a
    (assoc (utils/route-to events/navigate-shop-by-look)
           :data-test "home-banner")
    (case store-slug
      "peakmill"       (hero-image {:resizable_url "//ucarecdn.com/4de6e2fa-3bf1-4003-8496-7d39d3dd7adb/" :resizable_filename "15PercentOffHairExtensionsPeakmillMOB.jpg"}
                                   {:resizable_url "//ucarecdn.com/46fddeb4-d712-42a7-a6b5-a976c29ffd63/" :resizable_filename "15PercentOffHairExtensionsPeakmillCOM.jpg"})
      "touchedbytokyo" (hero-image {:resizable_url "//ucarecdn.com/aacdb1c7-bcde-485a-a33e-5f7413422f4d/" :resizable_filename "15PercentOffHairExtensionsTouchedByTokyoMOB.jpg"}
                                   {:resizable_url "//ucarecdn.com/d6f75046-4bbd-4856-95ff-c7cc53c2dcba/" :resizable_filename "15PercentOffHairExtensionsTouchedByTokyoCOM.jpg"})
      "msroshposh"     (hero-image {:resizable_url "//ucarecdn.com/c0ede2f7-c285-4146-ab27-616455d4d5da/" :resizable_filename "15PercentOffHairExtensionsMsRoshPoshMOB.jpg"}
                                   {:resizable_url "//ucarecdn.com/559e3bf3-5241-48e7-8407-6023a9b02b39/" :resizable_filename "15PercentOffHairExtensionsMsRoshPoshCOM.jpg"})
      (hero-image {:resizable_url "//ucarecdn.com/671e6abf-6d5c-4c4e-80ab-b867583567df/" :resizable_filename "15PercentOffHairExtensionsMayvennMOB.jpg"}
                  {:resizable_url "//ucarecdn.com/cf8453d3-cf85-427a-aaa4-d368f28bb7cc/" :resizable_filename "15PercentOffHairExtensionsMayvennCOM.jpg"}))]])

(def about-mayvenn
  (component/html
   [:div.container.py1.my1.py4-on-tb-dt.my4-on-tb-dt
    [:h1.line-length.mx-auto.center.p4 "why people love Mayvenn hair"]

    [:div.clearfix.h5
     [:div.col-on-tb-dt.col-4-on-tb-dt.p3
      [:h3.h2.center.mb3 "stylist recommended"]
      [:p.h5 "Mayvenn hair is the #1 recommended hair company by over 60,000 hair stylists across the country, making it the most trusted hair brand on the market."]]

     [:div.col-on-tb-dt.col-4-on-tb-dt.p3
      [:h3.h2.center.mb3 "30 day guarantee"]
      [:p.h5 "Try the best quality hair on the market risk free! Wear it, dye it, even cut it. If you’re not happy with your bundles, we will exchange them within 30 days for FREE!"]]

     [:div.col-on-tb-dt.col-4-on-tb-dt.p3
      [:h3.h2.center.mb3 "fast free shipping"]
      [:p.h5 "Mayvenn offers free standard shipping on all orders, no minimum necessary. In a hurry? Expedited shipping options are available for those who just can’t wait."]]]

    [:div.col-8.col-4-on-tb-dt.mt1.mx-auto
     (ui/teal-button
      (assoc (utils/route-to events/navigate-shop-by-look)
             :data-test "nav-shop-look")
      "shop our looks")]]))

(def video-autoplay
  (component/html
   (let [video-src  "https://embedwistia-a.akamaihd.net/deliveries/e88ed4645e104735c3bdcd095c370e5ccb1e1ef4/file.mp4"
         video-html (str "<video loop muted autoplay playsinline controls class=\"col-12\"><source src=\""
                         video-src
                         "\"></source></video>")]
     [:div
      [:div.center.hide-on-tb-dt.mbn2
       [:div.px3
        [:h1 "We love our customers"]
        [:p.h4 "And they love Mayvenn! Watch and see why they absolutely love wearing our hair."]
        [:div.col-8.my4.mx-auto
         (ui/teal-ghost-button
          (assoc (utils/route-to events/navigate-shop-by-look)
                 :data-test "nav-shop-look")
          "Shop our styles now")]]
       [:div.container.col-12.mx-auto {:dangerouslySetInnerHTML {:__html video-html}}]]
      [:div.center.bg-teal.py4.white.hide-on-mb
       [:h1.mt1 "We love our customers"]
       [:p.h4 "And they love Mayvenn! Watch and see why they absolutely love wearing our hair."]
       [:div.col-8.col-3-on-tb-dt.my4.mx-auto
        (ui/light-ghost-button
         (assoc (utils/route-to events/navigate-shop-by-look)
                :data-test "nav-shop-look")
         "Shop our styles now")]
       [:div.container.col-12.mx-auto.mb3 {:dangerouslySetInnerHTML {:__html video-html}}]]])))

(def talkable-banner
  (component/html
   [:h1.h2.container.py4
    [:a
     (utils/route-to events/navigate-friend-referrals {:query-params {:traffic_source "homepageBanner"}})
     (homepage-images
      (assets/path "/images/homepage/mobile_talkable_banner.png")
      (assets/path "/images/homepage/desktop_talkable_banner.png")
      "refer friends, earn rewards, get 20% off")]]))

(defn component [{:keys [featured-searches store-slug]} owner opts]
  (component/create
   [:div.m-auto
    [:section (banner store-slug)]
    [:section (popular-grid featured-searches)]
    [:section video-autoplay]
    [:section about-mayvenn]
    [:section talkable-banner]]))

(defn query [data]
  {:featured-searches (->> (named-searches/current-named-searches data)
                           (filter (comp #{"straight" "loose-wave" "body-wave" "deep-wave" "curly"} :slug)))
   :store-slug        (get-in data keypaths/store-slug)})

(defn built-component [data opts]
  (component/build component (query data) opts))
