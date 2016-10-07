(ns storefront.platform.ugc
  (:require [sablono.core :refer-macros [html]]
            [om.core :as om]
            [storefront.accessors.named-searches :as named-searches]
            [storefront.keypaths :as keypaths]
            [storefront.platform.carousel :as carousel]))

(defn image-thumbnail [{:keys [id photo user-handle purchase-link]}]
  [:div.p1
   [:div.relative.overflow-hidden
    {:style {:padding-top "100%"}} ;; To keep square aspect ratio. Refer to https://css-tricks.com/snippets/sass/maintain-aspect-ratio-mixin/
    [:img.col-12.absolute.top-0.block {:src photo}]]])

(defn component [{:keys [album]} owner opts]
  (om/component
   (html
    (when (seq album)
      [:div.center.mt4
       [:div.h2.medium.dark-gray.crush.m2 "#MayvennMade"]
       (om/build carousel/component
                 {:slides   (map image-thumbnail album)
                  :settings {:centerMode    true
                             ;; must be in px, because it gets parseInt'd for
                             ;; the slide width calculation
                             :centerPadding "36px"
                             ;; The breakpoints are mobile-last. That is, the
                             ;; default values apply to the largest screens, and
                             ;; 768 means 768 and below.
                             :slidesToShow  3
                             :responsive    [{:breakpoint 768
                                              :settings   {:slidesToShow 2}}]}}
                 opts)
       [:p.center.gray.m2
        "Want to show up on our homepage? "
        "Tag your best pictures wearing Mayvenn with " [:span.bold "#MayvennMade"]]]))))

(defn query [data]
  {:album (get-in data (conj keypaths/ugc-named-searches
                             (:slug (named-searches/current-named-search data))))})
