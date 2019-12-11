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
            [ui.molecules :as ui.M]))

(defcomponent layer-hero
  [data _ opts]
  [:div.mx-auto.relative {:style {:min-height "300px"}}
   (let [{:photo/keys [mob-uuid mob-url dsk-uuid dsk-url file-name alt navigation-message]} data]
     (component/build ui.M/hero
                      {:mob-uuid  mob-uuid
                       :mob-url   mob-url
                       :dsk-uuid  dsk-uuid
                       :dsk-url   dsk-url
                       :file-name file-name
                       :alt       alt
                       :opts      (merge {:class     "block"
                                          :style     {:min-height "300px"}}
                                         (when navigation-message {:data-test "hero-link"})
                                         (when navigation-message (apply utils/route-to navigation-message)))}))
   (when-let [buttons (:buttons data)]
     [:div.relative.flex.justify-center
      [:div.absolute.bottom-0.col-6-on-tb-dt.col-12.pb2.mb3-on-dt
       [:div.col.col-12.flex.justify-center
        (let [num-buttons (count buttons)]
          (for [button buttons]
            [:div.px2 {:class (str "col-" (if (= num-buttons 1) 9 6))} (apply ui/button-large-primary button)]))]]])])

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
   (when (or navigation-message href)
     (ui/button-large-primary
      (merge
       (when href
         {:href href})
       (when navigation-message
         (apply utils/route-to navigation-message))
       (when id
         {:data-test id}))
      value))))

(defn ^:private shop-cta-with-img
  [{:cta/keys [navigation-message href img value id]}]
  (component/html
   (when (or navigation-message href)
     [:a.my2
      (merge
       (when href
         {:href href})
       (when navigation-message
         (apply utils/route-to navigation-message))
       (when id
         {:data-test id}))
      (when img
        [:img {:width "30px"
               :height "30px"
               :src img}])
      [:div.underline.block.content-3.bold.p-color.shout.pb6
       value]])))

(defn divider
  [divider-img]
  [:div {:style {:background-image   divider-img
                 :background-position "center"
                 :background-repeat   "repeat-x"
                 :height              "24px"}}])

(defcomponent text-block
  [data _ _]
  [:div.pt10.pb2.px6.center.col-6-on-dt.mx-auto
   (when-let [n (:anchor/name data)]
     [:a {:name n}])
   (when-let [v (:header/value data)]
     [:div.h2 v])
   [:div.h5.mt3 (:body/value data)]
   ^:inline (shop-cta-with-img data)])

(defcomponent hero-image-component [{:screen/keys [seen?] :as data} owner opts]
  [:div (component/build ui.M/hero (merge data {:off-screen? (not seen?)}) nil)])

(defcomponent image-block
  [{:photo/keys [mob-uuid
                 dsk-uuid
                 file-name
                 alt]} _ _]
  [:div.center.mx-auto {:key (str mob-uuid)}
   (ui/screen-aware
    hero-image-component
    {:mob-uuid    mob-uuid
     :dsk-uuid    dsk-uuid
     :file-name   file-name
     :alt         alt}
    nil)])

(defcomponent checklist
  [data _ _]
  [:div.pb10.px6.center.col-6-on-dt.mx-auto
   (when-let [v (:header/value data)]
     [:div.h2 v])
   (when-let [v (:subheader/value data)]
     [:div.h5.mt6.mb4 v])
   [:ul.h6.list-img-purple-checkmark.left-align.mx-auto
    {:style {:width "max-content"}}
    (for [[i b] (map-indexed vector (:bullets data))]
      [:li.mb1.pl1 {:key (str i)} b])]])

(def p-color-play-video-mobile
  (svg/white-play-video {:class  "mr1 fill-p-color"
                         :height "30px"
                         :width  "30px"}))

(def p-color-play-video-desktop
  (svg/white-play-video {:class  "mr1 fill-p-color"
                         :height "41px"
                         :width  "41px"}))

