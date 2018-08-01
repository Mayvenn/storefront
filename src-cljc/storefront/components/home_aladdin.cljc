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
            [storefront.components.money-formatters :as mf]))

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
        dsk-uuid  "5980306e-71f9-4dca-984c-d816f79c9f98"]
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

(def teal-play-video
  (svg/white-play-video {:class  "mr1 fill-teal"
                         :height "30px"
                         :width  "30px"}))

(def what-our-customers-are-saying
  [:a.block.col-11.mx-auto
   (utils/route-to events/navigate-home {:query-params {:video "free-install"}})
   [:div.hide-on-mb-tb.flex.justify-center.py3
    (ui/ucare-img {:alt "" :width "212"}
                  "b016b985-affb-4c97-af0a-a1f1334c0c51")
    [:div.ml4.dark-gray
     [:div.h4.bold "#MayvennFreeInstall"]
     [:div.h4.my2 "See why customers love their FREE Install"]
     [:div.h5.teal.flex.items-center
      teal-play-video
      "WATCH NOW"]] ]

   [:div.hide-on-dt.flex.justify-center.py3
    [:div
     [:div.relative
      (ui/ucare-img {:alt "" :width "152"}
                    "b016b985-affb-4c97-af0a-a1f1334c0c51")
      [:div.absolute.top-0.bottom-0.left-0.right-0.flex.items-center.justify-center
       teal-play-video]]]
    [:div.ml2.dark-gray
     [:h6.bold.mbnp6 "#MayvennFreeInstall"]
     [:p.pt2.h7
      [:span {:style {:white-space "nowrap"}} "See why customers love their"]
      " "
      [:span {:style {:white-space "nowrap"}} "FREE Install"]]
     [:h6.teal.flex.items-center.mt2
      "WATCH NOW"]]]])

