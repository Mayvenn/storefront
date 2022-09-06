(ns homepage.shop-v2022-09
  (:require [homepage.ui-v2022-09 :as ui]
            [storefront.accessors.experiments :as experiments]
            [storefront.events :as events]
            [storefront.component :as c]
            [mayvenn.concept.email-capture :as email-capture]
            [storefront.events :as events]
            [storefront.keypaths :as k]))

(defn page
  "Binds app-state to template for classic experiences"
  [app-state]
  (let [cms        (get-in app-state k/cms)
        categories (get-in app-state k/categories)]
    (c/build ui/template (merge {:hero                (ui/hero-query cms :unified-fi)
                                 :shopping-categories (ui/shopping-categories-query categories)
                                 :blog1               {:blog/id        "different-ways"
                                                       :blog/target    [events/external-redirect-blog-page {:blog-path "/blog/hair/10-different-ways-to-style-your-edges/"}]
                                                       :blog/author    "Lauryn Jiles"
                                                       :blog/date      "13 Jul 2022"
                                                       :blog/read-time "2 min read"
                                                       :blog/heading   "10 Different Ways to Style Your Edges"
                                                       :blog/beginning "Styling your edges is not only a quick way to switch up your look, it can also take your hairstyle to the next level. Black women, once again, are trendsetters and started the trend of designing hairlines and edges years ago. With social media platforms like TikTok and Instagram, Black women have taken to these apps to showcase the many different ways that they style their edges, from the super simple to the intricate and dramatic."
                                                       :blog/ucare-id  "f6837168-70d8-4180-a5bf-7b4ec3d3eab4"}
                                 :blog2               {:blog/id        "entrepreher"
                                                       :blog/target    [events/external-redirect-blog-page {:blog-path "/blog/hustle/entreprenher-featuring-kortlynn-jenae"}]
                                                       :blog/author    "Brittany Johnson"
                                                       :blog/date      "8 Oct 2020"
                                                       :blog/read-time "8 min read"
                                                       :blog/heading   "EntreprenHER featuring Kortlynn Jenae"
                                                       :blog/beginning "EntreprenHER is a revamped series that focuses on highlighting Black women-identifying founders, changemakers and thought leaders. We believe that the versatility in our stories is our superpower - and we want to feature these amazing women because of the difference they’re making by following their dreams and amplifying their passions."
                                                       :blog/ucare-id  "e1a06009-7e87-401c-a93d-6ffd25fd933f"}}
                                (let [textfield-keypath email-capture/textfield-keypath
                                      email             (get-in app-state textfield-keypath)]
                                  {:email-capture.submit/target          [events/email-modal-submitted
                                                                          {:values {"email-capture-input" email}}]
                                   :email-capture.text-field/id          "homepage-email-capture-input"
                                   :email-capture.text-field/placeholder "Enter your Email"
                                   :email-capture.text-field/focused     (get-in app-state k/ui-focus)
                                   :email-capture.text-field/keypath     textfield-keypath
                                   :email-capture.text-field/errors      (get-in app-state (conj k/field-errors ["email"]))
                                   :email-capture.text-field/email       email})))))
