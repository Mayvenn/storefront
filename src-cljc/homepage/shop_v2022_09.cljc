(ns homepage.shop-v2022-09
  (:require [homepage.ui-v2022-09 :as ui]
            [storefront.accessors.experiments :as experiments]
            [storefront.component :as c]
            [storefront.keypaths :as k]))

(defn page
  "Binds app-state to template for classic experiences"
  [app-state]
  (let [cms                  (get-in app-state k/cms)
        ugc                  (get-in app-state k/cms-ugc-collection)
        categories           (get-in app-state k/categories)
        expanded-index       (get-in app-state k/faq-expanded-section)
        remove-free-install? (:remove-free-install (get-in app-state storefront.keypaths/features))]
    (c/build ui/template {:hero (ui/hero-query cms :unified-fi)})))
