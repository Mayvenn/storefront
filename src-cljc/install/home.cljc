(ns install.home
  (:require #?@(:cljs [[om.core :as om]
                       [goog.events]
                       [goog.dom]
                       [goog.style]
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
            [storefront.components.v2-home :as v2-home]
            [storefront.accessors.experiments :as experiments]))

(defn sticky-component
  [data owner opts]
  #?(:clj (component/create [:div])
     :cljs
     (letfn [(handle-scroll [e] (om/set-state! owner :show? (or
                                                             (om/get-state owner :show?)
                                                             (< 3493 (+ (.-y (goog.dom/getDocumentScroll))
                                                                        (.-height (goog.dom/getViewportSize)))))))
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
               [:div.border-top.border-dark-gray.border-width-2
                [:a.h2.bold.white.bg-purple.px3.py2.flex.items-center.justify-center
                 {:href "#"}
                 "Get $25 – Share Your Opinion"
                 [:div.rotate-180.stroke-white.ml2
                  (svg/dropdown-arrow {:height "16" :width "16"})]]]]]]))))))

(def visual-divider
  [:div.py2.mx-auto.teal.border-bottom.border-width-2.mb2-on-tb-dt
   {:style {:width "30px"}}])

(def header
  [:div.flex.items-center.justify-between.px6.px3-on-mb.py4.py2-on-mb
   [:div.px5.px0-on-mb
    [:img {:src (assets/path "/images/header_logo.svg")
           :style {:height "40px"}}]
    [:div.h7 "Questions? Text or call: "
     (ui/link :link/phone :a.inherit-color {} "1-310-733-0284")]]])

(defn easy-step-block [img title copy]
  [:div.py2.col-12.col-4-on-tb-dt
   [:div.flex.justify-center img]
   [:div.h5.teal.medium.titlize.my1 title]
   [:div.col-9.mx-auto.h6.dark-gray copy]])

(def three-easy-steps
  [:div.bg-transparent-teal.center.py8
   [:a {:name "3-easy-steps"}]
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

(def shop-bar
  [:div.bg-black.white.p4.h5.medium
   [:div.max-580.mx-auto.flex.items-center.justify-around
    [:div.px2 "Ready to shop now? Shop without a consultation!"]
    [:div.col-6.col-2-on-tb-dt.ml3
     (ui/teal-button (assoc (utils/route-to-shop events/navigate-home {:query-params {:utm_medium "fvlanding"}})
                            :data-test "shop"
                            :height-class "py1")
                     "Shop")]]])

(def hero
  [:a.bold.shadow.white.center.bg-light-gray
   {:href "#3-easy-steps"}
   (v2-home/hero-image {:mobile-url  "//ucarecdn.com/72853f68-1152-4dab-91d9-cee34ea81f74/"
                        :desktop-url "//ucarecdn.com/786dae46-6b54-45c1-849f-5e1f9344aa69/"
                        :file-name   "free-install-hero"
                        :alt         "Beautiful Virgin Hair Installed for FREE"})])

(defn ^:private component
  [{:keys [carousel-certified-stylist faq-accordion popup-data phone-capture?]} owner opts]
  (component/create
   [:div
    header
    hero
    three-easy-steps
    (component/build certified-stylists/component carousel-certified-stylist {})
    shop-bar
    (faq faq-accordion)
    contact-us
    (when phone-capture?
      (component/build sticky-component {} nil))]))

(defn ^:private query [data]
  {:popup-data                 #?(:cljs (popup/query data)
                                  :clj {})
   :carousel-certified-stylist {:stylist-gallery-index (get-in data keypaths/carousel-stylist-gallery-index)
                                :gallery-image-index   (get-in data keypaths/carousel-stylist-gallery-image-index)}
   :faq-accordion              {:expanded-indices (get-in data keypaths/accordion-freeinstall-home-expanded-indices)}
   :phone-capture?             (experiments/phone-capture? data)})

(defn built-component
  [data opts]
  (component/build component (query data) opts))

;; TODO Consider renaming file and event to something free install specific
(defmethod effects/perform-effects events/navigate-install-home [_ _ _ _ app-state]
  #?(:cljs (pixlee-hook/fetch-album-by-keyword :free-install-home)))

(defmethod transitions/transition-state events/control-install-landing-page-toggle-accordion
  [_ _ {index :index} app-state]
  (update-in app-state
             keypaths/accordion-freeinstall-home-expanded-indices
             (fn [existing-indicies]
               (if (contains? existing-indicies index)
                 #{}
                 #{index}))))
