(ns homepage.ui.zip-explanation
  (:require [storefront.component :as c]
            [storefront.events :as e]
            [storefront.components.ui :as ui]))


(c/defcomponent organism
  [{:zip-explanation/keys [id]} _ _]
  (when id
    [:div.bg-warm-gray
     [:div.split-organism.max-1080.mx-auto
      [:div.my-auto.mx-auto.split-left-on-tb-dt.split-bottom-on-mb.p4
       [:h2.title-1.canela
        "Hair Now, Pay Later"]
       [:div.mt4
        ;{:style {:max-width "400px"}}
        "Choose Zip at checkout to pay in 4 interest-free payments. Pay the first installment up front and the last three payments will automatically process every 2 weeks."]]
      [:div.split-right-on-tb-dt.split-top-on-mb
       (ui/img {:src "//ucarecdn.com/6f393076-2f1b-4a3b-8bbf-18cee5bf51f5/"
                :style {:object-fit "cover"}
                :width "100%"
                :height "100%"
                :alt "Example of how Zip works: 1st payment due today $112.50. 2nd payment in 2 weeks $112.50, 3rd payment in 4 weeks $112.50, Final payment in 6 weeks $112.50"
                :class "self-end p4-on-tb-dt"})]]]))
