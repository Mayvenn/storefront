(ns adventure.components.layered
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.components.accordion :as accordion]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.components.video :as video]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]
            #?@(:cljs [[goog.events.EventType :as EventType]
                       goog.dom
                       goog.style
                       goog.events])
            [ui.molecules :as ui.M]
            [storefront.config :as config]))

(defn ^:private vertical-squiggle
  [top]
  (component/html
   [:div.absolute.col-12.flex.justify-center
    {:style {:top top}}
    ^:inline (svg/vertical-squiggle {:style {:height "72px"}})]))

(defcomponent layer-hero
  [data _ opts]
  [:div.mx-auto.relative {:style {:min-height "300px"}}
   (let [{:keys [opts]} data]
     (component/build ui.M/hero (merge data
                                       {:opts (merge opts {:class     "block"
                                                           :style     {:min-height "300px"}
                                                           :data-test "hero-link"})})))])

(defcomponent free-standard-shipping-bar
  [_ _ _]
  [:div.mx-auto {:style {:height "3em"}}
   [:div.bg-black.flex.items-center.justify-center
    {:style {:height "2.25em"
             :margin-top "-1px"
             :padding-top "1px"}}
    [:div.px2
     (ui/ucare-img {:alt "" :height "25"}
                   "38d0a770-2dcd-47a3-a035-fc3ccad11037")]
    [:div.h7.white.medium
     "FREE standard shipping"]]])

(defn ^:private cta-with-chevron
  [{:cta/keys [navigation-message href value id]}]
  (component/html
   (if (or navigation-message href)
     (ui/button-large-primary
      (merge
       (when href
         {:href href})
       (when navigation-message
         (apply utils/route-to navigation-message))
       (when id
         {:data-test id}))
      value)
     [:span])))

(defn ^:private shop-cta
  [{:cta/keys [target value id]}]
  (component/html
   (if id
     (ui/button-large-primary
      (assoc (apply utils/route-to target) :data-test id) value)
     [:span])))

(defn ^:private shop-cta-with-icon
  [{:cta/keys [target icon value id aria-label]}]
  (component/html
   (if id
     [:a.my2
      (assoc (apply utils/route-to target)
             :data-test id
             :data-ref id
             :aria-label aria-label)
      (when icon
        (svg/symbolic->html icon))
      [:div.underline.block.content-3.bold.p-color.shout.pb6
       value]]
     [:span])))

(defn ^:private divider
  [divider-img]
  (component/html
   [:div {:style {:background-image    divider-img
                  :background-position "center"
                  :background-repeat   "repeat-x"
                  :height              "24px"}}]))

(defcomponent ^:private fullsize-image-component
  "Like the hero image, but not a link"
  [{:screen/keys [seen?] :as data} owner opts]
  [:div (component/build ui.M/fullsize-image (merge data {:off-screen? (not seen?)}) nil)])

(defcomponent image-block
  [data _ _]
  [:div.center.mx-auto {:key (str (:mob-uuid data))}
   (ui/screen-aware fullsize-image-component data nil)])

(defcomponent video-overlay
  [{:keys [close-nav-event video] :as data} _ _]
  (when video
    (component/build video/component
                     video
                     ;; NOTE(jeff): we use an invalid video slug to preserve back behavior. There probably should be
                     ;;             an investigation to why history is replaced when doing A -> B -> A navigation
                     ;;             (B is removed from history).
                     {:opts
                      {:close-attrs
                       (utils/route-to close-nav-event
                                       {:query-params {:video "0"}})}})))

(defcomponent find-out-more
  [data owner opts]
  [:div.col-12.bg-white.py8.flex.flex-column.items-center.justify-center.center
   (:section/opts data)
   (let [{:header/keys [value]} data]
     (when value
       [:h1.col-10.my2.h2 value]))
   (let [{:body/keys [value]} data]
     (when value
       [:div.col-10.my2.h5 value]))
   (cta-with-chevron data)])

(defcomponent ^:private ugc-image [{:screen/keys [seen?] :keys [image-url alt]} owner opts]
  (ui/aspect-ratio
   1 1
   (cond
     seen? [:img {:class "col-12"
                  :src   image-url
                  :alt   alt}]
     :else [:div.col-12 " "])))

