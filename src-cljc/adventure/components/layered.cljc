(ns adventure.components.layered
  (:require [mayvenn.visual.tools :refer [within with]]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.accordion :as accordion]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.components.video :as video]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.carousel :as carousel]
            [storefront.components.carousel :as carousel-2022]
            [storefront.platform.messages
             :refer [handle-message]
             :rename {handle-message publish}]
            [storefront.transitions :as transitions :refer [transition-state]]
            #?@(:cljs [[goog.events.EventType :as EventType]
                       [storefront.components.email-capture :as email-capture]
                       goog.dom
                       goog.style
                       goog.events])
            [ui.molecules :as ui.M]
            [storefront.config :as config]
            [storefront.effects :as fx]
            [storefront.keypaths :as keypaths]
            [homepage.ui.promises :as promises]
            [homepage.ui.contact-us :as contact-us]
            [markdown-to-hiccup.core :as markdown]
            retail.stores))

(defn ^:private vertical-squiggle
  [top]
  (component/html
   [:div.absolute.col-12.flex.justify-center
    {:style {:top top}}
    ^:inline (svg/vertical-squiggle {:style {:height "72px"}})]))

(defcomponent layer-hero
  [data _ opts]
  [:div.mx-auto.relative
   (let [{:keys [opts]} data]
     (component/build
      ui.M/hero
      (merge data
             {:off-screen? (not (:screen/seen? data true))}
             {:opts (merge opts {:class     "block"
                                 :data-test "hero-link"})})))])

(defcomponent ^:private fullsize-image-component
  "Another name for a hero without a target"
  [{:screen/keys [seen?] :as data} owner opts]
  [:div (component/build ui.M/hero
                         (merge data
                                {:off-screen? (not seen?)})
                         nil)])

(defcomponent image-block
  [data _ _]
  [:div.center.mx-auto {:key (str (:mob-uuid data))}
   (ui/screen-aware fullsize-image-component data nil)])


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
   (if (and target value)
     (ui/button-large-primary
      (assoc (apply utils/route-to target) :data-test id) value)
     [:span])))

(defn ^:private shop-cta-with-icon
  [{:cta/keys [target icon value id aria-label]}]
  (component/html
   (if (and target value)
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

(defn ^:private shop-cta-underline
  [{:cta/keys [target value id aria-label]}]
  (component/html
   (if (and target value)
     [:a.my2
      (assoc (apply utils/route-to target)
             :data-test id
             :data-ref id
             :aria-label aria-label)
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

(defcomponent ^:private ugc-image [{:screen/keys [seen?] :keys [image-url alt style max-size]} owner opts]
  (ui/aspect-ratio
   1 1
   (cond
     seen? (ui/img {:class       "col-12"
                    :style       style
                    :src         image-url
                    :square-size 1000
                    :max-size    max-size
                    :alt         alt})
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
         :max-size  400
         :alt       (or (:alt opts) alt)}
        nil)])]])

(defcomponent faq
  [{:keys [expanded-index sections title]} owner opts]
  [:div.bg-pale-purple.py6.px4-on-mb-tb
   [:div.mx-auto.col-6-on-dt
    [:h2.canela.title-1.center.my7 title]
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
    icon        :cta/icon
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
      [:div.title-2.canela.my4 body])
    [:div.pt3
     (cond
       icon    ^:inline (shop-cta-with-icon data)
       button? ^:inline (shop-cta data)
       :else   ^:inline (shop-cta-underline data))]]
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
         :max-size  400
         :alt       alt}
        nil)])]
   (shop-cta-with-icon data)])

(defcomponent lp-tiles
  [{title  :header/value
    images :images
    :as    data} _ _]
  [:div.py8.mx-auto.center
   (when title
     [:div.title-1.proxima.shout.bold title])
   [:div.pt1.pb4.grid.gap-1.mx1
    {:class (case (count images)
              1 "tiles-1"
              2 "tiles-2"
              3 "tiles-3"
              4 "tiles-4"
              "tiles-more")}
    (for [{:keys [image-url image alt label label-shout copy] :as image-data} images
          :let [navigation-message (:cta/navigation-message image-data)]]
      (cond
        (not (or image image-url))
        nil

        (:cta/navigation-message image-data)
        [:a.p1.border.border-cool-gray
         (merge (apply utils/route-to navigation-message)
                {:key (str image-url)})
         (ui/screen-aware
          ugc-image
          {:image-url (or (:url (:file image)) image-url)
           :max-size  400
           :alt       alt}
          nil)
         [:div.black.content-2.mt2
          (when label-shout
            {:class "proxima shout bold"})
          label]
         (when copy
           [:div.black.left-align.mt2
            copy])]

        :else
        [:div.col-6.col-3-on-tb-dt.p1
         {:key (str image-url)}
         (ui/screen-aware
          ugc-image
          {:image-url (or (:url (:file image)) image-url)
           :max-size  400
           :alt       alt}
          nil)
         [:div.black.content-2
          (when label-shout
            {:class "proxima shout loud"})
          label]
         (when copy
           [:div.black.left-align.mt2 copy])]
        )
      )]
   (let [{:keys [cta]} data]
     (if (:id cta)
       (ui/button-small-primary (merge {:class "inline"}
                                       (:attrs cta))
                                (:content cta))
       [:span]))])

