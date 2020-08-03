(ns homepage.unified-v2020-07
  (:require [homepage.ui-v2020-07 :as ui]
            [storefront.component :as c]
            [storefront.keypaths :as k]))

(defn page
  "Binds app-state to template for classic and aladdin experiences"
  [app-state]
  (let [cms            (get-in app-state k/cms)
        categories     (get-in app-state k/categories)
        ugc            (get-in app-state k/cms-ugc-collection)
        expanded-index (get-in app-state k/faq-expanded-section)
        menu           (get-in app-state k/store-service-menu)]
    (c/build
     ui/template
     (cond->
         {:contact-us           ui/contact-us-query
          :diishan              ui/diishan-query
          :guarantees           ui/guarantees-query
          :hero                 (ui/hero-query cms :unified)
          :hashtag-mayvenn-hair (ui/hashtag-mayvenn-hair-query ugc)
          :shopping-categories  (ui/shopping-categories-query categories)}

       (ui/offers? menu ui/a-la-carte-services)
       (merge {:a-la-carte-services ui/a-la-carte-query})

       (ui/offers? menu ui/mayvenn-installs)
       (merge {:mayvenn-install ui/mayvenn-install-query})

       (ui/offers? menu ui/services)
       (merge {:faq (ui/faq-query expanded-index)})))))
