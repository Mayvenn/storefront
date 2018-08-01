(ns storefront.components.home-aladdin
  (:require [clojure.string :as string]
            [storefront.accessors.auth :as auth]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.pixlee :as pixlee]
            #?(:cljs [storefront.hooks.pixlee :as pixlee.hook])
            [storefront.assets :as assets]
            [storefront.component :as component]
            [storefront.components.marquee :as marquee]
            [storefront.components.accordion :as accordion]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.components.modal-gallery :as modal-gallery]
            [storefront.transitions :as transitions]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.carousel :as carousel]
            [storefront.routes :as routes]
            [storefront.components.video :as video]
            [storefront.effects :as effects]
            [storefront.components.money-formatters :as mf]
            [storefront.components.aladdin :as aladdin]))

(defn hero-image [{:keys [desktop-url mobile-url file-name alt]}]
  [:picture
   ;; Tablet/Desktop
   [:source {:media   "(min-width: 750px)"
             :src-set (str desktop-url "-/format/auto/-/quality/best/" file-name " 1x")}]
   ;; Mobile
   [:img.block.col-12 {:src (str mobile-url "-/format/auto/" file-name)
                       :alt alt}]])

(defn hero []
  (let [file-name "free-install-hero"
        mob-uuid  "5980306e-71f9-4dca-984c-d816f79c9f98"
        dsk-uuid  "2acd6074-c481-4bbd-8667-151b5706d609"]
    [:div.bold.shadow.white.center.bg-light-gray
     (hero-image {:mobile-url  (str "//ucarecdn.com/" mob-uuid "/")
                  :desktop-url (str "//ucarecdn.com/" dsk-uuid "/")
                  :file-name   file-name
                  :alt         "Beautiful Virgin Hair Installed for FREE"})]))

(def free-shipping-banner
  [:div {:style {:height "3em"}}
   [:div.bg-black.flex.items-center.justify-center
    {:style {:height "2.25em"
             :margin-top "-1px"
             :padding-top "1px"}}
    [:div.px2
     (ui/ucare-img {:alt "" :height "25"}
                   "38d0a770-2dcd-47a3-a035-fc3ccad11037")]
    [:div.h6.white.light
     "FREE standard shipping"]]])

(def teal-play-video-mobile
  (svg/white-play-video {:class  "mr1 fill-teal"
                         :height "30px"
                         :width  "30px"}))

(def teal-play-video-desktop
  (svg/white-play-video {:class  "mr1 fill-teal"
                         :height "41px"
                         :width  "41px"}))

(def what-our-customers-are-saying
  (let [video-link (utils/route-to events/navigate-home {:query-params {:video "free-install"}})]
    [:div.col-11.mx-auto
     [:div.hide-on-mb-tb.flex.justify-center.py3
      [:a.block.relative
       video-link
       (ui/ucare-img {:alt "" :width "212"}
                     "b016b985-affb-4c97-af0a-a1f1334c0c51")
       [:div.absolute.top-0.bottom-0.left-0.right-0.flex.items-center.justify-center.bg-darken-3
        teal-play-video-desktop]]
      [:a.block.ml4.dark-gray
       video-link
       [:div.h4.bold "#MayvennFreeInstall"]
       [:div.h4.my2 "See why customers love their FREE Install"]
       [:div.h5.teal.flex.items-center "WATCH NOW"]]]

     [:div.hide-on-dt.flex.justify-center.py3
      [:a.block.relative
       video-link
       (ui/ucare-img {:alt "" :width "152"}
                     "b016b985-affb-4c97-af0a-a1f1334c0c51")
       [:div.absolute.top-0.bottom-0.left-0.right-0.flex.items-center.justify-center.bg-darken-3
        teal-play-video-mobile]]
      [:a.block.ml2.dark-gray
       video-link
       [:h6.bold.mbnp6 "#MayvennFreeInstall"]
       [:p.pt2.h7
        [:span {:style {:white-space "nowrap"}} "See why customers love their"]
        " "
        [:span {:style {:white-space "nowrap"}} "FREE Install"]]
       [:h6.teal.flex.items-center
        "WATCH NOW"]]]]))

(defn carousel-slide [{:as pixlee-image :keys [look-attributes links]}]
  [:div
   [:div.relative.m1
    (apply utils/route-to (:view-look links))
    (ui/aspect-ratio
     1 1
     [:img {:class "col-12"
            :src   (-> pixlee-image :imgs :original :src)}])
    (when-let [texture (:texture look-attributes)]
      [:div.absolute.flex.justify-end.bottom-0.right-0.mb2
       [:div {:style {:width       "0"
                      :height      "0"
                      :border-top  "28px solid rgba(159, 229, 213, 0.8)"
                      :border-left "21px solid transparent"}}]
       [:div.flex.items-center.px2.medium.h6.bg-transparent-light-teal
        texture]])]
   [:div.h6.mx1.mt1.dark-gray.medium (:price look-attributes)]])

