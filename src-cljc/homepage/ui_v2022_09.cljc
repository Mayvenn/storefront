(ns homepage.ui-v2022-09
  (:require adventure.keypaths
            [homepage.ui.promises :as promises]
            [homepage.ui.blog :as blog]
            [homepage.ui.hero :as hero]
            [homepage.ui.email-capture :as email-capture]
            [homepage.ui.shopping-categories :as shopping-categories]
            [catalog.ui.shop-these-looks :as shop-these-looks]
            [homepage.ui.zip-explanation :as zip-explanation]
            [mayvenn.visual.tools :as vt]
            [mayvenn.visual.ui.titles :as titles]
            [storefront.component :as c]
            [storefront.components.homepage-hero :as homepage-hero]
            [storefront.events :as e]
            [adventure.components.layered :as layered]))

(c/defcomponent template
  [{:keys [hero shopping-categories blog1 blog2 zip-explanation] :as data} _ _]
  [:div
   (c/build hero/organism-without-shipping-bar hero)
   (c/build promises/organism (vt/with :promises data))
   (when (seq (vt/with :not-your-average-hair-store data))
     ;; TODO: this div is an expediency, these designs belong in a component that
     ;;       contains its own layout, but this is faster right now. Come back and
     ;;       build a component that centers and has a max-width, etc.
     [:div.max-960.mx-auto.center.p4-on-mb.p8-on-tb-dt
      (titles/canela-huge-with-large-secondary
       (vt/with :not-your-average-hair-store data))])
   (c/build shopping-categories/organism shopping-categories)
   (c/build blog/organism blog1)
   (c/build shop-these-looks/organism data)
   (c/build blog/organism blog2)
   [:div.mt8
    (c/build zip-explanation/organism zip-explanation)
    (when (:email-capture/show? data)
      (c/build email-capture/organism (vt/with :email-capture data)))]])

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
            [{:keys  [page/slug copy/title catalog/category-id]
              ::keys [image-id]}]
            (merge {:shopping-categories.box/id          slug
                    :shopping-categories.box/target      [e/navigate-category
                                                          {:page/slug           slug
                                                           :catalog/category-id category-id}]
                    :shopping-categories.box/ucare-id    image-id
                    :shopping-categories.box/label       title}))))
    {:shopping-categories.box/id          "need-inspiration"
     :shopping-categories.box/target      [e/navigate-shop-by-look {:album-keyword :look}]
     :shopping-categories.box/ucare-id    "dd5a716b-c89a-47d5-8b57-adc4dd160add"
     :shopping-categories.box/alt-label   ["Need Inspiration?" "Try shop by look."]})})
