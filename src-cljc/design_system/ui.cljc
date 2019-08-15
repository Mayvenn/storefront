(ns design-system.ui
  (:require [storefront.component :as component]
            [ui.promo-banner :as promo-banner]
            [design-system.organisms :as organisms]
            [storefront.events :as events]))

;; - Atom is ui at the level of the browser
;; - Molecule is an element that has a meaning... a contract
;; - Organism is an element composed of molecules and atoms that is a merged contract

(def nowhere events/navigate-design-system-ui)

(def organisms
  [{:organism/label     :none
    :organism/component promo-banner/component
    :organism/query     {:promo/type :none}}
   {:organism/label     :adventure-freeinstall/applied
    :organism/component promo-banner/component
    :organism/query     {:promo/type :adventure-freeinstall/applied}}
   {:organism/label     :v2-freeinstall/eligible
    :organism/component promo-banner/component
    :organism/query     {:promo/type :v2-freeinstall/eligible}}
   {:organism/label     :v2-freeinstall/applied
    :organism/component promo-banner/component
    :organism/query     {:promo/type :v2-freeinstall/applied}}
   {:organism/label     :shop/freeinstall
    :organism/component promo-banner/component
    :organism/query     {:promo/type :shop/freeinstall}}
   {:organism/label     :basic-promo-banner
    :organism/component promo-banner/component
    :organism/query     {:promo/type :basic-promo-banner
                         :basic-promo-banner/promo "description"}}
   {:organism/label     :basic-promo-banner
    :organism/component promo-banner/component
    :organism/query     {:promo/type :basic-promo-banner
                         :basic-promo-banner/promo "description"}}])

(defn component
  [data owner opts]
  (component/create
   [:div.py3
    [:div.h1 "Common UI"]
    [:section
     [:div.h2 "Organisms"]
     [:section
      (organisms/demo organisms)]]]))

(defn built-component
  [data opts]
  (component/build component data nil))