(defcomponent video-overlay
  [data _ _]
  (when-let [video (:video data)]
    (component/build video/component
                     video
                     ;; NOTE(jeff): we use an invalid video slug to preserve back behavior. There probably should be
                     ;;             an investigation to why history is replaced when doing A -> B -> A navigation
                     ;;             (B is removed from history).
                     {:opts
                      {:close-attrs
                       (utils/route-to events/navigate-home
                                       {:query-params {:video "0"}})}})))

(defcomponent video-block
  [data _ _]
  (let [video-link (apply utils/route-to (:cta/navigation-message data))]
    [:div.col-11.mx-auto
     [:div.hide-on-mb-tb.flex.justify-center.my3
      [:a.block.relative
       video-link
       (ui/ucare-img {:alt "" :width "212"}
                     "c487eeef-0f84-4378-a9be-13dc7c311e23")
       [:div.absolute.top-0.bottom-0.left-0.right-0.flex.items-center.justify-center.bg-darken-3
        p-color-play-video-desktop]]
      [:a.block.ml4.black
       video-link
       [:div.h4.bold (:header/value data)]
       [:div.h4.my2 (:body/value data)]
       [:div.h5.p-color.flex.items-center.medium.shout (:cta/value data)]]]
     [:div.hide-on-dt.flex.justify-center.pb10.px4
      [:a.block.relative
       video-link
       (ui/defer-ucare-img {:alt "" :width "152"}
         "1b58b859-842a-44b1-885c-eac965eeaa0f")
       [:div.absolute.top-0.bottom-0.left-0.right-0.flex.items-center.justify-center.bg-darken-3
        p-color-play-video-mobile]]
      [:a.block.ml2.black
       video-link
       [:h6.bold.mbnp6 (:header/value data)]
       [:p.pt2.h7 (:body/value data)]
       [:h6.p-color.flex.items-center.medium.shout
        (:cta/value data)]]]]))

(defcomponent find-out-more
  [data owner opts]
  [:div.col-12.bg-white.py8.flex.flex-column.items-center.justify-center.center
   (:section/opts data)
   (let [{:header/keys [value]} data]
     (when value
       [:div.col-10.my2.h2 value]))
   (let [{:body/keys [value]} data]
     (when value
       [:div.col-10.my2.h5 value]))
   (cta-with-chevron data)])

(defn ^:private step
  [layer-id
   width-class
   {:as point
    :keys [icon/uuid icon/width]}
   idx]
  (component/html
   [:div.mt2.center.px1
    {:class width-class
     :key (str layer-id "-" idx "-" width-class)
     }
    [:div.flex.justify-center.items-end.my2
     {:style {:height "39px"}}
     (ui/defer-ucare-img {:alt (:header/value point) :width width} uuid)]
    [:div.h5.medium (:header/value point)]
    [:p.h6.mx-auto.black (:body/value point)]
    ^:inline (cta-with-chevron point)]))

(defcomponent bulleted-explainer
  [{:keys    [bullets]
    :as      data
    layer-id :layer/id} owner opts]
  [:div.col-12.py10.bg-cool-gray
   [:div.mt2.flex.flex-column.items-center
    (let [{:header/keys [value]} data]
      [:h2.center value])
    (let [{:subheader/keys [value]} data]
      [:div.h6.black value])]
   [:div.col-12.flex.flex-column.items-center.hide-on-dt
    (map-indexed (partial step layer-id "col-10") bullets)]
   [:div.mx-auto.col-11.flex.justify-center.hide-on-mb-tb
    (map-indexed (partial step layer-id "col-3") bullets)]
   [:div.center.pt3
    ^:inline (cta-with-chevron data)]])

(defcomponent ^:private ugc-image [{:screen/keys [seen?] :keys [image-url]} owner opts]
  (ui/aspect-ratio
   1 1
   (cond
     seen?          [:img {:class "col-12"
                           :src   image-url}]
     :else          [:div.col-12 " "])))

