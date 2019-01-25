(ns storefront.components.ugc
  (:require  [storefront.platform.component-utils :as util]
             [storefront.components.ui :as ui]
             [storefront.components.svg :as svg]
             [storefront.component :as component]
             #?@(:cljs [[goog.string]])))

(defn user-attribution [{:keys [user-handle social-service]}]
  [:div.flex.items-center
   [:div.flex-auto.dark-gray.medium {:style {:word-break "break-all"}} "@" user-handle]
   [:div.ml1.line-height-1 {:style {:width "1em" :height "1em"}}
    (svg/social-icon social-service)]])

(defn adventure-user-attribution [{:keys [user-handle social-service]}]
  [:div.flex.items-center
   [:div.flex-auto.dark-gray.medium
    {:style {:word-break "break-all"}}
    [:div.left "@" user-handle]]
   [:div.ml1.line-height-1 {:style {:width "1.5em" :height "1.5em"}}
    (svg/social-icon social-service)]])

(defn view-look-button [{{:keys [view-look view-other]} :links} button-copy nav-stack-item]
  (let [[nav-event nav-args] (or view-look view-other)]
    (ui/teal-button
     (merge
      (util/route-to nav-event nav-args nav-stack-item)
      (when view-look
        {:data-test (str "look-" (:look-id nav-args))}))
     button-copy)))

(defn content-view [{:keys [imgs content-type source-url] :as item}]
  (ui/aspect-ratio
   1 1
   {:class "bg-black"}
   (if (= content-type "video")
     [:video.container-size.block {:controls true}
      [:source {:src source-url}]]
     [:div.container-size.bg-cover.bg-no-repeat.bg-center
      {:style {:background-image (str "url(" (-> imgs :large :src) ")")}}])))

(defn adventure-component [{:keys [looks color-details]} owner {:keys [copy]}]
  (component/create
   [:div.flex.flex-wrap.mtn2.py4.px2.justify-center.justify-start-on-tb-dt
    (for [{:keys [id imgs] :as look} looks
          :let
          [{:keys [look-attributes social-service links]} look
           {:keys [view-look view-other]}                 links
           [nav-event nav-args]                           (or view-look
                                                              view-other)]]
      [:div.p2.col-12.col-6-on-tb.col-4-on-dt.left-align
       (merge {:key (str "small-" id)
               :data-test "adventure-look"}
              (util/route-to nav-event nav-args {:back-copy  (:back-copy copy)
                                                 :short-name (:short-name copy)}))
       [:div.border.border-light-gray
        [:div.relative
         (ui/aspect-ratio
          1 1
          {}
          [:img.col-12.block (:medium imgs)])
         (when-let [texture (:texture look-attributes)]
           [:div.absolute.flex.justify-end.bottom-0.right-0.mb8.black
            [:div {:style {:width       "0"
                           :height      "0"
                           :border-top  "28px solid rgba(159, 229, 213, 0.8)"
                           :border-left "21px solid transparent"}}]
            [:div.flex.items-center.px3.medium.h6.bg-transparent-light-teal
             texture]])]
        (let [color-detail (get color-details (:color look-attributes))]
          [:div.bg-white.p1.px2.pb2
           [:div.h5.medium.mt1.black
            [:div.flex.items-center.mb2
             [:div.flex-auto
              (if color-detail
                [:div.flex.items-center
                 [:img.mr2.lit.rounded-0
                  {:height "30px"
                   :width  "50px"
                   :src    (:option/rectangle-swatch color-detail)}]
                 (:option/name color-detail)]
                [:div.black.pyp1 "Check this out!"])]
             [:div.m1.self-end {:style {:width "20px" :height "20px"}}
              (svg/social-icon social-service)]]
            (cond (:lengths look-attributes) (:lengths look-attributes)
                  color-detail               [:span.black "Get this look!"]
                  :else                      nil)]])]])]))

(defn component [{:keys [looks color-details]} owner {:keys [copy]}]
  (component/create
   [:div.flex.flex-wrap.mtn2.py4.px2.justify-center.justify-start-on-tb-dt
    (for [{:keys [id imgs] :as look} looks
          :let [{:keys [look-attributes social-service links]} look
                {:keys [view-look view-other]} links
                [nav-event nav-args]           (or view-look view-other)]]
      [:div.p2.col-12.col-6-on-tb.col-4-on-dt {:key (str "small-" id)}
       [:div.relative
        (ui/aspect-ratio
         1 1
         {}
         [:img.col-12.block (:medium imgs)])
        (when-let [texture (:texture look-attributes)]
          [:div.absolute.flex.justify-end.bottom-0.right-0.mb8
           [:div {:style {:width       "0"
                          :height      "0"
                          :border-top  "28px solid rgba(159, 229, 213, 0.8)"
                          :border-left "21px solid transparent"}}]
           [:div.flex.items-center.px3.medium.h6.bg-transparent-light-teal
            texture]])]
       [:div.bg-light-gray.p1.px2.pb2
        [:div.h5.medium.mt1.mb2
         [:div.flex.items-center.mb2
          [:div.flex-auto
           (if-let [color-detail (get color-details (:color look-attributes))]
             [:div.flex.items-center
              [:img.mr2.lit.rounded-0
               {:height "30px"
                :width  "50px"
                :src    (:option/rectangle-swatch color-detail)}]
              (:option/name color-detail)]
             [:div.light-gray.pyp1 "Check this out!"])]
          [:div.m1.self-end {:style {:width "20px" :height "20px"}}
           (svg/social-icon social-service)]]
         (or (:lengths look-attributes) [:span.light-gray "Get this look!"])]
        (ui/underline-button
         (merge
          (util/route-to nav-event nav-args {:back-copy  (:back-copy copy)
                                             :short-name (:short-name copy)})
          (when view-look
            {:data-test (str "look-" (:look-id nav-args))}))
         [:span.bold (:button-copy copy)])]])]))
