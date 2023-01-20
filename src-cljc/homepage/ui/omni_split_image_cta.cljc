(ns homepage.ui.omni-split-image-cta
  (:require [storefront.component :as c]
            [storefront.events :as e]
            [storefront.components.ui :as ui]))


(c/defcomponent organism
  [{:keys [id target]} _ _]
  (when id
    [:div.bg-pale-purple.flex.flex-column-reverse-on-mb.justify-center.p8-on-tb-dt
     (ui/img {:src "http://placekitten.com/500/300?image=2"
              :width "500px"
              :alt "[Lorem Ipsum] This is how to cat"
              :class "mx6-on-tb-dt self-end"})
     [:div.my-auto.mx6.my8-on-mb
      [:h2.title-1.canela
       "[Lorem Ipsum] It's a basket of tiny cats!"]
      [:div.mt4
       {:style {:max-width "400px"}}
       [:div.shout.col-8.pt3 (ui/button-medium-primary
                              {:href      target
                               :data-test (str "go-to-" id)}
                              "[Lorem Ipsum] CAT CTA")]]]]))
