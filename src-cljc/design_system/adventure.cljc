(ns design-system.adventure
  (:require [storefront.component :as component :refer [defcomponent]]
            [design-system.organisms :as organisms]
            [storefront.events :as events]))

;; - Atom is ui at the level of the browser
;; - Molecule is an element that has a meaning... a contract
;; - Organism is an element composed of molecules and atoms that is a merged contract

(def nowhere events/navigate-design-system-adventure)

(def organisms
  [])

(defcomponent component
  [data owner opts]
  [:div.py3
   [:div.h1 "Adventure Template"]
   [:section
    [:div.h2 "Organisms"]
    [:section
     (organisms/demo organisms)]]])

(defn built-component
  [data opts]
  (component/build component data nil))
