(ns design-system.ui
  (:require [storefront.component :as component]
            [ui.promo-banner :as promo-banner]
            [ui.product-card :as product-card]
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
    :organism/query     {:promo/type               :basic-promo-banner
                         :basic-promo-banner/promo "description"}}
   {:organism/label     :basic-promo-banner
    :organism/component promo-banner/component
    :organism/query     {:promo/type               :basic-promo-banner
                         :basic-promo-banner/promo "description"}}
   {:organism/label     :product-card
    :organism/component product-card/organism
    :organism/query     {:card-image/alt                  ""
                         :length-range/shortest           "14″"
                         :card-image/src                  "//ucarecdn.com/d4d0b8ac-c86d-401c-a2fc-28acc5edf689/-/format/auto/"
                         :product-card/data-test          "product-indian-loose-wave-lace-frontals"
                         :product-card/sold-out?          false
                         :color-swatches/urls             #{"//ucarecdn.com/f7eb2f95-3283-4160-bdf9-38a87be676c2/-/format/auto/dark_blonde.png"
                                                           "//ucarecdn.com/9e15a581-6e80-401a-8cb2-0608fef474e9/-/format/auto/dark_blonde_dark_roots.png"
                                                           "//ucarecdn.com/85ede6dd-8e84-4096-ad5c-685d50dd99ec/-/format/auto/blonde.png"
                                                            "//ucarecdn.com/02f4a86c-12fa-47b3-8f50-078568e4f905/-/format/auto/blonde_dark_roots.png"}
                         :length-range/longest            "18″"
                         :product-card/cheapest-sku-price "$159"
                         :product-card/title              "Dyed Virgin Indian Loose Wave Lace Frontal"
                         :product-card/navigation-message [[:navigate :product :details]
                                                           {:catalog/product-id "1"
                                                            :page/slug          "indian-loose-wave-lace-frontals"
                                                            :query-params       {:SKU "ILWDBFLC14"}}]}}])

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
