(ns storefront.components.stylist.gallery-image-picker
  (:require [storefront.component :as component]
            [storefront.components.image-picker :as image-picker]
            [storefront.platform.component-utils :as utils]
            [storefront.events :as events]
            [storefront.assets :as assets]
            [storefront.keypaths :as keypaths]))

(defn component [{:keys [loaded-uploadcare?] :as args} owner opts]
  (component/create
   [:div
    (when loaded-uploadcare?
      (component/build image-picker/component args opts))]))

(defn query [data]
  {:loaded-uploadcare? (get-in data keypaths/loaded-uploadcare)
   :resizable-url      nil
   :on-success         events/uploadcare-api-success-upload-gallery
   :widget-config      {:multiple true}
   :selector           "gallery-photo"
   :back-link          [:a.dark-gray.block.mb2
                        (utils/route-to events/navigate-gallery)
                        [:img.px1.mbnp4 {:style {:height "1.25rem"}
                                         :src   (assets/path "/images/icons/carat-left.png")}]
                        "back to gallery"]})

(defn built-component [data opts]
  (component/build component (query data) nil))
