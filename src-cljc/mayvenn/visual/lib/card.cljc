(ns mayvenn.visual.lib.card
  "
  NOTE(corey)
  This namespaces has a few problems:
  - Not obvious whether all cards have the same data contract
  - Naming is based on usages right now, should be named
    by visual style or the kind of ui affordances made.
  "
  (:require [mayvenn.visual.ui.actions :as actions]
            [mayvenn.visual.ui.titles :as titles]
            [mayvenn.visual.ui.thumbnails :as thumbnails]
            ui.molecules
            [mayvenn.visual.tools :refer [with]]
            [storefront.component :as c]
            [storefront.components.ui :as ui]))

(c/defcomponent look-suggestion-1
  [{:as   data
    :keys [id index-label ucare-id primary secondary tertiary tertiary-note]} _ _]
  [:div.left-align.mx1.my1
   [:div.shout.proxima.title-3.mb1 index-label]
   [:div.bg-white
    [:div.flex.p3
     [:div.mr4
      (ui/img {:src ucare-id :width "80px"})]
     [:div.flex.flex-column
      {:style {:mind-width "175px"}}
      [:div primary]
      [:div secondary]
      [:div.content-1 tertiary]
      [:div.s-color.content-2 tertiary-note]
      [:div
       {:style {:width "175px"}}
       (actions/small-primary (with :action data))]]]]])

;; TODO(corey) this contract is different, prob should be a new ns
(c/defcomponent cart-item-1
  [data _ _]
  [:div.p2.flex.bg-white.items-center
   [:div.relative.self-start
    {:style {:min-width "70px"}}
    (thumbnails/stickered-square (with :thumbnail data))]

   [:div.flex-grow-1
    [:div.flex.items-center
     [:div.flex-grow-1
      (titles/proxima-content (with :title data))
      [:div
       (ui.molecules/stars-rating-molecule (with :stylist data))]]

     ;; price group
     [:div.flex.flex-column.self-stretch.items-end
      {:style {:min-width "67px"}}
      (titles/promixa-tiny-right (with :price-title data))]]]])
