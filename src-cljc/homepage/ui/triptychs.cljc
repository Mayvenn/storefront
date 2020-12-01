(ns homepage.ui.triptychs
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(defn ^:private square-deferred-ucare-img
  [ucare-id]
  (ui/aspect-ratio 1 1 (ui/defer-ucare-img
                         {:class "col-12"}
                         ucare-id)))

(defcomponent organism
  [{:keys [title
           subtitle
           target
           data]} _ _]
  [:div
   [:div.bg-warm-gray.ptj2-on-mb.pxj1-on-mb.pbj1-on-mb.ptj3-on-tb-dt.pxj1-on-tb-dt.pbj2-on-tb-dt
    [:div.title-1.canela.center.mb3 title]
    [:div.title-2.proxima.shout.center subtitle]]
   [:a.block
    (apply utils/route-to target)
    (->> data
         (mapv (fn triptych [{:keys [large-pic-right-on-mobile?
                                     image-ids
                                     id]}]
                 (let [[primary-id secondary-id tertiary-id] image-ids]
                   [:div.col-6-on-tb-dt.flex
                    (merge
                     {:key id}
                     (when large-pic-right-on-mobile?
                       {:class "flex-row-reverse-on-mb"}))
                    [:div.col-8 (square-deferred-ucare-img primary-id)]
                    [:div.flex.flex-column.col-4
                     (square-deferred-ucare-img secondary-id)
                     (square-deferred-ucare-img tertiary-id)]])))

         (into [:div.flex-on-tb-dt]))
    [:div.bg-warm-gray.relative.hide-on-tb-dt
     (ui/button-medium-secondary (assoc
                                  (apply utils/route-to target)
                                  :data-test "cta-shop-by-look"
                                  :class "absolute left-0 right-0 mx-auto"
                                  :style {:top "-70px"
                                          :width "220px"})
                                 "Shop by look")]
    [:div.bg-warm-gray.pyj2-on-tb-dt.relative.hide-on-mb
     (ui/button-medium-secondary (assoc
                                  (apply utils/route-to target)
                                  :data-test "cta-shop-by-look-desktop"
                                  :class "mx-auto pyd2"
                                  :style {:width "220px"})
                                 "Shop by look")]]])
