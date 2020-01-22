(ns catalog.icp
  (:require
   catalog.categories
   [storefront.assets :as assets]
   [storefront.component :as component :refer [defcomponent]]
   [storefront.components.ui :as ui]
   [storefront.events :as events]
   [storefront.platform.component-utils :as utils]
   [catalog.categories :as categories]
   [spice.core :as spice]
   [storefront.keypaths :as keypaths]))

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

(defcomponent ^:private drill-category-organism
  [{:drill-category/keys [id title description image-url target action-id action-label]} _ _]
  (when id
    [:div.py3.flex
     {:key       id
      :data-test id}
     [:div.mt1.mr3
      [:img {:src   (assets/path image-url)
             :width 62}]]
     [:div
      [:div.title-2.proxima.shout title]
      [:div.content-2.proxima.py1 description]
      (when action-id
        (ui/button-small-underline-primary
         (assoc (apply utils/route-to target)
                :data-test  action-id)
         action-label))]]))

(defcomponent ^:private drill-category-list-organism
  [{:drill-category-list/keys [values]} _ _]
  (when (seq values)
    [:div.py8.px4
     (mapv #(component/build drill-category-organism %
                             {:key (:drill-category/id %)})
           values)]))
(defn ^:private drill-category-list-query
  [_ categories]
  (let [ready-wear-wigs        (categories/id->category 25 categories)
        virgin-lace-front-wigs (categories/id->category 24 categories)
        virgin-360-wigs        (categories/id->category 26 categories)]
    {:drill-category-list/values
     (mapv
      (fn drill-category-query [category]
        {:drill-category/id           (:page/slug category)
         :drill-category/title        (:copy/title category)
         :drill-category/description  (:copy/description category)
         :drill-category/image-url    (:subcategory/image-uri category)
         :drill-category/target       [events/navigate-category category]
         :drill-category/action-id    (str "drill-category-action-" (:page/slug category))
         :drill-category/action-label (str "Shop " (:copy/title category))})
      [ready-wear-wigs virgin-lace-front-wigs virgin-360-wigs])}))

(def ^:private divider
  [:div
   {:style {:background-image    "url('//ucarecdn.com/73db5b08-860e-4e6c-b052-31ed6d951f00/-/resize/x24/')"
            :background-position "center center"
            :background-repeat   "repeat-x"
            :height              "24px"}}])

(defcomponent ^:private template
  "This lays out different ux pieces to form a cohesive ux experience"
  [{:keys [header footer category-hero drill-category-list]} _ _]
  [:div
   (component/build header-organism header)
   (component/build category-hero-organism category-hero)
   (component/build drill-category-list-organism drill-category-list)
   divider
   ;;    [:div "Categories list from selectors"]
   ;;    [:div "Products paginated list from selectors"]
   ;;    [:div "Educational content"]
   ;;    [:div "Recent blog posts"]
   ;;    [:div "Contact"]
   (component/build footer-organism footer)])

(defn ^:export page
  [app-state opts]
  (let [category   (catalog.categories/current-category app-state)
        categories (get-in app-state keypaths/categories)]
    (component/build template
                     {:header              {}
                      :footer              {}
                      :category-hero       (category-hero-query category)
                      :drill-category-list (drill-category-list-query category categories)}
                     opts)))
