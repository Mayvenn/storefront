(ns storefront.components.home
  (:require [clojure.string :as string]
            [storefront.accessors.auth :as auth]
            [storefront.accessors.experiments :as experiments]
            [storefront.assets :as assets]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.marquee :as marquee]
            [storefront.components.v2-home :as v2-home]
            [storefront.components.homepage-hero :as homepage-hero]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.routes :as routes]
            adventure.shop-home
            [ui.molecules :as ui.M]))

(defn product-image
  [{:keys [resizable_url resizable_filename alt]}]
  ;; Assumptions: 2 up on mobile, 3 up on tablet/desktop, within a .container. Does not account for 1px border.
  [:img.block.col-12 {:src     (str resizable_url "-/format/auto/-/resize/750x/-/quality/lightest/" resizable_filename)
                      :src-set (str resizable_url "-/format/auto/-/resize/750x/-/quality/lightest/" resizable_filename " 750w, "
                                    resizable_url "-/format/auto/-/resize/640x/-/quality/lightest/" resizable_filename " 640w")
                      :sizes   (str "(min-width: 1000px) 320px, "
                                    "(min-width: 750px) 240px, "
                                    "50vw")
                      :alt     alt}])

(defn category-image
  [{:keys [url filename alt]}]
  (component/html
   ;; Assumptions: 2 up on mobile, 3 up on tablet/desktop, within a .container. Does not account for 1px border.
   [:img.block.col-12 {:src     (str url "-/format/auto/-/resize/750x/-/quality/lightest/" filename)
                       :src-set (str url "-/format/auto/-/resize/750x/-/quality/lightest/" filename " 750w, "
                                     url "-/format/auto/-/resize/640x/-/quality/lightest/" filename " 640w")
                       :sizes   (str "(min-width: 1000px) 320px, "
                                     "(min-width: 750px) 240px, "
                                     "50vw")
                       :alt     alt}]))
(defn popular-grid [categories]
  (component/html
   (let [grid-block (fn [key content]
                      (component/html
                       [:div.col.col-6.col-4-on-tb-dt.border.border-white {:key key}
                        ^:inline (ui/aspect-ratio 4 3 content)]))]
     [:div.container.center.pb4
      [:div.flex.flex-column.py4
       [:h1.h4.order-2.px2
        "Human hair extensions, free shipping, free returns, and a 30 day guarantee"]
       [:h2.h1.order-1 "Shop Popular Styles"]]
      [:div
       (for [{:keys [page/slug images copy/title] :as category} categories]
         ^:inline
         (grid-block slug
                     [:a.absolute.overlay.overflow-hidden
                      ^:attrs (merge {:data-test (str "category-" slug)}
                                     (utils/route-to events/navigate-category category))
                      ^:inline (category-image (:home images))
                      [:h3.h2.white.absolute.col-12.titleize.mt1
                       {:style {:text-shadow "black 0px 0px 25px, black 0px 0px 25px"
                                :transform "translateY(-75%)"
                                :top "75%"}}
                       (let [[first-word & last-words] (string/split title #" ")]
                         [:div
                          [:div first-word]
                          [:div (string/join " " last-words)]])]]))
       ^:inline (grid-block "spare-block"
                   [:a.bg-cool-gray.white.absolute.overlay
                    ^:attrs (assoc (utils/route-to events/navigate-shop-by-look {:album-keyword :look})
                                   :data-test "nav-shop-look")
                    [:div.flex.container-size.justify-center.items-center
                     [:h3.hide-on-tb-dt
                      [:div "Need inspiration?"]
                      [:div "Try shop by look."]]
                     [:h3.h2.hide-on-mb
                      [:div "Need inspiration?"]
                      [:div "Try shop by look."]]]])]])))

(def free-installation-hero-data
  {:route-to-fn  (utils/route-to events/navigate-shop-by-look
                                 {:album-keyword :look
                                  :query-params  {:sha "freeinstall"}})
   :file-name    "free-installation-hero"
   :alt          "Free Mayvenn Install when you buy 3 bundles or more! Use code: FREEINSTALL"
   :mobile-uuid  "990f17ce-c786-450b-9556-419858ae43df"
   :desktop-uuid "e7cb3f57-e718-4964-be9f-1e2ec186dc1d"})

(defn feature-image [{:keys [desktop-url mobile-url file-name alt]}]
  ;; Assumptions: 2 up, within a .container. Does not account for 1px border.
  ;;          Large End
  ;; Desktop  480px
  ;; Tablet   360px
  ;; Mobile   375px
  (component/html
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
                        :alt     alt}]]))

