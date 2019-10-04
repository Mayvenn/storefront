(ns adventure.components.profile-card-with-gallery
  (:require [adventure.components.profile-card :as profile-card]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.ui :as ui]
            [storefront.platform.carousel :as carousel]
            [storefront.platform.component-utils :as utils]
            
            ))

(defcomponent component
  [{:keys [card-data gallery-data button]} _ _]
  [:div.p3.bg-white.h6.my2.col-12.col-8-on-tb-dt
    (component/build profile-card/component card-data nil)
    [:div.my2.m1-on-tb-dt.mb2-on-tb-dt
     [:div.h7.dark-gray.bold.left-align.mb1 (:title gallery-data)]
     [:div.mxn1
      (component/build carousel/component
                       {:slides   (map (fn [{:keys [key target-message ucare-img-url]}]
                                         [:div.px1
                                          (merge
                                           (apply (if (->> target-message ffirst (= :navigate))
                                                    utils/route-to
                                                    utils/fake-href) target-message)
                                           {:key key})
                                          (ui/aspect-ratio
                                           1 1
                                           [:img {:src   (str ucare-img-url "-/scale_crop/216x216/-/format/auto/")
                                                  :class "rounded col-12"}])])
                                       (:items gallery-data))
                        :settings {:swipe        true
                                   :initialSlide 0
                                   :arrows       true
                                   :dots         false
                                   :slidesToShow 3
                                   :infinite     true}}
                       {})]]
    (ui/teal-button
     (merge {:data-test (:data-test button)}
            (apply utils/fake-href (:target-message button)))
     [:div.flex.items-center.justify-center.inherit-color
      (:text button)])])
