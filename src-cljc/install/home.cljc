(ns install.home
  (:require #?@(:cljs [[om.core :as om]
                       [goog.events]
                       [goog.dom]
                       [storefront.hooks.pixlee :as pixlee-hook]
                       [storefront.components.popup :as popup]
                       [goog.events.EventType :as EventType]])
            [install.certified-stylists :as certified-stylists]
            [install.faq-accordion :as faq-accordion]
            [storefront.accessors.pixlee :as pixlee]
            [storefront.assets :as assets]
            [storefront.component :as component]
            [storefront.components.accordion :as accordion]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.carousel :as carousel]
            [storefront.platform.component-utils :as utils]
            [storefront.transitions :as transitions]
            [storefront.components.free-install-video :as free-install-video]))

(def visual-divider
  [:div.py2.mx-auto.teal.border-bottom.border-width-2.mb2-on-tb-dt
   {:style {:width "30px"}}])

(defn header [text-or-call-number]
  [:div.flex.items-center.justify-between.px6.px3-on-mb.py4.py2-on-mb
   [:div.px5.px0-on-mb
    [:img {:src (assets/path "/images/header_logo.svg")
           :style {:height "40px"}}]
    [:div.h7 "Questions? Text or call: "
     (ui/link :link/phone :a.inherit-color {} text-or-call-number)]]
   [:div.col.h5
    {:style {:width "100px"}}
    (ui/teal-button (assoc (utils/route-to-shop events/navigate-home {:query-params {:utm_medium "fvlanding"}})
                           :data-test "shop"
                           :height-class "py1")
                    "Shop")]])

(defn relative-header [{:keys [text-or-call-number]} owner opts]
  (component/create (header text-or-call-number)))

(defn fixed-header [{:keys [text-or-call-number]} owner opts]
  #?(:cljs
     (letfn [(handle-scroll [e] (om/set-state! owner :show? (< 750 (.-y (goog.dom/getDocumentScroll)))))]
       (reify
         om/IInitState
         (init-state [this]
           {:show? false})
         om/IDidMount
         (did-mount [this]
           (goog.events/listen js/window EventType/SCROLL handle-scroll))
         om/IWillUnmount
         (will-unmount [this]
           (goog.events/unlisten js/window EventType/SCROLL handle-scroll))
         om/IWillReceiveProps
         (will-receive-props [this next-props])
         om/IRenderState
         (render-state [this {:keys [show?]}]
           (component/html
            [:div.fixed.top-0.left-0.right-0.z4.bg-white.lit
             (if show?
               {:style {:margin-top "0"}
                :class "transition-2"}
               {:style {:margin-top "-100px"}})
             (header text-or-call-number)]))))
     :clj [:span]))

(defn ^:private stat-block [header content]
  [:div.center.p2
   [:div.bold.teal.letter-spacing-1 header]
   [:div.h6.line-height-1 content]])

(defn ^:private as-seen-in-logos [& logo-img-ids]
  (for [{:keys [id width]} logo-img-ids]
    (ui/ucare-img {:class "mx2 my2" :width width} id)))

(defn img-with-circle [diameter img-id content]
  (let [radius (quot diameter 2)]
    [:div
     {:style {:padding-bottom (str radius "px")}}
     [:div.relative
      [:div
       (ui/ucare-img {:class "col-12"} img-id)]
      [:div.bg-teal.border.border-white.border-width-3.circle.absolute.right-0.left-0.mx-auto.flex.items-center.justify-center
       {:style {:height (str diameter "px")
                :width (str diameter "px")
                :bottom (str "-" (- radius 4) "px")}}
       content]]]))

(defn ^:private easy-step [number title copy img-id]
  [:div.py4
   [:div.px6 (img-with-circle 60 img-id [:div.h1.bold.white number])]
   [:div.h3.px6-on-dt title]
   [:div.dark-gray.h6.px6-on-dt copy]])

(defn ^:private happy-customer [img-id testimony customer-name]
  [:div.p4.flex.items-start.justify-center.col.col-12.col-4-on-tb-dt
   [:div.col-5.col-6-on-tb.col-3-on-dt
    (ui/ucare-img {:class "col-12"} img-id)]
   [:div.px2.flex.flex-column.h5.hide-on-mb
    [:div.line-height-3 \“ testimony \”]
    [:div.bold "- "  customer-name]]
   [:div.px2.flex.flex-column.h6.hide-on-tb-dt
    [:div.line-height-3 \“ testimony \”]
    [:div.bold "- "  customer-name]]])

