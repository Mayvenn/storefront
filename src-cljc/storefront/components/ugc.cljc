(ns storefront.components.ugc
  (:require  [storefront.platform.component-utils :as util]
             [storefront.components.ui :as ui]
             [storefront.components.svg :as svg]
             #?@(:clj [[storefront.component-shim :as component]]
                 :cljs [[storefront.component :as component]
                        [goog.string]])))

(defn user-attribution [{:keys [user-handle social-service]}]
  [:div.flex.items-center
   [:div.flex-auto.dark-gray.medium {:style {:word-break "break-all"}} "@" user-handle]
   [:div.ml1.line-height-1 {:style {:width "1em" :height "1em"}}
    (svg/social-icon social-service)]])

(defn view-look-button [{{:keys [view-look view-other]} :links} button-copy nav-stack-item]
  (let [[nav-event nav-args] (or view-look view-other)]
    (ui/teal-button
     (merge
      (util/route-to nav-event nav-args nav-stack-item)
      (when view-look
        {:data-test (str "look-" (:look-id nav-args))}))
     button-copy)))

(defn image-thumbnail [img]
  [:img.col-12.block img])

(defn image-attribution [look {:keys [button-copy back-copy short-name]}]
  [:div.bg-light-gray.p1
   [:div.h5.mt1.mb2.mx3-on-mb.mx1-on-tb-dt
    (user-attribution look)]
   (view-look-button look button-copy {:back-copy back-copy
                                       :short-name short-name})])

(defn content-view [{:keys [imgs content-type source-url] :as item}]
  (ui/aspect-ratio
   1 1
   {:class "bg-black"}
   (if (= content-type "video")
     [:video.container-size.block {:controls true}
      [:source {:src source-url}]]
     [:div.container-size.bg-cover.bg-no-repeat.bg-center
      {:style {:background-image (str "url(" (-> imgs :large :src) ")")}}])))

(defn component [{:keys [looks]} owner {:keys [copy]}]
  (component/create
   [:div.container.clearfix.mtn2
    (for [{:keys [id imgs] :as look} looks]
      [:div
       {:key id}
       [:div.py2.col-12.col.hide-on-tb-dt {:key (str "small-" id)}
        (image-thumbnail (:medium imgs))
        (image-attribution look copy)]
       [:div.p2.col.col-4.hide-on-mb {:key (str "large-" id)}
        (ui/aspect-ratio
         1 1
         {:class "hoverable"}
         (image-thumbnail (:medium imgs))
         [:div.absolute.bottom-0.col-12.show-on-hover (image-attribution look copy)])]])]))

