(ns storefront.components.tabs-v202105
  (:require [storefront.platform.component-utils :as utils]
            [storefront.component :as c]
            [storefront.events :as e]
            [storefront.keypaths :as k]
            [storefront.transitions :as t]))

(c/defcomponent component
  "A set of tabs."
  [{:keys [tabs selected-tab]} _ _]
  [:div.flex.flex-wrap.content-2
   (for [{:keys [id title message not-selected-class]} tabs] 
     [:a.black.center.p2

      (merge       (apply (if (= (ffirst message) :navigate) 
                      utils/route-to
                      utils/fake-href) message)
             {:key       (str "tabs-" id)
              :data-test (str "nav-" (name id))
              :style     {:width (str (/ 100 (count tabs)) "%")}
              :class     (if (= id selected-tab)
                           "bg-white border-top border-p-color border-width-4"
                           (str "bg-cool-gray border-width-1 " not-selected-class))})
      title])])

(defmethod t/transition-state e/control-tab-selected
  [_ _ {:keys [tabs-id tab-id]} app-state]
  (assoc-in app-state (conj k/tabs tabs-id) tab-id))
