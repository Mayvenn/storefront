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
            [storefront.components.video :as video]
            [storefront.components.v2-home :as v2-home]))

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

(defn easy-step-block [img title copy]
  [:div.py3.col-12.col-4-on-tb-dt
   [:div.flex.justify-center img]
   [:div.h5.teal.medium.titlize.my1 title]
   [:div.col-9.mx-auto.h6.dark-gray copy]])

(def three-easy-steps
  [:div.bg-transparent-teal.center.py8
   [:div.mt2.mb1
    [:h2.black "Get a FREE Install"]
    [:div.h6.dark-gray "In 3 easy steps"]]
   [:div.flex.flex-wrap.items-start.justify-center
    (easy-step-block (ui/ucare-img {:width "45"} "947c8a8a-b3a4-41c3-bf7c-0f274ef86e51")
                     "Talk To A Mayvenn Stylist"
                     "Learn how to get your free install and choose your Mayvenn hair. Bundles start at $59.")
    (easy-step-block (ui/ucare-img {:width "38"} "bf6667b9-525e-4bc6-8e45-520d835e0797")
                     "Order Your Bundles"
                     "All bundles are high quality, 100% human hair and backed by our 30-day guarantee.")
    (easy-step-block (ui/ucare-img {:width "27"} "b06a282a-27a0-4a4c-aa85-77868556ac1d")
                     "Get Your Hair Installed For FREE"
                     "Visit your Mayvenn Stylist and get your hair installed absolutely free. ")]])

(defn star [type]
  [:span.mrp1
   (ui/ucare-img
    {:width "13"}
    (case type
      :whole "5a9df759-cf40-4599-8ce6-c61502635213"
      :half  "d3ff89f5-533c-418f-80ef-27aa68e40eb1"
      :empty "92d024c6-1e82-4561-925a-00d45862e358"
      nil))])

(defn star-rating
  [rating]
  (let [rounded-rating (-> rating (* 2) float numbers/round (/ 2) float)
        whole-stars    (int rounded-rating)
        half-stars     (if (== whole-stars rounded-rating) 0 1)
        empty-stars    (- 5 whole-stars half-stars)]
    [:div.flex.items-center
     (repeat whole-stars (star :whole))
     (repeat half-stars (star :half))
     (repeat empty-stars (star :empty))
     [:span.mlp2.h6 rating]]))

(defn stylist-card
  []
  [:div.bg-white.p2.h6
   [:div.flex
    [:div.mr2 (ui/ucare-img {:width "104"} "b06a282a-27a0-4a4c-aa85-77868556ac1d")]
    [:div.flex-grow-1
     [:div.h4 "Aundria Carter"]
     [:div (star-rating 2.6)]
     [:div "Giovanni Hair Salon"]
     [:div "555-555-5555"]
     [:div "Licensed Salon Stylist"]
     [:div "10 yrs Experience"]]]
   [:div.line-height-2.medium.dark-gray
    "Hello. I’m Aundria, head stylist at Giovanni Hair Salon in Oakland. "
    "Everyone that sits in my chair gets the royal treatment. "
    "A great look starts with tiny details so we’ll have a consultation either by phone or in person. "
    "We’ll create a plan together and then sit back and relax because you are in great hands."]
   [:div "Carousel"]
   [:div "CTA"]])

(def stylist-cards
  [:div.p3.bg-light-silver
   (stylist-card)])

(defn faq
  [faq-accordion]
  [:div.my10.px4.col-12.col-6-on-tb.col-4-on-dt.mx-auto
   [:div.pb2.h6.teal.bold.center.letter-spacing-3 "Q + A"]
   [:h2.center.my5 "Frequently Asked Questions"]
   (component/build accordion/component
                    (assoc faq-accordion :sections faq-accordion/free-install-sections)
                    {:opts {:section-click-event events/control-install-landing-page-toggle-accordion}})])

(defn contact-us-block [url svg title copy]
  [:a.block.py3.col-12.col-4-on-tb-dt
   {:href url}
   svg
   [:div.h6.teal.bold.titlize title]
   [:div.col-8.mx-auto.h6.black copy]])

(def contact-us
  [:div.bg-transparent-teal.center.py8
   [:h5.mt6.teal.letter-spacing-3.shout.bold "Contact Us"]
   [:h1.black.titleize "Have Questions?"]
   [:h5 "We are here to help you"]
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
     "Speak With Us"
     "1-310-733-0284")
    (contact-us-block
     (ui/email-url "help@mayvenn.com")
     (svg/icon-email {:height 39
                      :width  56})
     "Write To Us"
     "help@mayvenn.com")]])

(defn ^:private component
  [{:keys [header video carousel-certified-stylist ugc-carousel faq-accordion popup-data]} owner opts]
  (component/create
   [:div
    (when video
      (component/build video/component
                       video
                       ;; NOTE(jeff): we use an invalid video slug to preserve back behavior. There probably should be
                       ;;             an investigation to why history is replaced when doing A -> B -> A navigation
                       ;;             (B is removed from history).
                       {:opts {:close-attrs (utils/route-to events/navigate-install-home {:query-params {:video "0"}})}}))
    (component/build relative-header header nil)
    (component/build fixed-header header nil)
    (v2-home/hero)
    three-easy-steps
    (faq faq-accordion)
    contact-us]))

(defn ^:private query [data]
  {:header                     {:text-or-call-number "1-310-733-0284"}
   :video                      (get-in data keypaths/fvlanding-video)
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

(def ^:private slug->video
  (assoc certified-stylists/video-slug->video
         "free-install" {:youtube-id "cWkSO_2nnD4"}))

(defmethod transitions/transition-state events/navigate-install-home
  [_ _ {:keys [query-params]} app-state]
  (assoc-in app-state keypaths/fvlanding-video (slug->video (:video query-params))))

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
  (update-in app-state
             keypaths/accordion-freeinstall-home-expanded-indices
             (fn [existing-indicies]
               (if (contains? existing-indicies index)
                 #{}
                 #{index}))))
