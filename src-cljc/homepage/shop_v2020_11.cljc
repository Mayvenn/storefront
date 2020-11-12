(ns homepage.shop-v2020-11
  (:require [homepage.ui-v2020-11 :as ui]
            [storefront.component :as c]
            [storefront.keypaths :as k]))

(defn page
  "Binds app-state to template for shop subdomain"
  [app-state]
  (let [cms            (get-in app-state k/cms)
        ;; categories     (get-in app-state k/categories)
        ;; ugc            (get-in app-state k/cms-ugc-collection)
        expanded-index (get-in app-state k/faq-expanded-section)]
    (c/build
     ui/template
     {:hero                           (ui/hero-query cms :shop)
      :title-with-subtitle            ui/title-with-subtitle-query
      :square-image-and-text-diptychs ui/square-image-and-text-diptychs
      :faq                            (ui/faq-query (-> cms :faq :free-mayvenn-services) expanded-index)
      :contact-us                     ui/contact-us-query})))
