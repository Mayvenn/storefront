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

(defn fitting-image
  "Changes height for image to be full width."
  [mobile-image desktop-image alt-text]
  [:div
   [:img.hide-on-mb.block.col-12 {:src (str (:resizable_url desktop-image) (:resizable_filename desktop-image))
                                  :alt alt-text}]
   [:img.hide-on-tb-dt.block.col-12 {:src (str (:resizable_url mobile-image) (:resizable_filename mobile-image))
                                     :alt alt-text}]])

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
                       [:img.col-12 (ui/img-attrs model-grid :large)]
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

(defn hero [feature-block? store-slug]
  [:h1.h2
   [:a
    (assoc (utils/route-to events/navigate-shop-by-look)
           :data-test "home-banner")
    (case store-slug
      "peakmill"       (fitting-image {:resizable_url      "//ucarecdn.com/52b9b84d-5ccc-44b8-abbe-4b37f94a1ca0/"
                                       :resizable_filename "June-Homepage-Hero-Catch-The-Wave.jpg"}
                                      {:resizable_url      "//ucarecdn.com/13458c52-7f21-4a09-b21e-429cac284d01/"
                                       :resizable_filename "June-Homepage-Hero-Catch-The-Wave.jpg"}
                                      "Get 15% Off Hair Extensions Mayvenn")
      "touchedbytokyo" (fitting-image {:resizable_url      "//ucarecdn.com/52ecc4f7-06c3-4c47-9760-d73b5a79ed39/"
                                       :resizable_filename "June-Homepage-Hero-Catch-The-Wave.jpg"}
                                      {:resizable_url      "//ucarecdn.com/d4278fbd-dd34-4177-b10e-e7ed4eb5e14e/"
                                       :resizable_filename "June-Homepage-Hero-Catch-The-Wave.jpg"}
                                      "Get 15% Off Hair Extensions Mayvenn")
      "msroshposh"     (fitting-image {:resizable_url      "//ucarecdn.com/153c5870-c650-4af0-9f8c-04f514bfdff1/"
                                       :resizable_filename "June-Homepage-Hero-Catch-The-Wave.jpg"}
                                      {:resizable_url      "//ucarecdn.com/a9162d49-4f58-495b-adad-ccb15bae9541/"
                                       :resizable_filename "June-Homepage-Hero-Catch-The-Wave.jpg"}
                                      "Get 15% Off Hair Extensions Mayvenn")

      (fitting-image {:resizable_url      "//ucarecdn.com/a52a9db4-0e83-4a4a-a802-6805ce127337/"
                      :resizable_filename "June-Homepage-Hero-Catch-The-Wave.jpg"}
                     {:resizable_url      "//ucarecdn.com/bd739712-db44-405c-8469-f9aed3768ee2/"
                      :resizable_filename "June-Homepage-Hero-Catch-The-Wave.jpg"}
                     "Get 15% Off Hair Extensions Mayvenn"))]])

(def feature-blocks
  [:div.container.border-top.border-white
   [:div.col.col-6.border.border-white
    [:a
     (utils/route-to events/navigate-shop-by-look-details {:look-id 177858410})
     (fitting-image {:resizable_url      "//ucarecdn.com/d64294f0-e6d0-4b6d-94f1-7da819119c01/"
                     :resizable_filename "Shop-Indian-Straight-Hair.jpg"}
                    {:resizable_url      "//ucarecdn.com/04e2b2fc-e40a-4b52-8cb3-ba7bdfa84963/"
                     :resizable_filename "Shop-Indian-Straight-Hair.jpg"}
                    "Shop Indian Straight Hair")]]
   [:div.col.col-6.border.border-white
    [:a
     (utils/route-to events/navigate-shop-by-look-details {:look-id 178528561})
     (fitting-image {:resizable_url      "//ucarecdn.com/97994826-a043-499e-95a5-549b06ac35b8/"
                     :resizable_filename "Shop-Virgin-Hair-Bundle-Deals.jpg"}
                    {:resizable_url      "//ucarecdn.com/811aa020-2d64-4765-ae82-9980117cd6d8/"
                     :resizable_filename "Shop-Virgin-Hair-Bundle-Deals.jpg"}
                    "Shop Virgin Hair Bundle Deals")]]])

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

(defn component [{:keys [featured-searches feature-block? store-slug]} owner opts]
  (component/create
   [:div.m-auto
    [:section (hero feature-block? store-slug)]
    (when feature-block?
      [:section feature-blocks])
    [:section (popular-grid featured-searches)]
    [:section video-autoplay]
    [:section about-mayvenn]
    [:section talkable-banner]]))

(defn query [data]
  {:featured-searches (->> (named-searches/current-named-searches data)
                           (filter (comp #{"straight" "loose-wave" "body-wave" "deep-wave" "curly"} :slug)))
   :feature-block?    (experiments/feature-block? data)
   :store-slug        (get-in data keypaths/store-slug)})

(defn built-component [data opts]
  (component/build component (query data) opts))
