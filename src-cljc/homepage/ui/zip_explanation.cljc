(ns homepage.ui.zip-explanation
  (:require [storefront.component :as c]
            [storefront.events :as e]
            [storefront.components.ui :as ui]))


(c/defcomponent organism
  [{:zip-explanation/keys [id]} _ _]
  (when id
    [:div.bg-warm-gray.flex.flex-column-reverse-on-mb.justify-center.p8-on-tb-dt
     [:div.my-auto.mx6.my8-on-mb
      [:div.title-1.canela
       "Buy today, pay in 4 installments."]
      [:div.mt4
       {:style {:max-width "400px"}}
       "We partner with ZIP to create a seamless affordable customer experience. Choose Zip at checkout to pay later. You'll pay the first installment upfront, and the rest over 6 weeks."]]
     (ui/img {:src "//ucarecdn.com/6f393076-2f1b-4a3b-8bbf-18cee5bf51f5/"
              :width "300px"
              :class "mx6-on-tb-dt self-end"})]))
