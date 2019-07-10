(ns storefront.components.ugc
  (:require  [storefront.platform.component-utils :as util]
             [storefront.components.ui :as ui]
             [storefront.components.svg :as svg]
             [storefront.component :as component]
             #?@(:cljs [[goog.string]])))

(defn adventure-social-image-card-component
  [{:keys                [id image-url overlay description social-service icon-url title]
    [nav-event nav-args] :cta/navigation-message}
   owner
   {:keys [copy]}]
  (component/create
   [:div.mb2.px2-on-tb-dt.col-12.left-align
    (merge
     {:key       (str "small-" id)
      :data-test "adventure-look"}
     (util/route-to nav-event nav-args {:back-copy  (:back-copy copy)
                                        :short-name (:short-name copy)}))
    [:div
     [:div.relative.bg-white
      (ui/aspect-ratio
       1 1
       {:class "flex items-center"}
       [:img.col-12.block {:src image-url}])
      (when overlay
        [:div.absolute.flex.justify-end.bottom-0.right-0.mb8
         [:div {:style {:width       "0"
                        :height      "0"
                        :border-top  "28px solid rgba(159, 229, 213, 0.8)"
                        :border-left "21px solid transparent"}}]
         [:div.flex.items-center.px3.bold.h6.bg-transparent-light-teal.whisper
          overlay]])]
     [:div.bg-white.p1.px2.pb2
      [:div.h5.medium.mt1.mb2.black
       [:div.flex.items-center.justify-between.mb2
        [:div.flex.items-center
         (when icon-url
           [:img.mr2.rounded-0
            {:height "41px"
             :width  "64px"
             :src    icon-url}])
         [:div.flex.flex-column
          [:div.h3 title]
          [:div.regular description]]]
        [:div.m1.self-start {:style {:width "21px"
                                     :height "21px"
                                     :opacity 0.4}}
         (svg/social-icon social-service)]]
       [:div.mt2 (ui/underline-button {:height-class "py2"} "Shop Look")]]]]]))

(defn social-image-card-component
  [{:keys                [desktop-aware?
                          id image-url overlay description social-service icon-url title]
    [nav-event nav-args] :cta/navigation-message
    button-type          :cta/button-type}
   owner
   {:keys [copy]}]
  (let [cta-button-fn (case button-type
                        :teal-button      ui/teal-button
                        :underline-button ui/underline-button)]
    (component/create
     [:div.pb2.px1-on-tb-dt.col-12
      (merge {:key (str "small-" id)}
             (when desktop-aware?
               {:class "col-6-on-tb col-4-on-dt"}))
      [:div.relative.bg-white
       (ui/aspect-ratio
        1 1
        {:class "flex items-center"}
        [:img.col-12.block {:src image-url}])
       (when overlay
         [:div.absolute.flex.justify-end.bottom-0.right-0.mb8
          [:div {:style {:width       "0"
                         :height      "0"
                         :border-top  "28px solid rgba(159, 229, 213, 0.8)"
                         :border-left "21px solid transparent"}}]
          [:div.flex.items-center.px3.bold.h6.bg-transparent-light-teal.whisper.white
           overlay]])]
      [:div.bg-white.p1.px2.pb2
       [:div.h5.medium.mt1.mb2
        [:div.flex.items-center.justify-between.mb2
         [:div.flex.items-center
          (when icon-url
            [:img.mr2.rounded-0
             {:height "41px"
              :width  "64px"
              :src    icon-url}])
          [:div.flex.flex-column
           [:div.h3 title]
           [:div.regular description]]]
         [:div.m1.self-start {:style {:width "21px"
                                    :height "21px"
                                    :opacity 0.4}}
          (svg/social-icon social-service)]]]
       (when nav-event
         (cta-button-fn
          (merge
           (util/route-to nav-event nav-args {:back-copy  (:back-copy copy)
                                              :short-name (:short-name copy)})
           {:data-test (str "look-" id)})
          [:span.bold (:button-copy copy)]))]])))

