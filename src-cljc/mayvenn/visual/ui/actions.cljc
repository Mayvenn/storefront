(ns mayvenn.visual.ui.actions
  (:require [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(defn action-molecule
  [{:keys [id label target]}]
  (c/html
   [:div.mt4.col-10.col-8-on-tb
    (ui/button-medium-primary
     (merge {:data-test id}
            (apply utils/route-to target))
     label)]))