(defcomponent ugc
  [data owner opts]
  [:div.py8.col-10.mx-auto
   (let [{:header/keys [value]} data]
     [:h2.center value])
   (let [{:subheader/keys [value]} data]
     [:h6.center value])
   [:div.flex.flex-wrap.pt2
    (for [{:keys [image-url]} (:images data)]
      [:a.col-6.col-3-on-tb-dt.p1
       {:key (str image-url)}
       (ui/screen-aware
        ugc-image
        {:image-url image-url}
        nil)])]])

(defcomponent faq
  [{:keys [expanded-index sections]} owner opts]
  [:div.px6.mx-auto.col-6-on-dt.bg-pale-purple.py6
   [:div.canela.title-1.center.my7 "Frequently Asked Questions"]
   (component/build
    accordion/component
    {:expanded-indices #{expanded-index}
     :sections         (map
                        (fn [{:keys [title paragraphs]}]
                          {:title [:content-1 title]
                           :paragraphs paragraphs})
                        sections)}
    {:opts {:section-click-event events/faq-section-selected}})])

(defn ^:private contact-us-block [idx {:keys [url svg title copy]}]
  [:a.block.py3.col-12.col-4-on-tb-dt.black
   {:href url
    :key idx}
   svg
   [:div.proxima.title-2.mt1 title]
   [:div.col-8.mx-auto.p-color.content-2 copy]])

(defcomponent shop-contact
  [{title-value        :title/value
    subtitle-value     :subtitle/value
    sub-subtitle-value :sub-subtitle/value
    contact-us-blocks  :contact-us-blocks} _ _]
  [:div
   [:div.bg-warm-gray.center.py8
    [:h5.mt6.proxima.shout.title-2 title-value]
    [:h1.canela.title-1.pb1 subtitle-value]
    [:h5.proxima.content-2 sub-subtitle-value]
    [:div.stroke-s-color.pt4
     (svg/straight-line {:width  "1px"
                         :height "42px"})]
    [:div.flex.flex-wrap.items-baseline.justify-center.col-12.col-8-on-tb-dt.mx-auto
     (map-indexed (partial contact-us-block) contact-us-blocks)]]
   [:div.bg-p-color.pt1]])

(defcomponent shop-quote-img
  [{:quote/keys [img text primary-attribution secondary-attribution] :as data} _ _]
  (let [quotation-mark (svg/quotation-mark
                        {:width  "21px"
                         :height "18px"})]
    [:div.bg-calm-gray
    [:div.flex.mx5.my10
     [:div quotation-mark]
     [:div.canela.title-2.center.pt2.pb4 text]
     [:div.self-end.rotate-180 quotation-mark]]
    [:div.relative
     [:div.absolute.white.right-0.py8.px4.right-align
      [:div.proxima.title-2.shout primary-attribution]
      [:div secondary-attribution]]
     (ui/ucare-img {:class "block col-12"} img)]]))

(def sticky-footer
  #?(:clj (fn [_ _ _] (component/create "sticky-footer" [:div]))
     :cljs
     (component/create-dynamic
      "sticky-footer"
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
              (let [{:keys [show? content-height]}        (component/get-state this)
                    data                                  (component/get-props this)
                    {:cta/keys [href navigation-message]} data
                    content-height-ref                    (component/use-ref this "content-height")]
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
                    [:div {:ref content-height-ref}
                     [:div
                      [:div.h6.white.bg-black.medium.px3.py4.flex.items-center
                       [:div.col-7 "We can't wait to pay for your install!"]
                       [:div.col-1]
                       [:div.col-4
                        (ui/button-medium-primary (merge
                                                   (when navigation-message
                                                     (apply utils/route-to navigation-message))
                                                   {:data-test    "sticky-footer-get-started"
                                                    :href         href})
                                                  [:div.h7 "Get started"])]]]]]]]))))))

