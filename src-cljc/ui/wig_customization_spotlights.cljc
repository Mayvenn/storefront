(ns ui.wig-customization-spotlights
  (:require [storefront.component :as component]
            [storefront.components.ui :as ui]))


(def standard-data
  {:header/title    "Wig Customization"
   :header/subtitle "Here's how it works:"
   :sections        [{:title "Select your wig"
                      :copy  "Choose a pre-customized, factory-made, or tailor-made unit."
                      :url   "https://ucarecdn.com/1596ef7a-8ea8-4e2d-b98f-0e2083998cce/select_your_wig.png"}
                     {:title "We customize it"
                      :copy  "Choose from ten different customization services— we'll make your dream look come to life."
                      :url   "https://ucarecdn.com/b8902af1-9262-4369-ab88-35e82fd2f3b7/we_customize_it.png"}
                     {:title "Take it home"
                      :copy  "Rock your new unit the same day or pick it up within 2-5 days."
                      :url   "https://ucarecdn.com/8d4b8e12-48a7-4e90-8a41-3f1ef1267a93/take_it_home.png"}]})

(defn wig-customization-spotlight-section
  [ix {:keys [title copy url]}]
  [:div.flex.flex-column.items-center.pb4
   {:style {:max-width "250px"}}
   (ui/circle-ucare-img {:width "160" :alt ""} url)
   [:div.col-12.pt2.canela (->> ix inc (str "0"))]
   [:div.col-12.proxima.content-2.bold.shout title]
   [:div copy]])


(component/defcomponent component [{:header/keys [title subtitle] :as data} _owner _opts]
  [:div.wig-customization.flex.flex-column.items-center.p8.gap-4.bg-cool-gray
   [:div.canela.title-1.shout title]
   [:div.proxima.title-1.bold.shout subtitle]
   (into [:div.grid.gap-4] (map-indexed wig-customization-spotlight-section (:sections data)))])
