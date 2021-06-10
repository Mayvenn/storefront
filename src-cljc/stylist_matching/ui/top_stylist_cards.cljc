(ns stylist-matching.ui.top-stylist-cards
  (:require [mayvenn.visual.tools :refer [with]]
            [mayvenn.visual.ui.titles :as titles]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [stylist-matching.ui.stylist-cards :as card]
            [storefront.platform.component-utils :as utils]
            [ui.molecules :as molecules]))

(defn top-stylist-card-cta-molecule
  [{:stylist-card.cta/keys [id label target]}]
  (when id
    (component/html
     (ui/button-medium-primary
      (merge {:data-test id}
             (apply utils/fake-href target))
      label))))

(defn top-stylist-card-header-molecule
  [data]
  (let [{:stylist-card.header/keys [target id]} data]
    (when id
      [:div.col-12.flex.items-start.pb2.pt4
       (assoc (apply utils/route-to target) :data-test id)
       [:div.flex.justify-center.items-center.col-3.ml4
        (card/stylist-card-thumbnail-molecule data)]
       [:div.col-9.medium.px3
        (titles/proxima-small-left (with :crown data))
        (titles/proxima-left (with :stylist-card.title data))
        [:div.flex.items-center
         (molecules/stars-rating-molecule data)
         (card/stylist-ratings-molecule data)
         (card/stylist-just-added-molecule data)]
        (card/stylist-card-salon-name-molecule data)
        (card/stylist-card-address-marker-molecule data)
        (card/stylist-card-experience-molecule data)]])))

(defn top-stylist-information-points-molecule [data]
  [:div "TODO put the four info points here"])

(defcomponent organism
  [data _ {:keys [id]}]
  [:div.flex.flex-column.left-align.border.border-cool-gray.mx3.mt1.mb3.bg-white
   {:id        id
    :data-test id}
   (top-stylist-card-header-molecule data)
   [:div.col-12
    (if (:screen/seen? data)
      (card/stylist-card-gallery-molecule data)
      (ui/aspect-ratio 426 105 [:div]))]
   (top-stylist-information-points-molecule data)
   [:div.col-12.py3.px2 (top-stylist-card-cta-molecule data)]])
