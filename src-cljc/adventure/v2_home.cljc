(ns adventure.v2-home
  (:require [adventure.faq :as faq]
            #?@(:cljs [[om.core :as om]
                       [goog.events.EventType :as EventType]
                       goog.dom
                       goog.style
                       goog.events])
            [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.platform.component-utils :as utils]
            [storefront.events :as events]
            [storefront.components.video :as video]))

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
                 [:div.h6.white.bg-black.medium.px3.py4.flex.items-center
                  [:div.col-7 "We can't wait to pay for your install!"]
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
        mob-uuid  "c78397d4-96bb-4327-b1e2-4f1e05f8b662"
        dsk-uuid  "2a0dc3b9-cefa-4bca-b990-193198a82c80"]
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
     "FREE standard shipping"]]])

(def teal-play-video-mobile
  (svg/white-play-video {:class  "mr1 fill-teal"
                         :height "30px"
                         :width  "30px"}))

(def teal-play-video-desktop
  (svg/white-play-video {:class  "mr1 fill-teal"
                         :height "41px"
                         :width  "41px"}))

(def paying-for-your-next-appt
  [:div
   [:div.h2 "We're paying for your next hair appointment"]
   [:div.h4 "Purchase 3 or more bundles (closures or frontals included) and we’ll pay for you to get them installed. That’s a shampoo, condition, braid down, sew-in, and style, all on us."]
   [:div "What's included?"]
   [:ul.col-7.h6.list-img-purple-checkmark
    (mapv (fn [%] [:li.mb1 %])
          ["Shampoo and condition" "Braid down" "Sew-in and style" "Paid for by Mayvenn"])]])

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
       [:div.h4.bold "#MayvennFleeInstall"]
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
       [:p.pt2.h8 "Learn how you can get your " [:span.nowrap "FREE install"]]
       [:h6.teal.flex.items-center.medium.shout
        "Watch Now"]]]]))

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

(defn ^:private why-mayvenn-entry
  [{:keys [icon-uuid icon-width title description]}]
  [:div.col-12.my2.flex.flex-column.items-center.items-end
   {:class "col-3-on-dt"}
   [:div.flex.justify-center.items-end.mb1
    {:style {:height "35px"}}
    (ui/ucare-img {:alt title :width icon-width} icon-uuid)]
   [:div.h6.medium.mbnp4 title]

   [:p.h6.col-11.center description]])

(def why-mayvenn
  [:div.col-12.bg-transparent-teal.mt3.py8.px4
   [:div.col-11-on-dt.justify-center.flex.flex-wrap.mx-auto.pb2

    [:div.my2.flex.flex-column.items-center.col-12
     [:h2 "Mayvenn is More than a Hair Company"]
     [:div.h6.dark-gray "It's a movement"]]

    (why-mayvenn-entry {:icon-uuid   "ab1d2ed4-ff93-40e6-978a-721133ca88a7"
                        :icon-width  "29"
                        :title       "Top-Notch Customer Service"
                        :description "Our team is made up of hair experts ready to help you by phone, text, and email."})
    (why-mayvenn-entry {:icon-uuid   "8787e30c-2879-4a43-8d01-9d6790575084"
                        :icon-width  "52"
                        :title       "30 Day Guarantee"
                        :description "Wear it, dye it, even cut it! If you're not satisfied we'll exchange it within 30 days."})
    (why-mayvenn-entry {:icon-uuid   "e02561dd-c294-43b7-bb33-c40bfabea518"
                        :icon-width  "35"
                        :title       "100% Virgin Hair"
                        :description "Our hair is gently steam-processed and can last up to a year. Available in 8 textures and 5 shades."})
    (why-mayvenn-entry {:icon-uuid   "5a04ea88-b0f8-416b-a380-1da0baa8a114"
                        :icon-width  "35"
                        :title       "Certified Stylists"
                        :description "Our stylists are chosen because of their industry-leading standards. Both our hair and service are quality guaranteed."})]])