(defcomponent ugc
  [data owner opts]
  [:div.py8.col-10.mx-auto
   (let [{:header/keys [value]} data]
     [:h2.center value])
   (let [{:subheader/keys [value]} data]
     [:div.content-3.center value])
   [:div.flex.flex-wrap.pt2
    (for [{:keys [alt image-url] :as image-data} (:images data)]
      [:a.col-6.col-3-on-tb-dt.p1
       (merge (apply utils/route-to (:cta/navigation-message image-data))
              {:key        (str image-url)
               :aria-label (:cta/aria-label image-data)})
       (ui/screen-aware
        ugc-image
        {:image-url image-url
         :alt       (or (:alt opts) alt)}
        nil)])]])

(defcomponent faq
  [{:keys [expanded-index sections]} owner opts]
  [:div.bg-pale-purple.py6.px4-on-mb-tb
   [:div.mx-auto.col-6-on-dt
    [:h2.canela.title-1.center.my7 "Frequently Asked Questions"]
    ^:inline
    (component/build
     accordion/component
     {:expanded-indices #{expanded-index}
      :sections         (mapv
                         (fn [{:keys [title content]}]
                           {:title      [:content-1 title]
                            :content content})
                         sections)}
     {:opts {:section-click-event events/faq-section-selected}})]])

(defn ^:private contact-us-block [idx {:as data :keys [url title copy legal]}]
  [:a.block.py3.col-12.col-4-on-tb-dt.black
   {:href url
    :key idx}
   (svg/symbolic->html (:svg/symbol data))
   [:div.proxima.title-2.mt1 title]
   [:div.col-8.mx-auto.p-color.content-2 copy]
   (when legal [:div.col-8.mx-auto.content-3.black legal])])

(def shop-contact-query
  {:title/value        "Contact Us"
   :sub-subtitle/value "We're here to help"
   :subtitle/value     "Have Questions?"
   :contact-us-blocks
   [{:url        (ui/sms-url "346-49")
     :svg/symbol [:svg/icon-sms {:height 51
                                 :width  56}]
     :title      "Live Chat"
     :copy       "Text: 346-49"
     :legal      "Message & data rates may apply. Message frequency varies. See Terms & Privacy Policy."}
    {:url        (ui/phone-url config/support-phone-number)
     :svg/symbol [:svg/icon-call {:class  "bg-white fill-black stroke-black circle"
                                  :height 57
                                  :width  57}]
     :title      "Call Us"
     :copy       config/support-phone-number}
    {:url        (ui/email-url "help@mayvenn.com")
     :svg/symbol [:svg/icon-email {:height 39
                                   :width  56}]
     :title      "Email Us"
     :copy       "help@mayvenn.com"}]})

(defcomponent shop-contact
  [{title-value        :title/value
    subtitle-value     :subtitle/value
    sub-subtitle-value :sub-subtitle/value
    contact-us-blocks  :contact-us-blocks} _ _]
  [:div
   [:div.bg-warm-gray.center.py8
    [:h2.mt6.proxima.shout.title-2 ^String title-value]
    [:div.canela.title-1.pb1 ^String subtitle-value]
    [:div.proxima.content-2 ^String sub-subtitle-value]
    [:div.stroke-s-color.pt4
     ^:inline (svg/straight-line {:width  "1px"
                                  :height "42px"})]
    [:div.flex.flex-wrap.items-baseline.justify-center.col-12.col-8-on-tb-dt.mx-auto
     (map-indexed (partial contact-us-block) contact-us-blocks)]]])

