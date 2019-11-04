(ns stylist-matching.ui.stylist-cards
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.platform.carousel :as carousel]
            [storefront.platform.component-utils :as utils]
            
            ))

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
     (if value?
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

(defn control-stylist-card-address-marker-molecule
  [{:stylist-card.address-marker/keys [id value]}]
  (when id
    (component/html
     [:div.h7.col-12.flex.items-center
      value])))

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

(defn control-stylist-card-title-molecule
  [{:stylist-card.title/keys [id primary]}]
  (when id
    (component/html
     [:div.h3.line-height-1.light
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
              :class "rounded col-12"}])])))

(defn stylist-card-gallery-molecule
  [{:stylist-card.gallery/keys [id items]}]
  (when id
    (component/html
     [:div.px2
      (component/build carousel/component
                       {:slides   (map stylist-card-gallery-item-molecule items)
                        :settings {:nav       false
                                   :items     3}}
                       {})])))

(defn control-stylist-card-gallery-item-molecule
  [{:stylist-card.gallery-item/keys [id target ucare-id]}]
  (component/html
   [:a.block.px1
    (merge
     (apply utils/fake-href target)
     {:key id})
    (ui/aspect-ratio
     1 1
     [:img {:src   (str ucare-id "-/scale_crop/216x216/-/format/auto/")
            :class "rounded col-12"}])]))

(defn control-stylist-card-gallery-molecule
  [{:stylist-card.gallery/keys [id title items]}]
  (when id
    (component/html
     [:div
      [:h6.dark-gray.bold.left-align.mb2.ml1.h7 title]
      (component/build carousel/component
                       {:slides   (map control-stylist-card-gallery-item-molecule items)
                        :settings {:nav   false
                                   :items 3}}
                       {})])))

(defn stylist-card-cta-molecule
  [{:stylist-card.cta/keys [id label target]}]
  (when id
    (component/html
     (ui/underline-button
      (merge {:data-test id}
             (apply utils/fake-href target))
      label))))

(defn control-stylist-card-cta-molecule
  [{:stylist-card.cta/keys [id label target]}]
  (when id
    (component/html
     (ui/teal-button
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

(defn control-stylist-card-thumbnail-molecule
  "We want ucare-ids here but we do not have them"
  [{:stylist-card.thumbnail/keys [id ucare-id]}]
  (when id
    (component/html
     (ui/circle-picture {:width "104px"}
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

(defn control-stylist-card-header-molecule
  [{:stylist-card/keys [target id] :as data}]
  (when id
    [:div.col-12.flex.items-start.p2
     (assoc (apply utils/route-to target) :data-test id)
     [:div.flex.justify-center.items-center
      (control-stylist-card-thumbnail-molecule data)]
     [:div.medium.px2
      (control-stylist-card-title-molecule data)
      [:span.h7.flex.items-center.pyp2
       (stylist-card-stars-rating-molecule data)]
      (control-stylist-card-address-marker-molecule data)]]))

(defcomponent control-organism
  [data _ _]
  [:div.flex.flex-column.left-align.mx3.my3
    {:key (:react/key data)}
    (control-stylist-card-header-molecule data)
    [:div.col-12
     (control-stylist-card-gallery-molecule data)]
    [:div.col-12.p2
     (control-stylist-card-cta-molecule data)]])

(defcomponent experiment-organism
  [data _ _]
  [:div.flex.flex-column.left-align.rounded.border.border-light-gray.mx3.my3.bg-white
    {:key (:react/key data)}
    (stylist-card-header-molecule data)
    [:div.col-12
     (stylist-card-gallery-molecule data)]
    [:div.col-12.p2
     (stylist-card-cta-molecule data)]])
