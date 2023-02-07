(ns ui.wig-customization-spotlights
  (:require [storefront.component :as component]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]))


(def standard-data
  {:header/supertitle "Available In-Store Only"
   :header/title      "Make Your Dream Look a Reality"
   :header/subtitle   "Customize your wig with endless cut, color, and stylist possibilities."
   :sections/title    "Here's How It Works:"
   :sections          [{:title "Select your wig"
                        :copy  "Start with an in-stock wig or get one tailor-made by our resident Wig Artist."
                        :url   "https://ucarecdn.com/686bd480-8dd0-4b9e-bd29-d4a1f8652e87/select_your_wig.png"}
                       {:title "We customize it"
                        :copy  "Our experts can create a variety of one-of-a-kind customization for a style that's all your own."
                        :url   "https://ucarecdn.com/16c43ab7-d3ce-42f2-bef4-37674c59ca6b/we_customize_it.png"}
                       {:title "Take it home"
                        :copy  "Rock your new unit the same day, or choose a convenient 2-5 day pickup window."
                        :url   "https://ucarecdn.com/8d4b8e12-48a7-4e90-8a41-3f1ef1267a93/take_it_home.png"}]})

(defn wig-customization-spotlight-section
  [ix {:keys [title copy url]}]
  [:div.flex.flex-column.items-center
   {:style {:max-width "325px"}}
   (ui/circle-ucare-img {:width "160" :alt ""} url)
   [:div.col-12.pt2.canela (->> ix inc (str "0"))]
   [:div.col-12.proxima.content-2.bold.shout title]
   [:div copy]])

(component/defcomponent component [{:header/keys [supertitle title subtitle] :as data} _owner _opts]
  [:div.wig-customization.flex.flex-column.items-center.py8.px4.gap-4.bg-cool-gray
   [:div.proxima.title-2.shout supertitle]
   [:div.canela.title-2.shout title]
   [:div subtitle]
   [:div.flex.justify-center
    (svg/vertical-blackline {:style {:height "75px"}})]
   [:div.title-2.proxima.shout (:sections/title data)]
   (into [:div.grid.gap-8] (map-indexed wig-customization-spotlight-section (:sections data)))])
