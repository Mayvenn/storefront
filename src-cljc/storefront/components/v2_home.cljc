(ns storefront.components.v2-home
  (:require [storefront.accessors.auth :as auth]
            [storefront.accessors.contentful :as contentful]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.marquee :as marquee]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.components.video :as video]
            [storefront.components.v2 :as v2]
            [storefront.transitions :as transitions]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.carousel :as carousel]
            [ui.molecules :as ui.M]))

(defn free-shipping-banner []
  (component/build
   (fn [_ _ _]
     (component/create
      [:div {:style {:height "3em"}}
       [:div.bg-black.flex.items-center.justify-center
        {:style {:height "2.25em"
                 :margin-top "-1px"
                 :padding-top "1px"}}
        [:div.px2
         (ui/ucare-img {:alt "" :height "25"}
                       "38d0a770-2dcd-47a3-a035-fc3ccad11037")]
        [:div.h6.white.light
         "FREE standard shipping"]]]))))

(defn teal-play-video-mobile []
  (svg/white-play-video {:class  "mr1 fill-teal"
                         :height "30px"
                         :width  "30px"}))

(defn teal-play-video-desktop []
  (svg/white-play-video {:class  "mr1 fill-teal"
                         :height "41px"
                         :width  "41px"}))

(defn what-our-customers-are-saying []
  (component/build
   (fn [_ _ _]
     (component/create
      (let [video-link (utils/route-to events/navigate-home {:query-params {:video "free-install"}})]
        [:div.col-11.mx-auto
         [:div.hide-on-mb-tb.flex.justify-center.py3
          [:a.block.relative
           video-link
           (ui/defer-ucare-img {:alt "" :width "212"}
             "c487eeef-0f84-4378-a9be-13dc7c311e23")
           [:div.absolute.top-0.bottom-0.left-0.right-0.flex.items-center.justify-center.bg-darken-3
            ^:inline (teal-play-video-desktop)]]
          [:a.block.ml4.dark-gray
           video-link
           [:div.h4.bold "#MayvennFreeInstall"]
           [:div.h4.my2 "See why customers love their free Mayvenn Install"]
           [:div.h5.teal.flex.items-center.medium.shout
            "Watch Now"]]]

         [:div.hide-on-dt.flex.justify-center.py3
          [:a.block.relative
           video-link
           (ui/defer-ucare-img {:alt "" :width "152"}
             "1b58b859-842a-44b1-885c-eac965eeaa0f")
           [:div.absolute.top-0.bottom-0.left-0.right-0.flex.items-center.justify-center.bg-darken-3
            ^:inline (teal-play-video-mobile)]]
          [:a.block.ml2.dark-gray
           video-link
           [:h6.bold.mbnp6 "#MayvennFreeInstall"]
           [:p.pt2.h8
            [:span {:style {:white-space "nowrap"}} "See why customers love their free"]
            " "
            [:span {:style {:white-space "nowrap"}} "Mayvenn Install"]]
           [:h6.teal.flex.items-center.medium.shout
            "Watch Now"]]]])))))

(defn carousel-slide
  [{:as   social-card
    :keys [image-url
           description
           overlay
           cta/navigation-message]}]
  [:div
   [:div.relative.m1
    (when (seq navigation-message)
      (apply utils/route-to navigation-message))
    (ui/aspect-ratio
     1 1
     [:img {:class "col-12"
            :src   image-url}])
    (when overlay
      [:div.absolute.flex.justify-end.bottom-0.right-0.mb2
       [:div {:style {:width       "0"
                      :height      "0"
                      :border-top  "28px solid rgba(159, 229, 213, 0.8)"
                      :border-left "21px solid transparent"}}]
       [:div.flex.items-center.px2.medium.h6.bg-transparent-light-teal
        overlay]])]
   [:div.h6.mx1.mt1.dark-gray.medium description]])

(defn ^:private shop-button [album-keyword link-text]
  (ui/teal-button (merge
                   {:height-class "py2"}
                   (utils/route-to events/navigate-shop-by-look {:album-keyword album-keyword}))
                  [:span.bold
                   (str "Shop " link-text " Looks")]))

(defn ^:private style-carousel-component [images]
  (component/build carousel/component
                   {:slides   (mapv carousel-slide images)
                    :settings {:items    2
                               :controls false
                               :nav      false}}
                   {}))