(defn ^:private shop-button [album-keyword link-text]
  (ui/teal-button (merge
                   {:height-class "py2"}
                   (utils/route-to events/navigate-shop-by-look {:album-keyword album-keyword}))
                  [:span.bold
                   (str "Shop " link-text " Looks")]))

(defn ^:private style-carousel-component [images]
  (component/build carousel/component
                   {:slides   (mapv carousel-slide images)
                    :settings {:slidesToShow 2
                               :swipe        true
                               :arrows       true}}
                   {}))

(defn style-carousel [styles treatments origins link-text {:as ugc :keys [album-keyword]} album-first?]
  [:div.col-12.mt4
   [:div.hide-on-dt
    [:h5.bold.center styles]
    [:div.h6.center.mb2.dark-gray treatments [:span.px2 "•"] origins]
    (style-carousel-component (:images ugc))
    [:div.col-8.mx-auto.mt2
     (shop-button album-keyword link-text)]]

   [:div.hide-on-mb-tb
    [:div.flex.flex-wrap.justify-between
     [:div.col-4.flex.justify-center.items-center.flex-column.px2
      [:h4.medium.center styles]
      [:div.h5.center.mb2.dark-gray.col-12
       treatments [:span "•"] origins]

      [:div.mx-auto.mt2 (shop-button album-keyword link-text)]]

     [:div.col-8 (when album-first? {:style {:order -1}})
      (style-carousel-component (:images ugc))]]]])

(defn most-popular-looks [sleek-ugc wave-ugc]
  [:div.col-12.col-8-on-tb-dt.mt3.px2.py5.mx-auto
   [:div.my2.flex.flex-column.items-center
    [:h2.center "Our Most Popular Looks"]
    (style-carousel "Sleek & Straight"
                    "Virgin & Dyed Virgin Hair"
                    "Brazilian & Peruvian"
                    "Straight"
                    sleek-ugc
                    true)

    [:hr.hide-on-mb-tb.border-top.border-dark-silver.col-12.mx-auto.my6]

    (style-carousel "Wavy & Curly"
                    "Virgin & Dyed Virgin Hair"
                    "Brazilian, Malaysian & Peruvian"
                    "Wavy & Curly"
                    wave-ugc
                    false)]])

(defn free-install-mayvenn-grid [free-install-mayvenn-ugc]
  [:div.py8.col-10.mx-auto
   [:h2.center "#MayvennFreeInstall"]
   [:h6.center.dark-gray "Show off your look by tagging us with #MayvennFreeInstall"]
   [:div.flex.flex-wrap.pt2
    (for [{:keys [links imgs]} (:images free-install-mayvenn-ugc)]
      [:a.col-6.col-3-on-tb-dt.p1
       (when-let [view-look (:view-look links)]
         (apply utils/route-to view-look))
       (ui/aspect-ratio
        1 1
        [:img {:class "col-12"
               :src   (-> imgs :original :src)}])])]])

(def our-story
  (let [we-are-mayvenn-link (utils/route-to events/navigate-home {:query-params {:video "we-are-mayvenn"}})
        diishan-image       (ui/ucare-img {:class "col-12"} "e2186583-def8-4f97-95bc-180234b5d7f8")
        mikka-image         (ui/ucare-img {:class "col-12"} "838e25f5-cd4b-4e15-bfd9-8bdb4b2ac341")
        stylist-image       (ui/ucare-img {:class "col-12"} "6735b4d5-9b65-4fa9-96cd-871141b28672")
        diishan-image-2     (ui/ucare-img {:class "col-12"} "ec9e0533-9eee-41ae-a61b-8dc22f045cb5")]
    [:div.pt4.px4.pb8
     [:div.h2.center "A Better Hair Experience Starts Here"]
     [:h6.center.mb2.dark-gray "Founded in Oakland, CA • 2013"]

     [:div.hide-on-tb-dt
      [:div.flex.flex-wrap
       [:a.block.col-6.p1
        we-are-mayvenn-link
        [:div.relative
         diishan-image
         [:div.absolute.bg-darken-3.overlay.flex.items-center.justify-center
          teal-play-video-mobile]]]
       [:a.col-6.px2
        we-are-mayvenn-link
        [:h4.my1.dark-gray.medium "Our Story"]
        [:div.h6.teal.flex.items-center
         "Watch Now"]]
       [:div.col-6.p1 mikka-image]
       [:div.col-6.p1 stylist-image]
       [:div.col-6.px2.dark-gray
        [:h4.my2.line-height-1 "“We're committed to giving our customers and stylists the tools they need to feel empowered.“"]
        [:h6.medium.line-height-1 "- Diishan Imira"]
        [:h6 "CEO of Mayvenn"]]
       [:div.col-6.p1 diishan-image-2]]]

     [:div.hide-on-mb.pb4
      [:div.col-8.flex.flex-wrap.mx-auto
       [:div.col-6.flex.flex-wrap.items-center
        [:div.col-6.p1 mikka-image]
        [:div.col-6.p1 stylist-image]
        [:div.col-6.px1.pb1.dark-gray.flex.justify-start.flex-column
         [:div.h3.line-height-3.col-11
          "“We're committed to giving our customers and stylists the tools they need to feel empowered.“"]
         [:h6.medium.line-height-1.mt2 "- Diishan Imira"]
         [:h6.ml1 "CEO of Mayvenn"]]
        [:div.col-6.p1.flex diishan-image-2]]
       [:a.relative.col-6.p1
        we-are-mayvenn-link
        [:div.relative diishan-image
         [:div.absolute.overlay.flex.items-center.justify-center.bg-darken-3
          teal-play-video-desktop]]]]]]))