(defn ^:private embedded-carousel-slides [album]
  (map-indexed
   (fn [idx image]
     [:div.p1
      [:a (utils/fake-href events/control-install-landing-page-ugc-modal-open {:index idx})
       (ui/aspect-ratio
        1 1
        {:class "flex items-center"}
        [:img.col-12 (:large (:imgs image))])]])
   album))

(defn ^:private modal-carousel-slides [album]
  (map-indexed
   (fn [idx {:keys [imgs id user-handle social-service] :as image}]
     [:div
      (ui/aspect-ratio
       1 1
       {:class "flex items-center"}
       [:img.col-12 (:large imgs)])
      [:div.flex.items-center.justify-between.p2.h5
       [:div.dark-gray.medium.pl4 {:style {:word-break "break-all"}} "@" user-handle]
       [:div.mr4.mx1.line-height-1 {:style {:width "1em" :height "1em"}}
        (svg/social-icon social-service)]]
      [:a.col-11.btn.btn-primary.mx-auto.mb2
       (utils/route-to-shop events/navigate-shop-by-look-details
                            {:look-id       id
                             :album-keyword :free-install-home})
       "Shop This Look"]])
   album))

(defn icon-block [svg title copy]
  [:div.py3.col-12.col-4-on-tb-dt
   svg
   [:div.h6.teal.bold.titlize title]
   [:div.col-8.mx-auto.h6.black copy]])

