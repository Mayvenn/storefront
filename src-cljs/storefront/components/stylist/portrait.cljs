(ns storefront.components.stylist.portrait
  (:require [storefront.component :as component]
            [sablono.core :refer-macros [html]]
            [om.core :as om]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [storefront.hooks.uploadcare :as uploadcare]
            [storefront.assets :as assets]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.messages :refer [handle-message]]))

(defn inner-component [{:keys [portrait]} owner opts]
  (reify
    om/IDidMount
    (did-mount [_] (handle-message events/portrait-component-mounted {:selector "#stylist-portrait" :portrait portrait}))
    om/IWillUnmount
    (will-unmount [_] (handle-message events/portrait-component-will-unmount))
    om/IRender
    (render [_]
      (html
       [:div.container.sans-serif
        [:div.p2
         [:a.dark-gray.block.mb2
          (utils/route-to events/navigate-stylist-account-profile)
          [:img.px1.mbnp4 {:style {:height "1.25rem"}
                           :src   (assets/path "/images/icons/carat-left.png")}]
          "back to account"]
         [:h1.center "Select a source below"]]
        [:div#stylist-portrait ui/nbsp]]))))

(defn component [{:keys [loaded-uploadcare?] :as args} owner opts]
  (om/component
   (html
    [:div
     (when loaded-uploadcare?
       (om/build inner-component args opts))])))

(defn query [data]
  {:portrait           (get-in data (conj keypaths/stylist-manage-account :portrait))
   :loaded-uploadcare? (get-in data keypaths/loaded-uploadcare)})

(defn built-component [data opts]
  (component/build component (query data) opts))
