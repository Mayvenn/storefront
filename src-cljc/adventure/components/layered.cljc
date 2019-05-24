(ns adventure.components.layered
  (:require [clojure.string :as string]
            storefront.keypaths
            [storefront.component :as component]
            [storefront.components.accordion :as accordion]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.components.video :as video]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]
            #?@(:cljs [[om.core :as om]
                       [goog.events.EventType :as EventType]
                       goog.dom
                       goog.style
                       goog.events
                       [storefront.browser.scroll :as scroll]])))

(defn freeinstall-domain [environment path]
  (let [domain (case environment
                 "production" "freeinstall.mayvenn.com"
                 "acceptance" "freeinstall.diva-acceptance.com"
                 "freeinstall.storefront.localhost")]
    (str "//" domain path)))

(defn ->freeinstall-url [environment source path]
  (let [domain (case environment
                 "production" "freeinstall.mayvenn.com"
                 "acceptance" "freeinstall.diva-acceptance.com"
                 "freeinstall.storefront.localhost")]
    (str "//" domain path "?"
         (string/join
          "&"
          ["utm_medium=referral"
           (str "utm_source=" source)
           "utm_term=fi_shoptofreeinstall"]))))

(defmulti layer-view (fn [{:keys [layer/type]} _ _] type))

(defmethod layer-view :hero
  [data owner opts]
  (component/create
   (ui/ucare-img {:class "col-12"} (:photo/uuid data))))

(defn hero-image [{:keys [desktop-url mobile-url file-name alt]}]
  [:picture
   ;; Tablet/Desktop
   [:source {:media   "(min-width: 750px)"
             :src-set (str desktop-url "-/format/jpeg/-/quality/best/" file-name " 1x")}]
   ;; Mobile
   [:img.block.col-12 {:src (str mobile-url "-/format/jpeg/" file-name)
                       :alt alt}]])

(defmethod layer-view :hero-with-links
  [data _ _]
  (component/create
   [:div.mx-auto.relative {:style {:min-height "300px"}}
    (let [{:photo/keys [mob-uuid dsk-uuid file-name alt]} data]
      (hero-image {:mobile-url  (str "//ucarecdn.com/" mob-uuid "/")
                   :desktop-url (str "//ucarecdn.com/" dsk-uuid "/")
                   :file-name   file-name
                   :alt         alt}))
    [:div.relative.flex.justify-center
     [:div.absolute.bottom-0.col-6-on-tb-dt.col-12.pb2.mb3-on-dt
      [:div.col.col-12.flex.justify-center
       (let [num-buttons (count (:buttons data))]
         (for [button (:buttons data)]
           [:div.px2 {:class (str "col-" (if (= num-buttons 1) 9 6))} (apply ui/teal-button button)]))]]]]))

(defmethod layer-view :free-standard-shipping-bar
  [_ _ _]
  (component/create
   [:div.mx-auto {:style {:height "3em"}}
    [:div.bg-black.flex.items-center.justify-center
     {:style {:height "2.25em"
              :margin-top "-1px"
              :padding-top "1px"}}
     [:div.px2
      (ui/ucare-img {:alt "" :height "25"}
                    "38d0a770-2dcd-47a3-a035-fc3ccad11037")]
     [:div.h7.white.medium
      "FREE standard shipping"]]]))

(defn ^:private cta-with-chevron
  [{:cta/keys [navigation-message href value]}]
  (when (or navigation-message href)
    [:a.block.h4.medium.teal.my2
     (merge
      (when href
        {:href href})
      (when navigation-message
        (apply utils/route-to navigation-message)))
     value
     (svg/dropdown-arrow {:class  "stroke-teal ml2"
                          :style  {:stroke-width "3px"
                                   :transform "rotate(-90deg)"}
                          :height "14px"
                          :width  "14px"})]))

(defmethod layer-view :text-block
  [data _ _]
  (component/create
   [:div.pt10.pb2.px6.center.col-6-on-dt.mx-auto
    (when-let [n (:anchor/name data)]
      [:a {:name n}])
    (when-let [v (:header/value data)]
      [:div.h2 v])
    [:div.h5.dark-gray.mt3 (:body/value data)]
    (cta-with-chevron data)]))

