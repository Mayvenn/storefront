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
           data]} _ _]
  [:div
   [:div.bg-warm-gray.pt8.pb5
    [:div.title-1.canela.center.m3 title]
    [:div.title-2.proxima.shout.center.m3 subtitle]]
   (->> data
        (mapv (fn [{:keys [large-pic-right-on-mobile?
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
   ;; TODO fix this desktop version, and add a mobile version
   [:div.bg-warm-gray.hide-on-mb
    (ui/button-medium-secondary (assoc
                                 (apply utils/route-to [e/navigate-shop-by-look {:album-keyword :look}])
                                 :data-test "cta-shop-by-look")
                                "Shop by look")]])
