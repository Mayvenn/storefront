(ns storefront.components.home-aladdin
  (:require [clojure.string :as string]
            [storefront.accessors.auth :as auth]
            [storefront.accessors.experiments :as experiments]
            [storefront.assets :as assets]
            [storefront.component :as component]
            [storefront.components.marquee :as marquee]
            [storefront.components.accordion :as accordion]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.transitions :as transitions]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.carousel :as carousel]
            [storefront.routes :as routes]
            [storefront.components.aladdin-video :as aladdin-video]))

(defn hero-image [{:keys [desktop-url mobile-url file-name alt]}]
  [:picture
   ;; Tablet/Desktop
   [:source {:media   "(min-width: 750px)"
             :src-set (str desktop-url "-/format/auto/-/quality/best/" file-name " 1x")}]
   ;; Mobile
   [:img.block.col-12 {:src (str mobile-url "-/format/auto/" file-name)
                       :alt alt}]])

(defn hero []
  (let [file-name "100-off-installation-hero"
        alt       "$100 off your install when you buy 3 bundles or more! Use code: INSTALL"
        mob-uuid  "c18c6ca3-5ada-4ec5-80dc-208b3020f43e"
        dsk-uuid  "c18c6ca3-5ada-4ec5-80dc-208b3020f43e"]
    [:div
     [:div.relative
      [:div (hero-image {:mobile-url  (str "//ucarecdn.com/" mob-uuid "/")
                         :desktop-url (str "//ucarecdn.com/" dsk-uuid "/")
                         :file-name   file-name
                         :alt         alt})
       [:h1.bold.shadow.white.absolute.center
        {:style {:top "50%" :left "60px" :right "60px"}}
        "Beautiful Virgin Hair Installed for FREE"]]]]))

(def free-shipping-banner
  [:div {:style {:height "3em"}}
   [:div.bg-light-gray {:style {:height "2em"}}]
   [:div.mx-auto.medium.table.center.relative.h5 {:style {:top "-3em"}}
    [:div.table-cell.align-middle.mtp4
     (ui/ucare-img {:alt "" :width "50"}
                   "4c4912fe-934c-4ad3-b853-f4a932bdae1b")]
    [:div.table-cell.align-middle.pl3.h6
     "FREE standard shipping. Express available"]]])

(def what-our-customers-are-saying
  [:a
   (utils/route-to events/navigate-home {:query-params {:video "free-install"}})
   [:div.col-11.mx-auto
    [:div.flex.items-center.justify-center.py1
     [:div {:style {:height "88px"}}  ;; Without explicit height, div expands 4px downwards
      (ui/ucare-img {:alt "" :width "152"}
                    "b016b985-affb-4c97-af0a-a1f1334c0c51")]

     [:div.ml2
      [:h6.bold.dark-gray.mbnp6 "#FreeInstallMayvenn"]
      [:h7.dark-gray "What our customers are saying"]
      [:h6.teal.flex.items-center.mt2
       (svg/clear-play-video {:class "mr1 fill-teal"
                              :height  "20px"
                              :width   "20px"})
       "WATCH NOW"]]]]])

