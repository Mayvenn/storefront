(ns storefront.components.home
  (:require [storefront.platform.component-utils :as utils]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.platform.date :as date]
            [storefront.platform.images :as images]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.named-searches :as named-searches]
            [storefront.accessors.auth :as auth]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.components.marquee :as marquee]
            [storefront.assets :as assets]
            [storefront.platform.messages :refer [handle-message]]))

(defn product-image
  [{:keys [resizable_url resizable_filename alt]}]
  ;; Assumptions: 2 up on mobile, 3 up on tablet/desktop, within a .container. Does not account for 1px border.
  [:img.block.col-12 {:src     (str resizable_url "-/format/auto/-/resize/750x/" resizable_filename)
                      :src-set (str resizable_url "-/format/auto/-/resize/750x/-/quality/lightest/" resizable_filename " 750w, "
                                    resizable_url "-/format/auto/-/resize/640x/-/quality/lightest/" resizable_filename " 640w")
                      :sizes   (str "(min-width: 1000px) 320px, "
                                    "(min-width: 750px) 240px, "
                                    "50vw")
                      :alt     alt}])

(defn popular-grid [featured-searches]
  (let [grid-block (fn [key content]
                     [:div.col.col-6.col-4-on-tb-dt.border.border-white {:key key}
                      (ui/aspect-ratio 4 3 content)])]
    [:div.container.center.mb4.pb4
     [:div.flex.flex-column.my4
      [:h1.h4.order-2.px2 "100% Virgin Human Hair, always fast and free shipping"]
      [:h2.h1.order-1 "Shop our styles"]]
     [:div
      (for [{:keys [representative-images name slug]} featured-searches]
        (grid-block slug
                    [:a.absolute.overlay.overflow-hidden
                     (merge {:data-test (str "named-search-" slug)}
                            (utils/route-to events/navigate-named-search {:named-search-slug slug}))
                     (product-image (:model-grid representative-images))
                     [:h3.h2.white.absolute.col-12.titleize
                      {:style {:text-shadow "black 0px 0px 25px, black 0px 0px 25px"
                               :top         "50%"}}
                      name]]))
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

(defn hero-image [{:keys [desktop-url mobile-url file-name alt]}]
  [:picture
   ;; Tablet/Desktop
   [:source {:media   "(min-width: 750px)"
             :src-set (str desktop-url "-/format/auto/" file-name " 1x")}]
   ;; Mobile
   [:img.block.col-12 {:src (str mobile-url "-/format/auto/" file-name)
                       :alt alt}]])

(defn hero [store-slug]
  [:h1.h2
   [:a
    (assoc (utils/route-to events/navigate-shop-by-look)
           :data-test "home-banner")
    (hero-image {:mobile-url  "//ucarecdn.com/2aaff96b-3b51-4eb2-9c90-2e7806e13b15/"
                 :desktop-url "//ucarecdn.com/c3299c7f-3c17-443d-9d5a-2b60d7ea7a72/"
                 :file-name   "Water-Wave-Yaki-Straight-Homepage-Hero.jpg"
                 :alt         "New Water Wave and Yaki Straight are here"})]])

(defn feature-image [{:keys [desktop-url mobile-url file-name alt]}]
  ;; Assumptions: 2 up, within a .container. Does not account for 1px border.
  ;;          Large End
  ;; Desktop  480px
  ;; Tablet   360px
  ;; Mobile   375px
  [:picture
   ;; Desktop
   [:source {:media   "(min-width: 1000px)"
             :src-set (str desktop-url "-/format/auto/-/resize/480x/" file-name " 1x, "
                           desktop-url "-/format/auto/-/resize/960x/-/quality/lightest/" file-name " 2x")}]
   ;; Tablet
   [:source {:media   "(min-width: 750px)"
             :src-set (str desktop-url "-/format/auto/-/resize/360x/" file-name " 1x, "
                           desktop-url "-/format/auto/-/resize/720x/-/quality/lightest/" file-name " 2x")}]
   ;; Mobile
   [:img.block.col-12 {:src     (str mobile-url "-/format/auto/-/resize/375x/" file-name)
                       :src-set (str mobile-url "-/format/auto/-/resize/750x/-/quality/lightest/" file-name " 2x")
                       :alt     alt}]])

(def feature-blocks
  [:div.container.border-top.border-white
   [:div.col.col-6.border.border-white
    [:a
     (utils/route-to events/navigate-shop-by-look-details {:look-id 177858410})
     (feature-image {:mobile-url  "//ucarecdn.com/d64294f0-e6d0-4b6d-94f1-7da819119c01/"
                     :desktop-url "//ucarecdn.com/04e2b2fc-e40a-4b52-8cb3-ba7bdfa84963/"
                     :file-name   "Shop-Indian-Straight-Hair.jpg"
                     :alt         "Shop Indian Straight Hair"})]]
   [:div.col.col-6.border.border-white
    [:a
     (utils/route-to events/navigate-shop-by-look-details {:look-id 178528561})
     (feature-image {:mobile-url  "//ucarecdn.com/97994826-a043-499e-95a5-549b06ac35b8/"
                     :desktop-url "//ucarecdn.com/811aa020-2d64-4765-ae82-9980117cd6d8/"
                     :file-name   "Shop-Virgin-Hair-Bundle-Deals.jpg"
                     :alt         "Shop Virgin Hair Bundle Deals"})]]])

(defn drop-down-row [opts & content]
  (into [:a.inherit-color.block.center.h5.flex.items-center.justify-center
         (-> opts
             (assoc-in [:style :min-width] "200px")
             (assoc-in [:style :height] "39px"))]
        content))

(defn social-icon [path]
  [:img.ml2 {:style {:height "20px"}
             :src   path}] )

(def ^:private gallery-link
  (component/html
   (drop-down-row
    (utils/route-to events/navigate-gallery)
    "View gallery"
    (social-icon (assets/path "/images/share/stylist-gallery-icon.png")))))

(defn ^:private instagram-link [instagram-account]
  (drop-down-row
   {:href (marquee/instagram-url instagram-account)}
   "Follow on"
   (social-icon (assets/path "/images/share/instagram-icon.png"))))

(defn ^:private styleseat-link [styleseat-account]
  (drop-down-row
   {:href (marquee/styleseat-url styleseat-account)}
   "Book on"
   (social-icon (assets/path "/images/share/styleseat-logotype.png"))))

(defn store-welcome [signed-in {:keys [store-nickname portrait expanded?]} expandable?]
  [:div.flex.justify-center.items-center.border-bottom.border-gray.h5
   {:style {:height "50px"}}
   (case (marquee/portrait-status (-> signed-in ::auth/as (= :stylist)) portrait)
     ::marquee/show-what-we-have [:div.left.pr2 (marquee/stylist-portrait portrait)]
     ::marquee/ask-for-portrait  [:div.left.pr2 marquee/add-portrait-cta]
     ::marquee/show-nothing      [:div.left {:style {:height (str ui/header-image-size "px")}}])
   [:div.dark-gray
    [:span.black store-nickname "'s"] " shop"
    (when expandable?
      [:span.ml1 (ui/expand-icon expanded?)])]])

(defn store-info [signed-in {:keys [expanded?] :as store}]
  (when (-> signed-in ::auth/to (= :marketplace))
    (let [rows (marquee/actions store gallery-link instagram-link styleseat-link)]
      (if-not (boolean (seq rows))
        (store-welcome signed-in store false)
        (ui/drop-down
         expanded?
         keypaths/store-info-expanded
         [:div (store-welcome signed-in store true)]
         [:div.bg-white.absolute.left-0.right-0
          (for [row rows]
            [:div.border-gray.border-bottom row])] )))))

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
         image-src  "https://ucarecdn.com/17256a9e-78ef-4762-a5bd-7096bb9181c9/-/format/auto/testimonialpostervideoplay.jpg"
         video-html (str "<video onClick=\"this.play();\" loop muted poster=\""
                         image-src
                         "\" preload=\"none\" playsinline controls class=\"col-12\"><source src=\""
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

(defn talkable-image [{:keys [desktop-url mobile-url file-name alt]}]
  ;; Assumptions: within a .container. Does not account for 1px border.
  ;;          Large End
  ;; Desktop  960px
  ;; Tablet   720px
  ;; Mobile   750px
  [:picture
   ;; Desktop
   [:source {:media   "(min-width: 1000px)"
             :src-set (str desktop-url "-/format/auto/-/resize/960x/" file-name " 1x, "
                           desktop-url "-/format/auto/-/resize/1920x/-/quality/lightest/" file-name " 2x")}]
   ;; Tablet
   [:source {:media   "(min-width: 750px)"
             :src-set (str desktop-url "-/format/auto/-/resize/720x/" file-name " 1x, "
                           desktop-url "-/format/auto/-/resize/1440x/-/quality/lightest/" file-name " 2x")}]
   ;; Mobile
   [:img.block.col-12 {:src     (str mobile-url "-/format/auto/-/resize/750x/" file-name)
                       :src-set (str mobile-url "-/format/auto/-/resize/1500x/-/quality/lightest/" file-name " 2x")
                       :alt     alt}]])

(def talkable-banner
  (component/html
   [:h1.h2.container.py4
    [:a
     (utils/route-to events/navigate-friend-referrals {:query-params {:traffic_source "homepageBanner"}})
     (talkable-image {:mobile-url  "//ucarecdn.com/42c042cd-2f2d-4acb-bdab-2e7cbea128b2/"
                      :desktop-url "//ucarecdn.com/88b24eee-5389-4caa-93d6-5cd557e56103/"
                      :file-name   "talkable_banner.jpg"
                      :alt         "refer friends, earn rewards, get 20% off"})]]))

(defn component [{:keys [featured-searches signed-in store my-shop-bar?]} owner opts]
  (component/create
   [:div.m-auto
    [:section (hero (:store-slug store))]
    [:section feature-blocks]
    (when my-shop-bar?
      [:section.hide-on-tb-dt (store-info signed-in store)])
    [:section (popular-grid featured-searches)]
    [:section video-autoplay]
    [:section about-mayvenn]
    [:section talkable-banner]]))

(defn query [data]
  {:featured-searches (->> (named-searches/current-named-searches data)
                           (filter (comp #{"straight" "loose-wave" "body-wave" "deep-wave" "curly"} :slug)))
   :store             (marquee/query data)
   :signed-in         (auth/signed-in data)
   :my-shop-bar?      (experiments/my-shop-bar? data)})

(defn built-component [data opts]
  (component/build component (query data) opts))
