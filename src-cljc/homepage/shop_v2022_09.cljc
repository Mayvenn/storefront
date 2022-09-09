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
                                 :zip-explanation     {:zip-explanation/id "zip-explanation"}
                                 :blog1               {:blog/id        "dye-humann-hair-wig"
                                                       :blog/target    [events/external-redirect-blog-page {:blog-path "/blog/hair/how-to-dye-a-human-hair-wig/"}]
                                                       :blog/author    "Mayvenn"
                                                       :blog/date      "16 Mar 2020"
                                                       :blog/read-time "7 min read"
                                                       :blog/heading   "How to Dye a Human Hair Wig"
                                                       :blog/beginning "One of the best things about wearing wigs is their versatility. You can be subtle and reserved one day, then bold and vixen-like the next. Even better, human hair wigs can be dyed just like your natural hair. That means not only can your fashion change to suit your mood, but your hair color can, too."
                                                       :blog/ucare-id  "51e44ac7-1b45-4cb4-a880-79fb67789feb"}
                                 :blog2               {:blog/id        "entrepreher"
                                                       :blog/target    [events/external-redirect-blog-page {:blog-path "/blog/hustle/entreprenher-featuring-kortlynn-jenae"}]
                                                       :blog/author    "Brittany Johnson"
                                                       :blog/date      "8 Oct 2020"
                                                       :blog/read-time "8 min read"
                                                       :blog/heading   "EntreprenHER featuring Kortlynn Jenae"
                                                       :blog/beginning "EntreprenHER is a revamped series that focuses on highlighting Black women-identifying founders, changemakers and thought leaders. We believe that the versatility in our stories is our superpower - and we want to feature these amazing women because of the difference they’re making by following their dreams and amplifying their passions."
                                                       :blog/ucare-id  "e1a06009-7e87-401c-a93d-6ffd25fd933f"}}
                                {:shop-these-looks/row-1 [{:shop-these-looks.entry.cta/copy   "Wedding Looks"
                                                           :shop-these-looks.entry.cta/target events/navigate-shop-by-look-details
                                                           :shop-these-looks.entry.cta/args   {:album-keyword :aladdin-free-install
                                                                                               :look-id       "7e4F9lGZHmTphADJxRFL6a"}
                                                           :shop-these-looks.entry.img/src    "//ucarecdn.com/f4cfa086-edb1-4e8c-8245-6b68e93546b7/"}
                                                          {:shop-these-looks.entry.cta/copy   "Headband Wigs"
                                                           :shop-these-looks.entry.cta/target events/navigate-product-details
                                                           :shop-these-looks.entry.cta/args   {:catalog/product-id "236"
                                                                                               :page/slug          "brazilian-straight-bundles"
                                                                                               :query-params       {:SKU "BNSHBW26"}}
                                                           :shop-these-looks.entry.img/src    "//ucarecdn.com/c0fab4a2-9778-4d78-b375-7fa6cfbdf73a/"}]
                                 :shop-these-looks/row-2 [{:shop-these-looks.entry.cta/copy   "Fall Looks"
                                                           :shop-these-looks.entry.cta/target events/navigate-shop-by-look-details
                                                           :shop-these-looks.entry.cta/args   {:album-keyword :aladdin-free-install
                                                                                               :look-id       "1EI2pl3QUYwzJfYYWeuJ1y"}
                                                           :shop-these-looks.entry.img/src    "//ucarecdn.com/8892a95f-a42a-4bbd-9ae2-c2fbfea713b6/"}
                                                          {:shop-these-looks.entry.cta/copy   "Everyday Looks"
                                                           :shop-these-looks.entry.cta/target events/navigate-shop-by-look-details
                                                           :shop-these-looks.entry.cta/args   {:album-keyword :aladdin-free-install
                                                                                               :look-id       "HKBdgo45z8HNEDLp2cegk"}
                                                           :shop-these-looks.entry.img/src    "//ucarecdn.com/ab59be43-b931-4c5c-81a5-6ef6b9fa044f/"}
                                                          {:shop-these-looks.entry.cta/copy   "Blonde Looks"
                                                           :shop-these-looks.entry.cta/target events/navigate-shop-by-look-details
                                                           :shop-these-looks.entry.cta/args   {:album-keyword :aladdin-free-install
                                                                                               :look-id       "3fbgLUetQgvxCPz7Dcg4Yu"}
                                                           :shop-these-looks.entry.img/src    "//ucarecdn.com/7803fa50-b947-4b59-8c91-c88badc74fdc/"}
                                                          {:shop-these-looks.entry.cta/copy   "Low Maintenance Looks"
                                                           :shop-these-looks.entry.cta/target events/navigate-product-details
                                                           :shop-these-looks.entry.cta/args   {:catalog/product-id "112"
                                                                                               :page/slug          "106g-straight-seamless-clip-ins"
                                                                                               :query-params       {:SKU "CLIP-S-1B-20-160"}}
                                                           :shop-these-looks.entry.img/src    "//ucarecdn.com/da3e4862-1c43-4b57-ad43-509057f8fb2b/"}
                                                          {:shop-these-looks.entry.cta/copy   "Vacation Looks"
                                                           :shop-these-looks.entry.cta/target events/navigate-shop-by-look-details
                                                           :shop-these-looks.entry.cta/args   {:album-keyword :aladdin-free-install
                                                                                               :look-id       "51SUPn4tR88bvIAaUhVpo1"}
                                                           :shop-these-looks.entry.img/src    "//ucarecdn.com/6690dc6c-32c7-4a7b-9485-9c4f43fe991c/"}]}
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
