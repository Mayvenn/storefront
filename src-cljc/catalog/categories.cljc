(ns catalog.categories
  (:require [clojure.string :as string]
            [spice.maps :as maps]
            [spice.core :as spice]
            [storefront.keypaths :as keypaths]
            [catalog.keypaths]
            [catalog.selector :as selector]))

(def new-facet?
  ;; [<facet-slug> <option-slug>]
  #{[:hair/family "360-frontals"]
    [:hair/family "360-wigs"]
    [:hair/family "lace-front-wigs"]})

(def new-category?
  #{"360-frontals" "wigs"})

(defn copy [& sentences]
  (string/join " " sentences))

(defn category->seo [category-name description image-url]
  {:page/title            (str category-name " | Mayvenn")
   :opengraph/title       (str category-name " - Free shipping. Free 30 day returns.")
   :page.meta/description description
   :opengraph/description (copy "Machine-wefted and backed by our 30 Day Quality Guarantee, our"
                                category-name
                                "are the best quality products on the market and ships free!")
   ;; TODO make this a proper image and place under :selector/images
   :category/image-url    image-url})

(def closures
  [(merge {:catalog/category-id      "0"
           :copy/title               "Virgin Closures"
           :page/slug                "virgin-closures"
           :legacy/named-search-slug "closures"
           :catalog/department       #{"hair"}
           :hair/family              #{"closures"}
           :hair/color.process       #{"natural"}
           :hair/source              #{"virgin"}
           :selector/essentials      [:catalog/department :hair/family :hair/color.process :hair/source]
           :selector/electives       [:hair/origin :hair/texture :hair/base-material]
           :header/order             0
           :header/group             1
           :footer/order             10
           :copy/description         (copy "Save your precious strands and top your look off with the ultimate tool in protective weave styling."
                                           "Our collection of closures and frontals blend seamlessly with our bundles"
                                           "and can be customized to fit your unique look.")
           :videos                   {:learn-more {:url "https://www.youtube.com/embed/tOUPp6s034U"}}
           :images                   {:hero {:filename    "VirginClosures.jpg",
                                             :desktop-url "//ucarecdn.com/b1443b91-6dd3-44e7-b7c3-06876def1bd5/",
                                             :mobile-url  "//ucarecdn.com/e3d3c5c7-e4dc-4f37-96c0-19bb14785dfb/",
                                             :alt         "Virgin Closures"}}}
          (category->seo "Closures"
                         (copy "Lace Closures in Brazilian Straight, Malaysian Body Wave,"
                               "Peruvian Straight, Peruvian Body Wave,"
                               "Peruvian Yaki Straight, Indian Straight Lace and more.")
                         "//ucarecdn.com/12e8ebfe-06cd-411a-a6fb-909041723333/"))
   (merge {:catalog/category-id      "10"
           :copy/title               "Virgin 360 Frontals"
           :page/slug                "virgin-360-frontals"
           :legacy/named-search-slug "360-frontals"

           :catalog/department  #{"hair"}
           :hair/family         #{"360-frontals"}
           :hair/color.process  #{"natural"}
           :hair/source         #{"virgin"}
           :selector/essentials [:catalog/department :hair/family :hair/color.process :hair/source]
           :selector/electives  [:hair/origin :hair/texture :hair/base-material]

           :copy/description (copy "From your hairline to nape, we’ve got you covered with our revolutionary 360 Lace Frontal."
                                   "This one-of-a-kind frontal piece features freestyle parting, baby hairs,"
                                   "and low-density edges for a naturally flawless look.")
           :videos           {:learn-more {:url "https://www.youtube.com/embed/tOUPp6s034U"}}
           :images           {:hero {:filename    "360Frontals.jpg"
                                     :desktop-url "//ucarecdn.com/4f2075cf-ea01-4c85-a96a-928fc6a03cf8/"
                                     :mobile-url  "//ucarecdn.com/3fccae53-99a7-4e3a-b52a-54086b6eef22/"
                                     :alt         "360 Frontals"}}
           :footer/order     11
           :header/order     1
           :header/group     1}
          (category->seo "360 Frontals"
                         (copy "Mayvenn’s bundles and extensions with a naturally flawless look."
                               "These are our 360 Lace Frontals - Brazilian, Peruvian and Malaysian,"
                               "as Body, Straight and Loose Wave.")
                         "//ucarecdn.com/7837332a-2ca5-40dd-aa0e-86a2417cd723/"))
   (merge {:catalog/category-id      "1"
           :copy/title               "Virgin Frontals"
           :page/slug                "virgin-frontals"
           :legacy/named-search-slug "frontals"

           :catalog/department  #{"hair"}
           :hair/family         #{"frontals"}
           :hair/color.process  #{"natural"}
           :header/order        2
           :header/group        1
           :footer/order        12
           :hair/source         #{"virgin"}
           :selector/essentials [:catalog/department :hair/family :hair/color.process :hair/source]
           :selector/electives  [:hair/origin :hair/texture :hair/base-material]
           :copy/description    (copy "Save your precious strands and top your look off with the ultimate tool in protective weave styling."
                                      "Our collection of frontals blend seamlessly with our bundles and can be customized to fit your unique look.")
           :videos              {:learn-more {:url "https://www.youtube.com/embed/tOUPp6s034U"}}
           :images              {:hero {:filename    "VirginFrontals.jpg",
                                        :desktop-url "//ucarecdn.com/2c6c4831-7002-43b9-8bb4-afda60dd213b/",
                                        :mobile-url  "//ucarecdn.com/d353eb7b-2b70-4b03-ab9e-9144dc81954e/",
                                        :alt         "Virgin Frontals"}}}
          (category->seo "Frontals"
                         (copy "Brazilian, Peruvian, Indian and Malaysian hair frontals."
                               "Choose from popular textures such as Deep Wave," "Yaki Straight, Water Wave, Straight and more.")
                         "//ucarecdn.com/0c7d94c3-c00e-4812-9526-7bd669ac679c/"))
   (merge {:catalog/category-id      "17"
           :copy/title               "Dyed Virgin Closures"
           :page/slug                "dyed-virgin-closures"
           :legacy/named-search-slug "closures"
           :category/new?            false
           :catalog/department       #{"hair"}
           :hair/family              #{"closures"}
           :hair/color.process       #{"dyed"}
           :hair/source              #{"virgin"}
           :selector/essentials      [:catalog/department :hair/family :hair/color.process :hair/source]
           :selector/electives       [:hair/origin :hair/texture :hair/base-material :hair/color]
           :header/order             3
           :header/group             1
           :footer/order             13
           :copy/description         (copy "Save your precious strands and top your look off with the ultimate tool in protective weave styling."
                                           "Our collection of closures and frontals blend seamlessly with our bundles"
                                           "and can be customized to fit your unique look.")
           :videos                   {:learn-more {:url "https://www.youtube.com/embed/tOUPp6s034U"}}
           :images                   {:hero {:filename    "DyedVirginClosures.jpg",
                                             :desktop-url "//ucarecdn.com/8bb50b5c-dfc5-4565-8461-7e2a68a7733e/",
                                             :mobile-url  "//ucarecdn.com/8ae295fc-0252-499f-910a-73b29348414e/",
                                             :alt         "Dyed Vigin Closures"}}}
          (category->seo "Dyed Virgin Closures"
                         (copy "Save time and skip the hassle of fussing with toner"
                               "and bleach for the convenience of pre-dyed Closures."
                               "Free shipping. Free 30 day returns. Made with 100% dyed"
                               "virgin human hair extensions.")
                         "//ucarecdn.com/7082d52d-3a68-422c-9a81-47fba51c1c55/"))
   (merge {:catalog/category-id      "18"
           :copy/title               "Dyed Virgin Frontals"
           :page/slug                "dyed-virgin-frontals"
           :legacy/named-search-slug "frontals"
           :category/new?            false
           :catalog/department       #{"hair"}
           :hair/family              #{"frontals"}
           :hair/color.process       #{"dyed"}
           :hair/source              #{"virgin"}
           :header/order             4
           :header/group             1
           :footer/order             14
           :selector/essentials      [:catalog/department :hair/family :hair/color.process :hair/source]
           :selector/electives       [:hair/origin :hair/texture :hair/base-material :hair/color]
           :copy/description         (copy "Save your precious strands and top your look off with the ultimate tool in protective weave styling."
                                           "Our collection of frontals blend seamlessly with our bundles and can be customized to fit your unique look.")
           :videos                   {:learn-more {:url "https://www.youtube.com/embed/tOUPp6s034U"}}
           :images                   {:hero {:filename    "DyedVirginFrontals.jpg",
                                             :desktop-url "//ucarecdn.com/d861f26b-6a2f-4bd9-a6e4-d0b0b141527e/",
                                             :mobile-url  "//ucarecdn.com/e686f6fe-31a4-408c-84ee-0337f6f0902b/",
                                             :alt         "Dyed Virgin Frontals"}}}
          (category->seo "Dyed Virgin Frontals"
                         (copy "Save time and skip the hassle of fussing with toner"
                               "and bleach for the convenience of pre-dyed Frontals."
                               "Free shipping. Free 30 day returns. Made with 100% dyed"
                               "virgin human hair extensions.")
                         "//ucarecdn.com/e014902e-3fdb-46ba-ad63-581a4caa8ab0/"))])

(def virgin-hair
  [(merge {:catalog/category-id      "2"
           :copy/title               "Virgin Straight"
           :page/slug                "virgin-straight"
           :legacy/named-search-slug "straight"

           :catalog/department  #{"hair"}
           :hair/texture        #{"straight"}
           :hair/color.process  #{"natural"}
           :hair/source         #{"virgin"}
           :selector/essentials [:catalog/department :hair/texture :hair/color.process :hair/source]
           :selector/electives  [:hair/family :hair/origin :hair/base-material]
           :copy/description    (copy "For those who want it given to them"
                                      "straight, our collection of 100% virgin straight hair"
                                      "is your go-to for a sleek look with"
                                      "minimal effort.")
           :videos              {:learn-more {:url "https://www.youtube.com/embed/tOUPp6s034U"}}
           :images              {:hero {:filename    "VirginStraight.jpg",
                                        :desktop-url "//ucarecdn.com/5be83577-fc62-42fd-b7aa-cd35c09fa32a/",
                                        :mobile-url  "//ucarecdn.com/9ca3c6e7-1414-4702-be7b-6ea3428320de/",
                                        :alt         "Virgin Straight"}
                                 :home {:filename "StraightExtensionsMayvenn.jpg",
                                        :url      "//ucarecdn.com/cf60bdc3-09df-4ee3-87a8-d3cbdcefdd87/",
                                        :alt      "Straight Hair Extensions Mayvenn"}}
           :home/order          0
           :footer/order        0
           :header/order        0
           :header/group        0}
          (category->seo "Natural Straight Extensions"
                         (copy "Straight Brazilian weave, straight Indian hair and straight Peruvian hair."
                               "Our straight bundles are sleek from root to tip.")
                         "//ucarecdn.com/61662cc7-59f5-454b-8031-538516557eb0/"))
   (merge {:catalog/category-id      "3"
           :copy/title               "Virgin Yaki Straight"
           :page/slug                "virgin-yaki-straight"
           :legacy/named-search-slug "yaki-straight"

           :catalog/department  #{"hair"}
           :hair/texture        #{"yaki-straight"}
           :hair/color.process  #{"natural"}
           :hair/source         #{"virgin"}
           :selector/essentials [:catalog/department :hair/texture :hair/color.process :hair/source]
           :selector/electives  [:hair/family :hair/origin :hair/base-material]
           :copy/description    (copy "Tired of having to break out the hot"
                                      "tools for a textured straight look? Our Yaki"
                                      "Straight hair collection is here to save your"
                                      "strands! Yaki Straight hair matches the rhythm of"
                                      "your natural hair that's been pressed straight or"
                                      "freshly relaxed. Your flat iron has been officially"
                                      "cancelled.")
           :videos              {:learn-more {:url "https://www.youtube.com/embed/tOUPp6s034U"}}
           :images              {:hero {:filename    "VirginYakiStraight.jpg",
                                        :desktop-url "//ucarecdn.com/a49ac151-ff04-4bf5-895a-0636dc2dacd8/",
                                        :mobile-url  "//ucarecdn.com/249749df-11d7-4e1b-8ca7-f877af3b9c11/",
                                        :alt         "Virgin Yaki Straight"}}
           :footer/order        1
           :header/order        1
           :header/group        0}
          (category->seo "Yaki Straight Extensions"
                         (copy "Our Yaki Straight hair collection features both Peruvian and Brazilian straight hair bundles."
                               "With Lace Closure or Lace Frontals in different lengths.")
                         "//ucarecdn.com/98e8b217-73ee-475a-8f5e-2c3aaa56af42/"))
   (merge {:catalog/category-id      "4"
           :copy/title               "Virgin Kinky Straight"
           :page/slug                "virgin-kinky-straight"
           :legacy/named-search-slug "kinky-straight"
           :catalog/department       #{"hair"}
           :hair/texture             #{"kinky-straight"}
           :hair/color.process       #{"natural"}
           :hair/source              #{"virgin"}
           :selector/essentials      [:catalog/department :hair/texture :hair/color.process :hair/source]
           :selector/electives       [:hair/family :hair/origin :hair/base-material]
           :copy/description         (copy "Blending is a breeze with our Kinky Straight"
                                           "hair collection! Like a fresh blow out, the"
                                           "Kinky Straight hair texture moves freely and gives"
                                           "a naturally flawless look that mimics your own"
                                           "locks.")
           :videos                   {:learn-more {:url "https://www.youtube.com/embed/tOUPp6s034U"}}
           :images                   {:hero {:filename    "VirginKinkyStraight.jpg",
                                             :desktop-url "//ucarecdn.com/09d70755-37d5-401c-b483-c7a3dd1ef588/",
                                             :mobile-url  "//ucarecdn.com/297659dd-3b82-4f47-89ea-cdaf6045d923/",
                                             :alt         "Virgin Kinky Straight"}}
           :footer/order             2
           :header/order             2
           :header/group             0}
          (category->seo "Kinky Straight Extensions"
                         (copy "100% human hair bundles and extensions from Mayvenn."
                               "Peruvian and Brazilian Kinky Straight Lace Closures and Frontals.")
                         "//ucarecdn.com/7fe5f90f-4dad-454a-aa4b-b453fc4da3c4/"))
   (merge {:catalog/category-id      "5"
           :copy/title               "Virgin Body Wave"
           :page/slug                "virgin-body-wave"
           :legacy/named-search-slug "body-wave"
           :catalog/department       #{"hair"}
           :hair/texture             #{"body-wave"}
           :hair/color.process       #{"natural"}
           :hair/source              #{"virgin"}
           :selector/essentials      [:catalog/department :hair/texture :hair/color.process :hair/source]
           :selector/electives       [:hair/family :hair/origin :hair/base-material]
           :copy/description         (copy "Step into the spotlight with our collection of luscious Body Wave hair."
                                           "Body Wave is unbelievably soft and goes from straight to wavy and back again with ease.")
           :videos                   {:learn-more {:url "https://www.youtube.com/embed/tOUPp6s034U"}}
           :images                   {:hero {:filename    "VirginBodyWave.jpg",
                                             :desktop-url "//ucarecdn.com/5e0d4953-67af-4f53-a705-f3af1ed8704c/",
                                             :mobile-url  "//ucarecdn.com/e96d0e62-02ab-4834-a032-4a7cdb3a77b2/",
                                             :alt         "Virgin Body Wave"}
                                      :home {:filename "BodyWaveExtensionsMayvenn.jpg",
                                             :url      "//ucarecdn.com/1a3ce0a2-d8a4-4c72-b20b-62b5ff445096/",
                                             :alt      "Body Wave Hair Extensions Mayvenn"}}
           :home/order               1
           :footer/order             3
           :header/order             3
           :header/group             0}
          (category->seo "Body Wave Extensions"
                         (copy "Malaysian and Peruvian body wave silk, lace and 360 frontal bundles."
                               "Unbelievably soft and goes from straight to wavy and back again.")
                         "//ucarecdn.com/445c53df-f369-4ca6-a554-c9668c8968f1/"))
   (merge {:catalog/category-id      "6"
           :copy/title               "Virgin Loose Wave"
           :page/slug                "virgin-loose-wave"
           :legacy/named-search-slug "loose-wave"
           :catalog/department       #{"hair"}
           :hair/texture             #{"loose-wave"}
           :hair/color.process       #{"natural"}
           :hair/source              #{"virgin"}
           :selector/essentials      [:catalog/department :hair/texture :hair/color.process :hair/source]
           :selector/electives       [:hair/family :hair/origin :hair/base-material]
           :copy/description         (copy "For hair that holds a curl beautifully,"
                                           "our collection of 100% virgin Loose Wave hair"
                                           "is the perfect foundation for all your carefree,"
                                           "flirty, wavy looks.")
           :videos                   {:learn-more {:url "https://www.youtube.com/embed/tOUPp6s034U"}}
           :images                   {:hero {:filename    "VirginLooseWave.jpg",
                                             :desktop-url "//ucarecdn.com/5cb19377-2ad1-4015-90fd-0152ce8b9f16/",
                                             :mobile-url  "//ucarecdn.com/70979840-a033-48c4-881b-7505c2bf5109/",
                                             :alt         "Virgin Loose Wave"}
                                      :home {:filename "LooseWaveExtensionsMayvenn.jpg",
                                             :url      "//ucarecdn.com/1cfc4f20-1917-4c2b-87c2-68aa2dc7c77e/",
                                             :alt      "Loose Wave Hair Extensions Mayvenn"}}
           :home/order               2
           :footer/order             4
           :header/order             4
           :header/group             0}
          (category->seo "Loose Wave Extensions"
                         (copy "Mayvenn’s Brazilian, Peruvian and Indian loose wave bundles."
                               "Also includes loose wave lace closures. All are 100% virgin Loose Wave hair.")
                         "//ucarecdn.com/31be9341-a688-4f03-b754-a22a0a1f267e/"))
   (merge {:catalog/category-id      "7"
           :copy/title               "Virgin Water Wave"
           :page/slug                "Virgin-water-wave"
           :legacy/named-search-slug "water-wave"
           :catalog/department       #{"hair"}
           :hair/texture             #{"water-wave"}
           :hair/color.process       #{"natural"}
           :hair/source              #{"virgin"}
           :selector/essentials      [:catalog/department :hair/texture :hair/color.process :hair/source]
           :selector/electives       [:hair/family :hair/origin :hair/base-material]
           :copy/description         (copy "Ride the lush, carefree waves of the bundles,"
                                           "closures, and frontals in our Water Wave hair"
                                           "collection. For curls you can rock everywhere from"
                                           "the office to your tropical vacation, make a"
                                           "statement with Water Wave hair.")
           :videos                   {:learn-more {:url "https://www.youtube.com/embed/tOUPp6s034U"}}
           :images                   {:hero {:filename    "VirginWaterWave.jpg",
                                             :desktop-url "//ucarecdn.com/004230c4-f532-499c-a643-06481f2b5997/",
                                             :mobile-url  "//ucarecdn.com/d46cc08c-e17f-4529-b0c6-aeed97bf31ed/",
                                             :alt         "Virgin Water Wave"}}
           :footer/order             5
           :header/order             5
           :header/group             0}
          (category->seo "Water Wave Extensions"
                         (copy "Water Wave Bundles, Closures, and Frontals."
                               "Peruvian and Brazilian bundles."
                               "Mayvenn has hair extensions, bundles, closures, and frontals.")
                         "//ucarecdn.com/5f6c669f-8274-4bef-afa9-3c08813842f6/"))
   (merge {:catalog/category-id      "8"
           :copy/title               "Virgin Deep Wave"
           :page/slug                "virgin-deep-wave"
           :legacy/named-search-slug "deep-wave"
           :catalog/department       #{"hair"}
           :hair/texture             #{"deep-wave"}
           :hair/color.process       #{"natural"}
           :hair/source              #{"virgin"}
           :selector/essentials      [:catalog/department :hair/texture :hair/color.process :hair/source]
           :selector/electives       [:hair/family :hair/origin :hair/base-material]
           :copy/description         (copy "Reigning supreme in versatility, the Deep Wave hair collection features"
                                           "a soft, spiral wave full of body and bounce. Our deep wave hair is perfect"
                                           "for those who want big waves that make an even bigger splash.")
           :videos                   {:learn-more {:url "https://www.youtube.com/embed/tOUPp6s034U"}}
           :images                   {:hero {:filename    "DeepWave.jpg",
                                             :desktop-url "//ucarecdn.com/c7dd690e-f3ae-4b6b-9411-d9e7027b15c5/",
                                             :mobile-url  "//ucarecdn.com/b07b0298-902e-4dc2-bdf4-f8e179f06020/",
                                             :alt         "Virgin Deep Wave"}
                                      :home {:filename "DeepWaveExtensionsMayvenn.jpg",
                                             :url      "//ucarecdn.com/0432b04b-92fe-4e30-b18f-3747e4efef8c/",
                                             :alt      "Deep Wave Hair Extensions Mayvenn"}}
           :home/order               3
           :footer/order             6
           :header/order             6
           :header/group             0}
          (category->seo "Deep Wave Extensions"
                         (copy "Deep Wave bundles and closures, including Brazilian, Peruvian and Indian Deep Wave."
                               "Soft, spiral wave full of body and bounce.")
                         "//ucarecdn.com/49cc5837-8321-4331-9cec-d299d0de1887/"))
   (merge {:catalog/category-id      "9"
           :copy/title               "Virgin Curly"
           :page/slug                "virgin-curly"
           :legacy/named-search-slug "curly"
           :catalog/department       #{"hair"}
           :hair/texture             #{"curly"}
           :hair/color.process       #{"natural"}
           :hair/source              #{"virgin"}
           :selector/essentials      [:catalog/department :hair/texture :hair/color.process :hair/source]
           :selector/electives       [:hair/family :hair/origin :hair/base-material]
           :copy/description         (copy "Let your bold, beautiful curls take center stage! Our curly hair collection is a tight,"
                                           "kinky curl perfect for creating voluminous coily styles that mimic natural 3C hair textures.")
           :videos                   {:learn-more {:url "https://www.youtube.com/embed/tOUPp6s034U"}}
           :images                   {:hero {:filename    "VirginCurly.jpg",
                                             :desktop-url "//ucarecdn.com/29534c6f-54d0-4969-a1b1-b92b1dcde326/",
                                             :mobile-url  "//ucarecdn.com/7c77bfb9-53c3-4eb3-a4f9-6c9a456d93b4/",
                                             :alt         "Virgin Curly"}
                                      :home {:filename "CurlyExtensionsMayvenn.jpg",
                                             :url      "//ucarecdn.com/9bcf9e1b-4a8a-4a40-a9f4-414c22c629b4/",
                                             :alt      "Curly Hair Extensions Mayvenn"}}
           :home/order               4
           :footer/order             7
           :header/order             7
           :header/group             0}
          (category->seo "Curly Extensions"
                         (copy "Shop our Brazilian curly bundle, Peruvian Curly Bundle,"
                               "Peruvian Curly Lace closures and Curly Lace frontals."
                               "Perfect for creating voluminous coily styles.")
                         "//ucarecdn.com/128b68e2-bf3a-4d72-8e39-0c71662f9c86/"))])

(def wigs
  [(merge {:catalog/category-id "13"
           :footer/order        20
           :header/order        0
           :header/group        2

           :category/new? false

           :copy/title            "Wigs"
           :page/slug             "wigs"
           :copy/description      (copy "When getting a bundle install isn’t an option,"
                                        "these units will be your go-to protective style"
                                        "for achieving a brand new look at home. Made"
                                        "from 100% virgin human hair, and customizable"
                                        "to fit your unique look.")
           :images                {:hero {:filename    "Wigs.jpg"
                                          :desktop-url "//ucarecdn.com/0ce6c31c-1e35-47c7-a6aa-039ba57e19be/"
                                          :mobile-url  "//ucarecdn.com/9f7ce79a-867f-4455-adc5-5a784ba17e41/"
                                          :alt         "Wigs"}}
           :catalog/department    #{"hair"}
           :hair/family           #{"360-wigs" "lace-front-wigs"}
           :selector/essentials   [:hair/family :catalog/department]
           :selector/electives    [:hair/family :hair/texture :hair/origin :hair/color]
           :page/title            "Human Hair Wigs | Mayvenn"
           :opengraph/title       (copy "Mayvenn 360 and Lace Frontal Wigs - Free shipping."
                                        "Free 30 day returns. Made with 100% virgin human hair.")
           :page.meta/description (copy "Mayvenn’s Natural Lace Front Wigs and 360 Wigs."
                                        "Comes in different variations such as Brazilian and Malaysian, straight, deep wave and loose wave.")
           :opengraph/description (copy "100% virgin human hair, machine-wefted and backed by our"
                                        "30 Day Quality Guarantee, our Wigs can be customized to fit"
                                        "your unique look using the built-in combs and adjustable strap.")
           :image-url             "//ucarecdn.com/5269d7ef-b5c7-4475-9e9c-d16d7cbbdda3/"})])

(def stylist-exclusives
  [(merge {:catalog/category-id       "14"
           :auth/requires             #{:stylist}
           :footer/order              30
           :header/order              30
           :header/group              2
           :copy/title                "Stylist Exclusives"
           :page/slug                 "stylist-exclusives"
           :direct-to-details/id      "49"
           :direct-to-details/slug    "rings-kits"
           :direct-to-details/sku-id  "SK2"
           :catalog/department        #{"stylist-exclusives"},
           :kits/contents             #{"rings"},
           :stylist-exclusives/family #{"kits"}
           :selector/electives        []
           :selector/essentials       [:catalog/department :kits/contents :stylist-exclusives/family]
           :page/title                "Stylist Exclusives | Mayvenn"
           :opengraph/title           (copy "Stylist Exclusives - Free shipping."
                                            "Free 30 day returns. Made with 100% virgin human hair.")
           :page.meta/description     (copy "")
           :opengraph/description     (copy "")})])

(def dyed-hair-nav-roots
  [(merge {:catalog/category-id   "15"
           :copy/title            "Virgin Hair"
           :page/slug             "virgin-hair"
           :catalog/department    #{"hair"}
           :hair/color.process    #{"natural"}
           :hair/source           #{"virgin"},
           :hair/family           :query/missing
           :hair/texture          #{"straight"
                                    "kinky-straight"
                                    "yaki-straight"
                                    "body-wave"
                                    "deep-wave"
                                    "water-wave"
                                    "loose-wave"
                                    "curly"}
           :selector/essentials   [:catalog/department :hair/color.process :hair/texture :hair/source]
           :selector/electives    []
           :page/title            ""
           :opengraph/title       (copy "" "")
           :page.meta/description (copy "")
           :opengraph/description (copy "")})
   (merge {:catalog/category-id "16"
           :copy/title          "Dyed Virgin Hair"
           :page/slug           "dyed-virgin-hair"
           :category/new?       false
           :catalog/department  #{"hair"}
           :hair/family         #{"bundles" "closures" "frontals"}
           :hair/color.process  #{"dyed"}
           :hamburger/order     1
           :header/order        8
           :header/group        0
           :footer/order        8
           :hair/source         #{"virgin"}
           :selector/essentials [:catalog/department :hair/family :hair/color.process :hair/source]
           :selector/electives  [:hair/origin :hair/texture :hair/color]
           :videos              {:learn-more {:url "https://www.youtube.com/embed/tOUPp6s034U"}}
           :images              {:hero {:filename    "DyedVirginHair.jpg"
                                        :desktop-url "//ucarecdn.com/ffec1ede-b869-44f4-86e8-3615a78cf046/"
                                        :mobile-url  "//ucarecdn.com/0bec1329-ba01-4ece-a66f-4ad303311bf3/"
                                        :alt         "Dyed Virgin Hair"}}
           :copy/description    (copy "When natural brown isn’t cutting it,"
                                      "find your true color match with our Dyed Virgin Hair."
                                      "Save time and skip the hassle of fussing with toner and"
                                      "bleach for the convenience of pre-dyed hair extensions.")}
          (category->seo "Dyed Virgin Hair Extensions"
                         (copy "When natural brown isn’t cutting it, find your true color match with"
                               "our Dyed Virgin Hair. Free shipping. Free 30 day returns. Made with 100% dyed virgin human hair extensions.")
                         nil))

   (merge {:catalog/category-id "12"
           :copy/title          "Closures & Frontals"
           :page/slug           "closures-and-frontals"
           :catalog/department  #{"hair"}
           :hair/family         #{"closures" "frontals" "360-frontals"}
           :hair/color.process  #{"dyed" "natural"}
           :hair/source         #{"virgin" "human"}
           :selector/essentials [:catalog/department :hair/family :hair/color.process :hair/source]
           :hamburger/order     1
           :selector/electives  []
           :copy/description    (copy "Save your precious strands and top your look"
                                      "off with the ultimate tool in protective weave"
                                      "styling. Our collection of closures and frontals blend"
                                      "seamlessly with our bundles and can be customized"
                                      "to fit your unique look.")})])

