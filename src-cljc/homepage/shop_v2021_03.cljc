(ns homepage.shop-v2021-03
  (:require [homepage.ui-v2020-07 :as ui]
            [storefront.accessors.experiments :as experiments]
            [storefront.component :as c]
            [storefront.keypaths :as k]))

(defn page
  "Binds app-state to template for classic experiences"
  [app-state]
  (let [cms                       (get-in app-state k/cms)
        ugc                       (get-in app-state k/cms-ugc-collection)
        expanded-index            (get-in app-state k/faq-expanded-section)
        remove-free-install?      (:remove-free-install (get-in app-state storefront.keypaths/features))]
    (c/build ui/template {:contact-us             ui/contact-us-query
                          :diishan                ui/diishan-query
                          :guarantees             ui/guarantees-query
                          :hero                   (ui/hero-query cms :unified-fi)
                          :hashtag-mayvenn-hair   (ui/hashtag-mayvenn-hair-query ugc)
                          :install-specific-query (ui/install-specific-query app-state)
                          :remove-free-install?   remove-free-install?})))
