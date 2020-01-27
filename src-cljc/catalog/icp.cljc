(ns catalog.icp
  (:require [catalog.categories :as categories]
            catalog.keypaths
            [catalog.ui.product-list :as product-list]
            [spice.core :as spice]
            [storefront.assets :as assets]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [spice.maps :as maps]))

(defn ^:private vertical-squiggle-atom
  [top]
  (component/html
   [:div.relative
    [:div.absolute.col-12.flex.justify-center
     {:style {:top top}}
     ^:inline (svg/vertical-squiggle {:style {:height "72px"}})]]))

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
  [{:keys [subcategory-ids]} categories]
  (let [indexed-categories (maps/index-by :catalog/category-id categories)
        category-order     (zipmap subcategory-ids (range))]
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
      (->> (select-keys indexed-categories subcategory-ids)
           vals
           (sort-by (comp category-order :catalog/category-id))))}))

(def ^:private purple-divider-atom
  [:div
   {:style {:background-image    "url('//ucarecdn.com/73db5b08-860e-4e6c-b052-31ed6d951f00/-/resize/x24/')"
            :background-position "center center"
            :background-repeat   "repeat-x"
            :height              "24px"}}])

(def ^:private green-divider-atom
  [:div
   {:style {:background-image  "url('//ucarecdn.com/7e91271e-874c-4303-bc8a-00c8babb0d77/-/resize/x24/')"
            :background-position "center center"
            :background-repeat   "repeat-x"
            :height              "24px"}}])

(defcomponent content-box-organism
  [_ _ _]
  [:div.py8.px4.bg-cool-gray
   [:div.pb2
    [:div.proxima.title-2.bold.caps "Wigs 101:"]
    [:div.canela.title-1.pb2 "How to Choose"]
    [:div.canela.content-1 "There are a few main factors to consider when you’re choosing a wig. When you have a good sense of the look you want to achieve, your lifestyle and your budget, the rest will fall into place. Ask yourself the density, lace color, length of hair you want, and if you prefer virgin hair or dyed hair."]]

   [:div.py2
    [:div.proxima.title-2.bold.caps.pb1 "Cap Size"]
    [:div.canela.content-2 "Cap size ranges between 20-21 inches. If for any reason your wig doesn’t fit, reach out to Customer Service for details to return or exchange your product."]]

   [:div.py2
    [:div.proxima.title-2.bold.caps.pb1 "Density"]
    [:div.canela.content-2 "The fullest density clocks in at 200% - other measures are 180, 150 and 130. If the style you’re planning needs a lot of thickness, you should choose a higher density like 180 or 200. If you only need a little, consider 130 or 150."]]

   [:div.py2
    [:div.proxima.title-2.bold.caps.pb1 "Lace Color"]
    [:div.canela.content-2 "For a wig that blends in and looks as natural as possible, you’ll want to choose a lace backing shade that most closely matches your skin tone."]]

   [:div.py2
    [:div.proxima.title-2.bold.caps.pb1 "Length"]
    [:div.canela.content-2 "Short and sassy or drama down to your ankles? The choice is yours!"]]

   [:div.py2
    [:div.proxima.title-2.bold.caps.pb1 "Virgin & Dyed"]
    [:div.canela.content-2 "If you want to play with color, it helps to choose a wig that can be dyed—in other words, you’ll need a virgin wig. Or, you could choose a blonde or platinum wig and have it dyed the color you want. The most straight-forward choice is often to purchase a pre-dyed wig."]]

   [:div.py2
    [:div.proxima.title-2.bold.caps.pb1 "Free Wig Customization"]
    [:div.canela.content-2 "We’re here to make wigs easier and offer free knot bleaching, hairline plucking and lace cutting with each qualifying wig purchase. If your wig isn’t ready to wear, it includes free customization."]]

   [:div.py2
    [:div.proxima.title-2.bold.caps.pb1 "Still Have Questions?"]
    [:div.canela.content-2
     [:div "Customer Service can help!"] 
     [:div "Call (888) 562-7952 "]
     [:div "Monday through Friday from 8am-5pm PST."]]]])

(defcomponent ^:private template
  "This lays out different ux pieces to form a cohesive ux experience"
  [{:keys [header footer category-hero drill-category-list product-list]} _ _]
  [:div
   (component/build header-organism header)
   [:div.max-960.mx-auto
    (component/build category-hero-organism category-hero)
    (vertical-squiggle-atom "-36px")
    (component/build drill-category-list-organism drill-category-list)
    purple-divider-atom
    (component/build product-list/organism product-list)
    green-divider-atom
    (component/build content-box-organism {})]
   (component/build footer-organism footer)])

(defn query
  [app-state]
  (let [category   (catalog.categories/current-category app-state)
        categories (get-in app-state keypaths/categories)
        selections (get-in app-state catalog.keypaths/category-selections)
        products   (vals (get-in app-state keypaths/v2-products))]
    {:header              {}
     :footer              {}
     :category-hero       (category-hero-query category)
     :drill-category-list (drill-category-list-query category categories)
     :product-list        (product-list/query app-state category products selections)}))

(defn ^:export page
  [app-state opts]
  (let [page-data (get-in app-state catalog.keypaths/category-query)]
    (component/build template page-data opts)))