(def dyed-human-hair-category
  [(merge {:catalog/category-id "19"
           :copy/title          "Dyed 100% Human Hair"
           :page/slug           "dyed-100-human-hair"
           :category/new?       true
           :catalog/department  #{"hair"}
           :hair/family         #{"bundles" "closures"}
           :hair/texture        #{"straight"
                                  "body-wave"
                                  "deep-wave"
                                  "loose-wave"}
           :hair/color.process  #{"dyed"}
           :hair/source         #{"human"}
           :hamburger/order     1
           :header/order        9
           :header/group        0
           :footer/order        9
           :selector/essentials [:catalog/department :hair/family :hair/color.process :hair/source]
           :selector/electives  [:hair/origin :hair/texture :hair/base-material :hair/color]
           :videos              {:learn-more {:url "https://www.youtube.com/embed/tOUPp6s034U"}}
           :images              {:hero {:filename    "Dyed100PercentHumanHair.jpg"
                                        :desktop-url "//ucarecdn.com/5d04e5b5-f441-448c-9244-9839f081e543/"
                                        :mobile-url  "//ucarecdn.com/33202e80-fab7-4209-859e-e7809b3bef30/"
                                        :alt         "Dyed 100% Human Hair"}}
           :copy/description    (copy "These extensions are perfect for when you need a quick style swap or when you’re ballin’ on a budget."
                                      "Our dyed 100% human hair comes in a range of stylish hues, can take heat up to 350 degrees, and"
                                      "will last 1-3 months with proper care.")}
          (category->seo "Dyed 100% Human Hair Extensions"
                         (copy "Our dyed 100% human hair comes in a range of stylish hues, can take heat up to 350 degrees,"
                               "and will last 1-3 months with proper care. Free shipping."
                               "Free 30 day returns. Made with 100% human hair extensions.")
                         nil))])

