(ns ui.wig-services-menu
  (:require [storefront.components.money-formatters :as mf]
            [storefront.component :as component]))

(def service-menu-data
  {:header/title "Wig Services"
   :sections     [{:header/title "Lace Customization"
                   :items        [{:title "Basic Lace"
                                   :price 40}
                                  {:title "Lace Front"
                                   :price 50}
                                  {:title "360 Lace"
                                   :price 60}]}
                  {:header/title "Color Services"
                   :items        [{:title "Roots"
                                   :price 35}
                                  {:title "All-Over Color"
                                   :price 65}
                                  {:title "Multi All-Over Color"
                                   :price 75}
                                  {:title "Accent Highlights"
                                   :price 55}
                                  {:title "Partial Highlights"
                                   :price 75}
                                  {:title "Full Highlights"
                                   :price 85}]}
                  {:header/title "Cut"
                   :items        [{:title "Blunt Cut"
                                   :price 25}
                                  {:title "Layered Cut"
                                   :price 45}]}
                  {:header/title "Wig Styling"
                   :items        [{:title "Advanced Styling"
                                   :price 40}]}
                  {:header/title "Bundle Add-On"
                   :items        [{:title "1 Bundle"
                                   :price 20}
                                  {:title "2 Bundles"
                                   :price 40}]}
                  {:header/title "Maintainence Services"
                   :items        [{:title "Wig Maintainence"
                                   :price 50}]}]})

(defn wig-services-menu-item
  [ix {:keys [title price]}]
  [:div.flex.justify-between {:key ix}
   [:div title]
   [:div (mf/as-money-without-cents price)]])

(defn wig-services-menu-section
  [ix {:keys [header/title items]}]
  [:div.pt5 {:key ix}
   [:div.proxima.content-2.bold.shout title]
   (map-indexed wig-services-menu-item items)])

(component/defcomponent component [{:keys [header/title sections]} _owner _opts]
  [:div.bg-pale-purple.p6
   [:div.flex.flex-column.mx-auto.col-8-on-tb-dt
    {:style {:max-width "375px"}}
    [:div.center.canela.title-1 title]
    (map-indexed wig-services-menu-section sections)]])
