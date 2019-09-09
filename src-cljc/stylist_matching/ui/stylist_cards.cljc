(ns stylist-matching.ui.stylist-cards
  (:require [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.platform.carousel :as carousel]
            [storefront.platform.component-utils :as utils]
            [storefront.components.svg :as svg]
            [adventure.organisms.call-out-center :as call-out-center]
            [storefront.events :as events]))

(defn ^:private star [index type]
  [:span.mrp1
   {:key (str (name type) "-" index)}
   (case type
     :whole         (svg/whole-star {:height "13px" :width "13px"})
     :three-quarter (svg/three-quarter-star {:height "13px" :width "13px"})
     :half          (svg/half-star {:height "13px" :width "13px"})
     :empty         (svg/empty-star {:height "13px" :width "13px"})
     nil)])

(defn rating->stars
  "gets weird"
  [rating full-rating]
  (when (pos? full-rating)
    (conj
     (rating->stars (dec rating) (dec full-rating))
     (condp <= rating
       1    :whole
       0.75 :three-quarter
       0.50 :half-star
       :empty))))

(defn star-rating
  [rating]
  [:div.flex.items-center
   [:span.mrp2 rating]
   (map-indexed star (rating->stars rating 5))])

(defn checks-or-x-atom
  [label value?]
  [:div.inline-block
   [:div.mr2.flex.items-center
    [:span.mr1
     (if true
       (ui/ucare-img {:width 10}
                     "2560cee9-9ac7-4706-ade4-2f92d127b565")
       (svg/simple-x {:class "dark-silver"
                      :style {:width  8
                              :height 8}}))]
    label]])

(defn stylist-card-services-list-molecule
  [{:stylist-card.services-list/keys [id value]}]
  (when id
    (component/html
     [:div.h8.col-12.dark-gray
      value])))

(defn stylist-card-address-marker-molecule
  [{:stylist-card.address-marker/keys [id value]}]
  (when id
    (component/html
     [:div.h7.col-12.flex.items-center
      (svg/position {:width "10px"
                     :height "13px"
                     :class "flex-none mr1"})
      [:span.overflow-hidden.nowrap.dark-gray
       {:style {:text-overflow "ellipsis"}}
       value]])))

(defn stylist-card-stars-rating-molecule
  [{:rating/keys [value]}]
  (component/html
   [:div.h6.orange
    (star-rating value)]))

(defn stylist-card-title-molecule
  [{:stylist-card.title/keys [id primary]}]
  (when id
    (component/html
     [:div.h4.navy.line-height-1
      {:data-test id}
      primary])))

(defn stylist-card-gallery-item-molecule
  [{:stylist-card.gallery-item/keys [id target ucare-id]}]
  (when id
    (component/html
     [:div.px1
      (merge
       (apply utils/route-to target)
       {:key id})
      (ui/aspect-ratio
       1 1
       [:img {:src   (str ucare-id "-/scale_crop/216x216/-/format/auto/")
              :class "rounded col-12"}])])))

(defn stylist-card-gallery-molecule
  [{:stylist-card.gallery/keys [id items]}]
  (when id
    (component/html
     [:div.px2
      (component/build carousel/component
                       {:slides   (map stylist-card-gallery-item-molecule items)
                        :settings {:swipe        true
                                   :initialSlide 0
                                   :arrows       true
                                   :dots         false
                                   :slidesToShow 3
                                   :infinite     true}}
                       {})])))

(defn stylist-card-cta-molecule
  [{:stylist-card.cta/keys [id label target]}]
  (when id
    (component/html
     (ui/underline-button
      (merge {:data-test id}
             (apply utils/fake-href target))
      label))))

(defn stylist-card-thumbnail-molecule
  "We want ucare-ids here but we do not have them"
  [{:stylist-card.thumbnail/keys [id ucare-id]}]
  (when id
    (component/html
     (ui/circle-picture {:width "72px"}
                        (ui/square-image {:resizable-url ucare-id}
                                         72)))))
(defn stylist-card-header-molecule
  [{:stylist-card/keys [target id] :as data}]
  (when id
    [:div.col-12.flex.items-start.p2
     (assoc (apply utils/route-to target) :data-test id)
     [:div.flex.justify-center.items-center.col-3
      (stylist-card-thumbnail-molecule data)]
     [:div.col-9.medium.px2
      (stylist-card-title-molecule data)
      [:span.h7.flex.items-center
       (stylist-card-stars-rating-molecule data)]
      (stylist-card-address-marker-molecule data)
      (stylist-card-services-list-molecule data)]]))

(defn organism
  [data _ _]
  (component/create
   [:div.flex.flex-column.left-align.rounded.border.border-light-gray.m3.bg-white.p0
    {:key (:react/key data)}
    (stylist-card-header-molecule data)
    [:div.col-12
     (stylist-card-gallery-molecule data)]
    [:div.col-12.p2
     (stylist-card-cta-molecule data)]]))
