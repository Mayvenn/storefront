(ns catalog.icp
  (:require
   catalog.categories
   [storefront.component :as component :refer [defcomponent]]))

(defcomponent ^:private header-organism
  [_ _ _]
  [:div])

(defcomponent ^:private footer-organism
  [_ _ _]
  [:div])

(defn category-hero-title
  [{:category-hero.title/keys [id value]}]
  (when id
    [:div.title-1.canela.pt5 value]))

(defn category-hero-description
  "A ui element"
  [{:category-hero.description/keys [id value]}]
  (when id
    [:div.content-2.proxima.py5 value]))

(defcomponent ^:private category-hero-organism
  "This uses ui elements to build a piece of UX"
  [data _ _]
  [:div.bg-warm-gray.center.py5.px6
   (category-hero-title data)
   (category-hero-description data)])

(defcomponent ^:private template
  "This lays out different ux pieces to form a cohesive ux experience"
  [{:keys [header footer category-hero]} _ _]
  [:div
   (component/build header-organism header)
   (component/build category-hero-organism category-hero)
   ;;    [:div "Categories list from selectors"]
   ;;    [:div "Products paginated list from selectors"]
   ;;    [:div "Educational content"]
   ;;    [:div "Recent blog posts"]
   ;;    [:div "Contact"]
   (component/build footer-organism footer)])

(defn ^:private category-hero-query
  [category]
  {:category-hero.title/id       "category-hero-title"
   :category-hero.title/value    "Human Hair Wigs"
   :category-hero.description/id "category-hero-description"
   :category-hero.description/value
   (str
    "Want a fun, protective style that switches up your look, color or hair length instantly? "
    "Human hair wigs are the perfect choice. "
    "Get free customization with qualifying purchases.")})

(defn ^:export page
  [app-state opts]
  (let [category (catalog.categories/current-category app-state)]
    (component/build template
                     {:header {}
                      :footer {}
                      :category-hero (category-hero-query category)}
                     opts)))
