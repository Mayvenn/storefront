(ns design-system.classic
  (:require [storefront.component :as component]
            [design-system.organisms :as organisms]
            [storefront.events :as events]))

(def organisms
  [#_
   {:organism/label     :call-out-center
    :organism/component call-out-center/organism
    :organism/query     {:call-out-center/bg-class    "bg-lavender"
                         :call-out-center/bg-ucare-id "6a221a42-9a1f-4443-8ecc-595af233ab42"
                         :call-out-center/title       "Call Out Centered Title"
                         :call-out-center/subtitle    "Subtitle"
                         :cta/id                      "call-out-center"
                         :cta/target                  events/navigate-design-system-adventure
                         :cta/label                   "Call To Action"
                         :react/key                   :call-out-center}}])

(defn component
  [data owner opts]
  (component/create
   [:div.py3
    [:div.h1 "Classic Template"]
    [:section
     [:div.h2 "Organisms"]
     [:section
      (organisms/demo organisms)]]]))

(defn built-component
  [data opts]
  (component/build component data nil))