(def dyed-human-hair-closures-category
  [(merge {:catalog/category-id "20"
           :copy/title          "Dyed 100% Human Hair Closures"
           :page/slug           "dyed-100-human-hair-closures"
           :category/new?       true
           :catalog/department  #{"hair"}
           :hair/family         #{"closures"}
           :hair/texture        :query/missing
           :hair/color.process  #{"dyed"}
           :hair/source         #{"human"}
           :header/order        5
           :header/group        1
           :footer/order        15
           :selector/essentials [:catalog/department :hair/family :hair/color.process :hair/source]
           :selector/electives  [:hair/origin :hair/texture :hair/color]
           :videos              {:learn-more {:url "https://www.youtube.com/embed/tOUPp6s034U"}}
           :images              {:hero {:filename    "Dyed100PercentHumanClosures.jpg"
                                        :desktop-url "//ucarecdn.com/a0913745-d344-4c8b-af48-dd79ac421202/"
                                        :mobile-url  "//ucarecdn.com/5ee6ff8f-9a4c-4474-950b-f7943b57da46/"
                                        :alt         "Dyed 100% Human Hair Closures"}}
           :copy/description    (copy "Save your precious strands and top your look off with the ultimate tool in protective weave styling."
                                      "Our collection of closures and frontals blend seamlessly with our bundles and can be customized"
                                      "to fit your unique look.")}
          (category->seo "Dyed 100% Human Hair Closures"
                         (copy "Our collection of closures and frontals blend seamlessly with our bundles and can be customized"
                               "to fit your unique look. Free shipping. Free 30 day returns. Made with 100% human hair extensions.")
                         nil))])

