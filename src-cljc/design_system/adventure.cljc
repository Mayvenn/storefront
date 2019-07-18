(ns design-system.adventure
  (:require [adventure.organisms.call-out-center :as call-out-center]
            [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [clojure.string :as str]
            [clojure.pprint :as pprint]))

(defn- pp
  [form]
  (interpose [:br] (str/split-lines (with-out-str (pprint/pprint form)))))

(defn component
  [data owner opts]
  (component/create
   [:div.py3
    [:div.h1 "Adventure Template"]
    [:section
     [:div.h2 "Organisms"]
     [:section
      [:div.py3
       [:div.h3.px6.py2 "call-out-center"]
       [:div (component/build call-out-center/organism
                         {:call-out-center/bg-class    "bg-lavender"
                          :call-out-center/bg-ucare-id "6a221a42-9a1f-4443-8ecc-595af233ab42"
                          :call-out-center/title       "Call Out Centered Title"
                          :call-out-center/subtitle    "Subtitle"
                          :cta/id                      "call-out-center"
                          :cta/target                  events/navigate-design-system-adventure
                          :cta/label                   "Call To Action"
                          :react/key                   :call-out-center}
                         nil)]
       [:div.p6
        [:div "query"]
        [:code.h8.nowrap
         (pp {:call-out-center/bg-class    "bg-lavender"
              :call-out-center/bg-ucare-id "6a221a42-9a1f-4443-8ecc-595af233ab42"
              :call-out-center/title       "Call Out Centered Title"
              :call-out-center/subtitle    "Subtitle"
              :cta/id                      "call-out-center"
              :cta/target                  events/navigate-design-system-adventure
              :cta/label                   "Call To Action"
              :react/key                   :call-out-center})]]]]]]))

(defn built-component
  [data opts]
  (component/build component data nil))

