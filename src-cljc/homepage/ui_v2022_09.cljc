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
            [homepage.ui.blog :as blog]
            [homepage.ui.hero :as hero]
            [homepage.ui.promises :as promises]
            [homepage.ui.email-capture :as email-capture]
            [homepage.ui.shopping-categories :as shopping-categories]
            [catalog.ui.shop-these-looks :as shop-these-looks]
            [homepage.ui.zip-explanation :as zip-explanation]
            [storefront.accessors.contentful :as contentful]
            [storefront.component :as c]
            [storefront.components.homepage-hero :as homepage-hero]
            [storefront.components.ui :as ui]
            [storefront.events :as e]))

(c/defcomponent template
  [{:keys [hero shopping-categories blog1 blog2 zip-explanation] :as data} _ _]
  [:div
   (c/build hero/organism-without-shipping-bar hero)
   (c/build promises/organism {})
   (c/build shopping-categories/organism shopping-categories)
   (c/build blog/organism blog1)
   (c/build shop-these-looks/organism data)
   (c/build blog/organism blog2)
   (c/build zip-explanation/organism zip-explanation)
   (c/build email-capture/organism data)])

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
    (-> (homepage-hero/query hero-content)
        (assoc-in [:opts :data-test] "hero-link")
        (assoc-in [:opts :h1?] true))))

(defn shopping-categories-query
  [categories]
  {:list/boxes
   (conj
    (->> categories
         (filter :homepage.ui-v2022-09/order)
         (sort-by ::order)
         (mapv
          (fn category->box
            [{:keys [page/slug copy/title catalog/category-id]
              ::keys [image-id]}]
            {:shopping-categories.box/id       slug
             :shopping-categories.box/target   [e/navigate-category
                                                {:page/slug           slug
                                                 :catalog/category-id category-id}]
             :shopping-categories.box/ucare-id image-id
             :shopping-categories.box/label    title})))
    {:shopping-categories.box/id        "need-inspiration"
     :shopping-categories.box/target    [e/navigate-shop-by-look {:album-keyword :look}]
     :shopping-categories.box/alt-label ["Need Inspiration?" "Try shop by look."]})})
