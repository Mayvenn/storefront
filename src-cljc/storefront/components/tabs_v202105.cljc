(ns storefront.components.tabs-v202105
  (:require [storefront.platform.component-utils :as utils]))

(defn component [{:stylist-gallery-tabs/keys [tabs selected-tab]}]
  [:div.flex.flex-wrap.content-2
   (for [{:keys [id title navigate not-selected-class]} tabs]
     [:a.col-6.black.center.p2
      (merge (utils/route-to navigate)
             {:key       (str "gallery-tabs-" id)
              :data-test (str "nav-" (name id))
              :class     (if (= id selected-tab)
                           "bg-white border-top border-p-color border-width-4"
                           (str "bg-cool-gray border-width-1 " not-selected-class))})
      title])])
