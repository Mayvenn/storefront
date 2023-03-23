(ns homepage.shop-v2022-09
  (:require [homepage.ui-v2022-09 :as ui]
            [storefront.events :as e]
            [storefront.component :as c]
            [mayvenn.concept.email-capture :as email-capture]
            [mayvenn.visual.tools :as vt]
            [storefront.keypaths :as k]
            [storefront.accessors.experiments :as experiments]
            [adventure.components.layered :as layered]
            [storefront.components.carousel :as carousel]
            [mayvenn.concept.account :as accounts]
            [storefront.components.landing-page :as landing-page]))

(defn page
  "Binds app-state to template for classic experiences"
  [app-state]
  (let [cms        (get-in app-state k/cms)
        categories (get-in app-state k/categories)
        in-omni?   (:experience/omni (:experiences (accounts/<- app-state)))]
    (c/build ui/template
             (merge {:lp-data                 (when (:homepage-cms-update (get-in app-state k/features))
                                                {:layers
                                                 (mapv (partial landing-page/determine-and-shape-layer app-state)
                                                       (->> (if in-omni?
                                                              :omni
                                                              :unified)
                                                            (conj storefront.keypaths/cms-homepage)
                                                            (get-in app-state)
                                                            :body))})
                     :hero                    (when-not (:homepage-cms-update (get-in app-state k/features))
                                                (if in-omni?
                                                 (ui/hero-query cms :omni)
                                                 (ui/hero-query cms :unified-fi)))
                     :shopping-categories     (ui/shopping-categories-query categories)
                     :zip-explanation         {:zip-explanation/id "zip-explanation"}
                     :blog1                   {:blog/id        "dye-humann-hair-wig"
                                               :blog/target    "https://shop.mayvenn.com/blog/hair/how-to-dye-a-human-hair-wig/"
                                               :blog/author    "Mayvenn"
                                               :blog/date      "16 Mar 2020"
                                               :blog/read-time "7 min read"
                                               :blog/heading   "How to Dye a Human Hair Wig"
                                               :blog/beginning (str "One of the best things about wearing wigs is their versatility. "
                                                                    "You can be subtle and reserved one day, then bold and vixen-like "
                                                                    "the next. Even better, human hair wigs can be dyed just like your "
                                                                    "natural hair. That means not only can your fashion change to suit "
                                                                    "your mood, but your hair color can, too.")
                                               :blog/ucare-id  "51e44ac7-1b45-4cb4-a880-79fb67789feb"}
                     :blog2                   {:blog/id        "right-stylist"
                                               :blog/target    "https://shop.mayvenn.com/blog/hair/how-to-choose-the-right-hairstylist/"
                                               :blog/author    "Amber Coleman"
                                               :blog/date      "2 Aug 2021"
                                               :blog/read-time "3 min read"
                                               :blog/heading   "How to Choose the Right Hairstylist"
                                               :blog/beginning (str "Looking for a hairstylist that accommodates your hair needs can be "
                                                                    "a struggle, but it’s not impossible! There are a couple of key steps "
                                                                    "that go into finding a great stylist like research, recommendations, "
                                                                    "pricing, and consultations. Here are a few tips that’ll help you "
                                                                    "find the right stylist for all of your #hairgoals.")
                                               :blog/ucare-id  "70f4e431-6726-4484-b91f-1a2b294aee00"}}

                    (when in-omni?
                      (vt/within :not-your-average-hair-store
                                 {:primary "Not Your Average Hair Store"
                                  :secondary (str "Visit us today and get a FREE consultation, or personalize your next "
                                                        "wig for as low as $25 at a Mayvenn Beauty Lounge near you! "
                                                        "High-quality 100% virgin human hair wigs, extensions, bundles, and more in stock!")}))
                    (vt/within :promises (if in-omni?
                                           layered/promises-omni-query
                                           {:list/icons
                                            [{:promises.icon/symbol :svg/hand-heart,
                                              :promises.icon/title  "Top-Notch Service"}
                                             {:promises.icon/symbol :svg/shield,
                                              :promises.icon/title  "30 Day Guarantee"}
                                             {:promises.icon/symbol :svg/check-cloud,
                                              :promises.icon/title  "100% Virgin Human Hair"}
                                             {:promises.icon/symbol :svg/ship-truck,
                                              :promises.icon/title  "Free Standard Shipping"}]}))
                    {:shop-these-looks/row-1 [{:shop-these-looks.entry.cta/copy   "Everyday Glam"
                                               :shop-these-looks.entry.cta/target e/navigate-shared-cart
                                               :shop-these-looks.entry.cta/args   {:shared-cart-id "1NmziARQgM"}
                                               :shop-these-looks.entry.img/src    "//ucarecdn.com/9a8497da-72ee-4ebe-80f5-91dfbd3e5ae6/"}
                                              {:shop-these-looks.entry.cta/copy   "Quick & Easy Wigs"
                                               :shop-these-looks.entry.cta/target e/navigate-shared-cart
                                               :shop-these-looks.entry.cta/args   {:shared-cart-id "Rpiyh1zRC4"}
                                               :shop-these-looks.entry.img/src    "//ucarecdn.com/cfedbeb2-d113-4dc5-a5fe-d033455ca575/"}]
                     :shop-these-looks/row-2 [{:shop-these-looks.entry.cta/copy   "HD Lace"
                                               :shop-these-looks.entry.cta/target e/navigate-shop-by-look-details
                                               :shop-these-looks.entry.cta/args   {:album-keyword :look
                                                                                   :look-id       "1EI2pl3QUYwzJfYYWeuJ1y"}
                                               :shop-these-looks.entry.img/src    "//ucarecdn.com/8892a95f-a42a-4bbd-9ae2-c2fbfea713b6/"}
                                              {:shop-these-looks.entry.cta/copy   "Natural Texture"
                                               :shop-these-looks.entry.cta/target e/navigate-shop-by-look-details
                                               :shop-these-looks.entry.cta/args   {:album-keyword :look
                                                                                   :look-id       "HKBdgo45z8HNEDLp2cegk"}
                                               :shop-these-looks.entry.img/src    "//ucarecdn.com/ab59be43-b931-4c5c-81a5-6ef6b9fa044f/"}
                                              {:shop-these-looks.entry.cta/copy   "Blonde Hair"
                                               :shop-these-looks.entry.cta/target e/navigate-shop-by-look-details
                                               :shop-these-looks.entry.cta/args   {:album-keyword :look
                                                                                   :look-id       "3fbgLUetQgvxCPz7Dcg4Yu"}
                                               :shop-these-looks.entry.img/src    "//ucarecdn.com/7803fa50-b947-4b59-8c91-c88badc74fdc/"}
                                              {:shop-these-looks.entry.cta/copy   "Versatile Clip-Ins"
                                               :shop-these-looks.entry.cta/target e/navigate-product-details
                                               :shop-these-looks.entry.cta/args   {:catalog/product-id "112"
                                                                                   :page/slug          "160g-straight-seamless-clip-ins"
                                                                                   :query-params       {:SKU "CLIP-S-1B-20-160"}}
                                               :shop-these-looks.entry.img/src    "//ucarecdn.com/da3e4862-1c43-4b57-ad43-509057f8fb2b/"}
                                              {:shop-these-looks.entry.cta/copy   "Vacay Hair"
                                               :shop-these-looks.entry.cta/target e/navigate-shop-by-look-details
                                               :shop-these-looks.entry.cta/args   {:album-keyword :look
                                                                                   :look-id       "51SUPn4tR88bvIAaUhVpo1"}
                                               :shop-these-looks.entry.img/src    "//ucarecdn.com/6690dc6c-32c7-4a7b-9485-9c4f43fe991c/"}]}
                    (let [textfield-keypath email-capture/textfield-keypath
                          email             (get-in app-state textfield-keypath)
                          submitted?        (get-in app-state k/homepage-email-submitted)]
                      {:email-capture.submit/target             [e/homepage-email-submitted
                                                                 {:values {"email-capture-input" email}}]
                       :email-capture.text-field/placeholder    "Enter your Email"
                       :email-capture.text-field/focused        (get-in app-state k/ui-focus)
                       :email-capture.text-field/keypath        textfield-keypath
                       :email-capture.text-field/errors         (get-in app-state (conj k/field-errors ["email"]))
                       :email-capture.text-field/email          email
                       :email-capture.text-field/submitted-text (when submitted? "Thank you for subscribing.")
                       :email-capture/id                        "homepage-email-capture-input"
                       :email-capture/show?                     (not (experiments/footer-v22? app-state))})))))
