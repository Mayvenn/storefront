(ns homepage.classic-v2020-07
  (:require api.orders
            [homepage.ui-v2020-07 :as ui]
            [mayvenn.concept.account :as accounts]
            [storefront.component :as c]
            [storefront.keypaths :as k]
            [storefront.components.landing-page :as landing-page]))

(defn page
  "Binds app-state to template for classic experiences"
  [app-state]
  (let [cms                  (get-in app-state k/cms)
        categories           (get-in app-state k/categories)
        ugc                  (get-in app-state k/cms-ugc-collection)]
    (c/build ui/template (merge
                          {:phone-consult-cta  (merge (get-in app-state k/cms-phone-consult-cta)
                                                      (api.orders/current app-state)
                                                      {:place-id :shopping-homepage
                                                       :in-omni? (:experience/omni (:experiences (accounts/<- app-state)))})}
                          {:lp-data              {:layers
                                                  (mapv (partial landing-page/determine-and-shape-layer app-state)
                                                        (->> :classic
                                                             (conj k/cms-homepage)
                                                             (get-in app-state)
                                                             :body))}}

                          (when-not (:hide-old-classic-homepage (get-in app-state k/features))
                            {:contact-us           ui/contact-us-query
                             :diishan              ui/diishan-query
                             :guarantees           ui/guarantees-query
                             :hashtag-mayvenn-hair (ui/hashtag-mayvenn-hair-query ugc)
                             :shopping-categories  (ui/shopping-categories-query categories)})))))