(defn ^:private get-ucare-id-from-url
  [ucare-url]
  (last (re-find #"ucarecdn.com/([a-z0-9-]+)/" (str ucare-url))))

(defn free-install-step [{:keys [icon-uuid icon-width title description]}]
  [:div.col-12.col-4-on-dt.mt2.center
   [:div.flex.justify-center.items-end.mb2
    {:style {:height "39px"}}
    (ui/ucare-img {:alt title :width icon-width} icon-uuid)]
   [:div.h5.teal.medium title]
   [:p.h6.col-8.col-9-on-dt.mx-auto description]])

(defn get-a-free-install
  [{:keys [store
           gallery-ucare-ids
           stylist-portrait
           stylist-name
           stylist-gallery-open?]}]
  [:div.col-12.bg-transparent-teal.mt4.p8
   [:div.mt2.flex.flex-column.items-center
    [:h2 "Get a FREE Install"]
    [:div.h6.dark-gray "In three easy steps"]]

   [:div.col-8-on-dt.mx-auto.flex.flex-wrap
    (free-install-step {:icon-uuid   "e90526f9-546f-4a6d-a05a-3bea94aedc21"
                        :icon-width  "28"
                        :title       "Buy Any 3 Bundles or More"
                        :description "Including closures and frontals! Rest easy - your 100% virgin hair purchase is backed by our 30 day guarantee."})
    (free-install-step {:icon-uuid   "cddd38e0-f598-4aca-90fc-4350dd4469fb"
                        :icon-width  "35"
                        :title       "Get Your Voucher"
                        :description "We’ll send you a free install voucher via SMS and email after your order ships."})
    (free-install-step {:icon-uuid   "7712537c-3805-4d92-90b5-a899748a21c5"
                        :icon-width  "35"
                        :title       "Show Your Stylist The Voucher"
                        :description (if (contains? #{"store" "shop"}
                                                    (:store-slug store))
                                       (str "Redeem the voucher at your appointment with a Mayvenn stylist. "
                                            "Check out some of our certified stylists below:")
                                       "Redeem the voucher when you go in for your appointment with: ")})]

   [:div.mt2.flex.flex-column.items-center
    [:div.h6.my1.dark-gray "Your Stylist"]
    [:div.circle.hide-on-mb-tb
     (if (:resizable-url stylist-portrait)
       (ui/circle-picture {:width "100"} (ui/square-image stylist-portrait 100))
       (ui/circle-ucare-img {:width "100"} "23440740-c1ed-48a9-9816-7fc01f92ad2c"))]
    [:div.circle.hide-on-dt
     (if (:resizable-url stylist-portrait)
       (ui/circle-picture (ui/square-image stylist-portrait 70))
       (ui/circle-ucare-img {:width "70"} "23440740-c1ed-48a9-9816-7fc01f92ad2c"))]
    [:div.h5.bold stylist-name]
    [:div.h6
     (when (:licensed store)
       [:div.flex.items-center.dark-gray {:style {:height "1.5em"}}
        (svg/check {:class "stroke-teal" :height "2em" :width "2em"}) "Licensed"])
     [:div.flex.items-center.dark-gray {:style {:height "1.5em"}}
      (ui/ucare-img {:width "7" :class "pr2"} "bd307d38-277d-465b-8360-ac8717aedb03")
      (str (-> store :location :city) ", " (-> store :location :state-abbr))]]
    (when (seq gallery-ucare-ids)
      [:div.h6.pt1.flex.items-center
       (ui/ucare-img {:width "25"} "18ced560-296f-4b6c-9c82-79a4e8c15d95")
       [:a.ml1.teal.medium
        (utils/fake-href events/control-stylist-gallery-open)
        "Hair Gallery"]
       (modal-gallery/simple
        {:slides      (map modal-gallery/ucare-img-slide gallery-ucare-ids)
         :open?       stylist-gallery-open?
         :close-event events/control-stylist-gallery-close})])]])

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
    [:h2.center "Our Most Popular" [:br.hide-on-dt] " Looks"]
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

(defn hookup-entry [{:keys [icon-uuid icon-width title description]}]
  [:div.col-12.my2.flex.flex-column.items-center.col-3-on-dt.items-end
   [:div.flex.justify-center.items-end.mb2
    {:style {:height "49px"}}
    (ui/ucare-img {:alt title :width icon-width} icon-uuid)]
   [:div.h6.teal.medium title]
   [:p.h6.col-10.center description]])

(def the-hookup
  [:div.col-12.bg-transparent-teal.mt3.py8.px4
   [:div.col-11-on-dt.justify-center.flex.flex-wrap.mx-auto.pb2

    [:div.my2.flex.flex-column.items-center.col-12
     [:h2.titleize "Why mayvenn is right for you"]
     [:div.h6.dark-gray.titleize "It's not just about hair"]]

    (hookup-entry {:icon-uuid   "ab1d2ed4-ff93-40e6-978a-721133ca88a7"
                   :icon-width  "35"
                   :title       "Top Notch Customer Service"
                   :description "Our team is made up of hair experts ready to help you by phone, text, and email."})
    (hookup-entry {:icon-uuid   "8787e30c-2879-4a43-8d01-9d6790575084"
                   :icon-width  "52"
                   :title       "30 Day Guarantee"
                   :description "Wear it, dye it, event cut it! If you're not satisfied we'll exchange it within 30 days."})
    (hookup-entry {:icon-uuid   "e02561dd-c294-43b7-bb33-c40bfabea518"
                   :icon-width  "35"
                   :title       "100% Virgin Hair"
                   :description "Our hair is gently steam processed and can last up to a year. Available in 8 textures and 8 shades."})
    (hookup-entry {:icon-uuid   "3f622e92-6d95-49e2-a0c1-51a535b22975"
                   :icon-width  "35"
                   :title       "Free Install"
                   :description "Get your hair installed absolutely FREE!"})]])

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

(def ^:private faq-section-copy
  (let [phone-link (ui/link :link/phone :a.dark-gray {} "1-888-562-7952")]
    [(accordion/section [:h6 "How does this all work? How do I get a free install?"]
                        ["It’s easy! Mayvenn will pay your stylist directly for your install."
                         " Just purchase 3 bundles or more (frontals and closures count as bundles)"
                         " and use code FREEINSTALL at checkout. You’ll receive a voucher as soon"
                         " as your order ships. Schedule an appointment with your Mayvenn stylist,"
                         " and present the voucher to them at the appointment."
                         " Your stylist will receive the full payment for your install"
                         " immediately after the voucher has been scanned!"])
     (accordion/section [:h6 "What's included in the install?"]
                        ["Typically a full install includes a wash, braid down, and simple styling."
                         " Service details may vary so it would be best to check with your stylist"
                         " to confirm what is included."])
     (accordion/section [:h6 "How does the 30 day guarantee work?"]
                        ["Buy Mayvenn hair RISK FREE with easy returns and exchanges."]
                        ["EXCHANGES" [:br] "Wear it, dye it, even cut it! If you're not satified with your"
                         " hair, we'll exchange it within 30 days of purchase. Our customer service"
                         " team is ready to answer any questions you may have. Give us a call:"
                         phone-link]
                        ["RETURNS" [:br] "If you are not completely happy with your Mayvenn hair"
                         " before it is installed, we will refund your purchase if the"
                         " bundle is unopened and the hair is in its original condition."
                         " Give us a call to start your return:"
                         phone-link])
     (accordion/section [:h6 "Who is going to do my hair?"]
                        ["The free-install offer is only valid at your Mayvenn"
                         " stylist. If you are unsure if your stylist is"
                         " participating in the free-install offer, you can simply"
                         " ask them or contact Mayvenn customer service: "
                         phone-link
                         [:br] "Our stylists specialize in sew-in installs with leave-out, closures,"
                         " frontals, and 360 frontals so you can rest assured that we ahve a stylist"
                         " to help you achieve the look you want."])
     (accordion/section [:h6 "What if I want to get my hair done by another stylist?"
                         " Can I still get the free install?"]
                        ["You must get your hair done from a Mayvenn stylist in"
                         " order to get your hair installed for free."])
     (accordion/section [:h6 "Why should I order hair from Mayvenn?"]
                        ["Mayvenn is a Black owned company that offers 100% virgin hair."
                         " Our Virgin and Dyed Virgin hair can be found in a variety of textures from"
                         " straight to curly. Virgin hair starts at $54 per bundle."
                         " All orders are eligible for free shipping and backed by our 30 Day"
                         " Guarantee."])]))

(defn ^:private faq [{:keys [expanded-index]}]
  [:div.px6.col-5-on-dt.mx-auto
   [:h2.center "Frequently Asked Questions"]
   (component/build
    accordion/component
    {:expanded-indices #{expanded-index}
     :sections         faq-section-copy}
    {:opts {:section-click-event events/faq-section-selected}})])

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
       [:div.col-6.p1
        ;; we-are-mayvenn-link
        [:div.relative
         diishan-image
         [:div.absolute.bg-darken-4.top-0.bottom-0.left-0.right-0.flex.items-center.justify-center
          teal-play-video]]]
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
        diishan-image
        [:div.absolute.overlay.flex.flex-column.justify-between
         [:div ui/nbsp]
         [:div ui/nbsp]
         [:div.hide-on-dt ui/nbsp]
         [:div.flex.flex-column.items-center.justify-center.mt8
          [:h1.my1.dark-gray.bold "Our Story"]
          [:div.h5.teal.flex.items-center.medium
           teal-play-video
           "Watch Now"]]
         [:div ui/nbsp]]]]]]))

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
    [:section (get-a-free-install {:store                 store
                                   :gallery-ucare-ids     gallery-ucare-ids
                                   :stylist-portrait      (:portrait store)
                                   :stylist-name          (:store-nickname store)
                                   :stylist-gallery-open? stylist-gallery-open?})]
    [:section (most-popular-looks sleek-and-straight-ugc waves-and-curly-ugc)]
    [:section the-hookup]
    [:section (free-install-mayvenn-grid free-install-mayvenn-ugc)]
    [:hr.hide-on-mb-tb.border-top.border-dark-silver.col-9.mx-auto.mb6]
    [:section (faq faq-data)]
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
                                     (map (comp get-ucare-id-from-url :resizable-url)))
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
  {"we-are-mayvenn" {:youtube-id "hWJjyy5POTE"}})

(defmethod transitions/transition-state events/navigate-home
  [_ _ {:keys [query-params]} app-state]
  (assoc-in app-state keypaths/aladdin-video (slug->video (:video query-params))))

(defmethod effects/perform-effects events/aladdin-show-home
  [_ _ args prev-app-state app-state]
  #?(:cljs (do (pixlee.hook/fetch-album-by-keyword :sleek-and-straight)
               (pixlee.hook/fetch-album-by-keyword :waves-and-curly)
               (pixlee.hook/fetch-album-by-keyword :free-install-mayvenn))))