(defn component [{:keys [signed-in
                         homepage-data
                         store
                         categories
                         stylist-gallery-open?
                         show-talkable-banner?
                         seventy-five-off-install?
                         the-ville?
                         faq-data
                         video
                         sleek-and-straight-ugc
                         waves-and-curly-ugc
                         free-install-mayvenn-ugc
                         gallery-ucare-ids]
                  :as data}
                 owner
                 opts]
  (component/create
   [:div
    [:section (hero)]
    [:section free-shipping-banner]
    [:div (when video
            (component/build video/component
                             video
                             ;; NOTE(jeff): we use an invalid video slug to preserve back behavior. There probably should be
                             ;;             an investigation to why history is replaced when doing A -> B -> A navigation
                             ;;             (B is removed from history).
                             {:opts {:close-attrs (utils/route-to events/navigate-home {:query-params {:video "0"}})}}))
     [:section what-our-customers-are-saying]]
    [:section.py10.bg-transparent-teal
     (aladdin/get-a-free-install {:store                 store
                                  :gallery-ucare-ids     gallery-ucare-ids
                                  :stylist-portrait      (:portrait store)
                                  :stylist-name          (:store-nickname store)
                                  :stylist-gallery-open? stylist-gallery-open?})]
    [:section (most-popular-looks sleek-and-straight-ugc waves-and-curly-ugc)]
    [:section [:div aladdin/why-mayvenn-is-right-for-you]]
    [:section (free-install-mayvenn-grid free-install-mayvenn-ugc)]
    [:hr.hide-on-mb-tb.border-top.border-dark-silver.col-9.mx-auto.mb6]
    [:section (aladdin/faq faq-data)]
    [:hr.border-top.border-dark-silver.col-9.mx-auto.my6]
    [:section our-story]]))

(defn query [data]
  (let [seventy-five-off-install?   (experiments/seventy-five-off-install? data)
        the-ville?                  (experiments/the-ville? data)
        homepage-data               (get-in data keypaths/cms-homepage)
        store                       (marquee/query data)
        ugc                         (get-in data keypaths/ugc)
        free-install-mayvenn-images (pixlee/images-in-album ugc :free-install-mayvenn)
        sleek-and-straight-images   (pixlee/images-in-album ugc :sleek-and-straight)
        waves-and-curly-images      (pixlee/images-in-album ugc :waves-and-curly)]
    {:store                     store
     :gallery-ucare-ids         (->> store
                                     :gallery
                                     :images
                                     (map (comp aladdin/get-ucare-id-from-url :resizable-url)))
     :faq-data                  {:expanded-index (get-in data keypaths/faq-expanded-section)}
     :signed-in                 (auth/signed-in data)
     :categories                (->> (get-in data keypaths/categories)
                                     (filter :home/order)
                                     (sort-by :home/order))
     :video                     (get-in data keypaths/aladdin-video)
     :sleek-and-straight-ugc    {:images        sleek-and-straight-images
                                 :album-keyword :sleek-and-straight}
     :waves-and-curly-ugc       {:images        waves-and-curly-images
                                 :album-keyword :waves-and-curly}
     :free-install-mayvenn-ugc  {:images        free-install-mayvenn-images
                                 :album-keyword :free-install-mayvenn}
     :stylist-gallery-open?     (get-in data keypaths/carousel-stylist-gallery-open?)
     :seventy-five-off-install? seventy-five-off-install?
     :the-ville?                the-ville?
     :homepage-data             homepage-data
     :show-talkable-banner?     (not (and seventy-five-off-install? the-ville?))}))

(defn built-component [data opts]
  (component/build component (query data) opts))

(def ^:private slug->video
  {"we-are-mayvenn" {:youtube-id "hWJjyy5POTE"}
   "free-install"   {:youtube-id "cWkSO_2nnD4"}})

(defmethod transitions/transition-state events/navigate-home
  [_ _ {:keys [query-params]} app-state]
  (assoc-in app-state keypaths/aladdin-video (slug->video (:video query-params))))

(defmethod effects/perform-effects events/aladdin-show-home
  [_ _ args prev-app-state app-state]
  #?(:cljs (do (pixlee.hook/fetch-album-by-keyword :sleek-and-straight)
               (pixlee.hook/fetch-album-by-keyword :waves-and-curly)
               (pixlee.hook/fetch-album-by-keyword :free-install-mayvenn))))