(defcomponent shop-quote-img
  [{:quote/keys [dsk-ucare-id mob-ucare-id text primary-attribution secondary-attribution] :as data} _ _]
  (let [dsk-quotation-mark (svg/quotation-mark
                            {:width  "35px"
                             :height "30px"})
        mob-quotation-mark (svg/quotation-mark
                            {:width  "21px"
                             :height "18px"})]
    [:div ;; TB DT
     [:div.hide-on-mb.flex
      [:div.mx5.col-6.flex.items-center
       [:div
        [:div.flex.justify-center
         ^:inline (svg/mayvenn-logo {:width "52px" :height "30px"})]
        [:div.flex.justify-center
         [:div ^:inline dsk-quotation-mark]
         [:div.canela.title-1.center.mt2.mb4.col-7-on-dt.col-9-on-tb text]
         [:div.self-end.rotate-180 ^:inline dsk-quotation-mark]]]]
      [:div.relative.col-6
       [:div.absolute.white.right-0.py6.px4.right-align
        [:div.proxima.title-1.shout ^String primary-attribution]
        [:div ^String secondary-attribution]]
       ^:inline (ui/defer-ucare-img {:class "block col-12"
                                     :width "1000"} dsk-ucare-id)]]

     [:div.relative.hide-on-tb-dt ;; MB
      ^:inline (vertical-squiggle "-86px")
      [:div.flex.mx5.my10
       [:div ^:inline mob-quotation-mark]
       [:div.canela.title-2.center.pt2.pb4 ^String text]
       [:div.self-end.rotate-180 ^:inline mob-quotation-mark]]
      [:div.relative
       [:div.absolute.white.right-0.py8.px4.right-align
        [:div.proxima.title-2.shout ^String primary-attribution]
        [:div ^String secondary-attribution]]
       ^:inline (ui/defer-ucare-img {:class "block col-12"
                                     :width "600"} mob-ucare-id)]]]))

(def sticky-footer
  #?(:clj (fn [_ _ _] (component/create "sticky-footer" [:div]))
     :cljs
     (component/create-dynamic
      sticky-footer
      (constructor [this props]
                   (component/create-ref! this "content-height")
                   (set! (.-set-height this)
                         (fn [_]
                           (component/set-state! this
                                                 :content-height
                                                 (some-> (component/get-ref this "content-height")
                                                         goog.style/getSize
                                                         .-height))))
                   (set! (.-handle-scroll this)
                         (fn [e]
                           (component/set-state! this
                                                 :show?
                                                 (< 530
                                                    (.-y (goog.dom/getDocumentScroll))))))
                   {:show?          false
                    :content-height 0})
      (did-mount [this]
                 (.handle-scroll this nil) ;; manually fire once on load incase the page already scrolled
                 (.set-height this nil)
                 (goog.events/listen js/window EventType/SCROLL (.-handle-scroll this)))
      (will-unmount [this]
                    (goog.events/unlisten js/window EventType/SCROLL (.-handle-scroll this)))
      (render [this]
              (let [{:keys [show? content-height]} (component/get-state this)
                    data                           (component/get-props this)
                    {:cta/keys [href navigation-message label]
                     content   :sticky/content
                     id        :layer/id}          data
                    content-height-ref             (component/use-ref this "content-height")]
                (component/html
                 (if id
                   [:div.hide-on-dt
                    [:div.fixed.z4.bottom-0.left-0.right-0
                     {:style {:margin-bottom (str "-" content-height "px")
                              :box-shadow "0 -6px 6px rgba(0,0,0,0.06)"}}
                     ;; Using a separate element with reverse margin to prevent the
                     ;; sticky component from initially appearing on the page and then
                     ;; animate hiding.
                     [:div.transition-2
                      (if show?
                        {:style {:margin-bottom (str content-height "px")}}
                        {:style {:margin-bottom "0"}})
                      [:div {:ref content-height-ref}
                       [:div
                        [:div.h6.bg-white.medium.p3.flex.items-center.canela.content-3
                         [:div.col-7 content]
                         [:div.col-1]
                         [:div.col-4
                          (ui/button-small-primary (merge
                                                    (when navigation-message
                                                      (apply utils/route-to navigation-message))
                                                    {:data-test id})
                                                   (component/html [:div.h7 label]))]]]]]]]
                   [:div])))))))

(defcomponent shop-text-block
  [{anchor-name :anchor/name
    title       :header/value
    h1          :header/first-on-page? ; to set up h cascade correctly for a11y
    big-title   :big-header/content
    body        :body/value
    divider-img :divider-img
    button?     :cta/button?
    :as         data}
   _
   _]
  [:div
   [:div.pt10.pb2.px6.center.col-6-on-dt.mx-auto
    (when anchor-name
      [:a {:name anchor-name}])
    (when big-title
      (let [[secondary primary] big-title]
        [:h2.py1.shout
         [:div.title-1.proxima.my1 (:attrs secondary) (:text secondary)]
         [:div.title-1.canela.mt2.mb4 (:attrs primary) (:text primary)]]))
    (when title
      (if h1
        [:h1.title-1.canela title]
        [:div.title-1.canela title]))

    (when body
      [:div.title-2.canela body])
    [:div.pt3
     (if button?
       ^:inline (shop-cta data)
       ^:inline (shop-cta-with-icon data))]]
   (when divider-img
     ^:inline (divider divider-img))])

