(ns adventure.home
  (:require [adventure.keypaths :as keypaths]
            [adventure.faq :as faq]
            [storefront.accessors.auth :as auth]
            #?@(:cljs [[om.core :as om]
                       [goog.events.EventType :as EventType]
                       goog.dom
                       goog.style
                       goog.events])
            [storefront.component :as component]
            [storefront.components.marquee :as marquee]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.platform.component-utils :as utils]
            [storefront.events :as events]
            [storefront.components.video :as video]
            [storefront.components.v2 :as v2]))

(defn sticky-component
  [{:keys [next-page]} owner opts]
  #?(:clj (component/create [:div])
     :cljs
     (letfn [(handle-scroll [e] (om/set-state! owner :show? (< 530 (.-y (goog.dom/getDocumentScroll)))))
             (set-height [] (om/set-state! owner :content-height (some-> owner
                                                                         (om/get-node "content-height")
                                                                         goog.style/getSize
                                                                         .-height)))]
       (reify
         om/IInitState
         (init-state [this]
           {:show? false
            :content-height 0})
         om/IDidMount
         (did-mount [this]
           (handle-scroll nil) ;; manually fire once on load incase the page already scrolled
           (set-height)
           (goog.events/listen js/window EventType/SCROLL handle-scroll))
         om/IWillUnmount
         (will-unmount [this]
           (goog.events/unlisten js/window EventType/SCROLL handle-scroll))
         om/IWillReceiveProps
         (will-receive-props [this next-props]
           (set-height))
         om/IRenderState
         (render-state [this {:keys [show? content-height]}]
           (component/html
            [:div.hide-on-dt
             ;; padding div to allow content that's normally at the bottom to be visible
             [:div {:style {:height (str content-height "px")}}]
             [:div.fixed.z4.bottom-0.left-0.right-0
              {:style {:margin-bottom (str "-" content-height "px")}}
              ;; Using a separate element with reverse margin to prevent the
              ;; sticky component from initially appearing on the page and then
              ;; animate hiding.
              [:div.transition-2
               (if show?
                 {:style {:margin-bottom (str content-height "px")}}
                 {:style {:margin-bottom "0"}})
               [:div {:ref "content-height"}
                [:div
                 [:div.h6.white.bg-black.medium.px3.py2.flex.items-center
                  [:div.col-7 "We can't wait for you to get a FREE install."]
                  [:div.col-1]
                  [:div.col-4
                   (ui/teal-button (merge {:height-class "py2"}
                                          (utils/route-to next-page))
                                   [:div.h7 "Get started"])]]]]]]]))))))

(defn hero-image [{:keys [desktop-url mobile-url file-name alt]}]
  [:picture
   ;; Tablet/Desktop
   [:source {:media   "(min-width: 750px)"
             :src-set (str desktop-url "-/format/jpeg/-/quality/best/" file-name " 1x")}]
   ;; Mobile
   [:img.block.col-12 {:src (str mobile-url "-/format/jpeg/" file-name)
                       :alt alt}]])

(defn hero [next-page]
  (let [file-name "free-install-hero"
        mob-uuid  "5164ba48-d968-466b-9d13-6f5fbc9dcc3d"
        dsk-uuid  "85579511-f08d-4f77-a769-e31a3653e3f4"]
    [:a.bold.shadow.white.center.bg-light-gray
     (merge
      {:data-test "adventure-home-choice-get-started"}
      (utils/route-to next-page))
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
     "FREE Standard Shipping, Always"]]])

(def teal-play-video-mobile
  (svg/white-play-video {:class  "mr1 fill-teal"
                         :height "30px"
                         :width  "30px"}))

(def teal-play-video-desktop
  (svg/white-play-video {:class  "mr1 fill-teal"
                         :height "41px"
                         :width  "41px"}))

(def what-our-customers-are-saying
  (let [video-link (utils/route-to events/navigate-adventure-home {:query-params {:video "free-install"}})]
    [:div.col-11.mx-auto
     [:div.hide-on-mb-tb.flex.justify-center.py3
      [:a.block.relative
       video-link
       (ui/ucare-img {:alt "" :width "212"}
                     "c487eeef-0f84-4378-a9be-13dc7c311e23")
       [:div.absolute.top-0.bottom-0.left-0.right-0.flex.items-center.justify-center.bg-darken-3
        teal-play-video-desktop]]
      [:a.block.ml4.dark-gray
       video-link
       [:div.h4.bold "#MayvennFreeInstall"]
       [:div.h4.my2 "Learn about how to get your own FREE install"]
       [:div.h5.teal.flex.items-center.medium.shout
        "Watch Now"]]]

     [:div.hide-on-dt.flex.justify-center.py3
      [:a.block.relative
       video-link
       (ui/ucare-img {:alt "" :width "152"}
                     "1b58b859-842a-44b1-885c-eac965eeaa0f")
       [:div.absolute.top-0.bottom-0.left-0.right-0.flex.items-center.justify-center.bg-darken-3
        teal-play-video-mobile]]
      [:a.block.ml2.dark-gray
       video-link
       [:h6.bold.mbnp6 "#MayvennFreeInstall"]
       [:p.pt2.h8 "Learn how you can get your " [:span.nowrap "FREE Install"]]
       [:h6.teal.flex.items-center.medium.shout
        "Watch Now"]]]]))

