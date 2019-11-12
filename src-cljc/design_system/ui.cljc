(ns design-system.ui
  (:require [storefront.component :as component :refer [defcomponent]]
            [ui.promo-banner :as promo-banner]
            [catalog.ui.product-card :as product-card]
            [ui.product-list-header :as product-list-header]
            [design-system.organisms :as organisms]
            [storefront.events :as events]))

;; - Atom is ui at the level of the browser
;; - Molecule is an element that has a meaning... a contract
;; - Organism is an element composed of molecules and atoms that is a merged contract

(def nowhere events/navigate-design-system-ui)

(def organisms
  [])

(defcomponent component
  [data owner opts]
  [:div.py3
   [:div.h1 "Common UI"]
   [:section
    [:div.h2 "Organisms"]
    [:section
     (organisms/demo organisms)]]])

(defn built-component
  [data opts]
  (component/build component data nil))