(defn ^:private component
  [{:keys [header show-video? video carousel-certified-stylist ugc-carousel faq-accordion popup-data]} owner opts]
  (component/create
   [:div
    (when show-video?
      (component/build free-install-video/component
                       video
                       {:opts {:close-attrs (utils/route-to events/navigate-install-home {:query-params {:video "0"}})}}))
    (component/build relative-header header nil)
    (component/build fixed-header header nil)
    [:div.shadow.bg-cover.bg-center.bg-top.bg-free-install-landing.col-12.p4
     [:div.hide-on-mb.px6.py3
      [:div.teal.line-height-2.bold.pt2.shout {:style {:font-size "57px"}} "free install"]
      [:div.medium.line-height-2.col-6.white.mt2
       {:style {:font-size "36px"}}
       "Everyone in Fayetteville can get a FREE install by a Mayvenn certified stylist."]
      [:a.shout.white.flex.items-center.pt8.h4.medium
       (utils/route-to events/navigate-install-home {:query-params {:video "1"}})
       (svg/clear-play-video {:class        "mr2"
                              :fill         "white"
                              :fill-opacity "0.9"
                              :height       "50px"
                              :width        "50px"})
       "Watch Now"]]

     [:div.hide-on-tb-dt
      [:div.teal.h1.bold.pt2.shout "free install"]
      [:div.medium.letter-spacing-1.col-7.h3.white
       "Everyone in Fayetteville can get a FREE install by a Mayvenn certified stylist."]
      [:a.shout.white.flex.items-center.pt4.h6.medium
       (utils/route-to events/navigate-install-home {:query-params {:video "1"}})
       (svg/clear-play-video {:class        "mr2"
                              :fill         "white"
                              :fill-opacity "0.9"
                              :height       "50px"
                              :width        "50px"})
       "Watch Now"]]]

    [:div.flex.items-start.justify-center.p1.pt2.pb3.py5-on-tb-dt
     (stat-block "100,000+" "Mayvenn Stylists Nationwide")
     (stat-block "200,000+" "Happy Mayvenn Customers")
     (stat-block "100%" "Guaranteed Human Hair")]

    [:div.col-12.bg-gray.py2
     [:div.dark-gray.col-12.center.h7.medium.letter-spacing-4.p1 "AS SEEN IN"]
     (into [:div.flex.flex-wrap.justify-around.items-center]
           (as-seen-in-logos
            {:id "a2e763ea-1837-43fd-8531-440d18360e1e" :width "160"}
            {:id "74f56834-b879-415a-9e55-87a059767297" :width "75"}
            {:id "b1a3d9c1-80a0-4549-9603-36fb65b5bebb" :width "56"}
            {:id "4f8c1a9d-ab71-4881-97df-b4a724354faa" :width "45"}
            {:id "3428dfc2-bc0a-40f2-9bdd-c79df6abd63f" :width "150"}))]

    [:div.border.border-teal.border-width-2.mx3.center.p3.my6
     [:div
      [:div.py2
       [:div.teal.letter-spacing-6.bold.h6 "3 EASY STEPS"]
       [:div.h2 "Get a FREE install"]]
      [:div.h6 "Purchase 3 bundles of Mayvenn hair and your install by a Mayvenn Certified Stylist is FREE!"]]
     [:div.flex-on-tb-dt.items-start.justify-center
      (easy-step 1
                 "Buy 3 bundles or more"
                 "Closures and fronts count, too! Our hair is 100% human, backed by a 30 day guarantee and starts at $30 per bundle."
                 "fdcc8acc-443c-4b2f-b510-0d940297f997")
      (easy-step 2
                 "A Fayetteville, NC exclusive offer"
                 "Your Mayvenn order must be shipped to a qualified address in Fayetteville, NC."
                 "6263c536-f548-45dc-ba89-ca68ad7c44c8")
      (easy-step 3
                 "Book your FREE install"
                 "After completing your purchase, Mayvell will contact you to arrange your FREE install appointment with a Mayvenn Certified Stylists."
                 "52dcdffb-cc44-4f80-88c8-325de7c3fa62")]]

    [:div.bg-transparent-teal.py6
     [:div.pt4.teal.letter-spacing-6.bold.center.h6 "HAPPY CUSTOMERS"]
     [:div.px3.clearfix
      (happy-customer "dd6b26ed-1f15-437c-ac2c-e289d3a854fe"
                      "I freaking love Mayvenn hair, like they are BOMB.COM GIRL! Yass!"
                      "J Luxe")
      (happy-customer "014856c8-7fa5-40b7-a707-48361c37f04f"
                      "I'm 100% satisfied, that's all I can say, I'm 100% satisfied"
                      "Cara Scott")
      (happy-customer "b7258cae-1aac-4755-9f61-90e9908ff7a7"
                      "Ugh God you guys, like you don't understand, I love this hair."
                      "Tiona Chantel")]]

    [:div.center.pt6.px6
     [:div.pt4.teal.letter-spacing-6.bold.h6 "LICENSED STYLISTS"]
     [:div.h2 "Mayvenn Certified Stylists"]
     [:div.h6.pt2 "We have partnered with a select group of experienced and licensed stylists in Fayetteville, NC to give you a FREE high quality standard install."]]

    (component/build certified-stylists/component carousel-certified-stylist {})
    [:div.bg-transparent-teal.center
     [:h2.pt6.pb4 "#MayvennFreeInstall"]
     (let [index (:index ugc-carousel)]
       [:div
        (component/build carousel/component
                         {:slides   (embedded-carousel-slides (:album (:carousel-data ugc-carousel)))
                          :settings {:infinite     true
                                     :swipe        true
                                     ;; The breakpoints are mobile-last. That is, the
                                     ;; default values apply to the largest screens, and
                                     ;; 1000 means 1000 and below.
                                     :slidesToShow 10
                                     :responsive   [{:breakpoint 999
                                                     :settings   {:slidesToShow 7}}
                                                    {:breakpoint 749
                                                     :settings   {:slidesToShow 3}}]}}
                         opts)
        (when (:open? ugc-carousel)
          (let [close-attrs (utils/fake-href events/control-install-landing-page-ugc-modal-dismiss)]
            (ui/modal
             {:close-attrs close-attrs
              :col-class   "col-12"}
             [:div.bg-white.relative.col-11.mx-auto
              {:style {:max-width "750px"}}
              (component/build carousel/component
                               {:slides   (modal-carousel-slides (:album (:carousel-data ugc-carousel)))
                                :settings {:slidesToShow 1
                                           :initialSlide index}}
                               {})
              [:div.absolute
               {:style {:top "1.5rem" :right "1.5rem"}}
               (ui/modal-close {:class       "stroke-dark-gray fill-gray"
                                :close-attrs close-attrs})]])))])
     [:div.col-5-on-tb-dt.mx-auto.pb2-on-dt.center.dark-gray.p2
      [:p.h6.hide-on-tb-dt
       "Want a chance to be featured? Share your free install style by tagging us with #MayvennFreeInstall"]
      [:p.h5.hide-on-mb
       "Want a chance to be featured? Share your free install style by tagging us with #MayvennFreeInstall"]]]
    [:div.bg-black.white.p4.h5.medium
     [:div.max-580.mx-auto.flex.items-center.justify-around
      [:div.px2 "Buy 3 bundles or more and get a FREE install!"]
      [:div.col-6.col-2-on-tb-dt.ml3
       (ui/teal-button (assoc (utils/route-to-shop events/navigate-home {:query-params {:utm_medium "fvlanding"}})
                              :data-test "shop"
                              :height-class "py1")
                       "Shop")]]]

    [:div.bg-transparent-teal.center.py8
     [:h5.mt6.teal.letter-spacing-3.shout.bold "The Hookup"]
     [:h1.black "Treat Yourself"]
     visual-divider
     [:div.flex.flex-wrap.items-start.justify-center
      (icon-block (svg/coin-stack {:height 71
                                   :width  82})
                  "Free Install"
                  "Get your hair installed absolutely FREE!")
      (icon-block (svg/guarantee {:class  "bg-white fill-black stroke-black circle"
                                  :height 72
                                  :width  72})
                  "Risk Free"
                  "Wear it, dye it, style it. If you don’t love it your hair we’ll exchange it within 30 days of purchase.")
      (icon-block (svg/certified-ribbon {:height 71
                                         :width  51})
                  "Mayvenn Certified Stylists"
                  "All Mayvenn Certified Stylists are licenced and work in salons.")]]
    [:div.my10.px4.col-12.col-6-on-tb.col-4-on-dt.mx-auto
     [:div.pb2.h6.teal.bold.center.letter-spacing-3 "Q + A"]
     [:h2.center.my5 "Frequently Asked Questions"]
     (component/build accordion/component
                      (assoc faq-accordion :sections faq-accordion/free-install-sections)
                      {:opts {:section-click-event events/control-install-landing-page-toggle-accordion}})]

    [:div.bg-transparent-teal.center.py8
     [:h5.mt6.teal.letter-spacing-3.shout.bold "Contact Us"]
     [:h1.black.titleize "Have Questions?"]
     [:h5 "We are here to help you"]
     visual-divider
     [:div.flex.flex-wrap.items-baseline.justify-center.col-12.col-8-on-tb-dt.mx-auto
      (icon-block (svg/icon-sms {:height 51
                                 :width  56})
                  "Live Chat"
                  (ui/link :link/sms :a.black {} "1-310-0284"))
      (icon-block (svg/icon-call {:class  "bg-white fill-black stroke-black circle"
                                  :height 57
                                  :width  57})
                  "Speak With Us"
                  (ui/link :link/phone :a.black {} "1-310-0284"))
      (icon-block (svg/icon-email {:height 39
                                   :width  56})
                  "Write To Us"
                  (ui/link :link/email :a.black {} "help@mayvenn.com"))]]]))

