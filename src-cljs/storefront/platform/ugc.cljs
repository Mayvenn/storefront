(ns storefront.platform.ugc
  (:require [sablono.core :refer-macros [html]]
            [om.core :as om]
            [storefront.accessors.named-searches :as named-searches]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.platform.component-utils :as util]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.platform.carousel :as carousel]))

(defn image-view [photo-url]
  [:div.bg-black.relative.overflow-hidden
   {:style {:padding-top "100%"}} ;; To keep square aspect ratio. Refer to https://css-tricks.com/snippets/sass/maintain-aspect-ratio-mixin/
   [:div.absolute.overlay.bg-cover.bg-no-repeat.bg-center
    {:style {:background-image (str "url(" photo-url ")")}}]])

(defn video-view [video-url]
  [:div.bg-black.relative {:style {:padding-top "100%"}}
   [:div.absolute.overlay.overflow-hidden
    [:video.container-size.block {:controls true}
     [:source {:src video-url}]]]])

(defn unattributed-slide [idx {:keys [photo content-type]}]
  [:div.p1
   [:a (util/fake-href events/control-popup-ugc-category {:offset idx})
    [:div.relative.overflow-hidden
     {:style {:padding-top "100%"}} ;; To keep square aspect ratio. Refer to https://css-tricks.com/snippets/sass/maintain-aspect-ratio-mixin/
     [:div.absolute.overlay.flex.flex-column.justify-center
      [:img.col-12.block {:src photo}]]
     (when (= content-type "video")
       [:div.col-12.absolute {:style {:top "50%" :margin-top "-32px"}} svg/play-video-muted])]]])

(defn attributed-slide [{:keys [user-handle large-photo social-service content-type source-url] :as item}]
  [:div.m1.rounded-bottom
   (if (= content-type "video")
     (video-view source-url)
     (image-view large-photo))
   [:div.flex.items-center.rounded-bottom.bg-white.py2.px3
    [:div.flex-auto.gray.bold "@" user-handle]
    [:div.fill-gray.stroke-gray {:style {:width "15px" :height "15px"}}
     (svg/social-icon social-service)]]])

(defn component [{:keys [album]} owner opts]
  (om/component
   (html
    (when (seq album)
      [:div.center.mt4
       [:div.h2.medium.dark-gray.crush.m2 "#MayvennMade"]
       (om/build carousel/component
                 {:slides   (map-indexed unattributed-slide album)
                  :settings {:centerMode    true
                             ;; must be in px, because it gets parseInt'd for
                             ;; the slide width calculation
                             :centerPadding "36px"
                             ;; The breakpoints are mobile-last. That is, the
                             ;; default values apply to the largest screens, and
                             ;; 1000 means 1000 and below.
                             :slidesToShow  3
                             :responsive    [{:breakpoint 1000
                                              :settings   {:slidesToShow 2}}]}}
                 opts)
       [:p.center.gray.m2
        "Want to show up on our homepage? "
        "Tag your best pictures wearing Mayvenn with " [:span.bold "#MayvennMade"]]]))))

(defn query [data]
  {:album (get-in data (conj keypaths/ugc-named-searches
                             (:slug (named-searches/current-named-search data))))})

(defn popup-component [{:keys [offset ugc]} owner {:keys [on-close] :as opts}]
  (om/component
   (html
    (ui/modal
     {:on-close on-close
      :bg-class "bg-darken-4"}
     [:div.relative
      (om/build carousel/component
                {:slides       (map attributed-slide (:album ugc))
                 :settings     {:slidesToShow 1
                                :initialSlide offset}}
                {})
      [:div.absolute
       {:style {:top "1.5rem" :right "1.5rem"}}
       (ui/modal-close {:class    "stroke-dark-gray fill-dark-silver"
                        :on-close on-close})]]))))


(defn popup-query [data]
  {:offset (get-in data keypaths/ui-ugc-category-popup-offset)
   :ugc    (query data)})

(defn built-popup-component [data opts]
  (om/build popup-component (popup-query data) opts))
