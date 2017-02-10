(ns storefront.platform.ugc
  (:require [sablono.core :refer-macros [html]]
            [om.core :as om]
            [storefront.accessors.named-searches :as named-searches]
            [storefront.accessors.experiments :as experiments]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.platform.component-utils :as util]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.platform.carousel :as carousel]))

(defn carousel-slide [slug idx {:keys [imgs content-type]}]
  [:div.p1
   [:a (util/route-to events/navigate-ugc-category {:named-search-slug slug :query-params {:offset idx}})
    (ui/aspect-ratio
     1 1
     {:class "flex items-center"}
     [:img.col-12 (:medium imgs)]
     (when (= content-type "video")
       [:div.absolute.overlay.flex.items-center.justify-center
        svg/play-video-muted]))]])

(defn component [{:keys [album slug]} owner opts]
  (om/component
   (html
    (when (seq album)
      [:div.center.mt4
       [:div.h2.medium.dark-gray.crush.m2 "#MayvennMade"]
       (om/build carousel/component
                 {:slides   (map-indexed (partial carousel-slide slug) album)
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
  (let [slug (:slug (named-searches/current-named-search data))
        image-ids (get-in data (conj keypaths/ugc-albums slug))]
    {:slug  slug
     :album (map (get-in data keypaths/ugc-images) image-ids)}))

(defn content-view [{:keys [imgs content-type source-url] :as item}]
  (ui/aspect-ratio
   1 1
   {:class "bg-black"}
   (if (= content-type "video")
     [:video.container-size.block {:controls true}
      [:source {:src source-url}]]
     [:div.container-size.bg-cover.bg-no-repeat.bg-center
      {:style {:background-image (str "url(" (-> imgs :large :src) ")")}}])))

(defn view-look-button [{:keys [links]}]
  (let [{:keys [view-look view-other]} links]
    (ui/teal-button
     (apply util/route-to (or view-look view-other))
     "View this look")))

(defn user-attribution [{:keys [user-handle social-service]}]
  [:div.flex.items-center
   [:div.flex-auto.dark-gray.bold {:style {:word-break "break-all"}} "@" user-handle]
   [:div.ml1 {:style {:width "15px" :height "15px"}}
    (svg/social-icon social-service)]])

(defn popup-slide [shop-ugcwidget? {:keys [links] :as item}]
  [:div.m1.rounded-bottom
   (content-view item)
   [:div.bg-white.rounded-bottom.p2
    [:div.h5.px4 (user-attribution item)]
    (when (and shop-ugcwidget? (-> links :view-look boolean))
      [:div.mt2 (view-look-button item)])]])

(defn popup-component [{:keys [offset ugc shop-ugcwidget?]} owner opts]
  (om/component
   (html
    (let [close-attrs (util/route-to events/navigate-category {:named-search-slug (:slug ugc)})]
      (ui/modal
       {:close-attrs close-attrs}
       [:div.relative
        (om/build carousel/component
                  {:slides       (map (partial popup-slide shop-ugcwidget?) (:album ugc))
                   :settings     {:slidesToShow 1
                                  :initialSlide offset}}
                  {})
        [:div.absolute
         {:style {:top "1.5rem" :right "1.5rem"}}
         (ui/modal-close {:class    "stroke-dark-gray fill-gray"
                          :close-attrs close-attrs})]])))))

(defn popup-query [data]
  {:ugc             (query data)
   :offset          (get-in data keypaths/ui-ugc-category-popup-offset)
   :shop-ugcwidget? (experiments/shop-ugcwidget? data)})

(defn built-popup-component [data opts]
  (om/build popup-component (popup-query data) opts))
