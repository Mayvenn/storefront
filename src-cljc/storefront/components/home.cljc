(ns storefront.components.home
  (:require [storefront.platform.component-utils :as utils]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.accessors.auth :as auth]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.components.ui :as ui]
            [storefront.components.marquee :as marquee]
            [storefront.assets :as assets]
            [storefront.config :as config]
            [storefront.accessors.experiments :as experiments]
            [storefront.routes :as routes]
            [clojure.string :as string]
            [spice.date :as date]))

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
  ;; Assumptions: 2 up on mobile, 3 up on tablet/desktop, within a .container. Does not account for 1px border.
  [:img.block.col-12 {:src     (str url "-/format/auto/-/resize/750x/-/quality/lightest/" filename)
                      :src-set (str url "-/format/auto/-/resize/750x/-/quality/lightest/" filename " 750w, "
                                    url "-/format/auto/-/resize/640x/-/quality/lightest/" filename " 640w")
                      :sizes   (str "(min-width: 1000px) 320px, "
                                    "(min-width: 750px) 240px, "
                                    "50vw")
                      :alt     alt}])
(defn popular-grid [categories]
  (let [grid-block (fn [key content]
                     [:div.col.col-6.col-4-on-tb-dt.border.border-white {:key key}
                      (ui/aspect-ratio 4 3 content)])]
    [:div.container.center.pb4
     [:div.flex.flex-column.py4
      [:h1.h4.order-2.px2
       "Human hair extensions, free shipping, free returns, and a 30 day guarantee"]
      [:h2.h1.order-1 "Shop Popular Styles"]]
     [:div
      (for [{:keys [page/slug images copy/title] :as category} categories]
        (grid-block slug
                    [:a.absolute.overlay.overflow-hidden
                     (merge {:data-test (str "category-" slug)}
                            (utils/route-to events/navigate-category category))
                     (category-image (:home images))
                     [:h3.h2.white.absolute.col-12.titleize.mt1
                      {:style {:text-shadow "black 0px 0px 25px, black 0px 0px 25px"
                               :transform "translateY(-75%)"
                               :top "75%"}}
                      (let [[first-word & last-words] (string/split title #" ")]
                        [:div
                         [:div first-word]
                         [:div (string/join " " last-words)]])]]))
      (grid-block "spare-block"
                  [:a.bg-light-teal.white.absolute.overlay
                   (assoc (utils/route-to events/navigate-shop-by-look {:album-slug "look"})
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
             :src-set (str desktop-url "-/format/auto/-/quality/best/" file-name " 1x")}]
   ;; Mobile
   [:img.block.col-12 {:src (str mobile-url "-/format/auto/" file-name)
                       :alt alt}]])
(def hero
  [:h1.h2
   [:a
    (assoc (utils/route-to events/navigate-shop-by-look {:album-slug "look"})
           :data-test "home-banner")
    (let [file-name "Hair-Always-On-Beat-Fling.jpg"
          alt       "Hair always on beat! 25% off everything! Shop looks!"
          mob-uuid  "567ec976-be95-416f-a8ca-e49b3aefad8a"
          dsk-uuid  "eb9fb12d-cd4b-4348-bd49-dc529a93e3ef"]
      (hero-image {:mobile-url  (str "//ucarecdn.com/" mob-uuid "/")
                   :desktop-url (str "//ucarecdn.com/" dsk-uuid "/")
                   :file-name   file-name
                   :alt         alt}))]])

(defn cms-hero [{:keys [hero] :as homepage-data}]
  (when (seq homepage-data)
    (let [{:keys [mobile desktop alt path]} hero
          mobile-url                        (-> mobile :file :url)
          desktop-url                       (-> desktop :file :url)]
      [:h1.h2
       [:a (assoc (apply utils/route-to (routes/navigation-message-for path))
                  :data-test "home-banner")
        [:picture
         ;; Tablet/Desktop
         (for [img-type ["webp" "jpg"]]
           [(ui/source desktop-url
                       {:media   "(min-width: 750px)"
                        :src-set {"1x" {}}
                        :type    img-type})
            (ui/source mobile-url
                       {:src-set {"1x" {}}
                        :type    img-type})])
         [:img.block.col-12 {:src mobile-url :alt alt}]]]])))

(def free-installation-hero
  [:h1.h2
   [:a
    (assoc (utils/route-to events/navigate-shop-by-look {:album-slug "look"
                                                         :query-params {:sha "freeinstall"}})
           :data-test "home-banner")
    (let [file-name "free-installation-hero"
          alt       "Free install when you buy 3 bundles or more! Use code: FREEINSTALL"
          mob-uuid  "990f17ce-c786-450b-9556-419858ae43df"
          dsk-uuid  "e7cb3f57-e718-4964-be9f-1e2ec186dc1d"]
      (hero-image {:mobile-url  (str "//ucarecdn.com/" mob-uuid "/")
                   :desktop-url (str "//ucarecdn.com/" dsk-uuid "/")
                   :file-name   file-name
                   :alt         alt}))]])

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

(defn feature-blocks []
  (let [block :div.col.col-12.col-4-on-tb-dt.border.border-white]
    [:div.container.border-top.border-white
     [:div.col.col-12.my4 [:h1.center "Shop What's New"]]
     [block [:a (utils/route-to events/navigate-category {:catalog/category-id "16"
                                                          :page/slug           "dyed-virgin-hair"})
             (feature-image {:mobile-url  "//ucarecdn.com/3fa4212f-31a2-4525-a53f-f7aa988be858/"
                             :desktop-url "//ucarecdn.com/a7f2d90b-3c51-4b53-935f-92600392d345/"
                             :file-name   "Dyed-Virgin-Hair-Is-Here.png"
                             :alt         "Dyed Virgin Hair Is Here!"})]]
     [block [:a (utils/route-to events/navigate-category {:page/slug "dyed-100-human-hair" :catalog/category-id "19"})
             (feature-image {:mobile-url  "//ucarecdn.com/a1857e33-7536-48f7-8edc-fccb08b718b7/"
                             :desktop-url "//ucarecdn.com/f4a97396-b98b-4f62-b918-75c5d60d3315/"
                             :file-name   "dyed-100-human-hair.png"
                             :alt         "Dyed 100% Human Hair - Starting at $30!"})]]
     [block [:a (utils/route-to events/navigate-category {:catalog/category-id "21"
                                                          :page/slug           "seamless-clip-ins"})
             (feature-image {:mobile-url  "//ucarecdn.com/3fdbe21b-6826-4fb1-a8a3-eb73a37113c3/"
                             :desktop-url "//ucarecdn.com/766bf2c0-63d1-4aec-840f-f993928ae20e/"
                             :file-name   "clip-ins-9-colors-2-textures.png"
                             :alt         "Clip-ins available in 9 colors, 2 textures!"})]]]))

(defn cms-feature-block [{:keys [desktop mobile alt path]}]
  ;; Assumptions: 2 up, within a .container. Does not account for 1px border.
  ;;          Large End
  ;; Desktop  480px
  ;; Tablet   360px
  ;; Mobile   375px
  ;;
  (let [mobile-url  (-> mobile :file :url)
        desktop-url (-> desktop :file :url)
        tablet-url  (-> desktop :file :url)]
    [:div.col.col-12.col-4-on-tb-dt.border.border-white
     [:a (apply utils/route-to (routes/navigation-message-for path))
      [:picture
       (for [img-type ["webp" "jpg"]]
         [(ui/source desktop-url
                  {:media   "(min-width: 1000px)"
                   :type    img-type
                   :src-set {"1x" {:w "480"}
                             "2x" {:w "960"
                                   :q "50"}}})
          (ui/source tablet-url
                  {:media   "(min-width: 750px)"
                   :type    img-type
                   :src-set {"1x" {:w "360"}
                             "2x" {:w "720"
                                   :q "50"}}})
          (ui/source mobile-url
                  {:type    img-type
                   :src-set {"1x" {:w "375"}
                             "2x" {:w "750"
                                   :q "50"}}})])
       ;; Fallback
       [:img.block.col-12 {:src (str mobile-url "?w=375&fm=jpg")
                           :alt alt}]]]]))

(defn cms-feature-blocks
  [{:as homepage-data :keys [feature-1 feature-2 feature-3]}]
  (when (seq homepage-data)
    [:div.container.border-top.border-white
     [:div.col.col-12.my4 [:h1.center "Shop What's New"]]
     (cms-feature-block feature-1)
     (cms-feature-block feature-2)
     (cms-feature-block feature-3)]))

(defn drop-down-row [opts & content]
  (into [:a.inherit-color.block.center.h5.flex.items-center.justify-center
         (-> opts
             (assoc-in [:style :min-width] "200px")
             (assoc-in [:style :height] "39px"))]
        content))

(defn social-icon [path]
  [:img.ml2 {:style {:height "20px"}
             :src   path}])

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
            [:div.border-gray.border-bottom row])])))))