(def an-amazing-deal
  [:div
   [:div
    [:div.col-12.col-8-on-tb.col-6-on-dt.mx-auto.pb6.pt4
     [:div.center.clearfix.py3.h6.dark-gray
      [:h2.mt4.black.relative.z2.mb2 "Dollar for Dollar, Mayvenn Wins"]
      [:div.flex.justify-center
       [:div
        [:div.h6.mx-auto
         {:style {:width "170px"}}
         [:div.absolute.z1
          [:div.relative
           {:style {:top "-40px" :left "-40px"}}
           (ui/ucare-img {:width "250"} "0db72798-6c51-48f2-8206-9fd6d91a3ada")]]
         [:div.relative.z2
          [:div.img-logo.bg-no-repeat.bg-center.bg-contain.mb2.mx-auto
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
          [:div.col-6.mtp2
           [:div.h2.medium "$243"]]]]]]]

     (let [row (fn [text]
                 [:div.flex.justify-center.items-center.center.mx-auto.pt2.relative.z2
                  (ui/ucare-img {:width "9"} "60b946bd-f276-46fc-9d13-3c8562b28d81")
                  [:span.col-6 text]
                  (ui/ucare-img {:width "9"} "f277849f-a27b-48b4-804b-7d523b6d2442")])]
       [:div.clearfix.h6.dark-gray.px4.mx-auto.line-height-2
        (row "High quality hair")
        (row "Free Sew-In and Style")
        (row "30 Day Guarantee")
        (row "Black-owned Company")])]]])

(defn free-install-mayvenn-grid [free-install-mayvenn-ugc]
  [:div.py8.col-10.mx-auto
   [:h2.center "#MayvennFreeInstall"]
   [:h6.center.dark-gray "Showcase your new look by tagging #MayvennFreeInstall"]
   [:div.flex.flex-wrap.pt2
    (for [{:keys [imgs]} (:images free-install-mayvenn-ugc)]
      [:a.col-6.col-3-on-tb-dt.p1
       (ui/aspect-ratio
        1 1
        [:img {:class "col-12"
               :src   (-> imgs :original :src)}])])]])

(def our-story
  (let [we-are-mayvenn-link (utils/route-to events/navigate-adventure-home {:query-params {:video "we-are-mayvenn"}})
        diishan-image       (ui/ucare-img {:class "col-12"} "e2186583-def8-4f97-95bc-180234b5d7f8")
        mikka-image         (ui/ucare-img {:class "col-12"} "838e25f5-cd4b-4e15-bfd9-8bdb4b2ac341")
        stylist-image       (ui/ucare-img {:class "col-12"} "6735b4d5-9b65-4fa9-96cd-871141b28672")
        diishan-image-2     (ui/ucare-img {:class "col-12"} "ec9e0533-9eee-41ae-a61b-8dc22f045cb5")]
    [:div.pt4.px4.pb8
     [:div.h2.center "Better Hair + Better Service Begins Here"]
     [:h6.center.mb2.dark-gray "Founded in Oakland, CA 2013"]

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
          teal-play-video-desktop]]]]]]))

(defn get-a-free-install
  [{:keys [modal?]}]
  (let [step (fn [{:keys [icon-uuid icon-width title description]}]
               [:div.col-12.mt2.center
                (when (not modal?)
                  {:class "col-4-on-dt"})
                [:div.flex.justify-center.items-end.mb2
                 {:style {:height "39px"}}
                 (ui/ucare-img {:alt title :width icon-width} icon-uuid)]
                [:div.h5.teal.medium title]
                [:p.h6.col-10.col-9-on-dt.mx-auto description]])]

    [:div.col-12
     [:div.mt2.flex.flex-column.items-center
      [:h2 "Get a Free Install"]
      [:div.h6.dark-gray "In three easy steps"]]

     [:div.col-8-on-dt.mx-auto.flex.flex-wrap
      (step {:icon-uuid   "6b2b4eee-7063-46b6-8d9e-7f189d1c1add"
             :icon-width  "27"
             :title       "Find a Stylist in Your Area"
             :description "Mayvenn has Certified Stylists all over the country who are dedicated to providing your free install service."})
      (step {:icon-uuid   "e90526f9-546f-4a6d-a05a-3bea94aedc21"
             :icon-width  "28"
             :title       "Buy Any 3 Bundles or More"
             :description "This includes closures, frontals, and 360 frontals. Rest easy - your 100% virgin hair purchase is backed by our 30 day guarantee."})
      (step {:icon-uuid   "7712537c-3805-4d92-90b5-a899748a21c5"
             :icon-width  "35"
             :title       "Show Your Stylist the Voucher"
             :description "We’ll send you a free install voucher via SMS to use for payment at your appointment."})]]))