(defn feature-block [{:keys [desktop mobile alt path]}]
  ;; Assumptions: 2 up, within a .container. Does not account for 1px border.
  ;;          Large End
  ;; Desktop  480px
  ;; Tablet   360px
  ;; Mobile   375px
  ;;
  (let [mobile-url  (-> mobile :file :url)
        desktop-url (-> desktop :file :url)
        tablet-url  (-> desktop :file :url)]
    (component/html
     [:div.col.col-12.col-4-on-tb-dt.border.border-white
      [:a ^:attrs (apply utils/route-to (routes/navigation-message-for path))
       [:picture
        (for [img-type ["webp" "jpg"]
              src      [(ui/source desktop-url
                                   {:media   "(min-width: 1000px)"
                                    :type    img-type
                                    :src-set {"1x" {:w "625"}}})
                        (ui/source tablet-url
                                   {:media   "(min-width: 750px)"
                                    :type    img-type
                                    :src-set {"1x" {:w "625"
                                                    :q "75"}
                                              "2x" {:w "720"
                                                    :q "75"}}})
                        (ui/source mobile-url
                                   {:media   "(min-width: 376px)"
                                    :type    img-type
                                    :src-set {"1x" {:w "750"
                                                    :q "75"}
                                              "2x" {:w "1500"
                                                    :q "50"}}})
                        (ui/source mobile-url
                                   {:type    img-type
                                    :src-set {"1x" {:w "375"
                                                    :q "75"}
                                              "2x" {:w "750"
                                                    :q "50"}}})]]
          ^:inline src)
        ;; Fallback
        [:img.block.col-12 {:src (str mobile-url "?w=625&fm=jpg")
                            :alt alt}]]]])))

(defn feature-blocks
  [{:as features :keys [feature-1 feature-2 feature-3]}]
  (component/html
   (if (seq features)
     (let [{:keys [feature-1 feature-2 feature-3]} features]
       [:section
        [:div.container.border-top.border-white
         [:div.col.col-12.my4 [:h1.center "Shop What's New"]]
         ^:inline (feature-block feature-1)
         ^:inline (feature-block feature-2)
         ^:inline (feature-block feature-3)]])
     [:section])))

(defn drop-down-row [opts & content]
  (into [:a.inherit-color.block.center.h5.flex.items-center.justify-center
         (-> opts
             (assoc-in [:style :min-width] "200px")
             (assoc-in [:style :height] "39px"))]
        content))

(defn social-icon [ucare-uuid]
  (ui/ucare-img {:class "ml2"
                 :style {:height "20px"}} ucare-uuid))

(defn ^:private gallery-link []
  (component/html
   (drop-down-row
    (utils/route-to events/navigate-store-gallery)
    "View gallery"
    (social-icon "fa4eefff-7856-4a1b-8cdb-c8b228b62967"))))

(defn ^:private instagram-link [instagram-account]
  (drop-down-row
   {:href (marquee/instagram-url instagram-account)}
   "Follow on"
   (social-icon "1a4a3bd5-0fda-45f2-9bb4-3739b911390f")))

(defn ^:private styleseat-link [styleseat-account]
  (drop-down-row
   {:href (marquee/styleseat-url styleseat-account)}
   "Book on"
   (social-icon "c8f0a4b8-24f7-4de8-9c20-c6634b865bc1")))

(defn store-welcome [signed-in {:keys [store-nickname portrait expanded?]} expandable?]
  (component/html
   [:div.flex.justify-center.items-center.border-bottom.border-gray.h5
    {:style {:height "50px"}}
    (case (marquee/portrait-status (auth/stylist-on-own-store? signed-in) portrait)
      ::marquee/show-what-we-have [:div.left.pr2 (marquee/stylist-portrait portrait)]
      ::marquee/ask-for-portrait  [:div.left.pr2 marquee/add-portrait-cta]
      ::marquee/show-nothing      [:div.left {:style {:height (str ui/header-image-size "px")}}])
    [:div (str store-nickname "'s shop")
     (when expandable?
       [:span.ml1 ^:inline (ui/expand-icon expanded?)])]]))

(defn store-info [signed-in {:keys [expanded?] :as store}]
  (when (-> signed-in ::auth/to (= :marketplace))
    (let [rows (marquee/actions store (gallery-link) instagram-link styleseat-link)]
      (if-not (boolean (seq rows))
        (store-welcome signed-in store false)
        (ui/drop-down
         expanded?
         keypaths/store-info-expanded
         [:div (store-welcome signed-in store true)]
         [:div.bg-white.absolute.left-0.right-0
          (for [[index row] (map-indexed vector rows)]
            [:div.border-gray.border-bottom {:key (str "action-row-" index)} row])])))))

