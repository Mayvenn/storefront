(ns storefront.platform.ugc
  (:require [sablono.core :refer-macros [html]]
            [om.core :as om]
            [storefront.accessors.named-searches :as named-searches]
            [storefront.keypaths :as keypaths]
            [storefront.platform.carousel :as carousel]))

(defn image-thumbnail [{:keys [id photo user-handle purchase-link]}]
  [:div.p1
   [:img.col-12.block {:src photo}]
   #_[:div.my1.light-gray.bold.h4.right-align "@" user-handle]])

(defn component [{:keys [album]} owner opts]
  (om/component
   (html
    (when (seq album)
      [:div.center.mt4
       [:div.h2.medium.dark-gray.crush.m2 "#MayvennMade"]
       (om/build carousel/component {:slides   (map image-thumbnail album)
                                     :settings {:centerMode    true
                                                :centerPadding "36px"
                                                :slidesToShow  3
                                                ;; The breakpoints are mobile-last. That is, the
                                                ;; default values apply to the largest screens, and
                                                ;; 768 means 768 and below.
                                                :responsive [{:breakpoint 768
                                                              :settings   {:slidesToShow 2}}]}} opts)
       [:p.center.gray.m2
        "Want to show up on our homepage? "
        "Tag your best pictures wearing Mayvenn with " [:span.bold "#MayvennMade"]]]))))

(defn query [data]
  {:album (get-in data (conj keypaths/ugc-named-searches
                             (:slug (named-searches/current-named-search data))))})