(defn ^:private query [data]
  {:header                     {:text-or-call-number "1-310-733-0284"}
   :show-video?                (get-in data keypaths/fvlanding-show-video?)
   :video                      (free-install-video/query data)
   :carousel-certified-stylist {:index         (get-in data keypaths/carousel-certified-stylist-index)
                                :sliding?      (get-in data keypaths/carousel-certified-stylist-sliding?)
                                :gallery-open? (get-in data keypaths/carousel-stylist-gallery-open?)}

   :popup-data    #?(:cljs (popup/query data)
                     :clj {})
   :ugc-carousel  (when-let [ugc (get-in data keypaths/ugc)]
                    (when-let [images (pixlee/images-in-album ugc :free-install-home)]
                      {:carousel-data {:album images}
                       :index         (get-in data keypaths/fvlanding-carousel-ugc-index)
                       :open?         (get-in data keypaths/fvlanding-carousel-ugc-open?)}))
   :faq-accordion {:expanded-indices (get-in data keypaths/accordion-freeinstall-home-expanded-indices)}})

(defn built-component
  [data opts]
  (component/build component (query data) opts))

;; TODO Consider renaming file and event to something free install specific
(defmethod effects/perform-effects events/navigate-install-home [_ _ _ _ app-state]
  #?(:cljs (pixlee-hook/fetch-album-by-keyword :free-install-home)))

(defmethod transitions/transition-state events/navigate-install-home
  [_ _ {:keys [query-params]} app-state]
  (assoc-in app-state keypaths/fvlanding-show-video? (= "1" (:video query-params))))

(defmethod transitions/transition-state events/control-install-landing-page-ugc-modal-open
  [_ _ {:keys [index]} app-state]
  (-> app-state
      (assoc-in keypaths/fvlanding-carousel-ugc-open? true)
      (assoc-in keypaths/fvlanding-carousel-ugc-index index)))

(defmethod transitions/transition-state events/control-install-landing-page-ugc-modal-dismiss
  [_ _ _ app-state]
  (assoc-in app-state keypaths/fvlanding-carousel-ugc-open? false))

(defmethod transitions/transition-state events/control-install-landing-page-toggle-accordion
  [_ _ {index :index} app-state]
  (assoc-in app-state
             keypaths/accordion-freeinstall-home-expanded-indices
             #{index}))
