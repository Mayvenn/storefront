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

(defn image-view [{:keys [src]}]
  (ui/aspect-ratio
   1 1
   {:class "bg-black"}
   [:div.container-size.bg-cover.bg-no-repeat.bg-center
    {:style {:background-image (str "url(" src ")")}}]))

(defn video-view [video-url]
  (ui/aspect-ratio
   1 1
   {:class "bg-black"}
   [:video.container-size.block {:controls true}
    [:source {:src video-url}]]))

(defn unattributed-slide [slug idx {:keys [imgs content-type]}]
  [:div.p1
   [:a (util/route-to events/navigate-ugc-category {:named-search-slug slug :query-params {:offset idx}})
    (ui/aspect-ratio
     1 1
     {:class "flex items-center"}
     [:img.col-12 (:medium imgs)]
     (when (= content-type "video")
       [:div.absolute.overlay.flex.items-center.justify-center
        svg/play-video-muted]))]])

(defn attributed-slide [{:keys [user-handle imgs social-service content-type source-url] :as item}]
  [:div.m1.rounded-bottom
   (if (= content-type "video")
     (video-view source-url)
     (image-view (:large imgs)))
   [:div.flex.items-center.rounded-bottom.bg-white.py2.px3
    [:div.flex-auto.dark-gray.bold "@" user-handle]
    [:div.fill-dark-gray.stroke-dark-gray {:style {:width "15px" :height "15px"}}
     (svg/social-icon social-service)]]])

(defn component [{:keys [album slug]} owner opts]
  (om/component
   (html
    (when (seq album)
      [:div.center.mt4
       [:div.h2.medium.dark-gray.crush.m2 "#MayvennMade"]
       (om/build carousel/component
                 {:slides   (map-indexed (partial unattributed-slide slug) album)
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
       [:p.center.dark-gray.m2
        "Want to show up on our homepage? "
        "Tag your best pictures wearing Mayvenn with " [:span.bold "#MayvennMade"]]]))))

(defn query [data]
  (let [slug (:slug (named-searches/current-named-search data))]
    {:slug  slug
     :album (get-in data (conj keypaths/ugc-named-searches slug))}))

(defn popup-component [{:keys [offset ugc slug]} owner opts]
  (om/component
   (html
    (let [on-close (:on-click (util/route-to events/navigate-category {:named-search-slug slug}))]
      (ui/modal
       {:on-close on-close}
       [:div.relative
        (om/build carousel/component
                  {:slides       (map attributed-slide (:album ugc))
                   :settings     {:slidesToShow 1
                                  :initialSlide offset}}
                  {})
        [:div.absolute
         {:style {:top "1.5rem" :right "1.5rem"}}
         (ui/modal-close {:class    "stroke-dark-gray fill-gray"
                          :on-close on-close})]])))))


(defn popup-query [data]
  {:offset (get-in data keypaths/ui-ugc-category-popup-offset)
   :slug   (:slug (named-searches/current-named-search data))
   :ugc    (query data)})

(defn built-popup-component [data opts]
  (om/build popup-component (popup-query data) opts))
