(ns storefront.components.stylist.portrait
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.assets :as assets]
            [storefront.component :as component]
            [storefront.components.image-picker :as image-picker]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]))

(defn component [{:keys [loaded-uploadcare?] :as args} owner opts]
  (om/component
   (html
    [:div
     (when loaded-uploadcare?
       (om/build image-picker/component args opts))])))

(defn query [data]
  {:loaded-uploadcare? (get-in data keypaths/loaded-uploadcare)
   :resizable-url      (get-in data (conj keypaths/stylist-manage-account :portrait :resizable_url))
   :selector           "stylist-portrait"
   :back-link          [:a.dark-gray.block.mb2
                        (utils/route-to events/navigate-stylist-account-profile)
                        [:img.px1.mbnp4 {:style {:height "1.25rem"}
                                         :src   (assets/path "/images/icons/carat-left.png")}]
                        "back to account"]})

(defn built-component [data opts]
  (component/build component (query data) opts))
