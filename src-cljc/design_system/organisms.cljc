(ns design-system.organisms
  (:require [storefront.component :as component]
            [clojure.pprint :as pprint]))

(defn demo-component
  [{:organism/keys [label component query]} _ _]
  (component/create
   [:div.py3
    [:div.h3.px6.py2 (str label)]
    [:div (component/build component query nil)]
    [:div.p6
     [:div "query"]
     [:pre.h8 (with-out-str (pprint/pprint query))]]]))

(defn demo
  [organisms]
  (for [{:organism/keys [label] :as organism} organisms]
    (component/build demo-component organism {:react-key (str label)})))
