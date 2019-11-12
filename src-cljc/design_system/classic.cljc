(ns design-system.classic
  (:require [catalog.product-details :as product-details]
            [design-system.organisms :as organisms]
            [design-system.molecules :as molecules]
            [catalog.ui.molecules :as ui-molecules]
            [catalog.ui.add-to-cart :as add-to-cart]
            [catalog.ui.freeinstall-banner :as freeinstall-banner]
            [popup.organisms :as popup]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.effects :as effects]
            [storefront.events :as events]
            #?(:cljs [storefront.hooks.reviews :as reviews])
            [storefront.platform.component-utils :as utils]
            [ui.molecules :as ui.M]))

(def nowhere events/navigate-design-system-adventure)

(def organisms
  [])

(def molecules
  [])

(defcomponent component
  [data owner opts]
  [:div.py3
   [:div.h1 "Classic Template"]
   [:section
    [:div.h2 "Organisms"]
    [:section.p4
     (organisms/demo organisms (:organisms data))]]
   [:section
    [:div.h2 "Molecules"]
    [:section.p4
     (molecules/demo molecules)]]])

(defn built-component
  [{:keys [design-system]} opts]
  (component/build component design-system nil))

(defmethod effects/perform-effects events/navigate-design-system-classic
  [_ _ _ _ _]
  #?(:cljs (do
             (reviews/insert-reviews)
             ;; hack to unhack the fact that reviews expect two instances of reviews
             (js/setTimeout #(reviews/start) 2000))))

