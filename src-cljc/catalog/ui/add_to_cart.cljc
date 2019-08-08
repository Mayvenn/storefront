(ns catalog.ui.add-to-cart
  (:require [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.platform.component-utils :as utils]))

;; MOLECULES
(defn cta-molecule
  [{:cta/keys [id label target spinning? disabled?]}]
  (when (and id label target)
    (ui/teal-button
     (merge {:data-test id
             :spinning? (boolean spinning?)
             :disabled? (boolean disabled?)}
            (apply utils/fake-href target))
     [:div.flex.items-center.justify-center.inherit-color label]) ))

(defn freeinstall-add-to-cart-block
  [{:freeinstall-add-to-cart-block/keys [message link-label link-target footnote icon]}]
  [:div.flex
   [:div.col-2.flex.justify-center.items-center
    (ui/ucare-img {:width "20"} icon)]
   [:div.flex.flex-column.justify-end
    [:div.h7 message
     [:a.underline.navy
     (apply utils/send-event-callback link-target)
      link-label]]
    [:div.dark-gray.h8 footnote]]])

;; ORGANISM
(defn organism
  "Add to Cart organism"
  [data _ _]
  (component/create
   [:div.bg-light-silver
    (freeinstall-add-to-cart-block data)
    (cta-molecule data)]))