(def about-mayvenn
  (component/html
   [:div.container.py1.my1.pt2-on-tb-dt.mt4-on-tb-dt.center.mb1-on-tb-dt.center
    [:h1.line-length.mx-auto.pb4 "Why people love Mayvenn"]
    [:div.clearfix.h5.center
     [:div.col-on-tb-dt.col-4-on-tb-dt.p3
      [:img.mb1 {:src    "//ucarecdn.com/83a8e87b-a16f-4d22-b294-e11616088ced/iconstylistreco.png"
                 :width  "48px"
                 :height "70px"}]
      [:h3.h2 "Stylist Recommended"]
      [:p.h5 "Mayvenn hair is the #1 recommended hair company by over 100,000 hair stylists across the country, making it the most trusted hair brand on the market."]
      [:a.h5.teal (assoc (utils/route-to events/navigate-shop-by-look {:album-slug "look"})
                         :data-test "nav-shop-look")
       "Shop our looks"]]

     [:div.col-on-tb-dt.col-4-on-tb-dt.p3
      [:img.mb1 {:src    "//ucarecdn.com/a16d80a5-7bcf-4ec9-a5fc-5f8642d1542d/icon30dayguarantee.png"
                 :width  "48px"
                 :height "70px"}]
      [:h3.h2 "30 Day Guarantee"]
      [:p.h5 "Try the best quality hair on the market risk free! Wear it, dye it, even cut it. If you’re not happy with your bundles, we will exchange them within 30 days for FREE!"]
      [:a.h5.teal (assoc (utils/route-to events/navigate-content-guarantee)
                         :data-test "nav-our-guarantee")
       "Learn more about Our Guarantee"]]

     [:div.col-on-tb-dt.col-4-on-tb-dt.p3
      [:div.flex.justify-center {:style {:height "80px"}}
       [:img.self-center {:src    "//ucarecdn.com/647b33d9-a175-406c-82e0-3723c6767757/iconfastfreeshipping.png"
                          :height "44px"}]]
      [:h3.h2 "Fast Free Shipping"]
      [:p.h5 "Mayvenn offers free standard shipping on all orders, no minimum necessary. In a hurry? Expedited shipping options are available for those who just can’t wait."]
      [:a.h5.teal (assoc (utils/route-to events/navigate-shop-by-look {:album-slug "look"})
                         :data-test "nav-shop-look")
       "Shop our looks"]]]]))

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
        [:h1 "We Love Our Customers"]
        [:p.h4 "And they love Mayvenn! Watch and see why they absolutely love wearing our hair."]]
       [:div.container.col-12.mx-auto.mt6 {:dangerouslySetInnerHTML {:__html video-html}}]]
      [:div.center.bg-teal.py4.white.hide-on-mb
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

(def talkable-banner
  (component/html
   [:a
    (utils/route-to events/navigate-friend-referrals {:query-params {:traffic_source "homepageBanner"}})
    (hero-image {:mobile-url  "//ucarecdn.com/762369fb-6680-4e0a-bf99-4e6317f03f1d/"
                 :desktop-url "//ucarecdn.com/b11d90d3-ed57-4c18-a61b-d91b68e1cccb/"
                 :file-name   "talkable_banner_25.jpg"
                 :alt         "refer friends, earn rewards, get 25% off"})]))

(defn component [{:keys [signed-in store categories hero-element feature-elements]} owner opts]
  (component/create
   [:div.m-auto
    [:section hero-element]
    [:section.hide-on-tb-dt (store-info signed-in store)]
    [:section feature-elements]
    [:section (popular-grid categories)]
    [:section video-autoplay]
    [:section about-mayvenn]
    [:section talkable-banner]]))

(defn query [data]
  (let [the-ville?    (experiments/the-ville? data)
        cms?          (experiments/cms? data)
        homepage-data (get-in data keypaths/cms-homepage)]
    {:store           (marquee/query data)
     :signed-in       (auth/signed-in data)
     :categories      (->> (get-in data keypaths/categories)
                           (filter :home/order)
                           (sort-by :home/order))
     :hero-element    (cond
                        the-ville? free-installation-hero
                        cms?       (cms-hero homepage-data)
                        :else      hero)
     :feature-elements (if cms?
                        (cms-feature-blocks homepage-data)
                        (feature-blocks))}))

(defn built-component [data opts]
  (component/build component (query data) opts))
