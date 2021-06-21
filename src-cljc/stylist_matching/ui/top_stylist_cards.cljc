(ns stylist-matching.ui.top-stylist-cards
  (:require [mayvenn.visual.tools :refer [with]]
            [mayvenn.visual.ui.titles :as titles]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [stylist-matching.ui.stylist-cards :as stylist-results-card]
            [storefront.platform.carousel :as carousel]
            [storefront.platform.component-utils :as utils]
            [ui.molecules :as molecules]))

(defn stylist-card-thumbnail-molecule
  "We want ucare-ids here but we do not have them"
  [{:stylist-card.thumbnail/keys [id ucare-id] :as data}]
  (when id
    (component/html
     (if (:screen/seen? data)
       (ui/circle-picture {:width "72px"}
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

(defn stylist-card-salon-name-molecule
  [{:stylist-card.salon-name/keys [id value]}]
  (when id
    (component/html
     [:div.content-2.proxima
      value])))

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

(defn top-stylist-card-cta-molecule
  [{:stylist-card.cta/keys [id label target]}]
  (when id
    (component/html
     (ui/button-medium-primary
      (merge {:data-test id}
             (apply utils/fake-href target))
      label))))

(defn stylist-card-gallery-item-molecule
  [{:stylist-card.gallery-item/keys [id target ucare-id]}]
  (when id
    (component/html
     [:a.block.px1
      (merge
       (apply utils/route-to target)
       {:key id})
      (ui/aspect-ratio
       1 1
       [:img {:src   (str ucare-id "-/scale_crop/216x216/-/format/auto/")
              :class "col-12"}])])))

(defn stylist-card-gallery-molecule
  [{:stylist-card.gallery/keys [id items]}]
  (when id
    (component/html
     [:div.px2
      (component/build carousel/component
                       {:data     items
                        :settings {:controls true
                                   :nav      false
                                   :items    3
                                   ;; setting this to true causes some of our event listeners to
                                   ;; get dropped by tiny-slider.
                                   :loop     false}}
                       {:opts {:mode     :multi
                               :settings {:nav false}
                               :slides   (map stylist-card-gallery-item-molecule items)}})])))

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

(defn top-stylist-card-header-molecule
  [data]
  (let [{:stylist-card.header/keys [target id]} data]
    (when id
      [:div.col-12.flex.items-start.pb2.pt4
       (assoc (apply utils/route-to target) :data-test id)
       [:div.flex.justify-center.items-center.ml4
        (stylist-card-thumbnail-molecule data)]
       [:div.col-9.medium.px3
        (titles/proxima-small-left (with :crown data))
        (titles/proxima-left (with :stylist-card.title data))
        [:div.flex.items-center
         (molecules/stars-rating-molecule data)
         (stylist-ratings-molecule data)
         (stylist-just-added-molecule data)]
        (stylist-card-salon-name-molecule data)
        (stylist-card-address-marker-molecule data)
        (stylist-card-experience-molecule data)]])))

(defn top-stylist-information-points-molecule
  [{:keys [points]}]
  [:div.px2.pt3
   {:style {:display               "grid"
            :grid-template-columns "auto auto"}}
   (for [point points]
     [:div.pb1
      (titles/proxima-small-left point)])])

(defcomponent organism
  [data _ {:keys [id]}]
  [:div.flex.flex-column.left-align.border.border-cool-gray.mx3.mt1.mb3.bg-white
   {:id        id
    :data-test id}
   (top-stylist-card-header-molecule data)
   [:div.col-12
    (if (:screen/seen? data)
      (stylist-card-gallery-molecule data)
      (ui/aspect-ratio 426 105 [:div]))]
   (top-stylist-information-points-molecule (with :laurels data))
   [:div.col-12.py3.px2 (top-stylist-card-cta-molecule data)]])
