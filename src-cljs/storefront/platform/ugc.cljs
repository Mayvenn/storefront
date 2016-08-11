(ns storefront.platform.ugc
  (:require [sablono.core :refer-macros [html]]
            [om.core :as om]
            [storefront.accessors.taxons :as taxons]
            [storefront.accessors.experiments :as experiments]
            [storefront.events :as events]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.keypaths :as keypaths]))

(defn inner-component [{:keys [taxon container-id]} owner opts]
  (reify
    om/IDidMount
    (did-mount [this]
      (handle-message events/ugc-component-mounted {:taxon-slug (:slug taxon)
                                                    :container-id container-id}))
    om/IRender
    (render [this]
      (html
       [:div.center.mt4
        [:div.h1.medium.black.crush.line-height-1
         {:style {:margin-top "50px"}} ;; To match the white space at the top of the widget
         "#MayvennMade"]
        [:div {:id container-id}]]))))

(defn component [{:keys [taxon enabled? loaded?] :as data} owner opts]
  (om/component
   (html
    (when (and enabled? loaded?)
      [:div {:key (:slug taxon)}
       (om/build inner-component data opts)]))))

(defn query [data]
  {:enabled? (experiments/pixlee-product? data)
   :loaded?  (get-in data keypaths/loaded-pixlee)
   :taxon    (taxons/current-taxon data)})