(defn contact-us-block [url svg title copy]
  [:a.block.py3.col-12.col-4-on-tb-dt
   {:href url}
   svg
   [:div.h6.teal.bold.titlize title]
   [:div.col-8.mx-auto.h6.black copy]])

(def visual-divider
  [:div.py2.mx-auto.teal.border-bottom.border-width-2.mb2-on-tb-dt
   {:style {:width "30px"}}])

(def contact-us
  [:div.bg-transparent-teal.center.py8
   [:h5.mt6.teal.letter-spacing-3.shout.bold "Contact Us"]
   [:h1.black.titleize "Have Questions?"]
   [:h5 "We're here to help"]
   visual-divider
   [:div.flex.flex-wrap.items-baseline.justify-center.col-12.col-8-on-tb-dt.mx-auto
    (contact-us-block
     (ui/sms-url "1-310-733-0284")
     (svg/icon-sms {:height 51
                    :width  56})
     "Live Chat"
     "1-310-733-0284")
    (contact-us-block
     (ui/phone-url "1-310-733-0284")
     (svg/icon-call {:class  "bg-white fill-black stroke-black circle"
                     :height 57
                     :width  57})
     "Call Us"
     "1-310-733-0284")
    (contact-us-block
     (ui/email-url "help@mayvenn.com")
     (svg/icon-email {:height 39
                      :width  56})
     "Email Us"
     "help@mayvenn.com")]])

(defn component [{:keys [store
                         stylist-gallery-open?
                         from-shop-to-freeinstall?
                         faq-data
                         video
                         free-install-mayvenn-ugc
                         gallery-ucare-ids
                         next-page]
                  :as   data}
                 owner
                 opts]
  (component/create
   [:div
    [:div.bg-white.flex.items-center.flex-wrap
     {:style {:height "63px"}}
     (if from-shop-to-freeinstall?
       [:a.block.px3.inherit-color.col-3
        (merge {:data-test "adventure-back-to-shop"}
               (utils/route-to-shop events/navigate-home {}))
        [:div.flex.items-center.justify-center {:style {:height "24px" :width "20px"}}
         (ui/dark-back-arrow {:width "14"})]]
       [:div.col-3])
     [:div.col-6.img-logo.bg-no-repeat.bg-center.bg-contain.teal
      {:style {:height "38px"}}]
     [:div.col-3]]
    [:section (hero next-page)]
    [:section free-shipping-banner]
    [:a {:name "mayvenn-free-install-video"}]
    [:div
     (when video
       (component/build video/component
                        video
                        ;; NOTE(jeff): we use an invalid video slug to preserve back behavior. There probably should be
                        ;;             an investigation to why history is replaced when doing A -> B -> A navigation
                        ;;             (B is removed from history).
                        {:opts {:close-attrs (utils/route-to events/navigate-adventure-home {:query-params {:video "0"}})}}))
     [:section what-our-customers-are-saying]]
    [:section.py10.bg-transparent-teal
     (get-a-free-install {:store                 store
                          :gallery-ucare-ids     gallery-ucare-ids
                          :stylist-portrait      (:portrait store)
                          :stylist-name          (:store-nickname store)
                          :stylist-gallery-open? stylist-gallery-open?})]
    [:section an-amazing-deal]
    [:section [:div (v2/why-mayvenn-is-right-for-you)]]
    [:section (free-install-mayvenn-grid free-install-mayvenn-ugc)]
    [:hr.hide-on-mb-tb.border-top.border-dark-silver.col-9.mx-auto.mb6]
    [:section (faq/component faq-data)]
    [:hr.border-top.border-dark-silver.col-9.mx-auto.my6]
    [:section our-story]
    [:section contact-us]
    (component/build sticky-component {:next-page next-page} nil)]))

(defn query [data]
  (let [store (marquee/query data)]
    {:store                     store
     :gallery-ucare-ids         (->> store
                                     :gallery
                                     :images
                                     (filter (comp (partial = "approved") :status))
                                     (map (comp v2/get-ucare-id-from-url :resizable-url)))
     ;; TODO: get this faq data from checkout popup?
     :faq-data                  (faq/query data)
     :signed-in                 (auth/signed-in data)
     :video                     (get-in data keypaths/adventure-home-video)
     :from-shop-to-freeinstall? (get-in data keypaths/adventure-from-shop-to-freeinstall?)
     :next-page                 events/navigate-adventure-install-type}))

(defn built-component [data opts]
  (component/build component (query data) opts))
