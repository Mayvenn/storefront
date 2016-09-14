(ns storefront.platform.ugc
  (:require [sablono.core :refer-macros [html]]
            [om.core :as om]
            [storefront.accessors.named-searches :as named-searches]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.pixlee :as pixlee]
            [storefront.events :as events]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.keypaths :as keypaths]))

(defn inner-component [{:keys [pixlee-sku container-id]} owner opts]
  (reify
    om/IDidMount
    (did-mount [this]
      (handle-message events/ugc-component-mounted {:pixlee-sku   pixlee-sku
                                                    :container-id container-id}))

    om/IWillUnmount
    (will-unmount [this]
      (handle-message events/ugc-component-unmounted))

    om/IRender
    (render [this]
      (html
       [:div.center.mt4
        [:div.h1.medium.black.crush.line-height-1
         {:style {:margin-top "50px"}} ;; To match the white space at the top of the widget
         "#MayvennMade"]
        [:div {:id container-id}]
        [:div.center.dark-gray.px1
         {:style {:margin-top "30px"}} ;; To match the white space at the bottom of the widget
         "Want to show up on our homepage? Tag your best pictures wearing Mayvenn with " [:span.bold "#MayvennMade"]]]))))

(defn component [{:keys [pixlee-sku pixlee-loaded? content-available?] :as data} owner opts]
  (om/component
   (html
    (when (and pixlee-loaded? content-available?)
      [:div {:key pixlee-sku}
       (om/build inner-component data opts)]))))

(defn query [data]
  (let [named-search (named-searches/current-named-search data)]
    {:pixlee-loaded?     (get-in data keypaths/loaded-pixlee)
     :content-available? (pixlee/content-available? named-search)
     :pixlee-sku         (pixlee/sku named-search)}))
