(ns storefront.components.home-aladdin
  (:require [clojure.string :as string]
            [storefront.accessors.auth :as auth]
            [storefront.accessors.experiments :as experiments]
            [storefront.assets :as assets]
            [storefront.component :as component]
            [storefront.components.marquee :as marquee]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.routes :as routes]))

(defn hero-image [{:keys [desktop-url mobile-url file-name alt]}]
  [:picture
   ;; Tablet/Desktop
   [:source {:media   "(min-width: 750px)"
             :src-set (str desktop-url "-/format/auto/-/quality/best/" file-name " 1x")}]
   ;; Mobile
   [:img.block.col-12 {:src (str mobile-url "-/format/auto/" file-name)
                       :alt alt}]])

(def free-shipping-banner
  [:div
   [:div.bg-light-gray {:style {:height "2em"}}]
   [:div.mx-auto.medium.table.center.relative.h5 {:style {:top "-3em"}}
    [:div.table-cell.align-middle.mtp4
     (ui/ucare-img {:alt "" :width "50"}
                   "4c4912fe-934c-4ad3-b853-f4a932bdae1b")]
    [:div.table-cell.align-middle.pl3
     "FREE standard shipping. Express available"]]])

(defn hero []
  (let [file-name "100-off-installation-hero"
        alt       "$100 off your install when you buy 3 bundles or more! Use code: INSTALL"
        mob-uuid  "699e088d-aaf1-4b7a-b807-a5893573757f"
        dsk-uuid  "6b872af5-b447-440d-b872-6c8a1b669969"]
    [:div
     [:div.relative
      [:div (hero-image {:mobile-url  (str "//ucarecdn.com/" mob-uuid "/")
                         :desktop-url (str "//ucarecdn.com/" dsk-uuid "/")
                         :file-name   file-name
                         :alt         alt})
       [:h1.bold.shadow.white.absolute.center
        {:style {:top "50%" :left "90px" :right "90px"}}
        "Beautiful Virgin Hair Installed for FREE"]]]]))

(def what-our-customers-are-saying
  [:div
   [:div.col-11.mx-auto
    [:div.flex.items-center.justify-center
     [:div
      (ui/ucare-img {:alt "" :width "152" :height "85"}
                    "b016b985-affb-4c97-af0a-a1f1334c0c51")]

     [:div
      [:h6.bold "#FreeInstallMayvenn"]
      [:p.h7 "What our customers are saying"]
      (svg/clear-play-video {:class        "mr2"
                             :fill         "white"
                             :fill-opacity "0.9"
                             :height       "50px"
                             :width        "50px"})
      [:p.h6 "WATCH NOW"]]]]])

(def the-hookup
  [:div.col-12.bg-transparent-teal.mt3.py8.px4
   [:h2.h6.letter-spacing-7.bold.caps.teal.mb1.mt2 "The Hookup"]
   [:h3.h1 "Treat Yourself"]
   [:div.my2.mx-auto.bg-teal {:style {:width "30px" :height "2px"}}]

   [:div.my4
    (ui/ucare-img {:alt "" :width "72"}
                  "c81da7fe-f3fb-4728-8428-e1b93bdf34cc")
    [:h6.teal.bold "Free Install"]
    [:p.h6 "Get your hair installed absolutely FREE."]]

   [:div.my4
    (ui/ucare-img {:alt "" :width "72"}
                  "3bbc41a4-31c2-4817-ad9b-f32936d7a95f")
    [:h6.teal.bold "Risk Free"]
    [:p.h6 "Wear it, dye it, style it. If your don't love it your"
     " hair we'll exchange it within 30 days of purchase."]]

   [:div.my4
    [:div
     (ui/ucare-img {:alt "" :width "51"}
                   "1690834e-84c8-45c7-9047-57be544e89b0")]
    [:h6.teal.bold "Mayvenn Certified Stylists"]
    [:p.h6 "All Mayvenn Certified Stylists are licensed and work in salons."]]])


(def get-a-free-install [:div])
(def most-popular-looks [:div])
(def ugc-quadriptych [:div])
(def faq [:div])
(def our-story [:div])

(defn component [{:keys [signed-in homepage-data store categories show-talkable-banner? seventy-five-off-install? the-ville?] :as data} owner opts]
  (component/create
   [:div
    [:div.m-auto
     [:section (hero)]
     [:section free-shipping-banner]
     [:section what-our-customers-are-saying]
     [:section get-a-free-install]
     [:section most-popular-looks]
     [:section the-hookup]
     [:section ugc-quadriptych]
     [:section faq]
     [:section our-story]]]))

(defn query [data]
  (let [seventy-five-off-install? (experiments/seventy-five-off-install? data)
        the-ville?                (experiments/the-ville? data)
        homepage-data             (get-in data keypaths/cms-homepage)]
    {:store                     (marquee/query data)
     :signed-in                 (auth/signed-in data)
     :categories                (->> (get-in data keypaths/categories)
                                     (filter :home/order)
                                     (sort-by :home/order))
     :seventy-five-off-install? seventy-five-off-install?
     :the-ville?                the-ville?
     :homepage-data             homepage-data
     :show-talkable-banner?     (not (and seventy-five-off-install? the-ville?))}))

(defn built-component [data opts]
  (component/build component (query data) opts))
