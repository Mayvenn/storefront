(ns design-system.molecules
  (:require [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [clojure.pprint :as pprint]))

(defn demo-component
  [{:molecule/keys [label component query]} _ _]
  (component/create
   [:div.py3.border.border-black.bg-light-gray
    [:div.h3.px6.py2.bold (str label)]
    [:div.border.border-black
     (component/build component query nil)]
    [:div.p6
     [:div "query"]
     [:pre.h8 (with-out-str (pprint/pprint query))]]]))

(defn demo
  [molecules]
  (for [{:molecule/keys [label] :as molecule} molecules]
    [:div
     {:key (str "molecule-" label)}
     (component/build demo-component molecule nil)]))