(defmethod layer-view :escape-hatch
  [_ _ _]
  (component/create
   [:div.col-12.bg-fate-white.py8.flex.flex-column.items-center.justify-center
    [:div.h2.col-8.center "Not interested in a Mayvenn Install?"]

    [:a.block.h3.medium.teal.mt2.flex.items-center
     (utils/fake-href events/control-open-shop-escape-hatch)
     "Shop all products"
     [:div.flex.items-end.ml2 {:style {:transform "rotate(-90deg)"}}
      (svg/dropdown-arrow {:class  "stroke-teal"
                           :style  {:stroke-width "3px"}
                           :height "14px"
                           :width  "14px"})]]]))

(defmethod layer-view :image-block
  [{:photo/keys [mob-uuid
                 dsk-uuid
                 file-name
                 alt]} _ _]
  (component/create
   [:div.center.mx-auto
    (hero-image {:mobile-url  (str "//ucarecdn.com/" mob-uuid "/")
                 :desktop-url (str "//ucarecdn.com/" dsk-uuid "/")
                 :file-name   file-name
                 :alt         alt})]))

(defmethod layer-view :checklist
  [data _ _]
  (component/create
   [:div.pb10.px6.center.col-6-on-dt.mx-auto
    (when-let [v (:header/value data)]
      [:div.h2 v])
    (when-let [v (:subheader/value data)]
      [:div.h5.mt6.mb4 v])
    [:ul.h6.list-img-purple-checkmark.dark-gray.left-align.mx-auto
     {:style {:width "max-content"}}
     (for [b (:bullets data)]
       [:li.mb1.pl1 b])]]))

(def teal-play-video-mobile
  (svg/white-play-video {:class  "mr1 fill-teal"
                         :height "30px"
                         :width  "30px"}))

(def teal-play-video-desktop
  (svg/white-play-video {:class  "mr1 fill-teal"
                         :height "41px"
                         :width  "41px"}))

(defmethod layer-view :video-overlay
  [data _ _]
  (component/create
   (when-let [video (:video data)]
     (component/build video/component
                      video
                      ;; NOTE(jeff): we use an invalid video slug to preserve back behavior. There probably should be
                      ;;             an investigation to why history is replaced when doing A -> B -> A navigation
                      ;;             (B is removed from history).
                      {:opts
                       {:close-attrs
                        (utils/route-to events/navigate-home
                                        {:query-params {:video "0"}})}}))))

(defmethod layer-view :video-block
  [data _ _]
  (let [video-link (apply utils/route-to (:cta/navigation-message data))]
    (component/create
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
        [:div.h4.bold (:header/value data)]
        [:div.h4.my2 (:body/value data)]
        [:div.h5.teal.flex.items-center.medium.shout (:cta/value data)]]]

      [:div.hide-on-dt.flex.justify-center.pb10.px4
       [:a.block.relative
        video-link
        (ui/ucare-img {:alt "" :width "152"}
                      "1b58b859-842a-44b1-885c-eac965eeaa0f")
        [:div.absolute.top-0.bottom-0.left-0.right-0.flex.items-center.justify-center.bg-darken-3
         teal-play-video-mobile]]
       [:a.block.ml2.dark-gray
        video-link
        [:h6.bold.mbnp6 (:header/value data)]
        [:p.pt2.h7 (:body/value data)]
        [:h6.teal.flex.items-center.medium.shout
         (:cta/value data)]]]])))

(defmethod layer-view :find-out-more
  [data owner opts]
  (component/create
   [:div.col-12.bg-white.py8.flex.flex-column.items-center.justify-center.center
    (:section/opts data)
    (let [{:header/keys [value]} data]
      (when value
        [:div.col-10.my2.h2 value]))
    (let [{:body/keys [value]} data]
      (when value
        [:div.col-10.my2.h5.dark-gray value]))
    (cta-with-chevron data)]))

(defmethod layer-view :bulleted-explainer
  [data owner opts]
  (let [step (fn [width-class
                  {:as point
                   :keys [icon/uuid icon/width]}]
               [:div.mt2.center.px1
                {:class width-class}
                [:div.flex.justify-center.items-end.my2
                 {:style {:height "39px"}}
                 (ui/ucare-img {:alt (:header/title point) :width width} uuid)]
                [:div.h5.medium (:header/value point)]
                [:p.h6.mx-auto.dark-gray (:body/value point)]
                (cta-with-chevron point)])]

    (component/create
     [:div.col-12.py10.bg-transparent-teal
      [:div.mt2.flex.flex-column.items-center
       (let [{:header/keys [value]} data]
         [:h2.center value])
       (let [{:subheader/keys [value]} data]
         [:div.h6.dark-gray value])]
      (into
       [:div.col-12.flex.flex-column.items-center.hide-on-dt]
       (comp (map (partial step "col-10")))
       (:bullets data))
      (into
       [:div.mx-auto.col-11.flex.justify-center.hide-on-mb-tb]
       (comp (map (partial step "col-3")))
       (:bullets data))
      [:div.center.pt3
       (cta-with-chevron data)]])))