(defcomponent lp-images-with-copy
  [{title  :header/value
    images :images} _ _]
  [:div.m10
   (when title
     [:div.black.title-1.canela
      title])
   [:div.flex.flex-wrap.justify-around.mt5
    (for [{:keys [label alt image copy]} images]
      (when image
        [:div.col-12.col-3-on-tb-dt.p1.mx3
         {:key (str label alt)}
         (ui/screen-aware
          ugc-image
          {:image-url (:url (:file image))
           :max-size  400
           :alt       alt}
          nil)
         [:div.border-cool-gray.border-left.pl5
          (when label
            [:div.black.content-2.proxima.shout.bold
             label])
          (when copy
            [:div.black.content-2.left-align.mt2 copy])]]))]])

(defcomponent lp-email-capture
  [{:keys [email-capture-id template-content-id incentive fine-print-prefix] :as data} _ _]
  [:form.col-12.center.px1.max-580.my6.mx-auto.py6
   {:on-submit (apply utils/send-event-callback [events/control-landing-page-email-submit {:trigger-id          email-capture-id
                                                                                           :template-content-id template-content-id}])}
   [:div.mb2
    [:div.title-2.proxima.shout incentive]]
   #?(:cljs
      [:div.px3
       (email-capture/email-field data)
       (email-capture/cta data)
       email-capture/hr-divider
       (email-capture/fine-print fine-print-prefix)])])

(declare layer-view)
(defcomponent lp-split
  [{:keys [left-top right-bottom]} _ opts]
  [:div.flex.flex-column-on-mb
   [:div.col-6-on-tb-dt
    (layer-view left-top opts)]
   [:div.col-6-on-tb-dt
    (layer-view right-bottom opts)]])

(defcomponent lp-title-text-cta-background-color
  [data _ _]
  (let [title         (:header/value data)
        subtitle      (:body/value data)
        cta-copy      (:cta/value data)
        cta-url       (:cta/target data)
        bg-color      (:background/color data)
        content-color (:content/color data)]
    [:div.p4-on-mb.p8-on-tb-dt.flex.flex-column.justify-center
     (merge (when bg-color
              {:class (str "bg-" bg-color " " content-color)})
            {:style {:height "100%"}})
     [:div.canela.title-2.py1
      title]
     [:div.content-2.py1
      (markdown/component (markdown/md->hiccup subtitle))]
     (when cta-url
       [:div.mt2
        ;; TODO: consider alternatives to max-width
        {:style {:max-width "300px"}}
        (ui/button-medium-primary (utils/route-to events/external-redirect-url {:url cta-url})
                                  cta-copy)])]))

(defcomponent lp-image-text-block
  [{anchor-name      :anchor/name
    title            :header/value
    h1               :header/first-on-page? ; to set up h cascade correctly for a11y
    big-title        :big-header/content
    body             :body/value
    image            :image/url
    contentful-image :image/image
    alt              :image/alt
    copy             :text/copy
    divider-img      :divider-img
    button?          :cta/button?
    icon             :cta/icon
    :as              data}
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

    [:div.pt4
     (cond
       contentful-image
       (ui/screen-aware
        ugc-image
        {:image-url (:url (:file contentful-image))
         :max-size  750
         :alt       (:title contentful-image)}
        nil)

       image
       (ui/screen-aware
        ugc-image
        {:image-url image
         :alt       alt
         :style     {:width "100%"}})

       :else
       nil)

     (when copy [:div.content-2.p4 copy])]

    [:div
     (cond
       button? ^:inline (shop-cta data)
       icon    ^:inline (shop-cta-with-icon data)
       :else   ^:inline (shop-cta-underline data))]]
   (when divider-img
     ^:inline (divider divider-img))])

(defn image-body [i {:keys [url alt]}]
  (ui/aspect-ratio
   640 580
   (if (zero? i)
     (ui/img {:src url :class "col-12" :width "100%" :alt alt})
     (ui/defer-ucare-img
       {:class       "col-12"
        :alt         alt
        :width       640
        :placeholder (ui/large-spinner {:style {:height     "60px"
                                                :margin-top "130px"}})}
       url))))

