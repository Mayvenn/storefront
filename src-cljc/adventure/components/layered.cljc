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
  [{:cta/keys [target icon value id]}]
  (component/html
   (if id
     [:a.my2
      (assoc (apply utils/route-to target)
             :data-test id
             :data-ref id)
      (when icon
        icon)
      [:div.underline.block.content-3.bold.p-color.shout.pb6
       value]]
     [:span])))

(defn divider
  [divider-img]
  (component/html
   [:div {:style {:background-image    divider-img
                  :background-position "center"
                  :background-repeat   "repeat-x"
                  :height              "24px"}}]))

(defcomponent hero-image-component [{:screen/keys [seen?] :as data} owner opts]
  [:div (component/build ui.M/hero (merge data {:off-screen? (not seen?)}) nil)])

(defcomponent image-block
  [data _ _]
  [:div.center.mx-auto {:key (str (:mob-uuid data))}
   (ui/screen-aware hero-image-component data nil)])

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
   ^:inline
   (component/build
    accordion/component
    {:expanded-indices #{expanded-index}
     :sections         (mapv
                        (fn [{:keys [title paragraphs]}]
                          {:title      [:content-1 title]
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
    [:h5.mt6.proxima.shout.title-2 ^String title-value]
    [:h1.canela.title-1.pb1 ^String subtitle-value]
    [:h5.proxima.content-2 ^String sub-subtitle-value]
    [:div.stroke-s-color.pt4
     ^:inline (svg/straight-line {:width  "1px"
                                  :height "42px"})]
    [:div.flex.flex-wrap.items-baseline.justify-center.col-12.col-8-on-tb-dt.mx-auto
     (map-indexed (partial contact-us-block) contact-us-blocks)]]
   [:div.bg-p-color.pt1]])

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
    (when title
      [:div.title-1.canela
       title])
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
  [:div.col-12.bg-cool-gray.center.flex.flex-column.items-center.py2
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
    (for [[i bullet] (map-indexed vector bullets)]
      ^:inline (shop-step "mb-tb-" i bullet))]
   [:div.mx-auto.col-11.flex.justify-center.hide-on-mb-tb
    (for [[i bullet] (map-indexed vector bullets)]
      ^:inline (shop-step "dt-" i bullet))]
   ^:inline (shop-cta-with-icon data)])

(defn ^:private shop-icon-step
  [key-prefix
   idx
   {title :header/value
    body  :body/value
    icon  :icon/body
    width :icon/width}]
  (component/html
   [:div.pb1.pt6.col-6-on-tb-dt
    {:key (str key-prefix idx)}
    [:div
     {:width width}
     icon]
    [:div.title-2.proxima.py1.shout
     title]
    [:p.content-2.py1.col-10-on-tb-dt.mx-auto
     body]
    ^:inline (cta-with-chevron {})]))

(defcomponent shop-iconed-list
  [{:as            data
    :keys          [bullets]
    title-value    :title/value
    subtitle-value :subtitle/value
    layer-id       :layer/id}
   owner
   opts]
  [:div.col-12.bg-cool-gray.center.flex.flex-column.items-center.py6
   [:div.mt5.mb3
    (when title-value
      [:h2.title-1.proxima.shout.pb1
       (interpose [:br] title-value)])
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
    (for [{:keys [image-url]} images]
      [:a.col-6.col-3-on-tb-dt.p1
       {:key (str image-url)}
       (ui/screen-aware
        ugc-image
        {:image-url image-url}
        nil)])]
   (shop-cta-with-icon data)])

(defn layer-view [{:keys [layer/type] :as view-data} opts]
  (component/build
   (case type
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
   view-data opts))

(defcomponent component [{:keys [layers]} owner opts]
  [:div
   (for [[i layer-data] (map-indexed vector layers)]
     [:section {:key (str "section-" i)}
      ^:inline (layer-view layer-data opts)])])