(defmethod layer-view :ugc
  [data owner opts]
  (component/create
   [:div.py8.col-10.mx-auto
    (let [{:header/keys [value]} data]
      [:h2.center value])
    (let [{:subheader/keys [value]} data]
      [:h6.center.dark-gray value])
    [:div.flex.flex-wrap.pt2
     (for [{:keys [imgs]} (:images data)]
       [:a.col-6.col-3-on-tb-dt.p1
        (ui/aspect-ratio
         1 1
         [:img {:class "col-12"
                :src   (-> imgs :original :src)}])])]]))

(defmethod layer-view :faq
  [{:keys [expanded-index sections]} owner opts]
  (component/create
   [:div.px6.mx-auto.col-6-on-dt
    [:h2.center "Frequently Asked Questions"]
    (component/build
     accordion/component
     {:expanded-indices #{expanded-index}
      :sections         (map
                         (fn [{:keys [title paragraphs]}]
                           {:title [:h6 title]
                            :paragraphs paragraphs})
                         sections)}
     {:opts {:section-click-event events/faq-section-selected}})]))

(defn ^:private contact-us-block [url svg title copy]
  [:a.block.py3.col-12.col-4-on-tb-dt
   {:href url}
   svg
   [:div.h6.teal.bold.titlize title]
   [:div.col-8.mx-auto.h6.black copy]])

(defmethod layer-view :contact
  [_ _ _]
  (component/create
   [:div.bg-transparent-teal.center.py8
    [:h5.mt6.teal.letter-spacing-3.shout.bold "Contact Us"]
    [:h1.black.titleize "Have Questions?"]
    [:h5 "We're here to help"]
    [:div.py2.mx-auto.teal.border-bottom.border-width-2.mb2-on-tb-dt
     {:style {:width "30px"}}]
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
      "help@mayvenn.com")]]))

(defmethod layer-view :homepage-were-changing-the-game
  [{:cta/keys [navigation-message]} _ _]
  (component/create
   (let [we-are-mayvenn-link (apply utils/route-to navigation-message)
        diishan-image       (ui/ucare-img {:class "col-12"} "e2186583-def8-4f97-95bc-180234b5d7f8")
        mikka-image         (ui/ucare-img {:class "col-12"} "838e25f5-cd4b-4e15-bfd9-8bdb4b2ac341")
        stylist-image       (ui/ucare-img {:class "col-12"} "6735b4d5-9b65-4fa9-96cd-871141b28672")
        diishan-image-2     (ui/ucare-img {:class "col-12"} "ec9e0533-9eee-41ae-a61b-8dc22f045cb5")]
    [:div.pt10.px4.pb8
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
          teal-play-video-desktop]]]]]])))

(defmethod layer-view :sticky-footer
  [data owner opts]
  #?(:clj (component/create [:div])
     :cljs
     (letfn [(handle-scroll [e] (om/set-state! owner
                                               :show?
                                               (< 530
                                                  (.-y (goog.dom/getDocumentScroll)))))
             (set-height [] (om/set-state! owner
                                           :content-height
                                           (some-> owner
                                                   (om/get-node "content-height")
                                                   goog.style/getSize
                                                   .-height)))]
       (reify
         om/IInitState
         (init-state [this]
           {:show?          false
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
           (let [{:cta/keys [href navigation-message]} data]
             (component/html
              [:div.hide-on-dt
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
                     (ui/teal-button (merge
                                      (when navigation-message
                                        (apply utils/route-to navigation-message))
                                      {:height-class "py2"
                                       :data-test    "sticky-footer-get-started"
                                       :href         href})
                                     [:div.h7 "Get started"])]]]]]]])))))))

(defmethod layer-view :default
  [data _ _]
  (component/create [:div]))

(defn component [{:keys [layers]} owner opts]
  (component/create
   (into [:div]
         (comp
          (map-indexed (fn [i layer-data]
                         [:section {:key (str "section-" i)}
                          (component/build layer-view layer-data opts)])))
         layers)))