(defcomponent shop-framed-checklist
  [{:keys [bullets divider-img] header-value :header/value} _ _]
  [:div
   [:div.relative
    {:style {:margin-top "60px"}}
    (vertical-squiggle "-46px")
    [:div.col-10.col-6-on-dt.mx-auto.border.border-framed.flex.justify-center.mb8
     [:div.col-12.flex.flex-column.items-center.m5.py4
      {:style {:width "max-content"}}
      (when header-value
        [:div.proxima.title-2.shout.py1
         header-value])
      [:ul.col-12.list-purple-diamond
       {:style {:padding-left "15px"}}
       (for [[i b] (map-indexed vector bullets)]
         [:li.py1 {:key (str i)} b])]]]]
   (when divider-img
     ^:inline (divider divider-img))])

(defn ^:private shop-step
  [key-prefix
   idx
   {title :title/value
    body  :body/value}]
  (component/html
   [:div.p1
    {:key (str key-prefix idx)}
    [:div.title-2.canela.py1
     (str "0" (inc idx))]
    [:div.title-2.proxima.py1.shout
     title]
    [:p.content-2.py1 body]
    ^:inline (cta-with-chevron {})]))

(defcomponent shop-bulleted-explainer
  [{:as            data
    :keys          [bullets]
    subtitle-value :subtitle/value
    title-value    :title/value
    layer-id       :layer/id}
   owner
   opts]
  [:div.col-12.bg-warm-gray.center.flex.flex-column.items-center.py2
   [:div.mt2
    (when title-value
      [:h2.title-1.canela
       (interpose [:br] title-value)])
    (when subtitle-value
      [:div.title-1.proxima.shout.sub.my1
       (interpose [:br] subtitle-value)])]
   [:div.col-8.flex.flex-column.items-center.hide-on-dt
    [:div.stroke-s-color
     (svg/straight-line {:width  "1px"
                         :height "42px"})]
    (for [[i bullet] (map-indexed vector bullets)]
      ^:inline (shop-step "mb-tb-" i bullet))]
   [:div.mx-auto.col-11.flex.justify-center.hide-on-mb-tb.left-align
    (for [[i bullet] (map-indexed vector bullets)]
      ^:inline (shop-step "dt-" i bullet))]
   ^:inline (shop-cta-with-icon data)])

(defn ^:private shop-icon-step
  [key-prefix
   idx
   {title      :header/value
    body       :body/value
    svg-symbol :icon/symbol
    width      :icon/width}]
  (component/html
   [:div.pb1.pt6.col-6-on-tb-dt
    {:key (str key-prefix idx)}
    [:div
     {:width width}
     (svg/symbolic->html svg-symbol)]
    [:div.title-2.proxima.py1.shout
     title]
    [:p.content-2.py1.col-10-on-tb-dt.mx-auto
     body]
    ^:inline (cta-with-chevron {})]))

(defcomponent shop-iconed-list
  [{:as            data
    :keys          [bullets]
    subtitle-value :subtitle/value
    layer-id       :layer/id}
   owner
   opts]
  [:div.col-12.bg-cool-gray.center.flex.flex-column.items-center.py6
   [:div.mt5.mb3
    [:h2.title-1.proxima.shout.pb1
     [:div.img-logo.bg-no-repeat.bg-center.bg-contain {:style {:height "29px"}}]]
    (when subtitle-value
      [:div.title-1.canela.shout
       (interpose [:br] subtitle-value)])]
   [:div.col-8.flex.flex-column.items-center.hide-on-dt
    (for [[i bullet] (map-indexed vector bullets)]
      ^:inline (shop-icon-step (str layer-id "-mb-tb-") i bullet))]
   [:div.col-7.flex.flex-wrap.justify-between.hide-on-mb-tb
    (for [[i bullet] (map-indexed vector bullets)]
      ^:inline (shop-icon-step (str layer-id "-dt-") i bullet))]
   ^:inline (shop-cta-with-icon data)])

