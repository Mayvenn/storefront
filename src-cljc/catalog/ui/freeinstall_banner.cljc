(ns catalog.ui.freeinstall-banner
  (:require #?@(:cljs [[storefront.hooks.quadpay :as quadpay]])
            [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

;; MOLECULES

;; ORGANISM
(defn organism
  [{:freeinstall-banner/keys [show? title subtitle image-ucare-id button-id button-copy nav-event]} _ _]
  (component/create
   (when show?
     [:a.block.sticky.m3.black.pointer
      (merge {:data-test button-id
              :style {:height "245px"}}
             (apply utils/route-to nav-event))
      [:div.flex.bg-too-light-lavender.rounded.absolute.bottom-0.left-0.right-0
       {:style {:height "200px"}}
       [:div.p3
        [:div.bold.col-6 title]
        [:div.h7.dark-gray.col-6 subtitle]
        [:div.h7.bold.my1.col-4
         (ui/purple-button {:height-class :small} button-copy)]]]
      [:div.z5.absolute.bottom-0
       {:style {:right "-15px"}}
       (ui/ucare-img {:width "192"} image-ucare-id)]])))
