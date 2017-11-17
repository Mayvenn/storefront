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
            [clojure.string :as string]
            [spice.date :as date]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.black-friday :as black-friday]))

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
    [:div.container.center.mb4.pb4
     [:div.flex.flex-column.my4
      [:h1.h4.order-2.px2 "100% Virgin Human Hair, always fast and free shipping"]
      [:h2.h1.order-1 "Shop our styles"]]
     [:div
      (for [{:keys [page/slug images name] :as category} categories]
        (grid-block slug
                    [:a.absolute.overlay.overflow-hidden
                     (merge {:data-test (str "category-" slug)}
                            (utils/route-to events/navigate-category category))
                     (category-image (:home images))
                     [:h3.h2.white.absolute.col-12.titleize.mt1
                      {:style {:text-shadow "black 0px 0px 25px, black 0px 0px 25px"
                               :transform "translateY(-50%)"
                               :top "50%"}}
                      (let [[first-word & last-words] (string/split name #" ")]
                        [:div
                         [:div first-word]
                         [:div (string/join " " last-words)]])]]))
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
             :src-set (str desktop-url "-/format/auto/-/quality/best/" file-name " 1x")}]
   ;; Mobile
   [:img.block.col-12 {:src (str mobile-url "-/format/auto/" file-name)
                       :alt alt}]])

(defn hero [store-slug]
  [:h1.h2
   [:a
    (assoc (utils/route-to events/navigate-category {:page/slug "dyed-virgin-hair" :catalog/category-id "16"})
           :data-test "home-banner")
    (let [file-name "Classics-Made-Colorful.jpg"
          alt       "Dyed Virgin Hair is here! Shop dyed virgin hair."]
      (hero-image {:mobile-url  "//ucarecdn.com/138de0ea-3b6e-444e-8215-b869fb48ceab/"
                   :desktop-url "//ucarecdn.com/bb90862b-e578-46dd-9315-ac78c437b3af/"
                   :file-name   file-name
                   :alt         alt}))]])

(defn hero-black-friday [store-slug]
  [:h1.h2
   [:a
    (assoc (utils/route-to events/navigate-shop-bundle-deals)
           :data-test "home-banner")
    (let [file-name "Black-Friday-Deals-Are-Here.jpg"
          alt       "Black Friday Deals Are Here!"]
      (hero-image {:mobile-url  "//ucarecdn.com/a6762023-6285-458a-95a6-0084893958a7/"
                   :desktop-url "//ucarecdn.com/a74795b3-496a-4200-9966-684474459fb8/"
                   :file-name   file-name
                   :alt         alt}))]])

(defn hero-cyber-monday [store-slug]
  [:h1.h2
   [:a
    (assoc (utils/route-to events/navigate-shop-bundle-deals)
           :data-test "home-banner")
    (let [file-name "Cyber-Monday-Deals-Are-Here.jpg"
          alt       "Cyber Monday Deals Are Here!"]
      (hero-image {:mobile-url  "//ucarecdn.com/02c35bae-dafd-4327-b6a6-d8728b0a7da3/"
                   :desktop-url "//ucarecdn.com/b5343ac2-0b15-47c3-ac8b-be669be9927b/"
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

(def black-friday-deals-preview-feature-block
  [:a
   (utils/route-to events/navigate-shop-bundle-deals)
   (feature-image {:mobile-url  "//ucarecdn.com/8ab2db52-1fb4-4309-ab3c-007e5c6b7912/"
                   :desktop-url "//ucarecdn.com/9c2a515c-8cef-41ef-b665-34c01f88930b/"
                   :file-name   "Black-Friday-Deals-Preview.png"
                   :alt         "Black Friday Deals Preview"})])

(def default-left-feature-block
  [:a
   (utils/route-to events/navigate-shop-by-look-details {:look-id (:left config/feature-block-look-ids)})
   (feature-image {:mobile-url  "//ucarecdn.com/0d80edc3-afb2-4a6f-aee1-1cceff1cf93d/"
                   :desktop-url "//ucarecdn.com/54c13e5b-99cd-4dd4-ae00-9f47fc2158e9/"
                   :file-name   "Shop-Brazilian-Straight-10-inch-3-Bundle-Deal.png"
                   :alt         "Shop Brazilian Straight 10 inch 3 Bundle Deal"})])

(def feature-block-to-be-shown-on-black-friday
  [:a
   (utils/route-to events/navigate-category {:catalog/category-id "16"
                                             :page/slug           "dyed-virgin-hair"})
   (feature-image {:mobile-url  "//ucarecdn.com/163230da-c4a8-4352-96de-d025df1eff5d/"
                   :desktop-url "//ucarecdn.com/d7cf940b-1f49-4452-813e-fc1ff474b07e/"
                   :file-name   "Dyed-Virgin-Hair-Is-Here.png"
                   :alt         "Dyed Virgin Hair Is Here!"})])

(defn feature-blocks [black-friday-stage]
  [:div.container.border-top.border-white
   [:div.col.col-6.border.border-white
    (cond
      (#{:black-friday :cyber-monday} black-friday-stage)
      feature-block-to-be-shown-on-black-friday

      (= :black-friday-run-up black-friday-stage) ;; Make this the :else case before deploy
      black-friday-deals-preview-feature-block

      :else
      default-left-feature-block)]
    [:div.col.col-6.border.border-white
     [:a
      (utils/route-to events/navigate-shop-by-look-details {:look-id (:right config/feature-block-look-ids)})
      (feature-image {:mobile-url  "//ucarecdn.com/dc7a8c34-ab77-45b2-bc5d-ffe48be3f8e6/"
                      :desktop-url "//ucarecdn.com/979eb309-adbd-40c4-9b10-44c3e866983a/"
                      :file-name   "Shop-Malaysian-Body-Wave-10-inch-3-Bundle-Deal.png"
                      :alt         "Shop Malaysian Body Wave 10 inch 3 Bundle Deal"})]]])

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

(defn component [{:keys [signed-in store categories hero-fn black-friday-stage]} owner opts]
  (component/create
   [:div.m-auto
    [:section (hero-fn (:store-slug store))]
    [:section (feature-blocks black-friday-stage)]
    [:section.hide-on-tb-dt (store-info signed-in store)]
    [:section (popular-grid categories)]
    [:section video-autoplay]
    [:section about-mayvenn]
    [:section talkable-banner]]))

(defn query [data]
  (let [black-friday-stage (black-friday/stage data)]
    {:store              (marquee/query data)
     :signed-in          (auth/signed-in data)
     :categories         (->> (get-in data keypaths/categories)
                              (filter :home/order)
                              (sort-by :home/order))
     :black-friday-stage black-friday-stage
     :hero-fn            (condp = black-friday-stage
                           :black-friday hero-black-friday
                           :cyber-monday hero-cyber-monday
                           hero)}))

(defn built-component [data opts]
  (component/build component (query data) opts))
