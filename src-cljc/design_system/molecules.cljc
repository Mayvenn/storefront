(ns design-system.molecules
  (:require [storefront.component :as component :refer [defcomponent]]
            [clojure.pprint :as pprint]))

(defcomponent demo-component
  [{:molecule/keys [label component query]} _ _]
  [:div.py3.border.border-black.bg-cool-gray
   [:div.h3.px6.py2.bold (str label)]
   [:div.border.border-black
    (component/build component query nil)]
   [:div.p6
    [:div "query"]
    [:pre.h8 (with-out-str (pprint/pprint query))]]])

(defn demo
  [molecules]
  (for [{:molecule/keys [label] :as molecule} molecules]
    [:div
     {:key (str "molecule-" label)}
     (component/build demo-component molecule nil)]))