(defcomponent shop-text-block
  [{anchor-name :anchor/name
    title       :header/value
    body        :body/value
    divider-img :divider-img
    :as data}
   _
   _]
  [:div 
   [:div.pt10.pb2.px6.center.col-6-on-dt.mx-auto
    (when anchor-name
      [:a {:name anchor-name}])
    (when title
      [:div.title-1.canela
       title])
    (when body
      [:div.title-2.canela body])
    ^:inline
    [:div.pt3
     (shop-cta-with-img data)]]
   (when divider-img
     (divider divider-img))])

(defcomponent shop-framed-checklist
  [{:keys [bullets divider-img] header-value :header/value} _ _]
  [:div
   [:div.relative
    {:style {:margin-top "60px"}}
    [:div.absolute.col-12.flex.justify-center
     {:style {:top "-50px"}}
     [:object {:style {:height "67px"}
               :type  "image/svg+xml"
               :data  "/images/vertical-squiggle.svg"}]]

    [:div.col-10.col-6-on-dt.mx-auto.border.border-dotted.flex.justify-center.mb8
     [:div.col-12.flex.flex-column.items-center.m5
      {:style {:width "max-content"}}
      (when header-value
        [:div.proxima.title-2.shout.py1
         header-value])
      [:ul.col-12.list-purple-diamond
       {:style {:padding-left "15px"}}
       (for [[i b] (map-indexed vector bullets)]
         [:li.py1 {:key (str i)} b])]]]]
   (when divider-img
     (divider divider-img))])

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
  [:div.col-12.bg-cool-gray.center.flex.flex-column.items-center.my2
   [:div.mt2
    (when title-value
      [:h2.title-1.canela
       (interpose [:br] title-value)])
    (when subtitle-value
      [:div.title-1.proxima.shout.sub
       (interpose [:br] subtitle-value)])]
   [:div.col-8.flex.flex-column.items-center.hide-on-dt
    [:div.stroke-s-color
     (svg/straight-line {:width  "1px"
                         :height "42px"})]
    (map-indexed (partial shop-step (str layer-id "-mb-tb-"))
                 bullets)
    [:div.mx-auto.col-11.flex.justify-center.hide-on-mb-tb
     (map-indexed (partial shop-step (str layer-id "-dt-"))
                  bullets)]
    ^:inline (shop-cta-with-img data)]])

(defcomponent shop-ugc
  [{title  :header/value
    images :images
    :as    data} _ _]
  [:div.py8.col-10.mx-auto.center
   (when title
     [:div.title-2.proxima.shout.bold title])
   [:div.flex.flex-wrap.py3
    (for [{:keys [image-url]} images]
      [:a.col-6.col-3-on-tb-dt.p1
       {:key (str image-url)}
       (ui/screen-aware
        ugc-image
        {:image-url image-url}
        nil)])]
   ^:inline (shop-cta-with-img data)])

(defn layer-view [{:keys [layer/type] :as view-data} opts]
  (component/build
   (case type
     ;; REBRAND
     :shop-text-block         shop-text-block
     :shop-framed-checklist   shop-framed-checklist
     :shop-bulleted-explainer shop-bulleted-explainer
     :shop-ugc                shop-ugc
     :shop-quote-img          shop-quote-img
     :shop-contact                 shop-contact

     ;; LEGACY
     :image-block                     image-block
     :hero                            layer-hero
     :free-standard-shipping-bar      free-standard-shipping-bar
     :text-block                      text-block
     :checklist                       checklist
     :video-overlay                   video-overlay
     :video-block                     video-block
     :find-out-more                   find-out-more
     :bulleted-explainer              bulleted-explainer
     :ugc                             ugc
     :faq                             faq
     :sticky-footer                   sticky-footer)
   view-data opts))

(defcomponent component [{:keys [layers]} owner opts]
  [:div
   (map-indexed (fn [i layer-data]
                  (component/html
                   [:section {:key (str "section-" i)}
                    (layer-view layer-data opts)]))
                layers)])
