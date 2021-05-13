(ns storefront.components.tabs-v202105
  (:require [storefront.component :as component :refer [defcomponent defdynamic-component]]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]))

(def tabs
  [{:id                 :past-appointments
    :title              "past appointments"
    :navigate           events/navigate-gallery-appointments
    :not-selected-class "border-right"}
   {:id                 :my-gallery
    :title              "my gallery"
    :navigate           events/navigate-gallery-edit
    :not-selected-class "border-left"}])

(defn component [active-tab-name]
  [:div.flex.flex-wrap.content-2
   (for [{:keys [id title navigate not-selected-class] :as stuff} tabs]
     [:a.col-6.black.center.p2
      (merge (utils/route-to navigate)
             {:key       (str "gallery-tabs-" id)
              :data-test (str "nav-" (name id))
              :class     (if (= navigate active-tab-name)
                           "bg-white border-top border-p-color border-width-4"
                           (str "bg-cool-gray border-width-1 " not-selected-class))})
      title])])
