(ns stylist-matching.ui.stylist-cards
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.platform.carousel :as carousel]
            [storefront.platform.component-utils :as utils]
            [ui.molecules :as molecules]))

(defn checks-or-x-atom
  [label value?]
  [:div.inline-block.content-3.proxima
   [:div.mr2.flex.items-center
    [:span.mr1
     (if value?
       (svg/check-mark {:class "black"
                        :style {:width  10
                                :height 10}})
       (svg/x-sharp {:class  "black"
                     :style {:width  8
                             :height 8}}))]
    label]])

(defn stylist-card-services-list-molecule
  [{:stylist-card.services-list/keys [id value]}]
  (when id
    (component/html
     [:div.h8.col-12
      value])))

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
     [:div.content-3.col-12.flex.items-center
      (svg/map-pin {:class  "mrp3"
                    :width  "14px"
                    :height "14px"})
      [:span.overflow-hidden.nowrap
       {:style {:text-overflow "ellipsis"}}
       value]])))

(defn stylist-card-title-molecule
  [{:stylist-card.title/keys [id primary]}]
  (when id
    (component/html
     [:div.proxima.title-2.shout
      {:data-test id}
      primary])))

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
                       {:slides   (map stylist-card-gallery-item-molecule items)
                        :settings {:nav   false
                                   :items 3
                                   ;; setting this to true causes some of our event listeners to
                                   ;; get dropped by tiny-slider.
                                   :loop  false}}
                       {})])))

(defn stylist-card-cta-molecule
  [{:stylist-card.cta/keys [id label target]}]
  (when id
    (component/html
     (ui/button-medium-secondary
      (merge {:data-test id}
             (apply utils/fake-href target))
      label))))

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

(defn stylist-ratings-count-molecule
  [{:ratings/keys [rating-count]}]
  (when rating-count
    [:div.flex.items-center.content-3.ml1
     "(" rating-count ")"]))

(defn stylist-card-header-molecule
  [{:stylist-card.header/keys [target id] :as data}]
  (when id
    [:div.col-12.flex.items-start.pb2.pt4
     (assoc (apply utils/route-to target) :data-test id)
     [:div.flex.justify-center.items-center.col-3.ml4
      (stylist-card-thumbnail-molecule data)]
     [:div.col-9.medium.px3
      (stylist-card-title-molecule data)
      [:div.flex.items-center
       (molecules/stars-rating-molecule data)
       (stylist-ratings-count-molecule data)]
      (stylist-card-salon-name-molecule data)
      (stylist-card-address-marker-molecule data)
      (stylist-card-services-list-molecule data)]]))

(defcomponent organism
  [data _ {:keys [id]}]
  [:div.flex.flex-column.left-align.rounded.border.border-cool-gray.mx3.my3.bg-white
   {:id        id
    :data-test id}
   (stylist-card-header-molecule data)
   [:div.col-12
    (if (:screen/seen? data)
      (stylist-card-gallery-molecule data)
      (ui/aspect-ratio 426 105 [:div]))]
   [:div.col-12.py3.px2 (stylist-card-cta-molecule data)]])