(def our-story
  (let [we-are-mayvenn-link (utils/route-to events/navigate-adventure-home {:query-params {:video "we-are-mayvenn"}})
        diishan-image       (ui/ucare-img {:class "col-12"} "e2186583-def8-4f97-95bc-180234b5d7f8")
        mikka-image         (ui/ucare-img {:class "col-12"} "838e25f5-cd4b-4e15-bfd9-8bdb4b2ac341")
        stylist-image       (ui/ucare-img {:class "col-12"} "6735b4d5-9b65-4fa9-96cd-871141b28672")
        diishan-image-2     (ui/ucare-img {:class "col-12"} "ec9e0533-9eee-41ae-a61b-8dc22f045cb5")]
    [:div.pt4.px4.pb8
     [:div.h2.center "We're Changing The Game"]
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
        [:div.h6.teal.flex.items-center.medium.shout
         "Watch Now"]]
       [:div.col-6.p1 mikka-image]
       [:div.col-6.p1 stylist-image]
       [:div.col-6.px2.dark-gray
        [:h4.my2.line-height-1 "“You deserve quality extensions and exceptional service without the unreasonable price tag.“"]
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
          "“You deserve quality extensions and exceptional service without the unreasonable price tag.“"]
         [:h6.medium.line-height-1.mt2 "- Diishan Imira"]
         [:h6.ml1 "CEO of Mayvenn"]]
        [:div.col-6.p1.flex diishan-image-2]]
       [:a.relative.col-6.p1
        we-are-mayvenn-link
        [:div.relative diishan-image
         [:div.absolute.overlay.flex.items-center.justify-center.bg-darken-3
          teal-play-video-desktop]]]]]]))

(defn free-install-steps
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
      [:h2 "How It Works"]
      [:div.h6.dark-gray "It's simple"]]

     [:div.col-8-on-dt.mx-auto.flex.flex-wrap
      (step {:icon-uuid   "6b2b4eee-7063-46b6-8d9e-7f189d1c1add"
             :icon-width  "27"
             :title       "1. Choose a Mayvenn Certified Stylist"
             :description "We've partnered with thousands of top stylists around the nation. Choose one in your local area and we'll pay the stylist to do your install."})
      (step {:icon-uuid   "e90526f9-546f-4a6d-a05a-3bea94aedc21"
             :icon-width  "28"
             :title       "2. Buy Any Three Bundles or More"
             :description "This includes closures, frontals, and 360 frontals. Risk free — your virgin hair and service are covered by our 30 day guarantee."})
      (step {:icon-uuid   "7712537c-3805-4d92-90b5-a899748a21c5"
             :icon-width  "35"
             :title       "3. Schedule Your Appointment"
             :description "We’ll connect you to your Mayvenn Certified Stylist and book an install appointment that’s convenient for you."})]]))

(def certified-stylists
  [:div.col-6-on-tb-dt.col-12-on-mb.mx-auto
   {:style {:height "696px"
            :background-size     "cover"
            :background-position "center"
            :background-image    "url('//ucarecdn.com/491eefec-02f6-447d-942d-bd864aaad5ed/-/format/auto/bg.png')"}}
   [:div "Who's doing my hair?"]
   [:div "Our Certified Stylist are the best of the best. They're chosen because of their top-rated reviews, professionalism, and amazing work."]])

(def hair-quality
  [:div.col-6-on-tb-dt.col-12-on-mb.mx-auto
   {:style {:height "696px"
            :background-size     "cover"
            :background-position "center"
            :background-image    "url('//ucarecdn.com/6ea84755-e0ba-4ccc-861e-2d39a711a982/-/format/auto/bg.png')"}}

   [:div "Highest quality Virgin hair"]
   [:div "Our bundles, closures, and frontals are crafted with the highest industry standards and come in a variety of textures and colors."]])

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
     (ui/sms-url "346-49")
     (svg/icon-sms {:height 51
                    :width  56})
     "Live Chat"
     "Text: 346-49")
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
       [:a.block.inherit-color.col-3.flex.items.center
        (merge {:data-test "adventure-back-to-shop"}
               (utils/route-to-shop events/navigate-home {}))
        [:div.flex.items-center.justify-center {:style {:height "60px" :width "60px"}}
         (svg/back-arrow {:width "24px" :height "24px"})]]
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
     [:section paying-for-your-next-appt]
     [:section what-our-customers-are-saying]]
    [:section.py10.bg-transparent-teal
     (free-install-steps {:store                 store
                          :gallery-ucare-ids     gallery-ucare-ids
                          :stylist-portrait      (:portrait store)
                          :stylist-name          (:store-nickname store)
                          :stylist-gallery-open? stylist-gallery-open?})]
    [:section certified-stylists]
    [:section hair-quality]
    [:section (free-install-mayvenn-grid free-install-mayvenn-ugc)]
    [:section (faq/component faq-data)]
    [:section why-mayvenn]
    [:section our-story]
    [:section contact-us]
    (component/build sticky-component {:next-page next-page} nil)]))

#_[:hr.hide-on-mb-tb.border-top.border-dark-silver.col-9.mx-auto.mb6]
