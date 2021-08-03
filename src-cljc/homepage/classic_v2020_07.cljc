(ns homepage.classic-v2020-07
  (:require [homepage.ui-v2020-07 :as ui]
            [storefront.component :as c]
            [storefront.keypaths :as k]))

(defn page
  "Binds app-state to template for classic experiences"
  [app-state]
  (let [cms        (get-in app-state k/cms)
        categories (get-in app-state k/categories)
        ugc        (get-in app-state k/cms-ugc-collection)]
    (c/build ui/template {:contact-us           ui/contact-us-query
                          :diishan              ui/diishan-query
                          :guarantees           ui/guarantees-query
                          :hero                 (ui/hero-query cms :unified)
                          :hashtag-mayvenn-hair (ui/hashtag-mayvenn-hair-query ugc)
                          :shopping-categories  (ui/shopping-categories-query categories)})))
