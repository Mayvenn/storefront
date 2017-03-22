(ns storefront.components.stylist.gallery-image-picker
  (:require [storefront.component :as component]
            [storefront.components.image-picker :as image-picker]
            [storefront.events :as events]
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
   :back-link          {:navigation-event events/navigate-gallery
                        :back-copy "back to gallery"}})

(defn built-component [data opts]
  (component/build component (query data) nil))