(def seamless-clip-ins-category
  [{:catalog/category-id "21"
    :catalog/department  #{"hair"}
    :category/new?       true

    :hair/family         #{"seamless-clip-ins"}
    :selector/electives  [:hair/weight :hair/texture :hair/color]
    :selector/essentials [:catalog/department :hair/family]

    :copy/title       "Clip-Ins"
    :copy/description (copy "Get the hair of your dreams in an instant with our seamless clip-in extensions."
                            "Featuring a thin, polyurethane (PU) weft that flawlessly blends with your own hair."
                            "Ditch the tracks for a clip-in experience that is truly seamless.")

    :page/title "Clip-In Hair Extensions | Mayvenn"
    :page/slug  "seamless-clip-ins"
    :page.meta/description
    (copy "Get the hair of your dreams in an instant with our seamless clip-in extensions."
          "Featuring a thin, polyurethane (PU) weft that flawlessly blends with your own hair."
          "Free shipping. Free 30 day returns.""Made with 100% human hair extensions.")

    :opengraph/title       "Mayvenn Clip-In Hair Extensions - Free shipping. Free 30 day returns. Made with 100% human hair extensions."
    :opengraph/description "Blending flawlessly with your own hair and backed by our 30 Day Quality Guarantee, our seamless clip-in extensions are the best quality products on the market and ships free!"

    :images {:hero {:filename    "categories-header-clip.png"
                    :desktop-url "//ucarecdn.com/498c40b0-f957-4141-ac1f-99c144547a8f/"
                    :mobile-url  "//ucarecdn.com/34c3a8c3-a1fe-4475-ae80-c6713e56f6e8/"
                    :alt         "Clip-Ins"}}

    :footer/order 40
    :header/group 2
    :header/order 4}])

