(ns catalog.icp
  (:require
   catalog.categories
   [storefront.component :as component :refer [defcomponent]]
   [storefront.components.ui :as ui]
   [storefront.events :as events]
   [storefront.platform.component-utils :as utils]))

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
  [{:drill-category/keys [id title description image target action-id action-label]} _ _]
  (when id
    [:div.flex
     [:div.col-3
      [:div.bg-blue.mr3 image]]
     [:div.col-9
      [:div.title-2.proxima.shout title]
      [:div.content-2.proxima.py1 description]
      (when action-id
        (ui/button-small-underline-primary
         {:data-test action-id
          :on-click  (apply utils/send-event-callback target)}
         action-label))]]))
(defcomponent ^:private drill-category-list-organism
  [{:drill-category-list/keys [values]} _ _]
  (when (seq values)
    [:div.py8.px4
     (mapv #(component/build drill-category-organism %)
           values)]))
(defn ^:private drill-category-list-query
  [category]
  {:drill-category-list/values [{:drill-category/id           "ready-wear-wigs"
                                 :drill-category/title        "ready to wear wigs"
                                 :drill-category/description  (str "Made of authentic and high-quality human hair, "
                                                                   "ready to wear wigs are a quick, "
                                                                   "convenient way to change up your look instantly.
")
                                 :drill-category/image        "ready-wigs-img"
                                 :drill-category/target       [events/navigate-home]
                                 :drill-category/action-id    "drill-category-action-ready-to-wear-wigs"
                                 :drill-category/action-label "Shop Ready To Wear Wigs"}
                                {:drill-category/id           "ready-wear-wigs"
                                 :drill-category/title        "ready to wear wigs"
                                 :drill-category/description  (str "Made of authentic and high-quality human hair, "
                                                                   "ready to wear wigs are a quick, "
                                                                   "convenient way to change up your look instantly.
")
                                 :drill-category/image        "ready-wigs-img"
                                 :drill-category/target       [events/navigate-home]
                                 :drill-category/action-id    "drill-category-action-ready-to-wear-wigs"
                                 :drill-category/action-label "Shop Ready To Wear Wigs"}
                                {:drill-category/id           "ready-wear-wigs"
                                 :drill-category/title        "ready to wear wigs"
                                 :drill-category/description  (str "Made of authentic and high-quality human hair, "
                                                                   "ready to wear wigs are a quick, "
                                                                   "convenient way to change up your look instantly.
")
                                 :drill-category/image        "ready-wigs-img"
                                 :drill-category/target       [events/navigate-home]
                                 :drill-category/action-id    "drill-category-action-ready-to-wear-wigs"
                                 :drill-category/action-label "Shop Ready To Wear Wigs"}]})

(defcomponent ^:private template
  "This lays out different ux pieces to form a cohesive ux experience"
  [{:keys [header footer category-hero drill-category-list]} _ _]
  [:div
   (component/build header-organism header)
   (component/build category-hero-organism category-hero)
   (component/build drill-category-list-organism drill-category-list)
   ;;    [:div "Categories list from selectors"]
   ;;    [:div "Products paginated list from selectors"]
   ;;    [:div "Educational content"]
   ;;    [:div "Recent blog posts"]
   ;;    [:div "Contact"]
   (component/build footer-organism footer)])

(defn ^:export page
  [app-state opts]
  (let [category (catalog.categories/current-category app-state)]
    (component/build template
                     {:header              {}
                      :footer              {}
                      :category-hero       (category-hero-query category)
                      :drill-category-list (drill-category-list-query category)}
                     opts)))
