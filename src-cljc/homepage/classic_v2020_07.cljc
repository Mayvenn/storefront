(ns homepage.classic-v2020-07
  (:require [homepage.ui-v2020-07 :as ui]
            [storefront.component :as c]
            [storefront.keypaths :as k]
            [storefront.components.landing-page :as landing-page]))

(defn page
  "Binds app-state to template for classic experiences"
  [app-state]
  (let [cms                  (get-in app-state k/cms)
        categories           (get-in app-state k/categories)
        ugc                  (get-in app-state k/cms-ugc-collection)]
    (c/build ui/template {:contact-us           ui/contact-us-query
                          :lp-data              {:layers
                                                 (mapv (partial landing-page/determine-and-shape-layer app-state)
                                                       (->> :classic
                                                            (conj k/cms-homepage)
                                                            (get-in app-state)
                                                            :body))}
                          :diishan              ui/diishan-query
                          :guarantees           ui/guarantees-query
                          :hashtag-mayvenn-hair (ui/hashtag-mayvenn-hair-query ugc)
                          :shopping-categories  (ui/shopping-categories-query categories)})))
