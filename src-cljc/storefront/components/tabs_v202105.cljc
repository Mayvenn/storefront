(ns storefront.components.tabs-v202105
  (:require [storefront.platform.component-utils :as utils]
            [storefront.component :as c]))

(c/defcomponent component
  "A set of tabs."
  [{:keys [tabs selected-tab]} _ _]
  [:div.flex.flex-wrap.content-2
   (for [{:keys [id title message not-selected-class]} tabs]
     [:a.col-6.black.center.p2
      (merge (apply (if (= (ffirst message) :navigate) 
                      utils/route-to
                      utils/fake-href) message)
             {:key       (str "tabs-" id)
              :data-test (str "nav-" (name id))
              :class     (if (= id selected-tab)
                           "bg-white border-top border-p-color border-width-4"
                           (str "bg-cool-gray border-width-1 " not-selected-class))})
      title])])