(defn style-carousel [styles treatments origins link-text {:as ugc :keys [album-keyword]} album-first?]
  (component/html
   [:div.col-12.mt4
    [:div.hide-on-dt
     [:h5.bold.center styles]
     [:div.h6.center.mb2.dark-gray treatments [:span.px2 "•"] origins]
     ^:inline (style-carousel-component (:images ugc))
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
       ^:inline (style-carousel-component (:images ugc))]]]]))

(defn an-amazing-deal []
  (component/build
   (fn [_ _ _]
     (component/create
      [:div
       [:div
        [:div.col-12.col-8-on-tb.col-6-on-dt.mx-auto.pb6.pt4
         [:div.center.clearfix.py3.h6.dark-gray
          [:h2.mt4.black.relative.z2 "An Amazing Deal"]
          [:div.flex.justify-center
           [:div
            [:div.h6.mx-auto
             {:style {:width "170px"}}
             [:div.absolute.z1
              [:div.relative
               {:style {:top "-40px" :left "-40px"}}
               (ui/ucare-img {:width "250"} "0db72798-6c51-48f2-8206-9fd6d91a3ada")]]
             [:div.relative.z2
              [:div.img-logo.bg-no-repeat.bg-center.bg-contain.mb2
               {:style {:height "45px"}}]
              [:div.mb1 "3 bundles.............." [:span.medium "$189"]]
              [:div     "Install....................." [:span.medium "FREE"]]
              [:div.mt2.mx-auto.flex.items-center
               [:div.col-6.shout.line-height-1.left-align.px2.pl3
                [:div "Install"]
                [:div "+ Hair"]]
               [:div.col-6
                [:div.h1.purple.bold "$189"]]]]
             [:div.mx-auto.flex.items-center
              [:div.col-4]
              [:div.col-8.relative.z2
               [:div.italic.right-align "a $54 savings"]]]]]
           [:div.col-1]
           [:div.relative.z2
            [:div.h6.mx-auto
             {:style {:width "170px"}}
             [:div.mb2 {:style {:height "45px"}}
              [:div.pt1
               [:div.mt4
                (ui/ucare-img {:width "90" :class "mx-auto"}
                              "9f1657d1-c792-44fb-b8e7-20ce64c7dbf4")]]]
             [:div.mb1. "3 bundles.............." [:span.medium "$93"]]
             [:div      "Install..................." [:span.medium "$$$$"]]
             [:div.mt2.mx-auto.flex.items-center
              [:div.col-6.shout.line-height-1.left-align.pl4
               [:div "Install"]
               [:div "+ Hair"]]
              [:div.col-6
               [:div.h2.medium "$243"]]]]]]]

         (let [row (fn [text]
                     [:div.flex.justify-center.items-center.center.mx-auto.pt2.relative.z2
                      (ui/ucare-img {:width "9"} "60b946bd-f276-46fc-9d13-3c8562b28d81")
                      [:span.col-6 text]
                      (ui/ucare-img {:width "9"} "f277849f-a27b-48b4-804b-7d523b6d2442")])]
           [:div.clearfix.h6.dark-gray.px4.mx-auto.line-height-2
            (row "High quality hair")
            (row "FREE install")
            (row "30 Day Guarantee")
            (row "Black-owned company")])]]]))))

(defn most-popular-looks [sleek-ugc wave-ugc]
  (component/html
   [:div.col-12.col-8-on-tb-dt.mt3.px2.py5.mx-auto
    [:div.my2.flex.flex-column.items-center
     [:h2.center "Our Most Popular Looks"]
     ^:inline (style-carousel "Sleek & Straight"
                              "Virgin & Dyed Virgin Hair"
                              "Brazilian & Peruvian"
                              "Straight"
                              sleek-ugc
                              true)

     [:hr.hide-on-mb-tb.border-top.border-silver.col-12.mx-auto.my6]

     ^:inline (style-carousel "Wavy & Curly"
                              "Virgin & Dyed Virgin Hair"
                              "Brazilian, Malaysian & Peruvian"
                              "Wavy & Curly"
                              wave-ugc
                              false)]]))

(defn free-install-mayvenn-grid [free-install-mayvenn-ugc]
  (component/html
   [:div.py8.col-10.mx-auto
    [:h2.center "#MayvennFreeInstall"]
    [:h6.center.dark-gray "Show off your look by tagging us with #MayvennFreeInstall"]
    [:div.flex.flex-wrap.pt2
     (for [{:keys [cta/navigation-message image-url]} (:images free-install-mayvenn-ugc)]
       [:a.col-6.col-3-on-tb-dt.p1
        (merge
         {:key image-url}
         (when navigation-message
           (apply utils/route-to navigation-message)))
        (ui/aspect-ratio
         1 1
         [:img {:class "col-12"
                :src   image-url}])])]]))

