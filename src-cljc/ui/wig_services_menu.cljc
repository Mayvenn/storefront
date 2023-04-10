(ns ui.wig-services-menu
  (:require [storefront.components.money-formatters :as mf]
            [storefront.component :as component]
            [mayvenn.visual.tools :as vt]
            [mayvenn.visual.ui.titles :as titles]))

(def service-menu-data
  {:header/title "Service List"
   :header/id    "service-list"
   :sections     [{:header/title "Lace Customization"
                   :items        [{:title/primary "Basic Lace"
                                   :title/secondary (str "Cut lace, add elastic band, bleach knots, pluck knots, and basic styling. "
                                                         "Only available for Ready to Wear and closure wigs.")
                                   :price 40}
                                  {:title/primary "Lace Front"
                                   :title/secondary (str "Bleach knots, tint lace, pluck hairline and/or part, add baby hairs, and basic styling.")
                                   :price 50}
                                  {:title/primary "360 Lace"
                                   :title/secondary (str "Bleach knots, tint lace, pluck hairline and/or part, add baby hairs, and basic styling.")
                                   :price 60}]}
                  {:header/title "Color Services"
                   :items        [{:title/primary "Roots"
                                   :title/secondary (str "Color or lighten roots only and basic styling.")
                                   :price 35}
                                  {:title/primary "All-Over Color"
                                   :title/secondary (str "1 color is applied all over the unit from roots to ends or color lightening "
                                                         "of the entire unit and basic styling.")
                                   :price 65}
                                  {:title/primary "Multi All-Over Color"
                                   :title/secondary (str "2 to 3 colors are applied all over the unit from roots to ends and basic styling. "
                                                         "No lightening or bleaching.")
                                   :price 75}
                                  {:title/primary "Accent Highlights"
                                   :title/secondary (str "Face framing hand-painted or foil highlights and basic styling.")
                                   :price 55}
                                  {:title/primary "Partial Highlights"
                                   :title/secondary (str "Hand-painted or foil highlights are applied to the crown of the unit and basic styling.")
                                   :price 75}
                                  {:title/primary "Full Highlights"
                                   :title/secondary (str "Hand-painted or foil highlights are applied to the crown, back, and "
                                                         "sides of the unit and basic styling.")
                                   :price 85}]}
                  {:header/title "Cut"
                   :items        [{:title/primary "Blunt Cut"
                                   :title/secondary (str "Haircut without layers and basic styling.")
                                   :price 25}
                                  {:title/primary "Layered Cut"
                                   :title/secondary (str "Haircut with layers and basic styling.")
                                   :price 45}]}
                  {:header/title "Wig Styling"
                   :items        [{:title/primary "Advanced Styling"
                                   :title/secondary (str "Includes crimp styling, half-up half-down styles, full wand or small barrel curls, "
                                                         "braid accents, updos, and trending or specific looks.")
                                   :price 40}]}
                  {:header/title "Bundle Add-On"
                   :items        [{:title/primary "1 Bundle"
                                   :title/secondary (str "Sew in one additional bundle to the desired unit and basic styling. "
                                                         "Additional bundle not included in the price.")
                                   :price 20}
                                  {:title/primary "2 Bundles"
                                   :title/secondary (str "Sew in two additional bundles to the desired unit and basic styling. "
                                                         "Additional bundles not included in the price.")
                                   :price 40}]}]})

(defn wig-services-menu-item
  [ix {:as data :keys [price]}]
  [:div.flex.justify-between {:key ix}
   [:div
    (titles/proxima-tiny-left (vt/with :title data))]
   [:div (mf/as-money-without-cents price)]])

(defn wig-services-menu-section
  [ix {:keys [header/title items]}]
  [:div.pt5 {:key ix}
   [:div.proxima.content-2.bold.shout title]
   (map-indexed wig-services-menu-item items)])

(component/defcomponent component [{:keys [header/title header/id sections]} _owner _opts]
  [:div.bg-pale-purple.p6
   {:id id}
   [:div.flex.flex-column.mx-auto.col-8-on-tb-dt
    {:style {:max-width "375px"}}
    [:div.center.canela.title-1 title]
    (map-indexed wig-services-menu-section sections)]])