(defn carousel [images _]
  (component/build carousel/component
                   {:images images
                    :slides (map-indexed image-body images)}
                   {:opts {:settings {:edgePadding 0
                                      :items       1}}}))

(defcomponent lp-image-carousel
  [{:keys [id] :as data} _ _]
  [:div.mx8.py10
   (component/build carousel-2022/component
                    data
                    {:opts {:carousel/exhibit-thumbnail-component carousel-2022/product-carousel-thumbnail
                            :carousel/exhibit-highlight-component carousel-2022/product-carousel-highlight
                            :carousel/id                          id}})])

(defcomponent lp-video
  [{:keys [video open-modal? opts title]} _ _]
  [:div.mx-auto.max-580.py10.center
   (shop-cta-with-icon (within :cta {:target     (:target video)
                                     :icon       [:svg/play-video {:width  "30px"
                                                                   :height "30px"}]
                                     :value      title
                                     :id         "watch-video"
                                     :aria-label nil}))
   (when open-modal?
     (component/build video/component video opts))])

(defcomponent lp-reviews
  [{:keys [title reviews]} _ _]
  [:div.mx-auto.center.max-960
   [:div.canela.title-1 title]
   [:div.flex.p3.hide-on-tb-dt ; mobile
    [:div.flex (svg/quotation-mark {:class "fill-gray" :width "35px" :height "30px"})]
    [:div.mx-auto
     (map-indexed
      (fn [idx {:keys [name number-of-stars review-copy]}]
        [:div.my6
         [:div.my2.flex.justify-center (ui.M/stars-rating-molecule {:value number-of-stars
                                                                    :id    (str "review-" idx)
                                                                    :opts  {:height "25px"
                                                                            :width  "25px"}})]
         [:div.my2 review-copy]
         [:div.my2.proxima.shout.title-2 "- " name]])
      reviews)]
    [:div.rotate-180 (svg/quotation-mark {:class "fill-gray" :width "35px" :height "30px"})]]

   [:div.flex.justify-between.p3.hide-on-mb ; desktop
    [:div.flex (svg/quotation-mark {:class "fill-gray" :width "35px" :height "30px"})]
    [:div.flex.justify-between
     (map-indexed
      (fn [idx {:keys [name number-of-stars review-copy]}]
        [:div.col-4
         [:div.my2.flex.justify-center (ui.M/stars-rating-molecule {:value number-of-stars
                                                                    :id    (str "review-" idx)
                                                                    :opts  {:height "25px"
                                                                            :width  "25px"}})]
         [:div.my2 review-copy]
         [:div.my2.proxima.shout.title-2 "- " name]])
      reviews)]
    [:div.rotate-180 (svg/quotation-mark {:class "fill-gray" :width "35px" :height "30px"})]]])

(defcomponent lp-split-image
  [{:keys [alt image navigation-message]} _ _]
  (ui/img {:src   (:url (:file image))
           :style {:object-fit "cover"
                   :min-width  "50%"}
           :class "flex-auto col-12"
           :max-size 1024
           :alt   alt}))

;; Duplicated from src-cljc/homepage/ui_v2020_07.cljc to avoid circular dep
(def contact-us-query
  {:contact-us.title/primary   "Contact Us"
   :contact-us.title/secondary "We're here to help"
   :contact-us.body/primary    "Have Questions?"
   :list/contact-methods
   [{:contact-us.contact-method/uri         (ui/sms-url "346-49")
     :contact-us.contact-method/svg-symbol  [:svg/icon-sms {:height 51
                                                            :width  56}]
     :contact-us.contact-method/title       "Live Chat"
     :contact-us.contact-method/copy        "Text: 346-49"
     :contact-us.contact-method/legal-copy  "Message & data rates may apply. Message frequency varies."
     :contact-us.contact-method/legal-links [{:copy   "terms"
                                              :target [events/navigate-content-sms]}
                                             {:copy   "privacy policy"
                                              :target [events/navigate-content-privacy]}]}
    {:contact-us.contact-method/uri        (ui/phone-url "1 (855) 287-6868")
     :contact-us.contact-method/svg-symbol [:svg/icon-call {:class  "bg-white fill-black stroke-black circle"
                                                            :height 57
                                                            :width  57}]
     :contact-us.contact-method/title      "Call Us"
     :contact-us.contact-method/copy       "1 (855) 287-6868"}
    {:contact-us.contact-method/uri        (ui/email-url "help@mayvenn.com")
     :contact-us.contact-method/svg-symbol [:svg/icon-email {:height 39
                                                             :width  56}]
     :contact-us.contact-method/title      "Email Us"
     :contact-us.contact-method/copy       "help@mayvenn.com"}]})