(defn get-a-free-install
  [{:keys [stylist-headshot stylist-name]}]
  [:div.col-12.bg-transparent-teal.mt4.p8
   [:div.mt2.flex.flex-column.items-center
    [:h2 "Get a FREE Install"]
    [:div.h6.dark-gray "In 3 easy steps"]]

   [:div.mt4.flex.flex-column.items-center
    (ui/ucare-img {:alt "Buy ANY 3 Bundles or More" :width "20"} "ca7771b3-3b11-4207-a42e-301a98961c92")
    [:div.h5.teal.medium "Buy ANY 3 Bundles or More"]
    [:p.h6.col-8.center "Closures and frontals count, too! Our hair is virgin & backed by a 30-day guarantee."]]

   [:div.mt4.flex.flex-column.items-center
    (ui/ucare-img {:alt "Get Your Voucher" :width "39"} "06e84340-9dc6-4193-abc7-fbea15715084")
    [:div.h5.teal.medium "Get Your Voucher"]
    [:p.h6.col-8.center "We’ll send you a free-install voucher after purchase via SMS and email."]]

   [:div.mt4.flex.flex-column.items-center
    (ui/ucare-img {:alt "Show Your Stylist The Voucher" :width "24"} "b06a282a-27a0-4a4c-aa85-77868556ac1d")
    [:div.h5.teal.medium "Show Your Stylist The Voucher"]
    [:p.h6.col-8.center "Present the voucher when you go in for your appointment with:"]]

   [:div.mt2.flex.flex-column.items-center
    [:div.h6.my1.dark-gray "Your Stylist"]
    [:div.circle
     (ui/circle-ucare-img {:width "70px"} "63acc2ac-43cc-48cb-9db7-0361f01aaa25")]
    [:div.h5.bold
     "Aundria Carter"]
    [:div.h6
     [:div.flex.items-center.dark-gray {:style {:height "1.5em"}}
      (svg/check {:class "stroke-teal" :height "2em" :width "2em"}) "Licensed"]
     [:div.flex.items-center.dark-gray {:style {:height "1.5em"}}
      (svg/check {:class "stroke-teal" :height "2em" :width "2em"}) "Oakland, CA"]]
    [:div.h6.pt1.flex.items-center
     (svg/cascade {:style {:height "20px" :width "29px"}})
     [:span.ml1.teal.medium "Hair Gallery"]]]])

(defn carousel-slide [image-id caption]
  [:div
   (ui/aspect-ratio 1 1 (ui/ucare-img {:class "col-12 mx1"} image-id))
   [:div.h6.mt1.dark-gray caption]])

(defn style-carousel [styles treatments origins link]
  [:div.my3.col-12
   [:h5.bold.center styles]
   [:div.h6.center.mb2.dark-gray treatments [:span.px2 "•"] origins]
   (component/build carousel/component
                    {:slides   (repeat 5 (carousel-slide "63acc2ac-43cc-48cb-9db7-0361f01aaa25"
                                                         [:span [:span.bold "$280 "] "+ FREE Install"]))
                     :settings {:slidesToShow 2
                                :swipe        true
                                :arrows       true}}
                    {})
   [:div.col-9.mx-auto.mt2
    (ui/teal-button {:height-class "py2"}
                    (str "Shop " link " Looks"))]])

(def most-popular-looks
  [:div.col-12.col-6-on-tb.col-4-on-dt.mt3.py6.px2.mx-auto
   [:div.my2.flex.flex-column.items-center
    [:h2.center "Most Popular" [:br] "#FreeInstallMayvenn Looks"]
    (style-carousel "Sleek & Straight"
                    "Virgin & Dyed Virgin"
                    "Brazilian & Peruvian"
                    "Sleek & Straight")
    (style-carousel "Waves & Curls"
                    "Virgin & Dyed Virgin"
                    "Brazilian, Malaysian & Peruvian"
                    "Wave & Curl")]])

(def the-hookup
  [:div.col-12.bg-transparent-teal.mt3.py8.px4
   [:div.my2.flex.flex-column.items-center
    [:h2 "The Hookup"]
    [:div.h6.dark-gray "Why Mayvenn is right for you"]]

   [:div.mt6.flex.flex-column.items-center
    (ui/ucare-img {:alt "" :width "21"} "44a9227b-db64-42b0-ab06-5d19037baf8e")
    [:div.h6.teal.medium "World-Class Customer Service"]
    [:p.h6.col-10.center "Our experts have first-hand experience and are ready to help you by phone, text and email."]]

   [:div.mt6.flex.flex-column.items-center
    (ui/ucare-img {:alt "" :width "71"} "9b8cd1ed-200b-4a6a-bb5f-4335866859e5")
    [:div.h6.teal.medium "Risk-Free"]
    [:p.h6.col-10.center "Wear it, dye it, style it. If you don’t love your hair we’ll exchange it within 30 days of purchase."]]

   [:div.mt6.flex.flex-column.items-center
    (ui/ucare-img {:alt "" :width "40"} "7c436000-2d6b-4349-b3c9-1d7b4ae3bb21")
    [:div.h6.teal.medium "100% Human Hair"]
    [:p.h6.col-10.center "Available in Virgin and Dyed Virgin"]]

   [:div.mt6.flex.flex-column.items-center
    (ui/ucare-img {:alt "" :width "43"} "c81da7fe-f3fb-4728-8428-e1b93bdf34cc")
    [:div.h6.teal.medium "Free Install"]
    [:p.h6.col-10.center "Get your hair installed absolutely FREE!"]]])