(defcomponent shop-ugc
  [{title  :header/value
    images :images
    :as    data} _ _]
  [:div.py8.col-10.mx-auto.center
   (when title
     [:div.title-2.proxima.shout.bold title])
   [:div.flex.flex-wrap.py3
    (for [{:keys [image-url alt label] :as image-data} images]
      [:a.col-6.col-3-on-tb-dt.p1
       (merge (apply utils/route-to (:cta/navigation-message image-data))
              {:key (str image-url)})
       (ui/screen-aware
        ugc-image
        {:image-url image-url
         :alt       alt}
        nil)])]
   (shop-cta-with-icon data)])

(defcomponent lp-tiles
  [{title  :header/value
    images :images
    :as    data} _ _]
  [:div.py8.col-10.mx-auto.center
   (when title
     [:div.title-1.proxima.shout.bold title])
   [:div.flex.flex-wrap.py4.justify-center-on-tb-dt
    (for [{:keys [image-url alt label] :as image-data} images]
      [:a.col-6.col-3-on-tb-dt.p1
       (merge (apply utils/route-to (:cta/navigation-message image-data))
              {:key (str image-url)})
       (ui/img
        {:src   image-url
         :title title
         :class "col-12"
         :alt   alt})
       [:div.black.content-2 label]])]
   (let [{:keys [cta]} data]
     (if (:id cta)
       (ui/button-small-primary (merge {:class "inline"}
                                       (:attrs cta))
                                (:content cta))
       [:span]))])

(defcomponent lp-image-text-block
  [{anchor-name :anchor/name
    title       :header/value
    h1          :header/first-on-page? ; to set up h cascade correctly for a11y
    big-title   :big-header/content
    body        :body/value
    image       :image/url
    alt         :image/alt
    copy        :text/copy
    divider-img :divider-img
    button?     :cta/button?
    :as         data}
   _
   _]
  [:div
   [:div.pt10.pb2.px6.center.col-6-on-dt.mx-auto
    (when anchor-name
      [:a {:name anchor-name}])
    (when big-title
      (let [[secondary primary] big-title]
        [:h2.py1.shout
         [:div.title-1.proxima.my1 (:attrs secondary) (:text secondary)]
         [:div.title-1.canela.mt2.mb4 (:attrs primary) (:text primary)]]))
    (when title
      (if h1
        [:h1.title-1.canela title]
        [:h2.title-1.canela title]))

    (when image
      [(ui/img {:src   image
                :alt   alt
                :class "py4"
                :style {:width "100%"}})
       [:div.content-2.pb4 copy]])

    [:div.pt3
     (if button?
       ^:inline (shop-cta data)
       ^:inline (shop-cta-with-icon data))]]
   (when divider-img
     ^:inline (divider divider-img))])

(defcomponent title-with-subtitle
  [{:keys [primary secondary]} _ _]
  [:div.bg-refresh-gray.pyj3-on-mb.pyj3-on-tb-dt
   [:div.canela.title-1.center.mb3 primary]
   [:div.content-2-on-mb.content-1-on-tb-dt.center.max-580.mx-auto.pxj3-on-mb secondary]])

(defn layer-view [{:keys [layer/type] :as view-data} opts]
  (when type
    (component/build
     (case type
       :lp-tiles            lp-tiles
       :lp-image-text-block lp-image-text-block

       ;; REBRAND
       :shop-text-block         shop-text-block
       :shop-framed-checklist   shop-framed-checklist
       :shop-bulleted-explainer shop-bulleted-explainer
       :shop-ugc                shop-ugc
       :shop-iconed-list        shop-iconed-list
       :shop-quote-img          shop-quote-img
       :shop-contact            shop-contact
       :video-overlay           video-overlay

       ;; LEGACY
       :image-block                image-block
       :hero                       layer-hero
       :free-standard-shipping-bar free-standard-shipping-bar
       :find-out-more              find-out-more
       :ugc                        ugc
       :faq                        faq
       :sticky-footer              sticky-footer)
     view-data opts)))

(defcomponent component [{:keys [layers]} owner opts]
  [:div
   (for [[i layer-data] (map-indexed vector layers)]
     [:section {:key (str "section-" i)}
      ^:inline (layer-view layer-data opts)])])