(def color-details
  {"#2-chocolate-brown"
   {:title "#2 Chocolate Brown"
    :image "//ucarecdn.com/6f2160cb-bc75-48a5-8d2d-6c7bcf7f1215/-/format/auto/prodsitecolorswatch2.png"}
   "18-chestnut-blonde"
   {:title "#18 Chestnut Blonde"
    :image "//ucarecdn.com/171ded35-1e70-4132-8563-9599501a336a/-/format/auto/swatch-018-chestnut-blonde.png"}
   "#1-jet-black"
   {:title "#1 Jet Black"
    :image "//ucarecdn.com/81e6cd3d-6fae-41b1-a7a5-cbefd64a4526/-/format/auto/prodsitecolorswatch1.png"}
   "blonde-dark-roots"
   {:title "Blonde (#613) with Dark Roots (#1B)"
    :image "//ucarecdn.com/02f4a86c-12fa-47b3-8f50-078568e4f905/-/format/auto/blonde_dark_roots.png"}
   "#4-caramel-brown"
   {:title "#4 Caramel Brown"
    :image "//ucarecdn.com/b096b5c3-a469-4c2b-a188-c0de34e8231d/-/format/auto/prodsitecolorswatch4.png"}
   "1c-mocha-brown"
   {:title "#1C Mocha Brown"
    :image "//ucarecdn.com/b0b1f53f-4900-47d8-ba50-0aec454222f4/-/format/auto/swatch-01c-mocha-black.png"}
   "60-golden-ash-blonde"
   {:title "#60 Golden Ash Blonde"
    :image "//ucarecdn.com/d49e6650-a8a0-4d9d-8732-0f2210bf219c/-/format/auto/swatch-060-golden-blonde.png"}
   "dark-blonde-dark-roots"
   {:title "Dark Blonde (#27) with Dark Roots (#1B)"
    :image "//ucarecdn.com/9e15a581-6e80-401a-8cb2-0608fef474e9/-/format/auto/dark_blonde_dark_roots.png"}
   "613-bleach-blonde"
   {:title "#613 Bleach Blonde"
    :image "//ucarecdn.com/361d56ce-97e5-48fc-842a-d848d1cdfefb/-/format/auto/swatch-613-bleach-blonde.png"}
   "black"
   {:title "Natural Black"
    :image "//ucarecdn.com/c172523d-b231-49da-a9c1-51ec8a8e802a/-/format/auto/black_v1.png"}
   "dark-blonde"
   {:title "Dark Blonde (#27)"
    :image "//ucarecdn.com/f7eb2f95-3283-4160-bdf9-38a87be676c2/-/format/auto/dark_blonde.png"}
   "6-hazelnut-brown"
   {:title "#6 Hazelnut Brown"
    :image "//ucarecdn.com/798955ee-84e2-402e-a130-14085825996a/-/format/auto/swatch-006-hazelnut-brown.png"}
   "vibrant-burgundy"
   {:title "Vibrant Burgundy"
    :image "//ucarecdn.com/3629dcae-412e-44a0-bff8-df441beb9975/-/format/auto/prodsitecolorswatch99j.png"}
   "1b-soft-black"
   {:title "#1B Soft Black"
    :image "//ucarecdn.com/41276dc8-e77f-4f7a-b73a-465af9474f04/-/format/auto/prodsitecolorswatch01b.png"}
   "blonde"
   {:title "Blonde (#613)"
    :image "//ucarecdn.com/85ede6dd-8e84-4096-ad5c-685d50dd99ec/-/format/auto/blonde.png"}})

(defn shop-by-look-experiment-component [{:keys [looks]} owner {:keys [copy]}]
  (component/create
   [:div.container.clearfix.mtn2.p4
    (for [{:keys [id imgs] :as look} looks
          :let [{:keys [look-attributes social-service links]} look]]
      [:div
       {:key id}
       [:div.p2.col-12-on-mb.col-6-on-tb.col-4-on-dt.col {:key (str "small-" id)}
        [:div.relative
         [:img.col-12.block (:medium imgs)]
         (when-let [texture (:texture look-attributes)]
           [:div.absolute.flex.justify-end.items-stretch.bottom-0.right-0.mb8
            [:div {:style {:width       "0"
                           :height      "0"
                           :border-top  "28px solid rgba(159, 229, 213, 0.8)"
                           :border-left "21px solid transparent"}}]
            [:div.flex.items-center.px3.medium.h6.bg-transparent-light-teal
             texture]])]
        [:div.bg-light-gray.p1
         [:div.h5.medium.mt1.mb2.mx1
          [:div.flex.items-center
           [:div.flex-auto
            (when-let [color-detail (get color-details (:color look-attributes))]
              [:div.flex.items-center
               [:img.mr2 {:height "25px"
                          :width  "25px"
                          :src    (:image color-detail)}]
               (:title color-detail)])]
           [:div.self-end.line-height-1 {:style {:width "1em" :height "1em"}}
            (svg/social-icon social-service)]]
          [:div.flex-auto (:lengths look-attributes)]]
         (let [{:keys [view-look view-other]} links
               [nav-event nav-args]           (or view-look view-other)]
           (ui/underline-button
            (merge
             (util/route-to nav-event nav-args {:back-copy  (:back-copy copy)
                                                :short-name (:short-name copy)})
             (when view-look
               {:data-test (str "look-" (:look-id nav-args))}))
            [:span.bold (:button-copy copy)]))]]])]))
