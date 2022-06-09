(ns stylist-matching.ui.stylist-cards
  (:require [mayvenn.visual.tools :refer [with]]
            [mayvenn.visual.ui.titles :as titles]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.platform.carousel :as carousel]
            [storefront.platform.component-utils :as utils]
            [ui.molecules :as molecules]))

(defn stylist-card-top-stylist-badge
  [{:stylist-card.top-stylist.badge/keys [id icon primary]}]
  (when id
    (component/html
     [:div.flex.mb1
      {:data-test id}
      [:div.bg-p-color.white.button-font-4.shout.capped.pl1.pr3.flex.items-baseline.pyp2
       (svg/symbolic->html icon)
       primary]])))

(defn stylist-card-address-marker-molecule
  [{:stylist-card.address-marker/keys [id value]}]
  (when id
    (component/html
     [:div.content-3.col-12.flex.items-center.pyp1
      (svg/map-pin {:class  "mrp3 fill-p-color"
                    :width  "14px"
                    :height "14px"})
      [:span.overflow-hidden.nowrap
       {:style {:text-overflow "ellipsis"}}
       value]])))

(defn stylist-card-gallery-item-molecule
  [{:stylist-card.gallery-item/keys [id target ucare-id]}]
  (when id
    (component/html
     [:a.block.px1
      (merge
       (apply utils/route-to target)
       {:key        id
        :aria-label "See larger version of photo in gallery"})
      (ui/aspect-ratio
       1 1
       [:img {:src   (str ucare-id "-/scale_crop/216x216/-/format/auto/")
              :alt   "stylist's previous work"
              :class "col-12"}])])))

(defn desktop-stylist-card-gallery-molecule
  [{:stylist-card.gallery/keys [id items]}]
  (when id
    (component/html
     [:div.px2
      (component/build carousel/component
                       {:data   items
                        :slides (map stylist-card-gallery-item-molecule items)}
                       {:opts {:settings {:nav   false
                                          :items 6
                                          ;; setting this to true causes some of our event listeners to
                                          ;; get dropped by tiny-slider.
                                          :loop  false}}})])))

(defn stylist-card-gallery-molecule
  [{:stylist-card.gallery/keys [id items]}]
  (when id
    (component/html
     [:div.px2
      (component/build carousel/component
                       {:data   items
                        :slides (map stylist-card-gallery-item-molecule items)}
                       {:opts {:settings {:nav   false
                                          :items 3
                                          ;; setting this to true causes some of our event listeners to
                                          ;; get dropped by tiny-slider.
                                          :loop  false}}})])))

(defn stylist-card-cta-molecule
  [{:stylist-card.cta/keys [id label target] :keys [desktop?]}]
  (when id
    (component/html
     (ui/button-medium-secondary
      (merge {:data-test (when-not desktop? id)}
             (apply utils/fake-href target))
      label))))

(defn stylist-card-thumbnail-molecule
  "We want ucare-ids here but we do not have them"
  [{:stylist-card.thumbnail/keys [id ucare-id alt] :as data}]
  (when id
    (component/html
     (if (:screen/seen? data)
       (ui/circle-picture {:width "72px"
                           :alt   alt}
                          (ui/square-image {:resizable-url ucare-id}
                                           72))
       [:div {:style {:height "72px" :width "72px"}}]))))

(defn stylist-ratings-molecule
  [{:stylist-ratings/keys [id content]}]
  (when id
    [:div.flex.items-center.content-3.ml1
     {:data-test id}
     content]))

(defn stylist-just-added-molecule
  [{:stylist.just-added/keys [content id]}]
  (when id
    [:div.pb1
     [:div.content-3.proxima.bold.items-center.flex.border.border-dark-gray.px2
      {:data-test id}
      [:img {:src "https://ucarecdn.com/b0f70f0a-51bf-4369-b6b8-80480b54b6f1/-/format/auto/" :alt "" :width 9 :height 14}]
      [:div.pl1.shout.dark-gray.letter-spacing-1 content]]]))

(defn stylist-card-experience-molecule
  [{:stylist-experience/keys [content id]}]
  (when id
    [:div.content-3.col-12.flex.items-center.flex
     {:data-test id}
     (svg/experience-badge {:class  "mrp3 fill-p-color"
                            :style  {:margin-bottom "2px"}
                            :width  "12px"
                            :height "12px"})
     [:span content]]))

(defn top-stylist-information-points-molecule
  [{:keys [points desktop?]}]
  [:div.pt3.mx-auto
   {:style {:display               "grid"
            :grid-template-columns "auto 10px auto"
            :max-width             "305px"}}
   (interleave
    (for [{:keys [id icon primary]} points]
      [:div.pb1.proxima.content-3.flex
       [:div.flex.items-center.justify-center {:style {:width "25px"}}
        (when icon
          (svg/symbolic->html icon))]
       [:div (when-not desktop? {:data-test id}) primary]])
    (cycle [[:div] nil]))])

(defn stylist-card-header-molecule
  [{:stylist-card.header/keys [target id] :keys [desktop?] :as data}]
  (when id
    [:a.block.col-12.flex.items-start.pb2.pt4.inherit-color
     (assoc (apply utils/route-to target) :data-test (when-not desktop? id))
     [:div.flex.justify-center.items-center.col-3.ml4
      (stylist-card-thumbnail-molecule data)]
     [:div.col-9.medium.px3
      (stylist-card-top-stylist-badge data)
      (titles/proxima-left (with :stylist-card.title data))
      [:div.flex.items-center
       (molecules/stars-rating-molecule data)
       (stylist-ratings-molecule data)
       (stylist-just-added-molecule data)]
      (stylist-card-address-marker-molecule data)
      (stylist-card-experience-molecule data)]]))

(defcomponent organism
  [data _ {:keys [id]}]
  [:div.flex.flex-column.left-align.border.border-cool-gray.mx3.mt1.mb3.bg-white
   {:id        id
    :data-test id}
   (stylist-card-header-molecule data)
   [:div.col-12
    (if (:screen/seen? data)
      (stylist-card-gallery-molecule data)
      (ui/aspect-ratio 426 105 [:div]))]
   (top-stylist-information-points-molecule (with :stylist-card.top-stylist.laurels data))
   [:div.col-12.pt1.pb3.px2 (stylist-card-cta-molecule data)]])

(defcomponent desktop-organism
  [{:keys [stylist-card-header/id] :as data} _ {:keys [id]}]
  (let [data (assoc data :desktop? true)]
    [:div.flex.left-align.border.border-warm-gray.mx3.mt1.mb3.bg-white
     {:id        (str "desktop-" id)
      :data-test id}
     [:div.flex-column
      {:style {:max-width 360}}
      (stylist-card-header-molecule data)
      (top-stylist-information-points-molecule (with :stylist-card.top-stylist.laurels data))
      [:div.col-12.p2 (stylist-card-cta-molecule data)]]
     [:div.col-12.my3
      (if (:screen/seen? data)
        (desktop-stylist-card-gallery-molecule data)
        (ui/aspect-ratio 426 105 [:div]))]]))
