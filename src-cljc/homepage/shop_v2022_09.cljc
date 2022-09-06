(ns homepage.shop-v2022-09
  (:require [homepage.ui-v2022-09 :as ui]
            [storefront.accessors.experiments :as experiments]
            [storefront.events :as events]
            [storefront.component :as c]
            [mayvenn.concept.email-capture :as email-capture]
            [storefront.keypaths :as k]))

(defn page
  "Binds app-state to template for classic experiences"
  [app-state]
  (let [cms                  (get-in app-state k/cms)
        ugc                  (get-in app-state k/cms-ugc-collection)
        categories           (get-in app-state k/categories)
        expanded-index       (get-in app-state k/faq-expanded-section)
        remove-free-install? (:remove-free-install (get-in app-state storefront.keypaths/features))]
    (c/build ui/template {:hero                (ui/hero-query cms :unified-fi)
                          :shopping-categories (ui/shopping-categories-query categories)})
    (c/build ui/template (merge (let [textfield-keypath email-capture/textfield-keypath
                                      email             (get-in app-state textfield-keypath)]
                                  {:email-capture.submit/target          [events/email-modal-submitted
                                                                          {:values {"email-capture-input" email}}]
                                   :email-capture.text-field/id          "homepage-email-capture-input"
                                   :email-capture.text-field/placeholder "Enter your Email"
                                   :email-capture.text-field/focused     (get-in app-state k/ui-focus)
                                   :email-capture.text-field/keypath     textfield-keypath
                                   :email-capture.text-field/errors      (get-in app-state (conj k/field-errors ["email"]))
                                   :email-capture.text-field/email       email})
                                {:hero (ui/hero-query cms :unified-fi)}))))