(def ugc-quadriptych
  [:div.py10.px4
   [:h2.center "#FreeInstallMayvenn"]
   [:h6.center.dark-gray "Over 1,000 free installs & counting"]
   [:div.flex.flex-wrap
    [:div.col-6.col-3-on-tb-dt.p1 (ui/ucare-img {:class "col-12"} "63acc2ac-43cc-48cb-9db7-0361f01aaa25")]
    [:div.col-6.col-3-on-tb-dt.p1 (ui/ucare-img {:class "col-12"} "63acc2ac-43cc-48cb-9db7-0361f01aaa25")]
    [:div.col-6.col-3-on-tb-dt.p1 (ui/ucare-img {:class "col-12"} "63acc2ac-43cc-48cb-9db7-0361f01aaa25")]
    [:div.col-6.col-3-on-tb-dt.p1 (ui/ucare-img {:class "col-12"} "63acc2ac-43cc-48cb-9db7-0361f01aaa25")]]])

(def ^:private faq-section-copy
  [(accordion/section "How does this all work? How do I get a FREE INSTALL?"
                      ["It’s easy! Mayvenn will pay a stylist to install your hair."
                       " Just purchase 3"
                       " bundles or more (frontals and closures count as bundles) and use code"
                       " FREEINSTALL at checkout. Then, Mayvenn will reach out to you via email"
                       " or text to schedule an install with one of our Mayvenn Certified"
                       " Stylists, proudly based in Fayetteville, NC."])
   (accordion/section "Who is going to do my hair?"
                      ["Our Mayvenn Certified Stylists are among the best in Fayetteville, with an"
                       " average of 15 years of professional experience. Each of our stylists specializes"
                       " in sew-in installs with leave-out, closures, frontals, and 360 frontals so you"
                       " can feel confident that we have a stylist to achieve the look you want. Have a"
                       " specific idea in mind? Just let us know and we’ll match you with a stylist's"
                       " best suited to meet your needs."])
   (accordion/section "How does the 30 day guarantee work?"
                      ["Buy Mayvenn hair RISK FREE with easy returns and exchanges."]
                      ["EXCHANGES" [:br] "Wear it, dye it, even flat iron it. If you do not love your"
                       " Mayvenn hair we will exchange it within 30 days of purchase."
                       " Just call us:"
                       (ui/link :link/phone :a.dark-gray {} "1-888-562-7952")]
                      ["RETURNS" [:br] "If you are not completely happy with your Mayvenn hair"
                       " before it is installed, we will refund your purchase if the"
                       " bundle is unopened and the hair is in its original condition."
                       " Just call us:"
                       (ui/link :link/phone :a.dark-gray {} "1-888-562-7952")])
   (accordion/section "What if I want to get my hair done by another stylist?"
                      ["No, you must get your hair done from one of Mayvenn’s Certified Stylists in"
                       " order to get your hair installed for free. Our stylists are licensed and"
                       " experienced - the best in Fayetteville!"])
   (accordion/section "Why should I order hair from Mayvenn?"
                      ["Mayvenn hair is 100% human. Our Virgin, Dyed Virgin, and"
                       " 100% Human hair can be found in a variety of textures from"
                       " straight to curly. Virgin hair starts at $54 per bundle and"
                       " 100% Human hair starts at just $30 per bundle. All orders are"
                       " eligible for free shipping and backed by our 30 Day"
                       " Guarantee."])])

(defn ^:private faq [{:keys [expanded-index]}]
  [:div.px6
   [:h2.center "Frequently Asked Questions"]
   (component/build
    accordion/component
    {:expanded-indices #{expanded-index}
     :sections         faq-section-copy}
    {:opts {:section-click-event events/faq-section-selected}})])

