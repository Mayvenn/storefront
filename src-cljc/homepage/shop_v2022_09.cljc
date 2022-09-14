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
                                 :blog2               {:blog/id        "right-stylist"
                                                       :blog/target    [events/external-redirect-blog-page {:blog-path "/blog/hair/how-to-choose-the-right-hairstylist/"}]
                                                       :blog/author    "Amber Coleman"
                                                       :blog/date      "2 Aug 2021"
                                                       :blog/read-time "3 min read"
                                                       :blog/heading   "How to Choose the Right Hairstylist"
                                                       :blog/beginning "Looking for a hairstylist that accommodates your hair needs can be a struggle, but it’s not impossible! There are a couple of key steps that go into finding a great stylist like research, recommendations, pricing, and consultations. Here are a few tips that’ll help you find the right stylist for all of your #hairgoals."
                                                       :blog/ucare-id  "70f4e431-6726-4484-b91f-1a2b294aee00"}}
                                {:shop-these-looks/row-1 [{:shop-these-looks.entry.cta/copy   "Shop Wedding Hair"
                                                           :shop-these-looks.entry.cta/target events/navigate-shop-by-look-details
                                                           :shop-these-looks.entry.cta/args   {:album-keyword :look
                                                                                               :look-id       "7e4F9lGZHmTphADJxRFL6a"}
                                                           :shop-these-looks.entry.img/src    "//ucarecdn.com/f4cfa086-edb1-4e8c-8245-6b68e93546b7/"}
                                                          {:shop-these-looks.entry.cta/copy   "Quick & Easy Wigs"
                                                           :shop-these-looks.entry.cta/target events/navigate-product-details
                                                           :shop-these-looks.entry.cta/args   {:catalog/product-id "236"
                                                                                               :page/slug          "brazilian-straight-headband-wig"
                                                                                               :query-params       {:SKU "BNSHBW26"}}
                                                           :shop-these-looks.entry.img/src    "//ucarecdn.com/c0fab4a2-9778-4d78-b375-7fa6cfbdf73a/"}]
                                 :shop-these-looks/row-2 [{:shop-these-looks.entry.cta/copy   "Shop HD Lace"
                                                           :shop-these-looks.entry.cta/target events/navigate-shop-by-look-details
                                                           :shop-these-looks.entry.cta/args   {:album-keyword :look
                                                                                               :look-id       "1EI2pl3QUYwzJfYYWeuJ1y"}
                                                           :shop-these-looks.entry.img/src    "//ucarecdn.com/8892a95f-a42a-4bbd-9ae2-c2fbfea713b6/"}
                                                          {:shop-these-looks.entry.cta/copy   "Shop Natural Texture"
                                                           :shop-these-looks.entry.cta/target events/navigate-shop-by-look-details
                                                           :shop-these-looks.entry.cta/args   {:album-keyword :look
                                                                                               :look-id       "HKBdgo45z8HNEDLp2cegk"}
                                                           :shop-these-looks.entry.img/src    "//ucarecdn.com/ab59be43-b931-4c5c-81a5-6ef6b9fa044f/"}
                                                          {:shop-these-looks.entry.cta/copy   "Shop Blonde Hair"
                                                           :shop-these-looks.entry.cta/target events/navigate-shop-by-look-details
                                                           :shop-these-looks.entry.cta/args   {:album-keyword :look
                                                                                               :look-id       "3fbgLUetQgvxCPz7Dcg4Yu"}
                                                           :shop-these-looks.entry.img/src    "//ucarecdn.com/7803fa50-b947-4b59-8c91-c88badc74fdc/"}
                                                          {:shop-these-looks.entry.cta/copy   "Low Maintenance Clip-Ins"
                                                           :shop-these-looks.entry.cta/target events/navigate-product-details
                                                           :shop-these-looks.entry.cta/args   {:catalog/product-id "112"
                                                                                               :page/slug          "160g-straight-seamless-clip-ins"
                                                                                               :query-params       {:SKU "CLIP-S-1B-20-160"}}
                                                           :shop-these-looks.entry.img/src    "//ucarecdn.com/da3e4862-1c43-4b57-ad43-509057f8fb2b/"}
                                                          {:shop-these-looks.entry.cta/copy   "Vacay Hair"
                                                           :shop-these-looks.entry.cta/target events/navigate-shop-by-look-details
                                                           :shop-these-looks.entry.cta/args   {:album-keyword :look
                                                                                               :look-id       "51SUPn4tR88bvIAaUhVpo1"}
                                                           :shop-these-looks.entry.img/src    "//ucarecdn.com/6690dc6c-32c7-4a7b-9485-9c4f43fe991c/"}]}
                                (let [textfield-keypath email-capture/textfield-keypath
                                      email             (get-in app-state textfield-keypath)
                                      submitted?        (get-in app-state k/homepage-email-submitted)]
                                  {:email-capture.submit/target             [events/homepage-email-submitted
                                                                             {:values {"email-capture-input" email}}]
                                   :email-capture.text-field/id             "homepage-email-capture-input"
                                   :email-capture.text-field/placeholder    "Enter your Email"
                                   :email-capture.text-field/focused        (get-in app-state k/ui-focus)
                                   :email-capture.text-field/keypath        textfield-keypath
                                   :email-capture.text-field/errors         (get-in app-state (conj k/field-errors ["email"]))
                                   :email-capture.text-field/email          email
                                   :email-capture.text-field/submitted-text (when submitted? "Thank you for subscribing.")})))))
