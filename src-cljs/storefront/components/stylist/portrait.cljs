(ns storefront.components.stylist.portrait
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.component :as component]
            [storefront.components.image-picker :as image-picker]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defn component [{:keys [loaded-uploadcare?] :as args} owner opts]
  (om/component
   (html
    [:div
     (when loaded-uploadcare?
       (om/build image-picker/component args opts))])))

(defn query [data]
  {:loaded-uploadcare? (get-in data keypaths/loaded-uploadcare)
   :resizable-url      (get-in data (conj keypaths/stylist-manage-account :portrait :resizable_url))
   :on-success         events/uploadcare-api-success-upload-portrait
   :selector           "stylist-portrait"
   :back-link          {:navigation-event events/navigate-stylist-account-profile
                        :back-copy        "back to account"}})

(defn built-component [data opts]
  (component/build component (query data) opts))