(def our-story
  [:div.p4
   [:div.h2.center "Your beautiful, affordable hair starts here"]
   [:h6.center.mb4.dark-gray "Founded in Oakland, CA • 2014"]
   [:div.flex.flex-wrap
    [:div.col-6.px1 (ui/ucare-img {:class "col-12"} "4a92843c-b92d-48d3-8ac0-05b744b73584")]
    [:div.col-6.px2
     [:h4.my1.dark-gray.medium "Our Story"]
     [:h6.teal.flex.items-center
      (svg/clear-play-video {:class        "mr1 fill-teal"
                             :height       "20px"
                             :width        "20px"})
      "WATCH NOW"]]
    [:div.col-6.px1 (ui/ucare-img {:class "col-12"} "34512e2b-e6a8-47c2-bc7e-0471737fc920")]
    [:div.col-6.px1 (ui/ucare-img {:class "col-12"} "d800599c-a383-4b87-8c0d-d80bbaaf0817")]
    [:div.col-6.px2.dark-gray
     [:h4.my2.line-height-1 "“Our mission is to provide our community with an unbeatable product.”"]
     [:h6.medium.line-height-1 "- Diishan Imira"]
     [:h6 "CEO of Mayvenn"]]
    [:div.col-6.px1 (ui/ucare-img {:class "col-12"} "ec9e0533-9eee-41ae-a61b-8dc22f045cb5")]
   ]])

(defn component [{:keys [signed-in homepage-data store categories show-talkable-banner? seventy-five-off-install? the-ville? faq-data video] :as data} owner opts]
  (component/create
   [:div
    [:div
     [:section (hero)]
     [:section free-shipping-banner]
     [:div (when video
             (component/build aladdin-video/component
                              video
                              ;; NOTE(jeff): we use an invalid video slug to preserve back behavior. There probably should be
                              ;;             an investigation to why history is replaced when doing A -> B -> A navigation
                              ;;             (B is removed from history).
                              {:opts {:close-attrs (utils/route-to events/navigate-home {:query-params {:video "0"}})}}))
      [:section what-our-customers-are-saying]]
     [:section (get-a-free-install {})]
     [:section most-popular-looks]
     [:section the-hookup]
     [:section ugc-quadriptych]
     [:section (faq faq-data)]
     [:hr.border-top.border-dark-silver.col-9.mx-auto.my6]
     [:section our-story]]]))

(defn query [data]
  (let [seventy-five-off-install? (experiments/seventy-five-off-install? data)
        the-ville?                (experiments/the-ville? data)
        homepage-data             (get-in data keypaths/cms-homepage)]
    {:store                     (marquee/query data)
     :faq-data                  {:expanded-index (get-in data keypaths/faq-expanded-section)}
     :signed-in                 (auth/signed-in data)
     :categories                (->> (get-in data keypaths/categories)
                                     (filter :home/order)
                                     (sort-by :home/order))
     :video                     (aladdin-video/query data)
     :seventy-five-off-install? seventy-five-off-install?
     :the-ville?                the-ville?
     :homepage-data             homepage-data
     :show-talkable-banner?     (not (and seventy-five-off-install? the-ville?))}))

(defn built-component [data opts]
  (component/build component (query data) opts))

(def ^:private slug->video
  {"free-install" {:youtube-id "cWkSO_2nnD4"}})

;; Copied from storefront.components.free-install. TODO: DRY up
(defmethod transitions/transition-state events/navigate-home
  [_ _ {:keys [query-params]} app-state]
  (assoc-in app-state keypaths/aladdin-video (slug->video (:video query-params))))

;; Copied from storefront.components.free-install. TODO: DRY up
(defmethod transitions/transition-state events/faq-section-selected [_ _ {:keys [index]} app-state]
  (let [expanded-index (get-in app-state keypaths/faq-expanded-section)]
    (if (= index expanded-index)
      (assoc-in app-state keypaths/faq-expanded-section nil)
      (assoc-in app-state keypaths/faq-expanded-section index))))
