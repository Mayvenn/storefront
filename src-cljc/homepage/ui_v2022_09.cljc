(ns homepage.ui-v2022-09
  (:require adventure.keypaths
            [storefront.accessors.experiments :as experiments]
            [storefront.keypaths :as k]
            [adventure.components.layered :as layered]
            [homepage.ui.atoms :as A]
            [homepage.ui.contact-us :as contact-us]
            [homepage.ui.diishan :as diishan]
            [homepage.ui.faq :as faq]
            [homepage.ui.guarantees :as guarantees]
            [homepage.ui.hashtag-mayvenn-hair :as hashtag-mayvenn-hair]
            [homepage.ui.hero :as hero]
            [homepage.ui.promises :as promises]
            [homepage.ui.shopping-categories :as shopping-categories]
            [homepage.ui.zip-explanation :as zip-explanation]
            [storefront.accessors.contentful :as contentful]
            [storefront.component :as c]
            [storefront.components.homepage-hero :as homepage-hero]
            [storefront.components.ui :as ui]
            [storefront.events :as e]))

(c/defcomponent template
  [{:keys [hero]} _ _]
  [:div
   (c/build hero/organism-without-shipping-bar hero)
   (c/build promises/organism {})])

(defn hero-query
  "TODO homepage hero query is reused and complected

  decomplect:
  - handles extraction from cms
  - schematizes according to reused component"
  [cms experience]
  (let [hero-content
        (or
         (some-> cms :homepage experience :hero)
         ;; TODO handle cms failure fallback
         {})]
    (assoc-in (homepage-hero/query hero-content)
              [:opts :data-test]
              "hero-link")))