(defn our-story []
  (component/build
   (fn [_ _ _]
     (component/create
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
              ^:inline (teal-play-video-mobile)]]]
           [:a.col-6.px2
            we-are-mayvenn-link
            [:h4.my1.dark-gray.medium "Our Story"]
            [:div.h6.teal.flex.items-center.medium.shout
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
              ^:inline (teal-play-video-desktop)]]]]]])))))

(defn component [{:keys [signed-in
                         homepage-data
                         store
                         categories
                         stylist-gallery-open?
                         faq-data
                         video
                         sleek-and-straight-ugc
                         waves-and-curly-ugc
                         free-install-mayvenn-ugc
                         gallery-ucare-ids
                         hero-data]
                  :as data}
                 owner
                 opts]
   [:section (component/build ui.M/hero hero-data nil)]
   [:section ^:inline (free-shipping-banner)]
   [:a {:name "mayvenn-free-install-video"}]
   [:div
    ^:inline
    (when video
      (component/build video/component
                       video
                       ;; NOTE(jeff): we use an invalid video slug to preserve back behavior. There probably should be
                       ;;             an investigation to why history is replaced when doing A -> B -> A navigation
                       ;;             (B is removed from history).
                       {:opts {:close-attrs (utils/route-to events/navigate-home {:query-params {:video "0"}})}}))
    [:section ^:inline (what-our-customers-are-saying)]]
   [:section.py10.bg-transparent-teal
    ^:inline (v2/get-a-free-install {:store                 store
                                     :gallery-ucare-ids     gallery-ucare-ids
                                     :stylist-portrait      (:portrait store)
                                     :stylist-name          (:store-nickname store)
                                     :stylist-gallery-open? stylist-gallery-open?})]
   [:section ^:inline (an-amazing-deal)]
   [:hr.border-top.border-silver.col-9.mx-auto.my6]
   [:section ^:inline (most-popular-looks sleek-and-straight-ugc waves-and-curly-ugc)]
   [:section [:div (v2/why-mayvenn-is-right-for-you)]]
   [:section ^:inline (free-install-mayvenn-grid free-install-mayvenn-ugc)]
   [:hr.hide-on-mb-tb.border-top.border-silver.col-9.mx-auto.mb6]
   [:section (component/build v2/faq faq-data)]
   [:section ^:inline (our-story)]])

(defn query [data]
  (let [homepage-data      (get-in data keypaths/cms-homepage)
        store              (marquee/query data)
        cms-ugc-collection (get-in data keypaths/cms-ugc-collection)
        current-nav-event  (get-in data keypaths/navigation-event)]
    {:store                    store
     :gallery-ucare-ids        (->> store
                                    :gallery
                                    :images
                                    (filter (comp (partial = "approved") :status))
                                    (map (comp v2/get-ucare-id-from-url :resizable-url)))
     :faq-data                 {:expanded-index (get-in data keypaths/faq-expanded-section)}
     :signed-in                (auth/signed-in data)
     :categories               (->> (get-in data keypaths/categories)
                                    (filter :home/order)
                                    (sort-by :home/order))
     :video                    (get-in data keypaths/v2-ui-home-video)
     :sleek-and-straight-ugc   {:images        (contentful/album-kw->homepage-social-cards cms-ugc-collection current-nav-event :sleek-and-straight)
                                :album-keyword :sleek-and-straight}
     :waves-and-curly-ugc      {:images        (contentful/album-kw->homepage-social-cards cms-ugc-collection current-nav-event :waves-and-curly)
                                :album-keyword :waves-and-curly}
     :free-install-mayvenn-ugc {:images        (contentful/album-kw->homepage-social-cards cms-ugc-collection current-nav-event :free-install-mayvenn)
                                :album-keyword :free-install-mayvenn}
     :stylist-gallery-open?    (get-in data keypaths/carousel-stylist-gallery-open?)
     :homepage-data            homepage-data
     :hero-data                {:opts      (utils/scroll-href "mayvenn-free-install-video")
                                :file-name "free-install-hero"
                                :mob-uuid  "841c504e-fce9-404d-9652-a66b0af86057"
                                :dsk-uuid  "f423c9d9-1073-4b0c-86a5-dc810e18e505"
                                :alt       "Use code FREEINSTALL when you buy 3 bundles or more"}}))

(defn built-component [data opts]
  (component/build component (query data) opts))

