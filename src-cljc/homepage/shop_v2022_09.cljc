(ns homepage.shop-v2022-09
  (:require api.orders
            [homepage.ui-v2022-09 :as ui]
            [storefront.events :as e]
            [storefront.component :as c]
            [mayvenn.concept.email-capture :as email-capture]
            [mayvenn.visual.tools :as vt]
            [storefront.keypaths :as k]
            [storefront.accessors.experiments :as experiments]
            [adventure.components.layered :as layered]
            [storefront.components.carousel :as carousel]
            [mayvenn.concept.account :as accounts]
            [storefront.components.landing-page :as landing-page]))

(defn page
  "Binds app-state to template for classic experiences"
  [app-state]
  (let [in-omni?             (:experience/omni (:experiences (accounts/<- app-state)))
        cms-slug             (if in-omni?
                               :omni
                               :unified)]
    (c/build ui/template
             (merge
              {:layers
               (mapv (partial landing-page/determine-and-shape-layer app-state)
                     (->> cms-slug
                          (conj storefront.keypaths/cms-homepage)
                          (get-in app-state)
                          :body))}
              {:phone-consult-cta  (merge (get-in app-state k/cms-phone-consult-cta)
                                          (api.orders/current app-state)
                                          {:place-id :shopping-homepage
                                           :in-omni? (:experience/omni (:experiences (accounts/<- app-state)))})}))))
