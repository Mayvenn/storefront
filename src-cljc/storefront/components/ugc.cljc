(ns storefront.components.ugc
  (:require  [storefront.platform.component-utils :as util]
             [storefront.components.ui :as ui]
             [storefront.components.svg :as svg]
             [storefront.component :as component :refer [defcomponent]]
             #?@(:cljs [[goog.string]])))

(defcomponent ugc-image [{:screen/keys [seen?]
                          :hack/keys   [above-the-fold?]
                          :keys        [image-url overlay]} _ _]
  [:div.relative.bg-white
   (ui/aspect-ratio
    1 1
    {:class "flex items-center"}
    (if (or above-the-fold? seen?)
      (ui/ucare-img {:class           "col-12"
                     :picture-classes "col-12"}
                    image-url)
      [:div.col-12 " "]))
   (when overlay
     [:div.absolute.flex.justify-end.bottom-0.right-0.mb8
      [:div {:style {:width       "0"
                     :height      "0"
                     :border-top  "28px solid #dedbe5"
                     :border-left "21px solid transparent"}}]
      [:div.flex.items-center.px3.bold.h6.bg-pale-purple.whisper.white
       overlay]])])

(defcomponent social-image-card-component
  [{:keys                [desktop-aware?
                          id overlay description icon-url title]
    :screen/keys         [seen?]
    [nav-event nav-args] :cta/navigation-message
    button-type          :cta/button-type
    :as card}
   owner
   {:keys [copy]}]
  (let [cta-button-fn (case button-type
                        :p-color-button   ui/button-large-primary
                        :underline-button ui/button-medium-secondary)]
    [:div.pb2.px1-on-tb-dt.col-12
     (merge {:key (str "small-" id)}
            (when desktop-aware?
              {:class "col-6-on-tb col-4-on-dt"}))
     [:div.relative.bg-white
      (util/route-to nav-event nav-args {:back-copy  (:back-copy copy)
                                         :short-name (:short-name copy)})
      (ui/screen-aware ugc-image (select-keys card [:overlay :image-url :hack/above-the-fold?]))
      (when overlay
        [:div.absolute.flex.justify-end.bottom-0.right-0.mb8
         [:div {:style {:width       "0"
                        :height      "0"
                        :border-top  "28px solid #dedbe5"
                        :border-left "21px solid transparent"}}]
         [:div.flex.items-center.px3.bold.h6.bg-pale-purple.whisper.white
          overlay]])]
     [:div.bg-white.p1.px3.pb3
      [:div.h5.medium.mt1.mb2
       [:div.flex.items-center.justify-between.mb2
        [:div.flex.items-center
         (when (and seen? icon-url)
           [:img.mr2.rounded-0
            {:height "41px"
             :width  "64px"
             :src    icon-url}])
         [:div.flex.flex-column
          [:div.h3 title]
          [:div.regular description]]]
        [:div.m1.self-start {:style {:width  "21px"
                                     :height "21px"}}
         ^:inline (svg/instagram)]]]
      (when nav-event
        (cta-button-fn
         (merge
          (util/route-to nav-event nav-args {:back-copy  (:back-copy copy)
                                             :short-name (:short-name copy)})
          {:data-test (str "look-" id)})
         [:span.bold (:button-copy copy)]))]]))