(def tape-ins-category
  [{:catalog/category-id "22"
    :catalog/department  #{"hair"}
    :category/new?        true

    :hair/family         #{"tape-ins"}
    :selector/electives  [:hair/color :hair/weight :hair/length]
    :selector/essentials [:catalog/department :hair/family]

    :copy/title "Tape-Ins"

    :page/slug "tape-ins"

    :direct-to-details/id   "111"
    :direct-to-details/slug "50g-straight-tape-ins"

    :footer/order 50
    :header/group 2
    :header/order 5}])

(def menu-categories
  (concat virgin-hair
          closures
          dyed-human-hair-closures-category))

(def initial-categories
  (concat wigs
          stylist-exclusives
          dyed-hair-nav-roots
          menu-categories
          dyed-human-hair-category
          seamless-clip-ins-category
          tape-ins-category))

(defn id->category [id categories]
  (->> categories
       (filter (comp #{(str id)} :catalog/category-id))
       first))

(defn named-search->category [named-search-slug categories]
  (->> categories
       (filter #(= named-search-slug
                   (:legacy/named-search-slug %)))
       first))

(defn current-traverse-nav [data]
  (id->category (get-in data keypaths/current-traverse-nav-id)
                (get-in data keypaths/categories)))

(defn current-category [data]
  (id->category (get-in data catalog.keypaths/category-id)
                (get-in data keypaths/categories)))