(defcomponent lp-contact-us
  [_ _ _]
  (component/build contact-us/organism contact-us-query))

(defcomponent lp-divider-green-gray
  [_ _ _]
  (divider "url('//ucarecdn.com/7e91271e-874c-4303-bc8a-00c8babb0d77/-/resize/x24/')"))

(defcomponent lp-divider-purple-pink
  [_ _ _]
  (divider "url('//ucarecdn.com/ac332aa1-cc58-4c1b-b610-da363203388a/-/crop/301x24/bottom/')"))

(defcomponent service-list
  [_ _ _]
  (retail.stores/wig-services-menu retail.stores/service-menu-data))

(def promises-omni-query
  {:list/icons
   [{:promises.icon/symbol :svg/check-cloud,
     :promises.icon/title  "100% Virgin Human Hair"}
    {:promises.icon/symbol :svg/custom-wig-services,
     :promises.icon/title  "Custom Wig Services"}
    {:promises.icon/symbol :svg/hand-heart,
     :promises.icon/title  "Top Notch Service"}
    {:promises.icon/symbol :svg/shield,
     :promises.icon/title  "30-Day Guarantee"}]})

(defcomponent promises-omni
  [_ _ _]
  (component/build promises/organism promises-omni-query))

(def wig-customization-query
  {:header/title    "Wig Customization"
   :header/subtitle "Here's how it works:"
   :sections        [{:title "Select your wig"
                      :copy  "Choose a factory-made wig or get one tailor-made by our resident Wig Artist"
                      :url   "https://ucarecdn.com/1596ef7a-8ea8-4e2d-b98f-0e2083998cce/select_your_wig.png"}
                     {:title "We customize it"
                      :copy  "Tell us how you want your wig to look and we make it happen."
                      :url   "https://ucarecdn.com/b8902af1-9262-4369-ab88-35e82fd2f3b7/we_customize_it.png"}
                     {:title "Take it home"
                      :copy  "Rock your new unit the same day or pick it up within 2-5 days."
                      :url   "https://ucarecdn.com/8d4b8e12-48a7-4e90-8a41-3f1ef1267a93/take_it_home.png"}]}
  )

(defcomponent customize-wig
  [_ _ _]
  (retail.stores/wig-customization-spotlights wig-customization-query))

(defn layer-view [{:keys [layer/type] :as view-data} opts]
  (when type
    (component/build
     (case type
       :lp-tiles                           lp-tiles
       :lp-images-with-copy                lp-images-with-copy
       :lp-image-text-block                lp-image-text-block
       :lp-image-carousel                  lp-image-carousel
       :lp-video                           lp-video
       :lp-reviews                         lp-reviews
       :lp-email-capture                   lp-email-capture
       :lp-split                           lp-split
       :lp-title-text-cta-background-color lp-title-text-cta-background-color
       :lp-split-image                     lp-split-image
       :lp-contact-us                      lp-contact-us
       :lp-divider-green-gray              lp-divider-green-gray
       :lp-divider-purple-pink             lp-divider-purple-pink

       ;; REBRAND
       :shop-text-block         shop-text-block
       :shop-framed-checklist   shop-framed-checklist
       :shop-bulleted-explainer shop-bulleted-explainer
       :shop-ugc                shop-ugc
       :shop-iconed-list        shop-iconed-list
       :shop-quote-img          shop-quote-img
       :shop-contact            shop-contact
       :video-overlay           video-overlay


       :service-list        service-list
       :promises-omni       promises-omni
       :customize-wig       customize-wig


       ;; LEGACY
       :image-block                image-block
       :hero                       layer-hero
       :free-standard-shipping-bar free-standard-shipping-bar
       :find-out-more              find-out-more
       :ugc                        ugc
       :faq                        faq
       :sticky-footer              sticky-footer)
     view-data opts)))

(defn add-possible-h1 [index layer]
  (if (= index 1) ; ASSUMPTION: index 0 would be the hero image
    (merge layer {:header/first-on-page? true})
    layer))

(defcomponent component [{:keys [layers]} owner opts]
  [:div
   (for [[i layer-data] (map-indexed vector layers)]
     [:section {:key (str "section-" i)}
      ^:inline (layer-view (add-possible-h1 i layer-data) opts)])])

(defmethod fx/perform-effects events/control-landing-page-email-submit
  [_ _ {:keys [email-capture-id template-content-id] :as this} _ _]
  (publish events/biz|email-capture|captured {:trigger-id          email-capture-id
                                               :template-content-id template-content-id})
  (publish events/flash-show-success {:message "Successfully signed up"}))

#?(:cljs
 (defmethod transition-state events/control-landing-page-email-submit
   [_ _ _ app-state]
   (-> app-state
       (assoc-in [:models :email-capture :textfield] nil))))