(defn about-mayvenn []
  (component/html
   [:div.container.py1.my1.pt2-on-tb-dt.mt4-on-tb-dt.center.mb1-on-tb-dt.center
    [:h1.line-length.mx-auto.pb4 "Why people love Mayvenn"]
    [:div.clearfix.h5.center
     [:div.col-on-tb-dt.col-4-on-tb-dt.p3
      [:img.mb1 {:src    "//ucarecdn.com/83a8e87b-a16f-4d22-b294-e11616088ced/-/format/auto/iconstylistreco"
                 :width  "48px"
                 :height "70px"}]
      [:h3.h2 "Stylist Recommended"]
      [:p.h5 "Mayvenn hair is the #1 recommended hair company by over 100,000 hair stylists across the country, making it the most trusted hair brand on the market."]
      [:a.h5.p-color (assoc (utils/route-to events/navigate-shop-by-look {:album-keyword :look})
                            :data-test "nav-shop-look")
       "Shop our looks"]]

     [:div.col-on-tb-dt.col-4-on-tb-dt.p3
      [:img.mb1 {:src    "//ucarecdn.com/a16d80a5-7bcf-4ec9-a5fc-5f8642d1542d/-/format/auto/icon30dayguarantee"
                 :width  "48px"
                 :height "70px"}]
      [:h3.h2 "30 Day Guarantee"]
      [:p.h5 "Try the best quality hair on the market risk free! Wear it, dye it, even cut it. If you’re not happy with your bundles, we will exchange them within 30 days for FREE!"]
      [:a.h5.p-color (assoc (utils/route-to events/navigate-content-guarantee)
                            :data-test "nav-our-guarantee")
       "Learn more about Our Guarantee"]]

     [:div.col-on-tb-dt.col-4-on-tb-dt.p3
      [:div.flex.justify-center {:style {:height "80px"}}
       [:img.self-center {:src    "//ucarecdn.com/647b33d9-a175-406c-82e0-3723c6767757/-/format/auto/iconfastfreeshipping"
                          :height "44px"}]]
      [:h3.h2 "Fast Free Shipping"]
      [:p.h5 "Mayvenn offers free standard shipping on all orders, no minimum necessary. In a hurry? Expedited shipping options are available for those who just can’t wait."]
      [:a.h5.p-color (assoc (utils/route-to events/navigate-shop-by-look {:album-keyword :look})
                            :data-test "nav-shop-look")
       "Shop our looks"]]]]))

(defn video-autoplay []
  (let [video-src  "https://embedwistia-a.akamaihd.net/deliveries/e88ed4645e104735c3bdcd095c370e5ccb1e1ef4/file.mp4"
        image-src  "https://ucarecdn.com/17256a9e-78ef-4762-a5bd-7096bb9181c9/-/format/auto/testimonialpostervideoplay"
        video-html (str "<video onClick=\"this.play();\" loop muted poster=\""
                        image-src
                        "\" preload=\"none\" playsinline controls class=\"col-12\"><source src=\""
                        video-src
                        "\"></source></video>")]
    (component/html
     [:div
      [:div.center.hide-on-tb-dt.mbn2
       [:div.px3
        [:h1 "We Love Our Customers"]
        [:p.h4 "And they love Mayvenn! Watch and see why they absolutely love wearing our hair."]]
       [:div.container.col-12.mx-auto.mt6 {:dangerouslySetInnerHTML {:__html video-html}}]]
      [:div.center.bg-p-color.py4.white.hide-on-mb
       [:h1.mt1 "We love our customers"]
       [:p.h4 "And they love Mayvenn! Watch and see why they absolutely love wearing our hair."]
       [:div.container.col-12.mx-auto.mt6.mb4 {:dangerouslySetInnerHTML {:__html video-html}}]]])))

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

(defcomponent component
  [{:component/keys [features hero-data]
    :keys           [signed-in store categories] :as data} _ _]
  [:div.m-auto
   [:section
    [:h1.h2 ^:inline (component/build ui.molecules/hero hero-data)]]
   [:section.hide-on-tb-dt (store-info signed-in store)]
   ^:inline (feature-blocks features)
   [:section ^:inline (popular-grid categories)]
   [:section ^:inline (video-autoplay)]
   [:section ^:inline (about-mayvenn)]
   [:section ^:inline (component/build ui.M/hero
                                       {:mob-uuid  "762369fb-6680-4e0a-bf99-4e6317f03f1d"
                                        :dsk-uuid  "b11d90d3-ed57-4c18-a61b-d91b68e1cccb"
                                        :file-name "talkable_banner_25.jpg"
                                        :alt       "refer friends, earn rewards, get 25% off"
                                        :opts      (utils/route-to events/navigate-friend-referrals
                                                                   {:query-params {:campaign_tags  "25-off"
                                                                                   :traffic_source "homepageBanner"}})}
                                       nil)]])

(defn query
  [data]
  (let [homepage-cms (some-> data (get-in keypaths/cms-homepage) :classic)]
    {:store               (marquee/query data)
     :signed-in           (auth/signed-in data)
     :categories          (->> (get-in data keypaths/categories)
                               (filter :home/order)
                               (sort-by :home/order))
     :component/hero-data (homepage-hero/query (:hero homepage-cms))
     :component/features  (select-keys homepage-cms [:feature-1 :feature-2 :feature-3])}))

(defn built-component [data opts]
  (cond
    (experiments/v2-homepage? data)
    (v2-home/built-component data opts)

    (= "shop" (get-in data keypaths/store-slug))
    (adventure.shop-home/built-component data opts)

    :else
    (component/build component (query data) opts)))
