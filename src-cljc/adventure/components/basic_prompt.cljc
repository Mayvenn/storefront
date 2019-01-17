(ns adventure.components.basic-prompt
  (:require [adventure.components.header :as header]
            [spice.maps :as maps]
            [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(def rect-button-attrs
  {:height-class "py6"
   :style {:border-radius "3px"}})

(defn teal-rect-button [attrs & content]
  (ui/teal-button (maps/deep-merge rect-button-attrs attrs) content))

(defn white-rect-button [attrs & content]
  (ui/white-button (maps/deep-merge rect-button-attrs attrs) content))

(defn component
  [{:keys [background-overrides
           show-logo?
           prompt
           mini-prompt
           header-data
           background-image
           background-position
           button]} _ _]
  (let [button-component (if (= :teal (:color button))
                           teal-rect-button
                           white-rect-button)]
    (component/create
     [:div.bg-lavender.white.center.flex.flex-auto.flex-column
      (maps/deep-merge
       {:style {:background-image    (str "url(" background-image ")")
                :background-position "left 1em bottom"
                :background-repeat   "no-repeat"
                :background-size     "contain"}}
       background-overrides)
      (when header-data
        (header/built-component header-data nil))
      [:div.mx-auto.col-8.flex.flex-column.items-center
       (merge
        {:style {:height "246px"}}
        (when-not show-logo?
          {:class "justify-end"}))
       (when show-logo?
         [:div.flex.items-center.justify-center.center.mt4.mb6.pb3
          [:div.mr4
           (ui/ucare-img {:width "140"} "1970d88b-3798-4914-8a91-74288b09cc77")]])
       [:div
        [:div.h3.medium prompt]
        [:div.mt1.h5 mini-prompt]]]
      [:div.flex.flex-auto.items-end.pb2
       [:div.mx1.flex.flex-auto.px5
        (button-component (merge {:data-test (:data-test button)}
                                 (utils/route-to (:target button)))
                          (:text button))]]])))

;; Pages that use this component: Welcome/Home, find your stylist, shop-hair, match-stylist
