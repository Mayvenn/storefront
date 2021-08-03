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
        shopping-quiz-unified-fi? (experiments/shopping-quiz-unified-fi? app-state)]
    (c/build ui/template {:contact-us             ui/contact-us-query
                          :diishan                ui/diishan-query
                          :guarantees             ui/guarantees-query
                          :hero                   (ui/hero-query cms (if shopping-quiz-unified-fi?
                                                                       :unified-fi
                                                                       :shop))
                          :faq                    (ui/faq-query (-> cms :faq :free-mayvenn-services) expanded-index)
                          :hashtag-mayvenn-hair   (ui/hashtag-mayvenn-hair-query ugc)
                          :install-specific-query (ui/install-specific-query app-state)})))
